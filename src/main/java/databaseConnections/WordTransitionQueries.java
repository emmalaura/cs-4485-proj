package databaseConnections;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
public class WordTransitionQueries {
    /**
     * Data object representing a word that can follow a given word.
     */
    public static class TransitionRecord {
        public int secondWordId;
        public String nextWordText;
        public int count;
        public double probability;
    }

    /**
     * Inserts a new transition between two words, or increments the count if they've been seen together before.
     * This handles instruction 1B (tracking which words follow other words and how often).
     *
     * @param firstWordId The ID of the preceding word
     * @param secondWordId The ID of the following word
     */
    public static void insertOrUpdateTransition(int firstWordId, int secondWordId) {
        String sql = "INSERT INTO word_transitions (firstWordId, secondWordId, count, probability) " +
                     "VALUES (?, ?, 1, 0.0) " +
                     "ON DUPLICATE KEY UPDATE count = count + 1";
                     
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, firstWordId);
            pstmt.setInt(2, secondWordId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Error inserting/updating word transition: " + e.getMessage());
        }
    }

    /**
     * Fetches a list of all words that have ever followed a specific word.
     * This is absolutely crucial for Auto-Complete (Instruction 3B) and Generating Sentences (Instruction 3A).
     * The results are ordered by 'count' in descending order, making the most likely words appear first!
     *
     * @param firstWordId The ID of the starting word
     * @return A list of TransitionRecord objects, including the text of the next word.
     */
    public static List<TransitionRecord> getNextWords(int firstWordId) {
        List<TransitionRecord> options = new ArrayList<>();
        
        // This query specifically joins the transitions table with the words table 
        // extremely efficiently so you actually get the text of the word, not just an ID!
        String sql = "SELECT t.secondWordId, w.word AS nextWord, t.count, t.probability " +
                     "FROM word_transitions t " +
                     "JOIN words w ON t.secondWordId = w.wordId " +
                     "WHERE t.firstWordId = ? " +
                     "ORDER BY t.count DESC";
                     
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, firstWordId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TransitionRecord tr = new TransitionRecord();
                    tr.secondWordId = rs.getInt("secondWordId");
                    tr.nextWordText = rs.getString("nextWord");
                    tr.count = rs.getInt("count");
                    tr.probability = rs.getDouble("probability");
                    
                    options.add(tr);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching next words: " + e.getMessage());
        }
        
        return options;
    }

    /**
     * Updates the calculated probability for a specific transition.
     * This is highly recommended for implementing "various algorithms" (Instruction 3A).
     */
    public static void updateTransitionProbability(int firstWordId, int secondWordId, double probability) {
        String sql = "UPDATE word_transitions SET probability = ? WHERE firstWordId = ? AND secondWordId = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, probability);
            pstmt.setInt(2, firstWordId);
            pstmt.setInt(3, secondWordId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Error updating transition probability: " + e.getMessage());
        }
    }
}
