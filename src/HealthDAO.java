import java.sql.*;
import java.util.*;

public class HealthDAO {

    // --- DATA MODELS ---
    public static class HealthRecord {
        public double sleep = 0.0;
        public int steps = 0;
        public double weight = 0.0;
    }

    public static class Medication {
        public int medId;
        public int userId;
        public String name;
        public String dosage;
        public int gap;
    }

    public static class ActivityLog {
        public int logId;
        public String activity;
        public int duration; 
        public double distance; 
        public java.sql.Timestamp startTime;
        public java.sql.Timestamp endTime;
    }

    // =========================================================================
    // DAILY RECORDS & WEIGHT PERSISTENCE
    // =========================================================================
    
    public HealthRecord getDailyRecord(int userId, java.sql.Date date) {
        HealthRecord record = new HealthRecord();
        boolean foundToday = false;
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return record;

        String query = "SELECT SLEEP, STEPS, WEIGHT FROM HEALTH_RECORDS WHERE USER_ID = ? AND RECORD_DATE = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    record.sleep = rs.getDouble("SLEEP");
                    record.steps = rs.getInt("STEPS");
                    record.weight = rs.getDouble("WEIGHT");
                    foundToday = true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (!foundToday || record.weight == 0.0) {
            String wQuery = "SELECT WEIGHT FROM HEALTH_RECORDS WHERE USER_ID = ? AND WEIGHT > 0 ORDER BY RECORD_DATE DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(wQuery)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) record.weight = rs.getDouble("WEIGHT");
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return record;
    }

    public void saveDailyRecord(int userId, java.sql.Date date, HealthRecord record) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        String checkQuery = "SELECT RECORD_ID FROM HEALTH_RECORDS WHERE USER_ID = ? AND RECORD_DATE = ?";
        int existingId = -1;
        
        try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
            ps.setInt(1, userId);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) existingId = rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }

        String sql = (existingId != -1) ? 
            "UPDATE HEALTH_RECORDS SET SLEEP = ?, STEPS = ?, WEIGHT = ? WHERE RECORD_ID = ?" :
            "INSERT INTO HEALTH_RECORDS (USER_ID, SLEEP, STEPS, WEIGHT, RECORD_DATE) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (existingId != -1) {
                ps.setDouble(1, record.sleep); 
                ps.setInt(2, record.steps);
                ps.setDouble(3, record.weight); 
                ps.setInt(4, existingId);
            } else {
                ps.setInt(1, userId); 
                ps.setDouble(2, record.sleep);
                ps.setInt(3, record.steps); 
                ps.setDouble(4, record.weight);
                ps.setDate(5, date);
            }
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================================
    // CHARTING & STREAKS
    // =========================================================================

    public Map<java.time.LocalDate, Double> getWeeklyMetric(int userId, String columnName) {
        Map<java.time.LocalDate, Double> data = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            data.put(java.time.LocalDate.now().minusDays(i), 0.0);
        }

        Connection conn = DBConnection.getConnection();
        if (conn == null) return data;

        String query = "SELECT RECORD_DATE, " + columnName + " FROM HEALTH_RECORDS " +
                       "WHERE USER_ID = ? AND RECORD_DATE >= CURRENT_DATE - 6 ORDER BY RECORD_DATE ASC";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getDate("RECORD_DATE").toLocalDate(), rs.getDouble(columnName));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    public int getStepStreak(int userId) {
        int streak = 0;
        Connection conn = DBConnection.getConnection();
        if (conn == null) return 0;

        String query = "SELECT STEPS FROM HEALTH_RECORDS WHERE USER_ID = ? AND RECORD_DATE <= CURRENT_DATE ORDER BY RECORD_DATE DESC";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("STEPS") >= 10000) streak++; else break;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return streak;
    }

    // =========================================================================
    // ACTIVITY LOGGING
    // =========================================================================

    public List<ActivityLog> getActivities(int userId) {
        List<ActivityLog> logs = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return logs;

        String query = "SELECT * FROM TIME_LOGS WHERE USER_ID = ? ORDER BY START_TIME DESC";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityLog log = new ActivityLog();
                    log.logId = rs.getInt("LOG_ID"); 
                    log.activity = rs.getString("ACTIVITY");
                    log.duration = rs.getInt("DURATION"); 
                    log.distance = rs.getDouble("DISTANCE");
                    log.startTime = rs.getTimestamp("START_TIME"); 
                    log.endTime = rs.getTimestamp("END_TIME");
                    logs.add(log);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return logs;
    }

    public void addActivity(int userId, String activity, int duration, double distance, java.sql.Timestamp start, java.sql.Timestamp end) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        String query = "INSERT INTO TIME_LOGS (USER_ID, ACTIVITY, DURATION, DISTANCE, START_TIME, END_TIME, LOG_DATE) " +
                       "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId); 
            ps.setString(2, activity);
            ps.setInt(3, duration); 
            ps.setDouble(4, distance);
            ps.setTimestamp(5, start); 
            ps.setTimestamp(6, end);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteActivity(int logId) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM TIME_LOGS WHERE LOG_ID = ?")) {
            ps.setInt(1, logId); 
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================================
    // MEDICATIONS & USER PROFILE HELPERS
    // =========================================================================

    public List<Medication> getMedications(int userId) {
        List<Medication> meds = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return meds;

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM MEDICATIONS WHERE USER_ID = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medication m = new Medication();
                    m.medId = rs.getInt("MED_ID"); 
                    m.name = rs.getString("NAME");
                    m.dosage = rs.getString("DOSAGE"); 
                    m.gap = rs.getInt("GAP");
                    meds.add(m);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return meds;
    }

    public void addMedication(int userId, String name, String dosage, int gap) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        String query = "INSERT INTO MEDICATIONS (USER_ID, NAME, DOSAGE, GAP) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId); 
            ps.setString(2, name);
            ps.setString(3, dosage); 
            ps.setInt(4, gap);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteMedication(int medId) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM MEDICATIONS WHERE MED_ID = ?")) {
            ps.setInt(1, medId); 
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public double getUserHeight(int userId) {
        double height = 170.0;
        Connection conn = DBConnection.getConnection();
        if (conn == null) return height;

        try (PreparedStatement ps = conn.prepareStatement("SELECT HEIGHT FROM USERS WHERE USER_ID = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) height = rs.getDouble("HEIGHT");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return height;
    }
}