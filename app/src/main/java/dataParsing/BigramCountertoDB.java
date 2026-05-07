package dataParsing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import databaseConnections.dbConnection;

/**
 * BigramCountertoDB
 *
 * Processes one or more .txt files and writes the results directly into the
 * MySQL database (words, word_transitions, imported_files tables).
 *
 * Duplicate files are detected via SHA-256 checksum against the imported_files
 * table before any processing begins, so re-running with the same file is safe.
 *
 * Usage:
 *   build with: mvn clean compile
 *   run with: mvn exec:java "-Dexec.mainClass=dataParsing.BigramCountertoDB" "-Dexec.args=<txt file path(s)>"
 *   
 */
public class BigramCountertoDB {
    private int nextWordId = 1;
    // Patterns For Words and sentances
    private static final Pattern WORD_PATTERN   = Pattern.compile("\\b[a-zA-Z]+(?:'[a-zA-Z]+)*\\b");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    // Hashmaps for counts
    /** word text → wordId (as stored in DB) */
    private final Map<String, Integer> wordIds    = new LinkedHashMap<>();

    /** wordId → counts accumulated THIS session (added on top of DB values) */
    private final Map<Integer, Long>   totalOcc   = new HashMap<>();
    private final Map<Integer, Long>   startCount = new HashMap<>();
    private final Map<Integer, Long>   endCount   = new HashMap<>();

    private final Map<Long, Long> transitionCounts = new HashMap<>(200000);

    private final Set<String> seenChecksums = new HashSet<>();

    private final List<FileRecord> pendingFiles = new ArrayList<>();

    private Connection conn;

    // -------------------------------------------------------------------------
    // Internal record
    // -------------------------------------------------------------------------
    private static class FileRecord {
        String fileName;
        String filePath;
        long   fileSizeBytes;
        long   wordCount;
        long   sentenceCount;
        long   uniqueWords;
        String checksum;
    }

    // 1.  Load existing state from DB
    /**
     * Populates wordIds and seenChecksums from the database so
     * in-memory IDs remain consistent with what is already stored.
     */
    public void loadExistingFromDB(Connection conn) {
        loadWordIdsFromDB(conn);
        loadChecksumsFromDB(conn);
    }

    private void loadWordIdsFromDB(Connection conn) {
    String sql = "SELECT wordId, word FROM words";

    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        int maxId = 0; // track highest ID

        while (rs.next()) {
            int id = rs.getInt("wordId");
            String word = rs.getString("word");

            wordIds.put(word, id);

            if (id > maxId) {
                maxId = id;
            }
        }

        // 🔥 critical for fast wordId()
        nextWordId = maxId + 1;

        System.out.printf("Loaded %,d existing words from DB%n", wordIds.size());

    } catch (SQLException e) {
        System.err.println("Warning: could not load words from DB — " + e.getMessage());
    }
}

    private void loadChecksumsFromDB(Connection conn) {
        String sql = "SELECT checksum FROM imported_files WHERE checksum IS NOT NULL";
        try (Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                seenChecksums.add(rs.getString("checksum"));
            }
            System.out.printf("Loaded %,d existing file checksums from DB%n", seenChecksums.size());

        } catch (SQLException e) {
            System.err.println("Warning: could not load checksums from DB — " + e.getMessage());
        }
    }

    // 2.  Process a single text file
    // For more detailed commends of this section look at
    // The original BigramCounter.java, logic is mostly identical

    public void processFile(Path file) throws IOException, NoSuchAlgorithmException {
        byte[] raw      = Files.readAllBytes(file);
        String checksum = sha256Hex(raw);

        // checking for duplicates, skipping entire file if match found
        if (seenChecksums.contains(checksum)) {
            System.out.println("  Skipping (already imported): " + file.getFileName());
            return;
        }
        seenChecksums.add(checksum); //adds to list of seen checksums

        String text = new String(raw, StandardCharsets.UTF_8);
        //Creating a file record for this new file
        FileRecord fr = new FileRecord();
        fr.fileName      = file.getFileName().toString();
        fr.filePath      = file.toAbsolutePath().toString();
        fr.fileSizeBytes = raw.length;
        fr.checksum      = checksum;

        Map<Integer, Long> perFileWords = new LinkedHashMap<>();

        String[] sentenceChunks = SENTENCE_SPLIT.split(text);
        long sentenceCount = 0;
        long wordCount     = 0;

        for (String chunk : sentenceChunks) {
            List<String> words = tokenize(chunk);
            if (words.isEmpty()) continue;

            sentenceCount++;
            wordCount += words.size();

            for (int i = 0; i < words.size(); i++) {
                int wid = wordId(words.get(i));
                totalOcc.merge(wid, 1L, Long::sum);
                perFileWords.merge(wid, 1L, Long::sum);
                if (i == 0)                startCount.merge(wid, 1L, Long::sum);
                if (i == words.size() - 1) endCount.merge(wid,   1L, Long::sum);
            }

            for (int i = 0; i < words.size() - 1; i++) {
                int w1 = wordId(words.get(i));
                int w2 = wordId(words.get(i + 1));
                long key = ((long) w1 << 32) | (w2 & 0xffffffffL);
                transitionCounts.merge(key, 1L, Long::sum);
            }
        }

        fr.sentenceCount = sentenceCount;
        fr.wordCount     = wordCount;
        fr.uniqueWords   = perFileWords.size();
        pendingFiles.add(fr);

        System.out.printf("  Processed: %s  (%,d words, %,d sentences)%n",
                fr.fileName, fr.wordCount, fr.sentenceCount);
    }

    // 3.  Flush everything to the database
    /**
     * Writes all in-memory data to the DB in four steps:
     * 
     *   Upsert words (totalOccurrence, startCount, endCount)
     *   Upsert word_transitions (count)
     *   Recalculate and store transition probabilities
     *   Insert imported_files records
     * 
     */
    public void flushToDB(Connection conn) {
        System.out.println("\nFlushing data to database…");
        flushWords(conn);
        flushTransitions(conn);
        recalculateProbabilities(conn);
        flushImportedFiles(conn);
        printSummary();
    }

    // ---- Words -----------------------------------------------------------

    private void flushWords(Connection conn) {
        /*
         * INSERT … ON DUPLICATE KEY UPDATE lets us upsert the running deltas
         * accumulated in-memory without caring whether the row existed before.
         * The VALUES(col) reference picks up the value from the attempted INSERT
         * so we add only the NEW count from this session on top of whatever the
         * DB already holds.
         */
        String sql =
            "INSERT INTO words (wordId, word, totalOccurrence, startCount, endCount) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "  totalOccurrence = totalOccurrence + VALUES(totalOccurrence), " +
            "  startCount      = startCount      + VALUES(startCount), " +
            "  endCount        = endCount        + VALUES(endCount)";

        int batchSize = 500;
        int count     = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {


            for (Map.Entry<String, Integer> entry : wordIds.entrySet()) {
                String word = entry.getKey();
                int    id   = entry.getValue();

                long occ   = totalOcc.getOrDefault(id, 0L);
                long start = startCount.getOrDefault(id, 0L);
                long end   = endCount.getOrDefault(id, 0L);

                // Skip words we loaded from DB but never saw in THIS session
                if (occ == 0 && start == 0 && end == 0) continue;

                pstmt.setInt(1, id);
                pstmt.setString(2, word);
                pstmt.setLong(3, occ);
                pstmt.setLong(4, start);
                pstmt.setLong(5, end);
                pstmt.addBatch();
                count++;

                if (count % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }

            pstmt.executeBatch();
            System.out.printf("  Words upserted     : %,d rows%n", count);

        } catch (SQLException e) {
            System.err.println("Error flushing words: " + e.getMessage());
        }
    }

    // ---- Transitions -----------------------------------------------------

    private void flushTransitions(Connection conn) {
        String sql =
            "INSERT INTO word_transitions (firstWordId, secondWordId, count, probability) " +
            "VALUES (?, ?, ?, 0.0) " +
            "ON DUPLICATE KEY UPDATE count = count + VALUES(count)";

        int batchSize = 2000;
        int count     = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {


            for (Map.Entry<Long, Long> entry : transitionCounts.entrySet()) {
                long key = entry.getKey();
                int w1 = (int) (key >> 32);
                int w2 = (int) key;
                long     c     = entry.getValue();

                pstmt.setInt(1, w1);
                pstmt.setInt(2, w2);
                pstmt.setLong(3, c);
                pstmt.addBatch();
                count++;

                if (count % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }

            pstmt.executeBatch();
            System.out.printf("  Transitions upserted: %,d rows%n", count);

        } catch (SQLException e) {
            System.err.println("Error flushing transitions: " + e.getMessage());
        }
    }

    // ---- Recalculate probabilities ---------------------------------------
    //Note: This is now handled in the model training code, so this is no longer called

    /**
     * Updates every row in word_transitions so that probability = count / SUM(count)
     * for all transitions sharing the same firstWordId.
     * Done entirely in SQL
     */
    private void recalculateProbabilities(Connection conn) {
        /*
         * MySQL doesn't allow you to UPDATE a table while selecting from it in a
         * subquery in the same statement, so we join against a derived table
         * (the GROUP BY subquery aliased as `totals`) to get around that limit.
         */
        String sql =
            "UPDATE word_transitions wt " +
            "JOIN ( " +
            "    SELECT firstWordId, SUM(count) AS total " +
            "    FROM word_transitions " +
            "    GROUP BY firstWordId " +
            ") totals ON wt.firstWordId = totals.firstWordId " +
            "SET wt.probability = wt.count / totals.total";

        try (Statement  stmt = conn.createStatement()) {

            int rows = stmt.executeUpdate(sql);
            System.out.printf("  Probabilities updated: %,d rows%n", rows);

        } catch (SQLException e) {
            System.err.println("Error recalculating probabilities: " + e.getMessage());
        }
    }

    // ---- Imported files --------------------------------------------------

    private void flushImportedFiles(Connection conn) {
        String sql =
            "INSERT INTO imported_files " +
            "  (fileName, filePath, fileSizeBytes, wordCount, sentenceCount, uniqueWords, checksum) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int count = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {


            for (FileRecord fr : pendingFiles) {
                pstmt.setString(1, fr.fileName);
                pstmt.setString(2, fr.filePath);
                pstmt.setLong(3, fr.fileSizeBytes);
                pstmt.setLong(4, fr.wordCount);
                pstmt.setLong(5, fr.sentenceCount);
                pstmt.setLong(6, fr.uniqueWords);
                pstmt.setString(7, fr.checksum);
                pstmt.addBatch();
                count++;
            }

            pstmt.executeBatch();

            System.out.printf("  Imported files inserted: %,d rows%n", count);

        } catch (SQLException e) {
            System.err.println("Error inserting imported file record: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }


    // Here provide summary of processed files

    private void printSummary() {
        System.out.println("\nDatabase flush complete.");
        System.out.printf("  Unique words tracked   : %,d%n", wordIds.size());
        System.out.printf("  Unique transitions      : %,d%n", transitionCounts.size());
        System.out.printf("  Files processed this run: %,d%n", pendingFiles.size());
    }

    //  Helper / utility methods (same as regular BigramCounter)
    public static List<String> tokenize(String text) {
        text = text
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace('\u201B', '\'');
        List<String> words = new ArrayList<>();
        Matcher m = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) words.add(m.group());
        return words;
    }

    /**
     * Returns the wordId for a word, creating a new entry if the word hasn't
     * been seen before.  New words are given an ID that continues after the
     * highest existing DB id so there are no collisions.
     */
    private int wordId(String word) {
        return wordIds.computeIfAbsent(word, w -> nextWordId++);
    }   


    /** SHA-256 hex digest — credit: https://www.baeldung.com/sha-256-hashing-java */
    private static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java BigramCounter <folder OR file1.txt ...>");
            System.exit(1);
        }

        Connection conn = null;

        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            BigramCountertoDB counter = new BigramCountertoDB();

            // Load DB state
            counter.loadExistingFromDB(conn);

            Path inputPath = Paths.get(args[0]);

            List<Path> filesToProcess = new ArrayList<>();


            if (Files.isDirectory(inputPath)) {
                System.out.println("Processing directory: " + inputPath);
                //To cover when given a file directory
                //Goes through all txt files within given directory
                try {
                    Files.walk(inputPath)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                        .forEach(filesToProcess::add);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading directory: " + inputPath, e);
                }

            } else {
                //Otherwise treat same way as regular bigramCounter
                for (String arg : args) {
                    filesToProcess.add(Paths.get(arg));
                }
            }


            for (Path file : filesToProcess) {
                System.out.println("Processing: " + file);
                counter.processFile(file);
            }


            counter.flushToDB(conn);
            conn.commit();

            System.out.println("\nAll data committed successfully.");

        } catch (Exception e) {
            System.err.println("Error occurred, rolling back...");
            e.printStackTrace();

            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Rollback successful.");
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
}
