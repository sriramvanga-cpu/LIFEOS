public class Task {
    public int taskId;
    public int userId;
    public String title;
    
    public String category;     
    public String description;     
    
    public java.sql.Date startDate;
    public int startHour;
    public int startMin;
    public int endHour;
    public int endMin;

    public Task(int taskId, int userId, String title, String category, String description, 
                java.sql.Date startDate, int startHour, int startMin, int endHour, int endMin) {
        this.taskId = taskId;
        this.userId = userId;
        this.title = title;
        
        this.category = category;       // NEW
        this.description = description; // NEW
        
        this.startDate = startDate;
        this.startHour = startHour;
        this.startMin = startMin;
        this.endHour = endHour;
        this.endMin = endMin;
    }
}