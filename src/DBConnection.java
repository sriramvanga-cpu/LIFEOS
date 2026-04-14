import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:postgresql://ep-quiet-shadow-a1k4c74x-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_Jy46stNiGIUK";
    
    private static Connection sharedConnection; // Persistent connection

    public static Connection getConnection() {
        try {
            // Only create a new connection if the old one is closed or null
            if (sharedConnection == null || sharedConnection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                sharedConnection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
            return sharedConnection;
        } catch (Exception e) {
            System.err.println("Cloud Connection Failed: " + e.getMessage());
            return null;
        }
    }
}