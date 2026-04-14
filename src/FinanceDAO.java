import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Transaction {
    int id;
    double amount;
    String type;
    String category;
    String notes;
    int accountId;
    Integer toAccountId;
    Timestamp transTime;
    Date transDate;

    public Transaction(int id, double amount, String type, String category, String notes,
                       int accountId, Integer toAccountId, Timestamp transTime) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.notes = notes;
        this.accountId = accountId;
        this.toAccountId = toAccountId;
        this.transTime = transTime;
        this.transDate = transTime != null ? new Date(transTime.getTime()) : null;
    }
}

public class FinanceDAO {

    /**
     * Fetches transactions using the shared connection.
     */
    public List<Transaction> getTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();
        String query = "SELECT TRANSACTION_ID, AMOUNT, TYPE, CATEGORY, NOTES, ACCOUNT_ID, TO_ACCOUNT_ID, TRANSACTION_TIME " +
                "FROM TRANSACTIONS WHERE USER_ID = ? ORDER BY TRANSACTION_TIME DESC";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return list;

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer toAccount = rs.getObject("TO_ACCOUNT_ID") != null ? rs.getInt("TO_ACCOUNT_ID") : null;
                    list.add(new Transaction(
                            rs.getInt("TRANSACTION_ID"),
                            rs.getDouble("AMOUNT"),
                            rs.getString("TYPE"),
                            rs.getString("CATEGORY"),
                            rs.getString("NOTES"),
                            rs.getInt("ACCOUNT_ID"),
                            toAccount,
                            rs.getTimestamp("TRANSACTION_TIME")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Adds a transaction. Reuses connection to speed up multi-step balance updates.
     */
    public boolean addTransaction(int userId, double amount, String type, String category,
                                  String notes, int accountId, Integer toAccountId, Timestamp transTime) {
        if (amount <= 0) return false;

        String query = "INSERT INTO TRANSACTIONS (USER_ID, AMOUNT, TYPE, CATEGORY, NOTES, ACCOUNT_ID, TO_ACCOUNT_ID, TRANSACTION_TIME) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false); // Start transaction block
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                ps.setDouble(2, amount);
                ps.setString(3, type);
                ps.setString(4, category);
                ps.setString(5, notes);
                ps.setInt(6, accountId);
                if (toAccountId != null) ps.setInt(7, toAccountId);
                else ps.setNull(7, Types.INTEGER);
                ps.setTimestamp(8, transTime);
                ps.executeUpdate();
            }

            // Update account balances
            AccountDAO accDao = new AccountDAO();
            if ("EXPENSE".equals(type)) {
                accDao.updateBalance(accountId, amount, false);
            } else if ("INCOME".equals(type)) {
                accDao.updateBalance(accountId, amount, true);
            } else if ("TRANSFER".equals(type) && toAccountId != null) {
                accDao.updateBalance(accountId, amount, false);
                accDao.updateBalance(toAccountId, amount, true);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            // Restore default auto-commit behavior without closing the connection
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Deletes a transaction and reverses balance changes.
     */
    public boolean deleteTransaction(int transactionId) {
        String selectQuery = "SELECT TYPE, AMOUNT, ACCOUNT_ID, TO_ACCOUNT_ID FROM TRANSACTIONS WHERE TRANSACTION_ID = ?";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);
            
            String type = null;
            double amount = 0;
            int accountId = 0;
            Integer toAccountId = null;

            try (PreparedStatement ps = conn.prepareStatement(selectQuery)) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        type = rs.getString("TYPE");
                        amount = rs.getDouble("AMOUNT");
                        accountId = rs.getInt("ACCOUNT_ID");
                        toAccountId = rs.getObject("TO_ACCOUNT_ID") != null ? rs.getInt("TO_ACCOUNT_ID") : null;
                    } else {
                        return false;
                    }
                }
            }

            AccountDAO accDao = new AccountDAO();
            if ("EXPENSE".equals(type)) {
                accDao.updateBalance(accountId, amount, true);
            } else if ("INCOME".equals(type)) {
                accDao.updateBalance(accountId, amount, false);
            } else if ("TRANSFER".equals(type) && toAccountId != null) {
                accDao.updateBalance(accountId, amount, true);
                accDao.updateBalance(toAccountId, amount, false);
            }

            String deleteQuery = "DELETE FROM TRANSACTIONS WHERE TRANSACTION_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                ps.setInt(1, transactionId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}