package databaseConnections;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class dbConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/CS4485DB";
    private static final String USER = "javauser";
    private static final String PASSWORD = "cs4485";

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL,USER,PASSWORD);
    }

    public static void main(String[] args){

        try(Connection conn = getConnection()){
            System.out.println("Connected to MySQL successfully!");
        }catch (SQLException e){
            System.out.println("Connection Failed: "+ e.getMessage());
        }
    }
}
