import java.sql.*;
import java.util.*;

public class TaskDAO {

    // ===== GET TASKS FOR A SPECIFIC DATE =====
    public List<Task> getTasksByDate(int userId, java.sql.Date date) {
        List<Task> tasks = new ArrayList<>();

        String query = "SELECT * FROM TASKS WHERE USER_ID = ? AND START_TIME = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setDate(2, date);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tasks.add(mapTask(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tasks;
    }


    // ===== GET TASKS FOR A WHOLE MONTH =====
    public List<Task> getTasksByMonth(int userId, java.sql.Date anyDateInMonth) {
        List<Task> tasks = new ArrayList<>();

        String query = "SELECT * FROM TASKS WHERE USER_ID = ? " +
                       "AND START_TIME >= TRUNC(?, 'MM') " +
                       "AND START_TIME < ADD_MONTHS(TRUNC(?, 'MM'), 1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setDate(2, anyDateInMonth);
            ps.setDate(3, anyDateInMonth);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tasks.add(mapTask(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tasks;
    }


    // ===== ADD TASK =====
    public void addTask(int userId, String title, java.sql.Date date,
                        int startHour, int startMin,
                        int endHour, int endMin) {

        String query = "INSERT INTO TASKS " +
                "(TASK_ID, USER_ID, TITLE, START_TIME, START_HOUR, START_MIN, END_HOUR, END_MIN, STATUS) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            int taskId = (int) (System.currentTimeMillis() % 1000000);

            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            ps.setString(3, title);
            ps.setDate(4, date);
            ps.setInt(5, startHour);
            ps.setInt(6, startMin);
            ps.setInt(7, endHour);
            ps.setInt(8, endMin);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ===== COMMON MAPPER (CLEAN CODE) =====
    private Task mapTask(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("TASK_ID"),
                rs.getInt("USER_ID"),
                rs.getString("TITLE"),
                rs.getDate("START_TIME"),
                rs.getInt("START_HOUR"),
                rs.getInt("START_MIN"),
                rs.getInt("END_HOUR"),
                rs.getInt("END_MIN")
        );
    }

    public void deleteTask(int taskId) {
        String query = "DELETE FROM TASKS WHERE TASK_ID = ?";
        try (Connection conn = DBConnection.getConnection();
           PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}