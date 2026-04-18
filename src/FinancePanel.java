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
    private HorizontalStackedBarChart yearlyBarChart;
    private JSpinner yearSpinner;

    // Cache for transactions to avoid repeated DB queries
    private List<Transaction> cachedTransactions;
    private int lastFetchedYear = -1;
    private int lastFetchedMonth = -1;

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

        // Refresh data when tab changes, but reuse cached transactions if possible
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 0) refreshRecordsTab();
            else if (idx == 1) refreshAnalysisTab();
            else if (idx == 2) refreshAccountsList();
        });
    }

    // ---------- Helper to load transactions only once per month/year change ----------
    private void ensureTransactionsLoaded() {
        int currentYear = currentMonth.getYear();
        int currentMonthValue = currentMonth.getMonthValue();
        if (cachedTransactions == null || lastFetchedYear != currentYear || lastFetchedMonth != currentMonthValue) {
            cachedTransactions = db.getTransactions(userId);
            lastFetchedYear = currentYear;
            lastFetchedMonth = currentMonthValue;
        }
    }

    // ---------- Month Navigation ----------
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
            // Invalidate cache when month changes
            cachedTransactions = null;
            refreshAllData();
        });
        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateMonthLabel();
            cachedTransactions = null;
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
        ensureTransactionsLoaded();
        refreshRecordsTab();
        refreshAnalysisTab();
        refreshAccountsList();
    }

    // ---------- Records Tab ----------
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
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setPreferredSize(new Dimension(400, 50));
        addBtn.addActionListener(e -> {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new AddTransactionDialog(parentFrame, userId, () -> {
                // Invalidate cache after adding a transaction
                cachedTransactions = null;
                refreshAllData();
            }).setVisible(true);
        });

        panel.add(addBtn, BorderLayout.SOUTH);
        refreshRecordsTab();
        return panel;
    }

    private void refreshRecordsTab() {
        recordsListPanel.removeAll();
        ensureTransactionsLoaded();

        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        List<Transaction> monthTransactions = new ArrayList<>();
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : cachedTransactions) {
            LocalDate transDate = (t.transTime != null) ? t.transTime.toLocalDateTime().toLocalDate() : (t.transDate != null ? t.transDate.toLocalDate() : null);
            if (transDate != null && !transDate.isBefore(monthStart) && !transDate.isAfter(monthEnd)) {
                monthTransactions.add(t);
                if ("EXPENSE".equalsIgnoreCase(t.type)) totalExpense += t.amount;
                else if ("INCOME".equalsIgnoreCase(t.type)) totalIncome += t.amount;
            }
        }

        // Sort by date descending (newest first)
        monthTransactions.sort((a, b) -> {
            LocalDate da = (a.transTime != null) ? a.transTime.toLocalDateTime().toLocalDate() : a.transDate.toLocalDate();
            LocalDate db = (b.transTime != null) ? b.transTime.toLocalDateTime().toLocalDate() : b.transDate.toLocalDate();
            return db.compareTo(da);
        });

        for (Transaction t : monthTransactions) {
            recordsListPanel.add(createTransactionRow(t));
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
                if (db.deleteTransaction(t.id)) {
                    cachedTransactions = null; // invalidate cache
                    refreshAllData();
                }
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

    // ---------- Analysis Tab (Pie + Horizontal Stacked Bar) ----------
    private JPanel buildAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(WHITE);

        // Pie chart section
        JLabel pieTitle = new JLabel("Expense Breakdown");
        pieTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        pieTitle.setForeground(PRIMARY_BLUE);
        content.add(pieTitle);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        pieChartPanel = new PieChartPanel();
        pieChartPanel.setPreferredSize(new Dimension(600, 500)); // Larger, centered later
        pieChartPanel.setBackground(WHITE);
        content.add(pieChartPanel);
        content.add(Box.createRigidArea(new Dimension(0, 30)));

        // Horizontal stacked bar chart section
        JPanel yearlyHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        yearlyHeader.setBackground(WHITE);
        JLabel yearlyTitle = new JLabel("Monthly Expenses by Category");
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

        yearlyBarChart = new HorizontalStackedBarChart();
        yearlyBarChart.setPreferredSize(new Dimension(900, 450));
        yearlyBarChart.setBackground(WHITE);
        content.add(yearlyBarChart);

        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private void refreshAnalysisTab() {
        ensureTransactionsLoaded();
        if (pieChartPanel != null) {
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();
            Map<String, Double> categoryTotals = new HashMap<>();
            for (Transaction t : cachedTransactions) {
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
        if (yearlyBarChart != null && yearSpinner != null && cachedTransactions != null) {
            yearlyBarChart.setData(cachedTransactions, (Integer) yearSpinner.getValue());
        }
    }

    // ---------- IMPROVED PIE CHART (Larger, centered, readable labels) ----------
    // ---------- FIXED PIE CHART (Centered labels, correct math) ----------
    private class PieChartPanel extends JPanel {
        private Map<String, Double> data = new HashMap<>();
        private final Color[] COLORS = {
                new Color(66, 133, 244), new Color(219, 68, 55), new Color(244, 180, 0),
                new Color(15, 157, 88), new Color(171, 71, 188), new Color(255, 87, 34),
                new Color(0, 172, 193), new Color(158, 158, 158)
        };

        public void setData(Map<String, Double> data) { this.data = data; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("No data for this month", getWidth()/2 - 60, getHeight()/2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // layout math
            int padding = 60;
            int pieSize = Math.min(getWidth() - 250, getHeight() - padding * 2);
            int pieX = (getWidth() - 200 - pieSize) / 2 + 20; 
            int pieY = (getHeight() - pieSize) / 2;

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            double startAngle = 0.0;
            int colorIndex = 0;

            // 1. Draw the actual slices
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double angle = (entry.getValue() / total) * 360.0;
                g2.setColor(COLORS[colorIndex++ % COLORS.length]);
                g2.fill(new Arc2D.Double(pieX, pieY, pieSize, pieSize, startAngle, angle, Arc2D.PIE));
                startAngle += angle;
            }

            // 2. Draw labels inside the slices
            startAngle = 0.0;
            colorIndex = 0;
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            
            double radius = pieSize / 2.0;
            double centerX = pieX + radius;
            double centerY = pieY + radius;

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double angle = (entry.getValue() / total) * 360.0;
                
                // only draw labels for slices big enough to fit them ( > 4% )
                if (angle > 15) {
                    double midAngleDegrees = startAngle + (angle / 2.0);
                    double midAngleRad = Math.toRadians(midAngleDegrees);

                    // use radius * 0.65 to put the text inside the slice
                    int lx = (int) (centerX + (radius * 0.65) * Math.cos(midAngleRad));
                    int ly = (int) (centerY - (radius * 0.65) * Math.sin(midAngleRad));

                    String percent = String.format("%.1f%%", (entry.getValue() / total) * 100);
                    int tw = fm.stringWidth(percent);
                    int th = fm.getAscent();

                    // simple clean shadow for readability
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.drawString(percent, lx - tw/2 + 1, ly + th/2 + 1);
                    g2.setColor(Color.WHITE);
                    g2.drawString(percent, lx - tw/2, ly + th/2);
                }
                startAngle += angle;
            }

            // 3. Legend on the right
            int legendX = pieX + pieSize + 40;
            int legendY = pieY + 30;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            colorIndex = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                g2.setColor(COLORS[colorIndex % COLORS.length]);
                g2.fillRect(legendX, legendY, 14, 14);
                g2.setColor(Color.DARK_GRAY);
                String label = String.format("%s (₹%.0f)", entry.getKey(), entry.getValue());
                g2.drawString(label, legendX + 22, legendY + 12);
                legendY += 25;
                colorIndex++;
            }
        }
    }

    // ---------- HORIZONTAL STACKED BAR CHART (optimised, uses cached transactions) ----------
    private class HorizontalStackedBarChart extends JPanel {
        private Map<Integer, Map<String, Double>> monthlyCategoryData = new LinkedHashMap<>();
        private Map<String, Color> categoryColors = new HashMap<>();
        private double maxTotal = 0;
        private int selectedYear;

        public HorizontalStackedBarChart() {
            setBackground(WHITE);
        }

        public void setData(List<Transaction> transactions, int year) {
            this.selectedYear = year;
            monthlyCategoryData.clear();
            categoryColors.clear();

            // Prepare data: month -> category -> amount
            for (int m = 1; m <= 12; m++) {
                monthlyCategoryData.put(m, new HashMap<>());
            }

            for (Transaction t : transactions) {
                if (!"EXPENSE".equals(t.type)) continue;
                LocalDate d = (t.transTime != null) ? t.transTime.toLocalDateTime().toLocalDate() :
                        (t.transDate != null ? t.transDate.toLocalDate() : null);
                if (d != null && d.getYear() == year) {
                    int month = d.getMonthValue();
                    Map<String, Double> catMap = monthlyCategoryData.get(month);
                    catMap.merge(t.category, t.amount, Double::sum);
                }
            }

            // Assign consistent colors to categories
            Set<String> allCategories = new HashSet<>();
            for (Map<String, Double> catMap : monthlyCategoryData.values()) {
                allCategories.addAll(catMap.keySet());
            }
            Color[] preset = { new Color(66,133,244), new Color(219,68,55), new Color(244,180,0),
                    new Color(15,157,88), new Color(171,71,188), new Color(255,87,34),
                    new Color(0,172,193), new Color(158,158,158) };
            int idx = 0;
            for (String cat : allCategories) {
                categoryColors.put(cat, preset[idx % preset.length]);
                idx++;
            }

            // Find max total for scaling
            maxTotal = 0;
            for (int m = 1; m <= 12; m++) {
                double total = monthlyCategoryData.get(m).values().stream().mapToDouble(Double::doubleValue).sum();
                if (total > maxTotal) maxTotal = total;
            }
            if (maxTotal <= 0) maxTotal = 1.0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int paddingLeft = 70;
            int paddingRight = 180;
            int paddingTop = 40;
            int paddingBottom = 30;
            int chartWidth = width - paddingLeft - paddingRight;
            int barHeight = (height - paddingTop - paddingBottom) / 12;
            int barSpacing = 5;

            // Title
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            String title = "Monthly Expenses by Category – " + selectedYear;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, (width - fm.stringWidth(title)) / 2, paddingTop - 10);

            // Y-axis labels (months)
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            fm = g2.getFontMetrics();
            DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM");
            for (int month = 1; month <= 12; month++) {
                int y = paddingTop + (month-1) * (barHeight + barSpacing) + barHeight/2 + 5;
                String monthName = YearMonth.of(selectedYear, month).format(monthFormat);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(monthName, paddingLeft - fm.stringWidth(monthName) - 10, y);
            }

            // Draw bars
            for (int month = 1; month <= 12; month++) {
                int y = paddingTop + (month-1) * (barHeight + barSpacing);
                Map<String, Double> catMap = monthlyCategoryData.get(month);
                double total = catMap.values().stream().mapToDouble(Double::doubleValue).sum();
                double x = paddingLeft;
                for (Map.Entry<String, Double> entry : catMap.entrySet()) {
                    double amount = entry.getValue();
                    int segmentWidth = (int)((amount / maxTotal) * chartWidth);
                    if (segmentWidth < 1) continue;
                    g2.setColor(categoryColors.get(entry.getKey()));
                    g2.fillRect((int)x, y, segmentWidth, barHeight);
                    // Label inside segment if wide enough
                    if (segmentWidth > 40) {
                        double percent = (amount / total) * 100;
                        String label = String.format("%s %.0f%%", entry.getKey(), percent);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        g2.drawString(label, (int)x + 5, y + barHeight - 5);
                    } else if (segmentWidth > 20) {
                        double percent = (amount / total) * 100;
                        String pct = String.format("%.0f%%", percent);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        g2.drawString(pct, (int)x + 3, y + barHeight - 5);
                    }
                    x += segmentWidth;
                }
                // Total value at end of bar
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString(String.format("₹%.0f", total), (int)x + 5, y + barHeight - 5);
            }

            // Legend on the right
            int legendX = width - paddingRight + 10;
            int legendY = paddingTop + 20;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (Map.Entry<String, Color> entry : categoryColors.entrySet()) {
                g2.setColor(entry.getValue());
                g2.fillRect(legendX, legendY, 15, 15);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(entry.getKey(), legendX + 20, legendY + 12);
                legendY += 25;
            }
            g2.dispose();
        }
    }

    // ---------- Accounts Tab (with Delete button) ----------
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
        addAccountBtn.setOpaque(true);
        addAccountBtn.setBorderPainted(false);
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
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

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

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setForeground(EXPENSE_RED);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.addActionListener(e -> deleteAccount(acc));

        right.add(balanceLabel);
        right.add(editBtn);
        right.add(deleteBtn);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private void deleteAccount(Account acc) {
        // Check if account has any transactions (expense, income, or transfer)
        int count = db.getTransactionCountForAccount(acc.id);
        if (count > 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete account '" + acc.name + "' because it has " + count + " transactions.\nPlease delete or move all transactions first.",
                    "Account in use", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete account '" + acc.name + "'? This action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (accountDAO.deleteAccount(acc.id)) {
                refreshAccountsList();
                // Also invalidate transaction cache because any linked transactions would have been prevented anyway
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete account.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openAccountDialog(Account existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Account" : "Add Account", true);
        dialog.setSize(400, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.weightx = 1.0;

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Name:"), gbc);
        JTextField nameField = new JTextField(isEdit ? existing.name : "");
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(nameField, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Type:"), gbc);
        String[] types = {"Cash", "Bank", "Credit Card", "Savings", "Investment", "Other"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        if (isEdit) typeBox.setSelectedItem(existing.type.replace('_', ' '));
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(typeBox, gbc);

        gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Initial Balance:"), gbc);
        JSpinner balanceSpinner = new JSpinner(new SpinnerNumberModel(
                isEdit ? existing.balance : 0.0, -1000000.0, 1000000.0, 100.0));
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(balanceSpinner, gbc);

        gbc.gridy = 3; gbc.gridx = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Monthly Budget:"), gbc);
        JSpinner budgetSpinner = new JSpinner(new SpinnerNumberModel(
                isEdit ? existing.budget : 0.0, 0.0, 1000000.0, 100.0));
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(budgetSpinner, gbc);

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
            double bud = ((Number) budgetSpinner.getValue()).doubleValue();

            boolean success;
            if (isEdit) {
                success = accountDAO.updateAccount(existing.id, name, type, bud);
                if (success && Math.abs(bal - existing.balance) > 0.01) {
                    accountDAO.updateBalance(existing.id, bal - existing.balance, bal > existing.balance);
                }
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