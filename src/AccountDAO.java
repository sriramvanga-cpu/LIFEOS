import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Account {
    int id;
    int userId;
    String name;
    String type;
    double balance;
    double budget; // NEW: Added budget field
    Date createdAt;

    // UPDATED: Constructor now requires the budget parameter
    public Account(int id, int userId, String name, String type, double balance, double budget, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.balance = balance;
        this.budget = budget;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}

public class AccountDAO {

    /**
     * Fetches all accounts including the budget column.
     */
    public List<Account> getAccounts(int userId) {
        List<Account> list = new ArrayList<>();
        // UPDATED: Added BUDGET to the SELECT query
        String query = "SELECT ACCOUNT_ID, USER_ID, NAME, TYPE, BALANCE, BUDGET, CREATED_AT FROM ACCOUNTS WHERE USER_ID = ? ORDER BY TYPE, NAME";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return list;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // FIX: Passing rs.getDouble("BUDGET") as the 6th argument
                    list.add(new Account(
                            rs.getInt("ACCOUNT_ID"),
                            rs.getInt("USER_ID"),
                            rs.getString("NAME"),
                            rs.getString("TYPE"),
                            rs.getDouble("BALANCE"),
                            rs.getDouble("BUDGET"), 
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
     * Adds a new account with an initial budget.
     */
    public boolean addAccount(int userId, String name, String type, double initialBalance, double budget) {
        // UPDATED: Query now includes the BUDGET column
        String query = "INSERT INTO ACCOUNTS (USER_ID, NAME, TYPE, BALANCE, BUDGET) VALUES (?, ?, ?, ?, ?)";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setDouble(4, initialBalance);
            ps.setDouble(5, budget);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates account details and budget.
     */
    public boolean updateAccount(int accountId, String name, String type, double budget) {
        // UPDATED: Query now allows updating the BUDGET
        String query = "UPDATE ACCOUNTS SET NAME = ?, TYPE = ?, BUDGET = ? WHERE ACCOUNT_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setDouble(3, budget);
            ps.setInt(4, accountId);
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