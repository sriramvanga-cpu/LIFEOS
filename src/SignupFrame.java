import javax.swing.*;
import java.awt.*;
import java.awt.event.*; // using the standard awt events to fix the click bug
import java.sql.Date;

public class SignupFrame extends JFrame {

    private JFrame previousFrame;
    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField dobField; 
    private JLabel errorLabel;

    private UserDAO dao = new UserDAO();

    public SignupFrame(JFrame previousFrame) {
        this.previousFrame = previousFrame;
        
        setTitle("LifeOS - Sign Up");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(240, 248, 255));

        // main white container for the form
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(40, 60, 40, 60)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(100, 149, 237));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(title, gbc);

        gbc.gridwidth = 1;

        // name input
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Name:"), gbc);
        nameField = new JTextField(20);
        nameField.setPreferredSize(new Dimension(260, 36));
        gbc.gridx = 1; panel.add(nameField, gbc);

        // email input
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(20);
        emailField.setPreferredSize(new Dimension(260, 36));
        gbc.gridx = 1; panel.add(emailField, gbc);

        // password input
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(260, 36));
        gbc.gridx = 1; panel.add(passwordField, gbc);

        // dob input (needs yyyy-mm-dd)
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("DOB (YYYY-MM-DD):"), gbc);
        dobField = new JTextField(20);
        dobField.setPreferredSize(new Dimension(260, 36));
        gbc.gridx = 1; panel.add(dobField, gbc);

        // show errors here
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(errorLabel, gbc);

        JButton signupBtn = new JButton("Create Account");
        signupBtn.setBackground(new Color(100, 149, 237));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setOpaque(true);
        signupBtn.setBorderPainted(false);
        signupBtn.setPreferredSize(new Dimension(180, 42));
        gbc.gridy = 6; panel.add(signupBtn, gbc);

        // link to go back to login
        JLabel loginLink = new JLabel("<HTML><U>Already have an account? Login</U></HTML>");
        loginLink.setForeground(new Color(100, 149, 237));
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 7; panel.add(loginLink, gbc);

        signupBtn.addActionListener(e -> handleSignup());

        // fixed: using java.awt.event.MouseEvent so the link actually clicks
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(false);
                if (previousFrame != null) {
                    previousFrame.setVisible(true);
                }
            }
        });

        add(panel);
        setVisible(true);
    }

    private void handleSignup() {
        errorLabel.setText("");

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String dobText = dobField.getText().trim();

        // check if anything is empty
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || dobText.isEmpty()) {
            errorLabel.setText("All fields are required");
            return;
        }

        // strict check: must have @ and end with .com
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\\.com$";
        if (!email.matches(emailRegex)) {
            errorLabel.setText("Please enter a valid .com email address");
            return;
        }

        // don't allow short passwords
        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters");
            return;
        }

        // try to parse the date
        java.sql.Date dob;
        try {
            dob = java.sql.Date.valueOf(dobText); 
        } catch (Exception ex) {
            errorLabel.setText("Invalid DOB format (YYYY-MM-DD)");
            return;
        }

        try {
            // send data to the user dao
            dao.registerUser(name, email, password, dob); 
            
            JOptionPane.showMessageDialog(this, "Account created!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // go back to login
            setVisible(false);
            if (previousFrame != null) {
                previousFrame.setVisible(true);
            }
        } catch (java.sql.SQLException ex) {
            // handle the "email already exists" error from neon
            if (ex.getMessage().contains("duplicate key") || ex.getMessage().contains("unique constraint")) {
                errorLabel.setText("Email is already taken.");
            } else {
                errorLabel.setText("DB Error: " + ex.getMessage());
            }
        } catch (Exception ex) {
            errorLabel.setText("Signup failed: " + ex.getMessage());
        }
    }
}