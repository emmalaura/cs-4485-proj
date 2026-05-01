package databaseConnections;
import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class dbConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/CS4485DB";
    private static final String USER = "javauser";
    private static final String PASSWORD = "cs4485";
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(20000);
        dataSource = new HikariDataSource(config);
    }
    public static Connection getConnection() throws SQLException{
        return dataSource.getConnection();
    }

    public static void main(String[] args){

        try(Connection conn = getConnection()){
            System.out.println("Connected to MySQL successfully!");
        }catch (SQLException e){
            System.out.println("Connection Failed: "+ e.getMessage());
        }
    }
}
