import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserDAO {
    
    /**
     * Registers a new user using the shared connection.
     */
    public void registerUser(String name, String email, String password, java.sql.Date dob) throws SQLException {
        String query = "INSERT INTO USERS (NAME, EMAIL, PASSWORD_HASH, DOB, HEIGHT) VALUES (?,?,?,?, 170)";
        
        // Use the shared connection without closing it
        Connection conn = DBConnection.getConnection();
        if (conn == null) throw new SQLException("Could not connect to database");
        
        String hashed = PasswordUtil.HashPassword(password);
        
        // Only the PreparedStatement is in try-with-resources
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashed);
            ps.setDate(4, dob);

            ps.executeUpdate();
            System.out.println("User registered in Neon");
        }
    }

    /**
     * Validates user credentials. 
     */
    public int login(String email, String password) {
        String query = "SELECT USER_ID, PASSWORD_HASH FROM USERS WHERE EMAIL = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return -1;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("PASSWORD_HASH");
                    String inputHash = PasswordUtil.HashPassword(password);

                    if (storedHash.equals(inputHash)) {
                        return rs.getInt("USER_ID");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Fetches user profile details.
     */
    public Map<String, String> getUserDetails(int userId) {
        Map<String, String> userDetails = new HashMap<>();
        String query = "SELECT NAME, EMAIL, DOB, HEIGHT FROM USERS WHERE USER_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return userDetails;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    userDetails.put("name", rs.getString("NAME"));
                    userDetails.put("email", rs.getString("EMAIL"));
                    userDetails.put("dob", rs.getDate("DOB") != null ? rs.getDate("DOB").toString() : "");
                    
                    double height = rs.getDouble("HEIGHT");
                    if (rs.wasNull() || height == 0) height = 170.0;
                    userDetails.put("height", String.valueOf(height));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userDetails;
    }

    /**
     * Updates profile information.
     */
    public boolean updateUserDetails(int userId, String name, String email, java.sql.Date dob, double height) {
        String query = "UPDATE USERS SET NAME=?, EMAIL=?, DOB=?, HEIGHT=? WHERE USER_ID=?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setDate(3, dob);
            ps.setDouble(4, height);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    /**
     * Securely updates the user password.
     */
    public boolean updatePassword(int userId, String currentPassword, String newPassword) {
        String query = "SELECT PASSWORD_HASH FROM USERS WHERE USER_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            String storedHash = null;
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("PASSWORD_HASH");
                    }
                }
            }

            if (storedHash != null && storedHash.equals(PasswordUtil.HashPassword(currentPassword))) {
                String update = "UPDATE USERS SET PASSWORD_HASH=? WHERE USER_ID=?";
                try (PreparedStatement ps2 = conn.prepareStatement(update)) {
                    ps2.setString(1, PasswordUtil.HashPassword(newPassword));
                    ps2.setInt(2, userId);
                    return ps2.executeUpdate() > 0;
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return false;
    }
}