import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.sql.Timestamp;

public class FinancePanel extends JPanel {
    private final Color PRIMARY_BLUE = new Color(66, 133, 244);
    private final Color LIGHT_BLUE = new Color(232, 240, 255);
    private final Color WHITE = Color.WHITE;
    private final Color EXPENSE_RED = new Color(220, 53, 69);
    private final Color INCOME_GREEN = new Color(40, 167, 69);
    private final Color TRANSFER_PURPLE = new Color(111, 66, 193);

    private int userId;
    private FinanceDAO db;
    private AccountDAO accountDAO;

    private YearMonth currentMonth = YearMonth.now();
    private JLabel monthLabel;

    private JPanel recordsListPanel;
    private JLabel expenseVal, incomeVal, totalVal;
    private JPanel accountsListPanel;
    private JTabbedPane tabbedPane;
    private PieChartPanel pieChartPanel;
    private YearlyExpenseBarChart yearlyBarChart;
    private JSpinner yearSpinner;

    public FinancePanel(int userId) {
        this.userId = userId;
        this.db = new FinanceDAO();
        this.accountDAO = new AccountDAO();
        setLayout(new BorderLayout());
        setBackground(WHITE);

        JPanel monthBar = buildMonthNavigationBar();
        add(monthBar, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(WHITE);
        tabbedPane.setFocusable(false);

        tabbedPane.addTab(" Records ", buildRecordsPanel());
        tabbedPane.addTab(" Analysis ", buildAnalysisPanel());
        tabbedPane.addTab(" Accounts ", buildAccountsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) {
                refreshRecordsTab();
            } else if (tabbedPane.getSelectedIndex() == 1) {
                refreshAnalysisTab();
            } else if (tabbedPane.getSelectedIndex() == 2) {
                refreshAccountsList();
            }
        });
    }

    private JPanel buildMonthNavigationBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(WHITE);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");
        styleNavButton(prevBtn);
        styleNavButton(nextBtn);

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        updateMonthLabel();

        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateMonthLabel();
            refreshAllData();
        });
        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateMonthLabel();
            refreshAllData();
        });

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        center.setBackground(WHITE);
        center.add(prevBtn);
        center.add(monthLabel);
        center.add(nextBtn);

        bar.add(center, BorderLayout.CENTER);
        return bar;
    }

    private void styleNavButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(245, 245, 245));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void updateMonthLabel() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    }

    public void refreshAllData() {
        refreshRecordsTab();
        refreshAnalysisTab();
        refreshAccountsList();
    }

    private JPanel buildRecordsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);

        JPanel header = new JPanel(new GridLayout(1, 3));
        header.setBackground(PRIMARY_BLUE);
        header.setPreferredSize(new Dimension(400, 70));

        expenseVal = new JLabel("₹0.00", SwingConstants.CENTER);
        incomeVal = new JLabel("₹0.00", SwingConstants.CENTER);
        totalVal = new JLabel("₹0.00", SwingConstants.CENTER);

        header.add(createStatItem("EXPENSE", expenseVal));
        header.add(createStatItem("INCOME", incomeVal));
        header.add(createStatItem("TOTAL", totalVal));
        panel.add(header, BorderLayout.NORTH);

        recordsListPanel = new JPanel();
        recordsListPanel.setLayout(new BoxLayout(recordsListPanel, BoxLayout.Y_AXIS));
        recordsListPanel.setBackground(WHITE);

        JScrollPane scrollPane = new JScrollPane(recordsListPanel);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton addBtn = new JButton("+ Add Record");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addBtn.setBackground(PRIMARY_BLUE);
        addBtn.setForeground(WHITE);
        
        //   MAC COMPATIBILITY  
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        //         -

        addBtn.setFocusPainted(false);
        addBtn.setPreferredSize(new Dimension(400, 50));
        addBtn.addActionListener(e -> {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new AddTransactionDialog(parentFrame, userId, () -> {
                refreshRecordsTab();
                refreshAccountsList();
                refreshAnalysisTab();
            }).setVisible(true);
        });

        panel.add(addBtn, BorderLayout.SOUTH);
        refreshRecordsTab();
        return panel;
    }

    private void refreshRecordsTab() {
        recordsListPanel.removeAll();
        List<Transaction> transactions = db.getTransactions(userId);

        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        List<Transaction> monthTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            LocalDate transDate = (t.transTime != null) ? t.transTime.toLocalDateTime().toLocalDate() : (t.transDate != null ? t.transDate.toLocalDate() : null);
            if (transDate != null && !transDate.isBefore(monthStart) && !transDate.isAfter(monthEnd)) {
                monthTransactions.add(t);
            }
        }

        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : monthTransactions) {
            recordsListPanel.add(createTransactionRow(t));
            if ("EXPENSE".equalsIgnoreCase(t.type)) totalExpense += t.amount;
            else if ("INCOME".equalsIgnoreCase(t.type)) totalIncome += t.amount;
        }

        expenseVal.setText("₹" + String.format("%.2f", totalExpense));
        incomeVal.setText("₹" + String.format("%.2f", totalIncome));
        totalVal.setText("₹" + String.format("%.2f", (totalIncome - totalExpense)));

        recordsListPanel.revalidate();
        recordsListPanel.repaint();
    }

    private JPanel createStatItem(String title, JLabel valueLabel) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setOpaque(false);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setForeground(LIGHT_BLUE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 10));
        valueLabel.setForeground(WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        p.add(t);
        p.add(valueLabel);
        return p;
    }

    private JPanel createTransactionRow(Transaction t) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(WHITE);
        row.setMaximumSize(new Dimension(2000, 55));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_BLUE),
                new EmptyBorder(6, 15, 6, 15)));

        String accountName = getAccountName(t.accountId);
        String toAccountName = t.toAccountId != null ? getAccountName(t.toAccountId) : "";

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        JLabel catLabel = new JLabel(t.category);
        catLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        leftPanel.add(catLabel);

        String detailText = "TRANSFER".equals(t.type) ? accountName + " → " + toAccountName : accountName;
        if (t.notes != null && !t.notes.isEmpty()) detailText += " • " + t.notes;
        JLabel detailLabel = new JLabel(detailText);
        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailLabel.setForeground(Color.GRAY);
        leftPanel.add(detailLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        String amountStr;
        Color amountColor;
        switch (t.type) {
            case "EXPENSE": amountStr = "-₹" + String.format("%.2f", t.amount); amountColor = EXPENSE_RED; break;
            case "INCOME": amountStr = "+₹" + String.format("%.2f", t.amount); amountColor = INCOME_GREEN; break;
            default: amountStr = "↻₹" + String.format("%.2f", t.amount); amountColor = TRANSFER_PURPLE; break;
        }

        JLabel val = new JLabel(amountStr);
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(amountColor);

        JButton deleteBtn = new JButton("✕");
        deleteBtn.setForeground(Color.RED);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Delete transaction?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (db.deleteTransaction(t.id)) { refreshRecordsTab(); refreshAccountsList(); refreshAnalysisTab(); }
            }
        });

        rightPanel.add(val);
        rightPanel.add(deleteBtn);
        row.add(leftPanel, BorderLayout.WEST);
        row.add(rightPanel, BorderLayout.EAST);
        return row;
    }

    private String getAccountName(int accountId) {
        List<Account> accounts = accountDAO.getAccounts(userId);
        for (Account acc : accounts) if (acc.id == accountId) return acc.name;
        return "Unknown";
    }

    private JPanel buildAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(WHITE);

        JLabel title = new JLabel("Expense Breakdown");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(PRIMARY_BLUE);
        content.add(title);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        pieChartPanel = new PieChartPanel();
        pieChartPanel.setPreferredSize(new Dimension(500, 400));
        pieChartPanel.setBackground(WHITE);
        content.add(pieChartPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel yearlyHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        yearlyHeader.setBackground(WHITE);
        JLabel yearlyTitle = new JLabel("Yearly Trend");
        yearlyTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        yearlyTitle.setForeground(PRIMARY_BLUE);
        yearlyHeader.add(yearlyTitle);
        yearlyHeader.add(Box.createRigidArea(new Dimension(20, 0)));

        yearSpinner = new JSpinner(new SpinnerNumberModel(YearMonth.now().getYear(), 2000, 2100, 1));
        yearSpinner.addChangeListener(e -> refreshYearlyChart());
        yearlyHeader.add(new JLabel("Year: "));
        yearlyHeader.add(yearSpinner);
        content.add(yearlyHeader);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        yearlyBarChart = new YearlyExpenseBarChart();
        content.add(yearlyBarChart);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private void refreshAnalysisTab() {
        if (pieChartPanel != null) {
            List<Transaction> all = db.getTransactions(userId);
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();
            Map<String, Double> categoryTotals = new HashMap<>();
            for (Transaction t : all) {
                if (!"EXPENSE".equals(t.type)) continue;
                LocalDate d = (t.transTime != null) ? t.transTime.toLocalDateTime().toLocalDate() : (t.transDate != null ? t.transDate.toLocalDate() : null);
                if (d != null && !d.isBefore(monthStart) && !d.isAfter(monthEnd)) {
                    categoryTotals.merge(t.category, t.amount, Double::sum);
                }
            }
            pieChartPanel.setData(categoryTotals);
        }
        refreshYearlyChart();
    }

    private void refreshYearlyChart() {
        if (yearlyBarChart != null && yearSpinner != null) {
            yearlyBarChart.setData(db.getTransactions(userId), (Integer) yearSpinner.getValue());
        }
    }

    private class PieChartPanel extends JPanel {
        private Map<String, Double> data = new HashMap<>();
        private final Color[] COLORS = { new Color(66, 133, 244), new Color(219, 68, 55), new Color(244, 180, 0), new Color(15, 157, 88), new Color(171, 71, 188) };
        public void setData(Map<String, Double> data) { this.data = data; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 100;
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            double total = data.values().stream().mapToDouble(Double::doubleValue).sum(), startAngle = 0.0;
            int colorIndex = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double angle = (entry.getValue() / total) * 360.0;
                g2.setColor(COLORS[colorIndex++ % COLORS.length]);
                g2.fill(new Arc2D.Double(x, y, size, size, startAngle, angle, Arc2D.PIE));
                startAngle += angle;
            }
        }
    }

    private JPanel buildAccountsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(new EmptyBorder(20, 20, 10, 20));
        JLabel title = new JLabel("Your Accounts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(PRIMARY_BLUE);
        header.add(title, BorderLayout.WEST);

        JButton addAccountBtn = new JButton("+ Add Account");
        addAccountBtn.setBackground(PRIMARY_BLUE);
        addAccountBtn.setForeground(WHITE);
        
        //   MAC COMPATIBILITY  
        addAccountBtn.setOpaque(true);
        addAccountBtn.setBorderPainted(false);
        //         -

        addAccountBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addAccountBtn.addActionListener(e -> openAccountDialog(null));
        header.add(addAccountBtn, BorderLayout.EAST);

        accountsListPanel = new JPanel();
        accountsListPanel.setLayout(new BoxLayout(accountsListPanel, BoxLayout.Y_AXIS));
        accountsListPanel.setBackground(WHITE);

        JScrollPane scrollPane = new JScrollPane(accountsListPanel);
        scrollPane.setBorder(null);
        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        refreshAccountsList();
        return panel;
    }

    private void refreshAccountsList() {
        accountsListPanel.removeAll();
        List<Account> accounts = accountDAO.getAccounts(userId);
        for (Account acc : accounts) {
            accountsListPanel.add(createAccountCard(acc));
            accountsListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        accountsListPanel.revalidate();
        accountsListPanel.repaint();
    }

    private JPanel createAccountCard(Account acc) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(15, 20, 15, 20)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120)); // Taller for progress bar

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel nameLabel = new JLabel(acc.name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel typeLabel = new JLabel(acc.type.replace('_', ' '));
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        typeLabel.setForeground(Color.GRAY);
        left.add(nameLabel);
        left.add(typeLabel);

        //   BUDGETING PROGRESS BAR  
        // This assumes you added the 'budget' field to the Account class
        if (acc.budget > 0) {
            left.add(Box.createRigidArea(new Dimension(0, 10)));
            JProgressBar budgetBar = new JProgressBar(0, (int)acc.budget);
            budgetBar.setValue((int)Math.abs(acc.balance));
            budgetBar.setForeground(acc.balance > acc.budget ? EXPENSE_RED : INCOME_GREEN);
            budgetBar.setPreferredSize(new Dimension(150, 8));
            
            JLabel budgetLabel = new JLabel(String.format("Budget: ₹%.2f / ₹%.2f", Math.abs(acc.balance), acc.budget));
            budgetLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            left.add(budgetBar);
            left.add(budgetLabel);
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setOpaque(false);
        JLabel balanceLabel = new JLabel("₹" + String.format("%.2f", acc.balance));
        balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        balanceLabel.setForeground(acc.balance < 0 ? EXPENSE_RED : PRIMARY_BLUE);

        JButton editBtn = new JButton("Edit");
        editBtn.setForeground(PRIMARY_BLUE);
        editBtn.setContentAreaFilled(false);
        editBtn.addActionListener(e -> openAccountDialog(acc));

        right.add(balanceLabel);
        right.add(editBtn);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private void openAccountDialog(Account existing) {
    boolean isEdit = existing != null;
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            isEdit ? "Edit Account" : "Add Account", true);
    dialog.setSize(400, 380); // Increased height for the budget field
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout());

    JPanel form = new JPanel(new GridBagLayout());
    form.setBackground(WHITE);
    form.setBorder(new EmptyBorder(20, 20, 20, 20));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(8, 5, 8, 5);
    gbc.weightx = 1.0;

    //   Name Field  
    gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.3;
    form.add(new JLabel("Name:"), gbc);
    JTextField nameField = new JTextField(isEdit ? existing.name : "");
    gbc.gridx = 1; gbc.weightx = 0.7;
    form.add(nameField, gbc);

    //   Type Field  
    gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0.3;
    form.add(new JLabel("Type:"), gbc);
    String[] types = {"Cash", "Bank", "Credit Card", "Savings", "Investment", "Other"};
    JComboBox<String> typeBox = new JComboBox<>(types);
    if (isEdit) {
        typeBox.setSelectedItem(existing.type.replace('_', ' '));
    }
    gbc.gridx = 1; gbc.weightx = 0.7;
    form.add(typeBox, gbc);

    //   Balance Field  
    gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0.3;
    form.add(new JLabel("Initial Balance:"), gbc);
    JSpinner balanceSpinner = new JSpinner(new SpinnerNumberModel(
            isEdit ? existing.balance : 0.0, -1000000.0, 1000000.0, 100.0));
    gbc.gridx = 1; gbc.weightx = 0.7;
    form.add(balanceSpinner, gbc);

    //   NEW: BUDGET FIELD  
    gbc.gridy = 3; gbc.gridx = 0; gbc.weightx = 0.3;
    form.add(new JLabel("Monthly Budget:"), gbc);
    JSpinner budgetSpinner = new JSpinner(new SpinnerNumberModel(
            isEdit ? existing.budget : 0.0, 0.0, 1000000.0, 100.0));
    gbc.gridx = 1; gbc.weightx = 0.7;
    form.add(budgetSpinner, gbc);

    //   Action Buttons  
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.setBackground(WHITE);
    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(e -> dialog.dispose());
    
    JButton save = new JButton(isEdit ? "Save" : "Add");
    save.setBackground(PRIMARY_BLUE);
    save.setForeground(WHITE);
    save.setOpaque(true); 
    save.setBorderPainted(false);
    
    save.addActionListener(e -> {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Name is required.");
            return;
        }
        
        String type = ((String) typeBox.getSelectedItem()).toUpperCase().replace(' ', '_');
        double bal = ((Number) balanceSpinner.getValue()).doubleValue();
        double bud = ((Number) budgetSpinner.getValue()).doubleValue(); // Get the budget value

        boolean success;
        // UPDATED: Now passing 'bud' as the required extra argument
        if (isEdit) {
            success = accountDAO.updateAccount(existing.id, name, type, bud);
        } else {
            success = accountDAO.addAccount(userId, name, type, bal, bud);
        }

        if (success) {
            refreshAccountsList();
            dialog.dispose();
        } else {
            JOptionPane.showMessageDialog(dialog, "Operation failed.");
        }
    });
    
    buttons.add(cancel);
    buttons.add(save);

    dialog.add(form, BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);
    dialog.setVisible(true);
}
}