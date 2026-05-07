package databaseConnections;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ImportedFilesQueries {
    /**
     * Records a newly imported file in the database.
     *
     * @param fileName The name of the file
     * @param filePath The full path of the file
     * @param fileSizeBytes Size of the file
     * @param wordCount Total words parsed
     * @param sentenceCount Total sentences parsed
     * @param uniqueWords Unique words found
     * @param checksum A unique hash of the file to prevent duplicate imports
     * @return The auto-generated fileId, or -1 if insertion fails
     */
    public static int insertImportedFile(String fileName, String filePath, long fileSizeBytes, 
                                         int wordCount, int sentenceCount, int uniqueWords, String checksum) {
        String sql = "INSERT INTO imported_files (fileName, filePath, fileSizeBytes, wordCount, sentenceCount, uniqueWords, checksum) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
                     
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, fileName);
            pstmt.setString(2, filePath);
            pstmt.setLong(3, fileSizeBytes);
            pstmt.setInt(4, wordCount);
            pstmt.setInt(5, sentenceCount);
            pstmt.setInt(6, uniqueWords);
            pstmt.setString(7, checksum);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error inserting imported file: " + e.getMessage());
        }
        
        return -1;
    }

    /**
     * Checks if a file has already been imported using its checksum.
     * This prevents re-parsing large files unnecessarily.
     *
     * @param checksum The SHA-256 (or similar) hash of the file contents
     * @return true if already imported, false otherwise
     */
    public static boolean checkIfFileImported(String checksum) {
        String sql = "SELECT COUNT(*) FROM imported_files WHERE checksum = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, checksum);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking if file exists: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Retrieves all imported files (useful for reporting back to the user).
     *
     * @return A list of file names that have been imported
     */
    public static List<String> getAllImportedFiles() {
        List<String> files = new ArrayList<>();
        String sql = "SELECT fileName FROM imported_files ORDER BY importedAt DESC";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                files.add(rs.getString("fileName"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving imported files: " + e.getMessage());
        }

        return files;
    }
    /** Stores metadata for each file imported into the database */

    public static class ImportedFileRecord {
        public String fileName;
        public int wordCount;
        public java.sql.Timestamp importedAt;
    }
    /** Retrieves all imported file records from the database with full details, and is ordered
    from recently imported to the oldest.
     */
    public static List<ImportedFileRecord> getAllImportedFilesDetailed() {
        List<ImportedFileRecord> files = new ArrayList<>();
        String sql = "SELECT fileName, wordCount, importedAt FROM imported_files ORDER BY importedAt DESC";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ImportedFileRecord rec = new ImportedFileRecord();
                rec.fileName = rs.getString("fileName");
                rec.wordCount = rs.getInt("wordCount");
                rec.importedAt = rs.getTimestamp("importedAt");
                files.add(rec);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving detailed imported files: " + e.getMessage());
        }

        return files;
    }
}
