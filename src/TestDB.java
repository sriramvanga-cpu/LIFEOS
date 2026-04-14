import java.sql.Date;

public class TestDB {
    public static void main(String[] args) {

        UserDAO dao = new UserDAO();

        //Register (run once)
        Date dob = Date.valueOf("2004-05-10");
        dao.registerUser(1, "Sriram", "sriram@email.com", "1234", dob);

        // Login test
        //boolean success = dao.login("sriram@email.com", "1234");

        if (success) {
            System.out.println("LOGIN SUCCESS");
        } else {
            System.out.println("LOGIN FAILED");
        }
    }
}