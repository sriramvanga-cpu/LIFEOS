import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Account {
    int id;
    int userId;
    String name;
    String type;
    double balance;
    Date createdAt;

    public Account(int id, int userId, String name, String type, double balance, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}

public class AccountDAO {

    /**
     * Fetches all accounts using the shared connection.
     */
    public List<Account> getAccounts(int userId) {
        List<Account> list = new ArrayList<>();
        String query = "SELECT ACCOUNT_ID, USER_ID, NAME, TYPE, BALANCE, CREATED_AT FROM ACCOUNTS WHERE USER_ID = ? ORDER BY TYPE, NAME";

        // Use the shared connection without closing it
        Connection conn = DBConnection.getConnection();
        if (conn == null) return list;

        // Close ONLY the PreparedStatement and ResultSet
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Account(
                            rs.getInt("ACCOUNT_ID"),
                            rs.getInt("USER_ID"),
                            rs.getString("NAME"),
                            rs.getString("TYPE"),
                            rs.getDouble("BALANCE"),
                            rs.getDate("CREATED_AT")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Adds a new account. Connection stays open for future transactions.
     */
    public boolean addAccount(int userId, String name, String type, double initialBalance) {
        String query = "INSERT INTO ACCOUNTS (USER_ID, NAME, TYPE, BALANCE) VALUES (?, ?, ?, ?)";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setDouble(4, initialBalance);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates account details using the shared connection.
     */
    public boolean updateAccount(int accountId, String name, String type) {
        String query = "UPDATE ACCOUNTS SET NAME = ?, TYPE = ? WHERE ACCOUNT_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setInt(3, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes an account. Connection stays open.
     */
    public boolean deleteAccount(int accountId) {
        String query = "DELETE FROM ACCOUNTS WHERE ACCOUNT_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates account balance. Fast execution due to reused connection.
     */
    public boolean updateBalance(int accountId, double amount, boolean isCredit) {
        String query = "UPDATE ACCOUNTS SET BALANCE = BALANCE " + (isCredit ? "+" : "-") + " ? WHERE ACCOUNT_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, amount);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}