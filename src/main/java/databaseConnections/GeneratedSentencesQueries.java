package databaseConnections;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GeneratedSentencesQueries {

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
}
