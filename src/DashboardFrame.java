import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class DashboardFrame extends JFrame {

    private JPanel mainPanel;
    private int currentUserId;
    private HealthPanel cachedHealthPanel;
    private FinancePanel cachedFinancePanel;

    // We keep class-level references to the buttons so we can change their colors later
    private JButton timeBtn, healthBtn, financeBtn, profileBtn;

    public DashboardFrame(int userId) {
        this.currentUserId = userId;
        
        //   THE MAGIC LINE: Force Java to use your modern OS theme  
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("LifeOS Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //   SLEEK TOP NAV BAR  
        // Changed to match the Google Blue from your TimePanel
        Color primaryBlue = new Color(66, 133, 244); 
        
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(primaryBlue);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Generous padding

        // APP LOGO / TITLE
        JLabel appLogo = new JLabel("LifeOS");
        appLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appLogo.setForeground(Color.WHITE);
        appLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));

        // LEFT NAVIGATION (Modules)
        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftNav.setOpaque(false);
        leftNav.add(appLogo);

        timeBtn = createNavButton("Time");
        healthBtn = createNavButton("Health");
        financeBtn = createNavButton("Finance");
        profileBtn = createNavButton("Profile");

        leftNav.add(timeBtn);
        leftNav.add(healthBtn);
        leftNav.add(financeBtn);
        leftNav.add(profileBtn);

        // RIGHT NAVIGATION (Logout)
        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightNav.setOpaque(false);
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setForeground(primaryBlue); 
        logoutBtn.setBackground(Color.WHITE); 
        logoutBtn.setFocusPainted(false);
        logoutBtn.setOpaque(true);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        
        rightNav.add(logoutBtn);

        topBar.add(leftNav, BorderLayout.WEST);
        topBar.add(rightNav, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // MAIN CONTENT AREA
        mainPanel = new JPanel();
        mainPanel.setBackground(new Color(245, 247, 250)); // Soft off-white background
        add(mainPanel, BorderLayout.CENTER);

        // 🔁 BUTTON ACTIONS WITH ACTIVE STATES
        timeBtn.addActionListener(e -> { setActiveTab(timeBtn); showTime(); });
        healthBtn.addActionListener(e -> { setActiveTab(healthBtn); showHealth(); });
        financeBtn.addActionListener(e -> { setActiveTab(financeBtn); showFinance(); });
        profileBtn.addActionListener(e -> { setActiveTab(profileBtn); showProfile(); });
        
        // LOGOUT ACTION
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame();
            }
        });

        // Start on the Time module
        setActiveTab(timeBtn);
        showTime();

        setVisible(true);
    }

    //   HELPER TO CREATE CLEAN NAV BUTTONS  
    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(66, 133, 244)); // Default Blue
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    //   VISUAL FEEDBACK FOR ACTIVE TABS  
    private void setActiveTab(JButton activeBtn) {
        JButton[] allTabs = {timeBtn, healthBtn, financeBtn, profileBtn};
        
        for (JButton btn : allTabs) {
            if (btn == activeBtn) {
                // FIX: Pure white background with blue text for the active tab
                btn.setBackground(Color.WHITE); 
                btn.setForeground(new Color(66, 133, 244));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
            } else {
                // Inactive tabs stay solid blue
                btn.setBackground(new Color(66, 133, 244)); 
                btn.setForeground(Color.WHITE);
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            }
        }
    }

    // MODULE VIEWS

    private void showTime() { 
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(new TimePanel(currentUserId), BorderLayout.CENTER);
        refresh();
    }

    private void showHealth() {
    mainPanel.removeAll();
    mainPanel.setLayout(new BorderLayout()); 
    
    if (cachedHealthPanel == null) {
        cachedHealthPanel = new HealthPanel(currentUserId);
    }
    
    mainPanel.add(cachedHealthPanel, BorderLayout.CENTER);
    refresh();
    }

    private void showFinance() {
    mainPanel.removeAll();
    mainPanel.setLayout(new BorderLayout());
    
    if (cachedFinancePanel == null) {
        cachedFinancePanel = new FinancePanel(currentUserId);
    } else {
        cachedFinancePanel.refreshAllData();
    }
    
    mainPanel.add(cachedFinancePanel, BorderLayout.CENTER);
    refresh();
}

    private void showProfile() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(new ProfilePanel(currentUserId), BorderLayout.CENTER);
        refresh();
    }

    private void refresh() {
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}