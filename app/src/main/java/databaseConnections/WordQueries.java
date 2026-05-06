package databaseConnections;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
public class WordQueries {
    /**
     * A simple data object to hold word information from the database.
     */
    public static class WordRecord {
        public int wordId;
        public String word;
        public int totalOccurrence;
        public int startCount;
        public int endCount;
    }

    /**
     * Inserts a new word into the database, or updates its counts if it already exists.
     * This handles the core counting logic as you read through text files.
     *
     * @param word The literal text of the word
     * @param isStart True if the word is the first word of a sentence
     * @param isEnd True if the word is the last word of a sentence
     */
    public static void insertOrUpdateWord(String word, boolean isStart, boolean isEnd) {
        int startInc = isStart ? 1 : 0;
        int endInc = isEnd ? 1 : 0;
        
        // This query inserts the word if it's new. If the word already exists (UNIQUE constraint), 
        // it updates the occurrences and counts instead of crashing!
        String sql = "INSERT INTO words (word, totalOccurrence, startCount, endCount) " +
                     "VALUES (?, 1, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "totalOccurrence = totalOccurrence + 1, " +
                     "startCount = startCount + ?, " +
                     "endCount = endCount + ?";
                     
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, word);
            pstmt.setInt(2, startInc);
            pstmt.setInt(3, endInc);
            pstmt.setInt(4, startInc);
            pstmt.setInt(5, endInc);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Error inserting or updating word (" + word + "): " + e.getMessage());
        }
    }

    /**
     * Finds the database ID of a specific word. 
     * Useful when linking words together in the word_transitions table!
     *
     * @param word The literal text of the word
     * @return The wordId, or -1 if the word is not in the database
     */
    public static int getWordId(String word) {
        String sql = "SELECT wordId FROM words WHERE word = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, word);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("wordId");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching word ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves all words in the system for UI reporting.
     * 
     * @param sortByFrequency True to sort by most frequent, false to sort alphabetically.
     * @return A list of WordRecord objects containing the word details.
     */
    public static List<WordRecord> getAllWords(boolean sortByFrequency) {
        List<WordRecord> results = new ArrayList<>();
        
        // Changes the sort parameter based on instructions
        String orderBy = sortByFrequency ? "totalOccurrence DESC" : "word ASC";
        String sql = "SELECT wordId, word, totalOccurrence, startCount, endCount FROM words ORDER BY " + orderBy;
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                WordRecord wr = new WordRecord();
                wr.wordId = rs.getInt("wordId");
                wr.word = rs.getString("word");
                wr.totalOccurrence = rs.getInt("totalOccurrence");
                wr.startCount = rs.getInt("startCount");
                wr.endCount = rs.getInt("endCount");
                results.add(wr);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving words list: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Updates specific information for a word (Feature requested in Project Instructions #4).
     */
    public static void updateWordInfo(int wordId, String newWordText) {
        String sql = "UPDATE words SET word = ? WHERE wordId = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newWordText);
            pstmt.setInt(2, wordId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Error updating word info: " + e.getMessage());
        }
    }
/**
 * Retrieves the name of the source file that a word was imported from,
 * matched by comparing the word's creation date to the file's import date.
 */
    public static String getSourceFileForWord(int wordId) {
        String sql = "SELECT f.fileName FROM imported_files f " +
                "JOIN words w ON DATE(f.importedAt) = DATE(w.createdAt) " +
                "WHERE w.wordId = ? LIMIT 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("fileName");
        } catch (SQLException e) {
            System.out.println("Error getting source file: " + e.getMessage());
        }
        return "N/A";
    }
/**
 * Retrieves the date a word was added to the database, formatted as {YYYY-MM-DD}.
 * The time portion of the timestamp is truncated, returning only the date component.
 */
    public static String getDateAddedForWord(int wordId) {
        String sql = "SELECT createdAt FROM words WHERE wordId = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("createdAt").substring(0, 10);
        } catch (SQLException e) {
            System.out.println("Error getting date added: " + e.getMessage());
        }
        return "N/A";
    }
}
