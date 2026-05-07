package databaseConnections;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GeneratedSentencesQueries {

    public static class SentenceRecord {
        public String sentenceText;
        public String createdAt;
    }

    /**
     * Inserts a generated sentence into the database.
     *
     * @param sentenceText The text of the sentence.
     * @param startingWordId The ID of the starting word (can be null).
     * @param generationMethod Method used for generation (e.g. "bigram", "trigram").
     * @param sentenceLength The number of words in the sentence.
     * @return The auto-generated sentenceId, or -1 if insertion fails.
     */
    public static int insertGeneratedSentence(String sentenceText, Integer startingWordId, String generationMethod, int sentenceLength) {
        String sql = "INSERT INTO generated_sentences (sentenceText, startingWordId, generationMethod, sentenceLength) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, sentenceText);
            
            if (startingWordId != null) {
                pstmt.setInt(2, startingWordId);
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(3, generationMethod);
            pstmt.setInt(4, sentenceLength);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error inserting generated sentence: " + e.getMessage());
        }
        
        return -1;
    }

    /**
     * Retrieves all generated sentences from the database.
     *
     * @return A list of all sentence texts.
     */
    public static List<String> getAllGeneratedSentences() {
        List<String> sentences = new ArrayList<>();
        String sql = "SELECT sentenceText FROM generated_sentences";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                sentences.add(rs.getString("sentenceText"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving generated sentences: " + e.getMessage());
        }
        
        return sentences;
    }

    /**
     * Retrieves all generated sentences with their creation dates.
     *
     * @return A list of SentenceRecord objects.
     */
    public static List<SentenceRecord> getAllGeneratedSentenceRecords() {
        List<SentenceRecord> sentences = new ArrayList<>();
        String sql = "SELECT sentenceText, DATE(createdAt) as dateStr FROM generated_sentences ORDER BY createdAt DESC";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                SentenceRecord sr = new SentenceRecord();
                sr.sentenceText = rs.getString("sentenceText");
                sr.createdAt = rs.getString("dateStr");
                sentences.add(sr);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving generated sentences: " + e.getMessage());
        }
        
        return sentences;
    }

    /**
     * Checks whether a given sentence already exists in the database.
     * Useful for checking for duplicates before saving.
     *
     * @param sentenceText The text of the sentence to check.
     * @return true if the sentence exists, false otherwise.
     */
    public static boolean checkIfSentenceExists(String sentenceText) {
        String sql = "SELECT COUNT(*) FROM generated_sentences WHERE sentenceText = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, sentenceText);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking if sentence exists: " + e.getMessage());
        }
        
        return false;
    }
}
