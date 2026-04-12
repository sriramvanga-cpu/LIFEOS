public class Task {
    public int taskId;
    public int userId;
    public String title;
    public java.sql.Date startDate;
    public int startHour;
    public int startMin;
    public int endHour;
    public int endMin;

    public Task(int taskId, int userId, String title, java.sql.Date startDate,
                int startHour, int startMin, int endHour, int endMin) {
        this.taskId = taskId;
        this.userId = userId;
        this.title = title;
        this.startDate = startDate;
        this.startHour = startHour;
        this.startMin = startMin;
        this.endHour = endHour;
        this.endMin = endMin;
    }
}