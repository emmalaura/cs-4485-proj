import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.*;

/**
BigramCounter

Processes one or more .txt files and writes/updates four CSVs whose columns
match the database schema. If the CSVs already exist in the output directory,
their data is loaded first so each run accumulates rather than overwrites.

words.csv                 → words table
word_transitions.csv      → word_transitions table
imported_files.csv        → imported_files table
word_file_occurrences.csv → word_file_occurrences table

Repeat files are check for by checksum and skipped if already processed

Usage:
   java BigramCounter <outputDir> <file1.txt> [file2.txt ...]
*/
public class BigramCounter {

    
    // Patterns For Words and sentances
    private static final Pattern WORD_PATTERN   = Pattern.compile("\\b[a-zA-Z]+(?:'[a-zA-Z]+)*\\b");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    // Hashmaps for counts
    private final Map<String, Integer> wordIds    = new LinkedHashMap<>();
    private final Map<Integer, Long>   totalOcc   = new HashMap<>();
    private final Map<Integer, Long>   startCount = new HashMap<>();
    private final Map<Integer, Long>   endCount   = new HashMap<>();

    //Map for transition count
    private final Map<String, Long> transitionCounts = new LinkedHashMap<>();

    //List of files for checking repeats, and storing new files
    private final List<FileRecord>                 files         = new ArrayList<>();
    private final Set<String>                      seenChecksums = new HashSet<>();
    //For doing occurences by file
    private final Map<Integer, Map<Integer, Long>> fileWordOcc   = new LinkedHashMap<>();

    //File data record to make file handling and duplicate checking more convenient
    private static class FileRecord {
        int    fileId;
        String fileName;
        String filePath;
        long   fileSizeBytes;
        long   wordCount;
        long   sentenceCount;
        long   uniqueWords;
        String checksum;
    }


    /*Method used to load csv files into memory if they already exist
      Creates them if not existing, modifies if they do exist
      File data is filled into respective Maps from earlier
      Each load method follows same format:
        Check if file exists
        Try to read with a buffered reader

    */

    
    //method tries to load outputDir csv files if they do exist
    //Broken up into 4 helper functions for each respsective csv file
    //Each of them will simply return without doing anything if the file doesn't exist
    public void loadExisting(Path dir) throws IOException {
        loadWordsCSV(dir.resolve("words.csv"));
        loadTransitionsCSV(dir.resolve("word_transitions.csv"));
        loadImportedFilesCSV(dir.resolve("imported_files.csv"));
        loadWordFileOccurrencesCSV(dir.resolve("word_file_occurrences.csv"));
    }

    private void loadWordsCSV(Path path) throws IOException {
        if (!Files.exists(path)) return; //Check if file exists, if not return
        System.out.println("Loading existing: " + path.getFileName()); //Indicate file is being loaded
        try (BufferedReader br = openReader(path)) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) { //keep reading until end
                String[] c = splitCSVLine(line); //Use split method to split into fields
                if (c.length < 5) continue; //Making sure all fields are full
                //Store info in the row in variables
                int    id    = Integer.parseInt(c[0].trim()); 
                String word  = c[1].trim();
                long   total = Long.parseLong(c[2].trim());
                long   start = Long.parseLong(c[3].trim());
                long   end   = Long.parseLong(c[4].trim());
                //Insert information into respective Maps
                wordIds.put(word, id); 
                totalOcc.put(id, total);
                startCount.put(id, start);
                endCount.put(id, end);
            }
        }
        System.out.printf("  Loaded %,d words%n", wordIds.size());
    }

    private void loadTransitionsCSV(Path path) throws IOException {
        if (!Files.exists(path)) return;
        System.out.println("Loading existing: " + path.getFileName());
        try (BufferedReader br = openReader(path)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) { //read until done
                String[] c = splitCSVLine(line); //use helper to split fields
                if (c.length < 3) continue; //make sure row has full fields
                String key   = c[0].trim() + ":" + c[1].trim(); //Get word pair ID as key
                long   count = Long.parseLong(c[2].trim()); //get count as the value
                // probability is recomputed on write since it changes with occurances
                // thus only need the counts read in
                transitionCounts.put(key, count);
            }
        }
        System.out.printf("  Loaded %,d transitions%n", transitionCounts.size());
    }

    private void loadImportedFilesCSV(Path path) throws IOException {
        if (!Files.exists(path)) return;
        System.out.println("Loading existing: " + path.getFileName());
        try (BufferedReader br = openReader(path)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) { //Read until end
                String[] c = splitCSVLine(line);
                if (c.length < 8) continue; //ensure enough fields in read row
                FileRecord fr = new FileRecord(); //create filerecord
                fr.fileId        = Integer.parseInt(c[0].trim()); //Store respective fields in their attributes
                fr.fileName      = c[1].trim();
                fr.filePath      = c[2].trim();
                fr.fileSizeBytes = Long.parseLong(c[3].trim());
                fr.wordCount     = Long.parseLong(c[4].trim());
                fr.sentenceCount = Long.parseLong(c[5].trim());
                fr.uniqueWords   = Long.parseLong(c[6].trim());
                fr.checksum      = c[7].trim();
                files.add(fr); //Add to file record list
                seenChecksums.add(fr.checksum); //Also add the checksum so no repeat processes
                fileWordOcc.putIfAbsent(fr.fileId, new LinkedHashMap<>()); //Add a new map for the file for filewordoccurances
            }
        }
        System.out.printf("  Loaded %,d file records%n", files.size());
    }

    private void loadWordFileOccurrencesCSV(Path path) throws IOException {
        if (!Files.exists(path)) return;
        System.out.println("Loading existing: " + path.getFileName());
        long rows = 0;
        try (BufferedReader br = openReader(path)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {//read until end
                String[] c = splitCSVLine(line); //helper to split fields into string[] for the row
                if (c.length < 3) continue; //make sure enough fields in the row
                int  wordId = Integer.parseInt(c[0].trim()); //store fields in variables
                int  fileId = Integer.parseInt(c[1].trim());
                long count  = Long.parseLong(c[2].trim());
                //Here Use fileId as Key to its own file occurance map
                //And enter (wordId, Count) as key,value into that map
                fileWordOcc.computeIfAbsent(fileId, k -> new LinkedHashMap<>()) .put(wordId, count);
                rows++; //keeping count of rows
            }
        }
        System.out.printf("  Loaded %,d word-file occurrence rows%n", rows);
    }

    //Actual method for Processing each file

    public void processFile(Path file) throws IOException, NoSuchAlgorithmException {
        byte[] raw      = Files.readAllBytes(file); //Take the raw byte input of the file
        String checksum = sha256Hex(raw); //Use to calculate checksum with hash method

        if (seenChecksums.contains(checksum)) { //Check if checksum already seen
            System.out.println("  Skipping (already imported): " + file.getFileName()); //Output that file was skipped if so 
            return;                                                                     //And return 
        }
        seenChecksums.add(checksum); //Add to the list of seen checksums

        String text = new String(raw, StandardCharsets.UTF_8); //Turn raw byte input into String
        //Create a FildRecord and populate its fields
        FileRecord fr = new FileRecord();
        fr.fileId        = files.size() + 1;
        fr.fileName      = file.getFileName().toString();
        fr.filePath      = file.toAbsolutePath().toString();
        fr.fileSizeBytes = raw.length;
        fr.checksum      = checksum;
        files.add(fr); //Add to list of files

        Map<Integer, Long> perFileWords = new LinkedHashMap<>(); //Create the per file occurances map
        fileWordOcc.put(fr.fileId, perFileWords); //Ad to overall map

        String[] sentenceChunks = SENTENCE_SPLIT.split(text); //Use sentence pattern to split by punctuation
        long sentenceCount = 0; //initialize counts for sentences and words
        long wordCount     = 0; 

        for (String chunk : sentenceChunks) { //Per sentence
            List<String> words = tokenize(chunk); //tokenize and add to words list
            if (words.isEmpty()) continue;

            sentenceCount++; //keep count of sentences
            wordCount += words.size(); //Also add to total words

            //Processing words individually for count purposes

            for (int i = 0; i < words.size(); i++) { //For each word
                int wid = wordId(words.get(i)); //Get that words Id
                totalOcc.merge(wid, 1L, Long::sum); //Update the counts in the maps
                perFileWords.merge(wid, 1L, Long::sum);
                if (i == 0)                 startCount.merge(wid, 1L, Long::sum); //If beginning, add to start of sentence count accordingly
                if (i == words.size() - 1) endCount.merge(wid,   1L, Long::sum); //Same as above but for end
            }

            //Processing word pairs 

            for (int i = 0; i < words.size() - 1; i++) { //For each word until 2nd to last 
                int w1 = wordId(words.get(i)); //Get first word's ID
                int w2 = wordId(words.get(i + 1)); //Get second word's ID
                transitionCounts.merge(w1 + ":" + w2, 1L, Long::sum); //Use w1ID and w2ID and update transition Map accordingly
            }
        }
        //At this point, done processing the input file for function call
        //Update File totals
        fr.sentenceCount = sentenceCount;
        fr.wordCount     = wordCount;
        fr.uniqueWords   = perFileWords.size();
        //Output Per file data
        System.out.printf("  Processed: %s  (%,d words, %,d sentences)%n",
                fr.fileName, fr.wordCount, fr.sentenceCount);
    }

    //This method writes back the data in the Maps to the csvs

    public void writeCSVs(String outputDir) throws IOException {
        Path dir = Paths.get(outputDir); //Get the designated output directory
        Files.createDirectories(dir); 
        //For each csv to be written, call their respective helper method
        writeWordsCSV(dir.resolve("words.csv"));
        writeTransitionsCSV(dir.resolve("word_transitions.csv"));
        writeImportedFilesCSV(dir.resolve("imported_files.csv"));
        writeWordFileOccurrencesCSV(dir.resolve("word_file_occurrences.csv"));
        //At this point, final writes for overall counts done
        //Output to console overall row counts for debugging purposes
        System.out.println("\nCSVs updated in: " + dir.toAbsolutePath());
        System.out.printf("  words                  : %,d rows%n", wordIds.size());
        System.out.printf("  word_transitions       : %,d rows%n", transitionCounts.size());
        System.out.printf("  imported_files         : %,d rows%n", files.size());
        long occRows = fileWordOcc.values().stream().mapToLong(Map::size).sum();
        System.out.printf("  word_file_occurrences  : %,d rows%n", occRows);
    }

    //Below are the helper methods used above to write to each individual output csv file

    private void writeWordsCSV(Path path) throws IOException {
        try (PrintWriter pw = openWriter(path)) { //Open the file, creating it if it doesn't exist
            pw.println("wordId,word,totalOccurrence,startCount,endCount"); //enter header
            for (Map.Entry<String, Integer> e : wordIds.entrySet()) { //Use the wordID map
                int id = e.getValue(); //Get the id using getValue for each containted word
                pw.printf("%d,%s,%d,%d,%d%n", //Print all respective fields row by row
                        id,
                        csvQuote(e.getKey()),
                        totalOcc.getOrDefault(id, 0L),
                        startCount.getOrDefault(id, 0L),
                        endCount.getOrDefault(id, 0L));
            }
        }
    }

    private void writeTransitionsCSV(Path path) throws IOException {
        try (PrintWriter pw = openWriter(path)) {
            pw.println("firstWordId,secondWordId,count,probability"); //Header
            for (Map.Entry<String, Long> e : transitionCounts.entrySet()) { //Use transitionCounts Map data 
                String[] parts    = e.getKey().split(":"); //and enter it row by row
                int      w1       = Integer.parseInt(parts[0]);
                int      w2       = Integer.parseInt(parts[1]);
                long     count    = e.getValue();
                long     outgoing = totalOcc.getOrDefault(w1, 1L);
                double   prob     = (double) count / outgoing;
                pw.printf("%d,%d,%d,%.8f%n", w1, w2, count, prob);
            }
        }
    }

    private void writeImportedFilesCSV(Path path) throws IOException {
        try (PrintWriter pw = openWriter(path)) {
            pw.println("fileId,fileName,filePath,fileSizeBytes,wordCount,sentenceCount,uniqueWords,checksum");//Header
            for (FileRecord fr : files) {
                pw.printf("%d,%s,%s,%d,%d,%d,%d,%s%n", //Row by row info per file
                        fr.fileId,
                        csvQuote(fr.fileName),
                        csvQuote(fr.filePath),
                        fr.fileSizeBytes,
                        fr.wordCount,
                        fr.sentenceCount,
                        fr.uniqueWords,
                        csvQuote(fr.checksum));
            }
        }
    }

    private void writeWordFileOccurrencesCSV(Path path) throws IOException {
        try (PrintWriter pw = openWriter(path)) {
            pw.println("wordId,fileId,count");//header
            for (Map.Entry<Integer, Map<Integer, Long>> fe : fileWordOcc.entrySet()) {//use fileoccurances map to get word occurances per file
                int fileId = fe.getKey();
                for (Map.Entry<Integer, Long> we : fe.getValue().entrySet()) {
                    pw.printf("%d,%d,%d%n", we.getKey(), fileId, we.getValue());
                }
            }
        }
    }

    //Other helper methods used during processing

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
    //Returns wordID or assigns if needed
    private int wordId(String word) {
        return wordIds.computeIfAbsent(word, w -> wordIds.size() + 1);
    }
    //Calculates Hash for files, credit to https://www.baeldung.com/sha-256-hashing-java
    private static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     Helper method used when loading in csvs
     */
    private static String[] splitCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false; //flag to handle fields in quotes
        for (int i = 0; i < line.length(); i++) { //iterates through characters
            char ch = line.charAt(i); //Takes next character and checks if it's a quote
            if (inQuotes) { //If a quote, treat as quote, if not: add to the current stringbuilder
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false; //Must be ending quote, so no longer in quotes
                    }
                } else {
                    sb.append(ch); //Otherwise (normal character) append to string
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {  //If chracter comma, know field is done
                    fields.add(sb.toString()); //Add build string to field
                    sb.setLength(0); //initiate new string to be built
                } else {
                    sb.append(ch); //Otherwise (normal character) append to string
                }
            }
        }
        fields.add(sb.toString()); //Once out of chracters, last field ended, append to fields
        return fields.toArray(new String[0]); //Return all built fields as array
    }

    private static String csvQuote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static PrintWriter openWriter(Path path) throws IOException {
        return new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)));
    }

    private static BufferedReader openReader(Path path) throws IOException {
        return new BufferedReader(
                new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8));
    }

    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java BigramCounter <outputDir> <file1.txt> [file2.txt ...]"); //Currently have input as list of files, may change to input folder if it gets annoying enough
            System.exit(1);
        }

        String outputDir = args[0]; //store argument for outputDirectory
        BigramCounter counter = new BigramCounter();

        // Load whatever CSVs already exist in the output directory
        try {
            counter.loadExisting(Paths.get(outputDir));
        } catch (IOException e) {
            System.err.println("Warning: could not load existing CSVs — " + e.getMessage());
        }

        // Process each new file
        for (int i = 1; i < args.length; i++) {
            Path file = Paths.get(args[i]); //Get each file
            System.out.println("Processing: " + file); //Output indication it's being processed
            try {
                counter.processFile(file); //Use processFile to process
            } catch (IOException e) { 
                System.err.println("  I/O error: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("  SHA-256 unavailable: " + e.getMessage());
            }
        }

        // Write everything back out (all contained in writeCSVs function)
        try {
            counter.writeCSVs(outputDir);
        } catch (IOException e) {
            System.err.println("Failed to write CSVs: " + e.getMessage());
            System.exit(1);
        }
    }
}
