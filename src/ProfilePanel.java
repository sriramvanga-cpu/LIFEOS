import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;

public class ProfilePanel extends JPanel {

    private int userId;
    private UserDAO dao = new UserDAO();
    private Map<String, String> userDetails;

    public ProfilePanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout());
        setBackground(new Color(250, 251, 253));
        
        loadProfileData();
    }

    private void loadProfileData() {
        removeAll();
        userDetails = dao.getUserDetails(userId);

        // Header
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);
        topSection.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(66, 133, 244));
        topSection.add(title, BorderLayout.WEST);
        
        add(topSection, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true),
            BorderFactory.createEmptyBorder(40, 50, 40, 50)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addInfoRow(card, gbc, 0, "Name:", userDetails.getOrDefault("name", "N/A"));
        addInfoRow(card, gbc, 1, "Email:", userDetails.getOrDefault("email", "N/A"));
        addInfoRow(card, gbc, 2, "Date of Birth:", userDetails.getOrDefault("dob", "N/A"));
        addInfoRow(card, gbc, 3, "Height (cm):", userDetails.getOrDefault("height", "170.0"));

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JButton editBtn = createButton("Edit Profile", new Color(66, 133, 244), Color.WHITE);
        editBtn.addActionListener(e -> openEditDialog());
        
        JButton passBtn = createButton("Change Password", Color.WHITE, new Color(100, 100, 100));
        passBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        passBtn.addActionListener(e -> openPasswordDialog());

        buttonPanel.add(editBtn);
        buttonPanel.add(passBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(buttonPanel, gbc);

        centerPanel.add(card);
        add(centerPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void addInfoRow(JPanel card, GridBagConstraints gbc, int row, String labelStr, String valStr) {
        gbc.gridy = row;
        
        gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel label = new JLabel(labelStr);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(Color.GRAY);
        card.add(label, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JLabel val = new JLabel(valStr);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        card.add(val, gbc);
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(bg.equals(Color.WHITE));
        btn.setPreferredSize(new Dimension(160, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void openEditDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog((Frame) parent, "Edit Profile", true);
        dialog.setSize(400, 400); // Made slightly taller for the error label
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(userDetails.get("name"));
        JTextField emailField = new JTextField(userDetails.get("email"));
        
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(Double.parseDouble(userDetails.get("height")), 50.0, 300.0, 0.5));
        
        SpinnerDateModel dobModel = new SpinnerDateModel();
        JSpinner dobSpinner = new JSpinner(dobModel);
        dobSpinner.setEditor(new JSpinner.DateEditor(dobSpinner, "yyyy-MM-dd"));
        try {
            dobSpinner.setValue(java.sql.Date.valueOf(userDetails.get("dob")));
        } catch (Exception e) { /* Ignore parsing errors for empty DBs */ }

        gbc.gridy=0; gbc.gridx=0; form.add(new JLabel("Name:"), gbc); gbc.gridx=1; form.add(nameField, gbc);
        gbc.gridy=1; gbc.gridx=0; form.add(new JLabel("Email:"), gbc); gbc.gridx=1; form.add(emailField, gbc);
        gbc.gridy=2; gbc.gridx=0; form.add(new JLabel("DOB:"), gbc); gbc.gridx=1; form.add(dobSpinner, gbc);
        gbc.gridy=3; gbc.gridx=0; form.add(new JLabel("Height (cm):"), gbc); gbc.gridx=1; form.add(heightSpinner, gbc);

        // INLINE ERROR LABEL  
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy=4; gbc.gridx=0; gbc.gridwidth=2; 
        gbc.insets = new Insets(0, 10, 0, 10);
        form.add(errorLabel, gbc);

        JButton saveBtn = createButton("Save Changes", new Color(52, 168, 83), Color.WHITE);
        saveBtn.addActionListener(e -> {
            errorLabel.setText(" ");
            
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            Date dobDate = (Date) dobSpinner.getValue();
            double height = (Double) heightSpinner.getValue();

            if (name.isEmpty() || email.isEmpty()) {
                errorLabel.setForeground(Color.RED);
                errorLabel.setText("Fields cannot be empty.");
                return;
            }

            boolean success = dao.updateUserDetails(userId, name, email, new java.sql.Date(dobDate.getTime()), height);
            if (success) {
                errorLabel.setForeground(new Color(52, 168, 83));
                errorLabel.setText("Profile Updated Successfully!");
                
                // Close automatically after 1 second
                Timer timer = new Timer(1000, evt -> {
                    dialog.dispose();
                    loadProfileData(); 
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                errorLabel.setForeground(Color.RED);
                errorLabel.setText("Failed to update profile.");
            }
        });

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER)); 
        foot.setBackground(Color.WHITE); 
        foot.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        foot.add(saveBtn);
        
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(foot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void openPasswordDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog((Frame) parent, "Change Password", true);
        
        dialog.setSize(450, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPasswordField currentPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();

        Dimension fieldSize = new Dimension(200, 35);
        currentPass.setPreferredSize(fieldSize);
        newPass.setPreferredSize(fieldSize);
        confirmPass.setPreferredSize(fieldSize);

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel curLbl = new JLabel("Current Password:");
        curLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        form.add(curLbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; 
        form.add(currentPass, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel newLbl = new JLabel("New Password:");
        newLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        form.add(newLbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; 
        form.add(newPass, gbc);

        gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0.3;
        JLabel confLbl = new JLabel("Confirm New:");
        confLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        form.add(confLbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; 
        form.add(confirmPass, gbc);

        // INLINE ERROR LABEL  
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 0, 10);
        form.add(errorLabel, gbc);

        JButton saveBtn = createButton("Update Password", new Color(234, 67, 53), Color.WHITE);
        saveBtn.addActionListener(e -> {
            errorLabel.setText(" ");
            
            String curr = new String(currentPass.getPassword());
            String n1 = new String(newPass.getPassword());
            String n2 = new String(confirmPass.getPassword());

            if (curr.isEmpty() || n1.isEmpty() || n2.isEmpty()) {
                errorLabel.setForeground(Color.RED);
                errorLabel.setText("All fields are required.");
                return;
            }
            if (!n1.equals(n2)) {
                errorLabel.setForeground(Color.RED);
                errorLabel.setText("New passwords do not match.");
                return;
            }

            boolean success = dao.updatePassword(userId, curr, n1);
            if (success) {
                errorLabel.setForeground(new Color(52, 168, 83));
                errorLabel.setText("Password changed securely!");
                
                // Close automatically after 1 second
                Timer timer = new Timer(1000, evt -> dialog.dispose());
                timer.setRepeats(false);
                timer.start();
            } else {
                errorLabel.setForeground(Color.RED);
                errorLabel.setText("Incorrect current password.");
            }
        });

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER)); 
        foot.setBackground(Color.WHITE); 
        foot.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        foot.add(saveBtn);
        
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(foot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}