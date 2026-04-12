import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DashboardFrame extends JFrame {

    private JPanel mainPanel;

    public DashboardFrame() {
        setTitle("LifeOS Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 🔝 TOP BAR
        JPanel topBar = new JPanel();
        topBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(new Color(100, 149, 237));

        JButton timeBtn = new JButton("Time");
        JButton healthBtn = new JButton("Health");
        JButton financeBtn = new JButton("Finance");

        styleTopButton(timeBtn);
        styleTopButton(healthBtn);
        styleTopButton(financeBtn);

        topBar.add(timeBtn);
        topBar.add(healthBtn);
        topBar.add(financeBtn);

        add(topBar, BorderLayout.NORTH);

        // 🧠 MAIN PANEL (CHANGES)
        mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);

        add(mainPanel, BorderLayout.CENTER);

        // 🔁 BUTTON ACTIONS
        timeBtn.addActionListener(e -> showTime());
        healthBtn.addActionListener(e -> showHealth());
        financeBtn.addActionListener(e -> showFinance());

        // open Time module by default
        showTime();

        setVisible(true);
    }

    private void styleTopButton(JButton btn) {
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(100, 149, 237));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
    }

    // MODULE VIEWS

    private void showTime() { 
    mainPanel.removeAll();
    mainPanel.setLayout(new BorderLayout());

    mainPanel.add(new TimePanel(), BorderLayout.CENTER);

    refresh();
    }

    private void showHealth() {
        mainPanel.removeAll();
        mainPanel.add(new JLabel("Health Module"));
        refresh();
    }

    private void showFinance() {
        mainPanel.removeAll();
        mainPanel.add(new JLabel("Finance Module"));
        refresh();
    }

    private void refresh() {
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}