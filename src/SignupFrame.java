import javax.swing.*;

import org.w3c.dom.events.MouseEvent;

import java.awt.*;
import java.awt.event.*;
import java.sql.Date;

public class SignupFrame extends JFrame {

    private JFrame previousFrame;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField dobField; // YYYY-MM-DD
    private JLabel errorLabel;

    private UserDAO dao = new UserDAO();

    public SignupFrame(JFrame previousFrame) {
        this.previousFrame = previousFrame;
        setTitle("LifeOS - Sign Up");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(240, 248, 255));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(40, 60, 40, 60)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(100, 149, 237));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(title, gbc);

        gbc.gridwidth = 1;

        // Name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(nameLabel, gbc);

        nameField = new JTextField(20);
        nameField.setPreferredSize(new Dimension(260, 36));
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nameField.setBackground(Color.WHITE);
        nameField.setForeground(Color.BLACK);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        // Email
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        emailField.setPreferredSize(new Dimension(260, 36));
        emailField.setFont(new Font("Arial", Font.PLAIN, 16));
        emailField.setBackground(Color.WHITE);
        emailField.setForeground(Color.BLACK);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        // Password
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(260, 36));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setBackground(Color.WHITE);
        passwordField.setForeground(Color.BLACK);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // DOB
        JLabel dobLabel = new JLabel("DOB (YYYY-MM-DD):");
        dobLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(dobLabel, gbc);

        dobField = new JTextField(20);
        dobField.setPreferredSize(new Dimension(260, 36));
        dobField.setFont(new Font("Arial", Font.PLAIN, 16));
        dobField.setBackground(Color.WHITE);
        dobField.setForeground(Color.BLACK);
        gbc.gridx = 1;
        panel.add(dobField, gbc);

        // Error label
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        errorLabel.setForeground(Color.RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(errorLabel, gbc);

        // Buttons
        JButton signupBtn = new JButton("Create Account");
        signupBtn.setFont(new Font("Arial", Font.BOLD, 16));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setBackground(new Color(100, 149, 237));
        signupBtn.setOpaque(true);
        signupBtn.setBorderPainted(false);
        signupBtn.setFocusPainted(false);
        signupBtn.setPreferredSize(new Dimension(180, 42));

        gbc.gridy = 6;
        panel.add(signupBtn, gbc);

        JLabel loginLink = new JLabel("<HTML><U>Already have an account? Login</U></HTML>");
        loginLink.setFont(new Font("Arial", Font.PLAIN, 14));
        loginLink.setForeground(new Color(100, 149, 237));
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));

        gbc.gridy = 7;
        panel.add(loginLink, gbc);

        // Actions
        signupBtn.addActionListener(e -> handleSignup());

        loginLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setVisible(false);
                previousFrame.setVisible(true);
            }
        });

        add(panel);
        setVisible(true);
    }

    private void handleSignup() {
    // Reset error message at the start of every attempt
    errorLabel.setText("");

    String name = nameField.getText().trim();
    String email = emailField.getText().trim();
    String password = new String(passwordField.getPassword());
    String dobText = dobField.getText().trim();

    // 1. Basic Empty Field Validation
    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || dobText.isEmpty()) {
        errorLabel.setText("All fields are required");
        return;
    }

    // 2. Robust Email Validation
    String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\\.com$";
    if (!email.matches(emailRegex)) {
        errorLabel.setText("Please enter a valid email address");
        return;
    }

    // 3. Password Strength Check
    if (password.length() < 6) {
        errorLabel.setText("Password must be at least 6 characters");
        return;
    }

    // 4. Date of Birth Format Validation
    java.sql.Date dob;
    try {
        dob = java.sql.Date.valueOf(dobText); //YYYY-MM-DD
    } catch (Exception ex) {
        errorLabel.setText("Invalid DOB format (YYYY-MM-DD)");
        return;
    }

    // 5. Database Registration with Detailed Error Feedback
    try {
        dao.registerUser(name, email, password, dob); 
        
        JOptionPane.showMessageDialog(this, "Account created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        
        setVisible(false);
        if (previousFrame != null) {
            previousFrame.setVisible(true);
        }
    } catch (java.sql.SQLException ex) {
        // Specific check for existing emails
        if (ex.getMessage().contains("duplicate key") || ex.getMessage().contains("unique constraint")) {
            errorLabel.setText("Email is already registered. Please login.");
        } else {
            errorLabel.setText("Database Error: " + ex.getMessage());
        }
        ex.printStackTrace();
    } catch (Exception ex) {
        // Catch all other errors
        errorLabel.setText("Signup failed: " + ex.getMessage());
        ex.printStackTrace();
    }
}
}