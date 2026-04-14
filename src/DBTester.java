import java.sql.Connection;
import java.sql.DriverManager;

public class DBTester {
    public static void main(String[] args) {
        // Your specific Neon credentials
        String url = "jdbc:postgresql://ep-quiet-shadow-a1k4c74x-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_Jy46stNiGIUK";

        System.out.println("--- Starting Connection Test ---");
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("1. Driver Loaded Successfully.");
            
            Connection conn = DriverManager.getConnection(url, user, pass);
            if (conn != null) {
                System.out.println("2. ✅ SUCCESS! Connected to Neon Cloud.");
                conn.close();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("2. ❌ ERROR: Driver not found in classpath.");
        } catch (java.sql.SQLException e) {
            System.err.println("2. ❌ ERROR: SQL State: " + e.getSQLState());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace(); // This prints the vital 'why'
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}