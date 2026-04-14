import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Calendar;
import java.util.Date;

public class AddTransactionDialog extends JDialog {

    private final Color PRIMARY_BLUE = new Color(66, 133, 244);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);
    private final Color DARK_GRAY = Color.DARK_GRAY;
    private final Color INCOME_GREEN = new Color(40, 167, 69);
    private final Color EXPENSE_RED = new Color(220, 53, 69);
    private final Color TRANSFER_PURPLE = new Color(111, 66, 193);

    private int userId;
    private Runnable onSuccess;
    private List<Account> accounts;

    private String currentMode = "EXPENSE";
    private JLabel displayLabel;
    private JComboBox<Account> fromAccountBox, toAccountBox;
    private JComboBox<String> categoryBox;
    private JTextField notesField;
    private JButton expenseBtn, incomeBtn, transferBtn;
    private JSpinner dateSpinner, timeSpinner;
    private JLabel dateTimeLabel;
    private JPanel toAccountPanel;  // <-- field to control visibility

    private double currentTotal = 0;
    private String currentInput = "0";
    private String lastOperator = "";
    private boolean isNewInput = true;

    public AddTransactionDialog(JFrame parent, int userId, Runnable onSuccess) {
        super(parent, "Add Transaction", true);
        this.userId = userId;
        this.onSuccess = onSuccess;
        this.accounts = new AccountDAO().getAccounts(userId);

        if (accounts.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Please add at least one account in the Accounts tab first.",
                    "No Accounts", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        setSize(400, 720);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(WHITE);

        JPanel modePanel = new JPanel(new GridLayout(1, 3, 0, 0));
        modePanel.setBackground(WHITE);
        modePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        expenseBtn = createModeButton("EXPENSE", EXPENSE_RED);
        incomeBtn = createModeButton("INCOME", INCOME_GREEN);
        transferBtn = createModeButton("TRANSFER", TRANSFER_PURPLE);

        modePanel.add(expenseBtn);
        modePanel.add(incomeBtn);
        modePanel.add(transferBtn);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(WHITE);
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fromAccountPanel = createLabeledComponent("Account", false);
        fromAccountBox = new JComboBox<>(accounts.toArray(new Account[0]));
        fromAccountBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        fromAccountBox.setBackground(WHITE);
        fromAccountPanel.add(fromAccountBox, BorderLayout.CENTER);
        formPanel.add(fromAccountPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Create and store toAccountPanel
        toAccountPanel = createLabeledComponent("To Account", false);
        toAccountBox = new JComboBox<>(accounts.toArray(new Account[0]));
        toAccountBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        toAccountBox.setBackground(WHITE);
        toAccountPanel.add(toAccountBox, BorderLayout.CENTER);
        toAccountPanel.setVisible(false);
        formPanel.add(toAccountPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel categoryPanel = createLabeledComponent("Category", false);
        String[] categories = {"Food & Dining", "Transport", "Shopping", "Salary", "Bills", "Entertainment", "Other"};
        categoryBox = new JComboBox<>(categories);
        categoryBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        categoryBox.setBackground(WHITE);
        categoryPanel.add(categoryBox, BorderLayout.CENTER);
        formPanel.add(categoryPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel notesPanel = createLabeledComponent("Add notes", true);
        notesField = new JTextField();
        notesField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        notesField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        notesPanel.add(notesField, BorderLayout.CENTER);
        formPanel.add(notesPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        displayLabel = new JLabel("0", SwingConstants.RIGHT);
        displayLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        displayLabel.setForeground(PRIMARY_BLUE);
        displayLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        displayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(displayLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel keypadPanel = new JPanel(new GridLayout(4, 4, 8, 8));
        keypadPanel.setBackground(WHITE);
        keypadPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        String[] buttons = {
                "+", "7", "8", "9",
                "-", "4", "5", "6",
                "×", "1", "2", "3",
                "÷", "0", ".", "="
        };

        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 20));
            btn.setFocusPainted(false);
            btn.setBackground(WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            if (text.matches("[\\+\\-×÷=]")) {
                btn.setForeground(PRIMARY_BLUE);
            }

            btn.addActionListener(e -> handleKeypad(text));
            keypadPanel.add(btn);
        }
        formPanel.add(keypadPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        dateTimePanel.setBackground(WHITE);

        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MMM dd, yyyy"));
        dateSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateTimePanel.add(dateSpinner);

        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "hh:mm a"));
        timeSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateTimePanel.add(timeSpinner);

        dateTimeLabel = new JLabel();
        dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateTimeLabel.setForeground(Color.GRAY);
        dateTimePanel.add(dateTimeLabel);
        formPanel.add(dateTimePanel);

        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBackground(WHITE);
        actionPanel.setBorder(new EmptyBorder(10, 15, 15, 15));

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setForeground(Color.GRAY);
        cancelBtn.setBackground(WHITE);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("SAVE");
        saveBtn.setBackground(PRIMARY_BLUE);
        saveBtn.setForeground(WHITE);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        saveBtn.setPreferredSize(new Dimension(100, 40));
        saveBtn.addActionListener(e -> saveTransaction());

        actionPanel.add(cancelBtn, BorderLayout.WEST);
        actionPanel.add(saveBtn, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(modePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        setActiveMode("EXPENSE");
        updateDateTimeLabel();
    }

    private JPanel createLabeledComponent(String labelText, boolean isNote) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(WHITE);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(Color.DARK_GRAY);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private JButton createModeButton(String text, Color activeColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(WHITE);
        btn.setForeground(Color.GRAY);
        btn.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, WHITE));
        btn.addActionListener(e -> setActiveMode(text));
        return btn;
    }

    private void setActiveMode(String mode) {
        this.currentMode = mode;
        expenseBtn.setBackground(WHITE);
        incomeBtn.setBackground(WHITE);
        transferBtn.setBackground(WHITE);
        expenseBtn.setForeground(Color.GRAY);
        incomeBtn.setForeground(Color.GRAY);
        transferBtn.setForeground(Color.GRAY);
        expenseBtn.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, WHITE));
        incomeBtn.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, WHITE));
        transferBtn.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, WHITE));

        Color activeColor;
        JButton activeBtn;
        switch (mode) {
            case "INCOME":
                activeColor = INCOME_GREEN;
                activeBtn = incomeBtn;
                break;
            case "TRANSFER":
                activeColor = TRANSFER_PURPLE;
                activeBtn = transferBtn;
                break;
            default:
                activeColor = EXPENSE_RED;
                activeBtn = expenseBtn;
                break;
        }
        activeBtn.setForeground(activeColor);
        activeBtn.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, activeColor));

        toAccountPanel.setVisible("TRANSFER".equals(mode));
        // Revalidate the form panel's container
        if (toAccountPanel.getParent() instanceof JComponent) {
            ((JComponent) toAccountPanel.getParent()).revalidate();
        }
    }

    private void handleKeypad(String key) {
        // ... unchanged ...
        switch (key) {
            case "+":
            case "-":
            case "×":
            case "÷":
                calculateStep();
                lastOperator = key;
                isNewInput = true;
                displayLabel.setText(formatNumber(currentTotal));
                break;
            case "=":
                calculateStep();
                lastOperator = "";
                isNewInput = true;
                displayLabel.setText(formatNumber(currentTotal));
                break;
            case ".":
                if (!currentInput.contains(".")) {
                    currentInput += ".";
                    isNewInput = false;
                    displayLabel.setText(currentInput);
                }
                break;
            default:
                if (isNewInput) {
                    currentInput = key;
                    isNewInput = false;
                } else {
                    currentInput += key;
                }
                displayLabel.setText(currentInput);
        }
    }

    private void calculateStep() {
        double inputVal = 0;
        try {
            inputVal = Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {}
        if (lastOperator.isEmpty()) {
            currentTotal = inputVal;
        } else {
            switch (lastOperator) {
                case "+": currentTotal += inputVal; break;
                case "-": currentTotal -= inputVal; break;
                case "×": currentTotal *= inputVal; break;
                case "÷": if (inputVal != 0) currentTotal /= inputVal; break;
            }
        }
        currentInput = String.valueOf(currentTotal);
    }

    private String formatNumber(double num) {
        if (num == (long) num) return String.format("%d", (long) num);
        else return String.format("%.2f", num);
    }

    private void updateDateTimeLabel() {
        LocalDateTime now = LocalDateTime.now();
        dateTimeLabel.setText(now.format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a")));
    }

    private void saveTransaction() {
        if (currentTotal <= 0) {
            JOptionPane.showMessageDialog(this, "Amount must be greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Account fromAccount = (Account) fromAccountBox.getSelectedItem();
        Account toAccount = (Account) toAccountBox.getSelectedItem();
        String category = (String) categoryBox.getSelectedItem();
        String notes = notesField.getText().trim();

        if (fromAccount == null) {
            JOptionPane.showMessageDialog(this, "Please select an account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("TRANSFER".equals(currentMode) && toAccount == null) {
            JOptionPane.showMessageDialog(this, "Please select a destination account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date date = (Date) dateSpinner.getValue();
        Date time = (Date) timeSpinner.getValue();
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        Calendar calTime = Calendar.getInstance();
        calTime.setTime(time);
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        Timestamp transTime = new Timestamp(calDate.getTimeInMillis());

        FinanceDAO db = new FinanceDAO();
        boolean success = db.addTransaction(userId, currentTotal, currentMode, category, notes,
                fromAccount.id, toAccount != null ? toAccount.id : null, transTime);

        if (success) {
            onSuccess.run();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save transaction.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}