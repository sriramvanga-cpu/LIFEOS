import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class YearlyExpenseBarChart extends JPanel {
    private Map<Integer, Double> monthlyTotals = new LinkedHashMap<>(); // month (1-12) -> total
    private double maxTotal = 0;
    private int selectedYear;
    private final Color BAR_COLOR = new Color(66, 133, 244);
    private final Color BAR_HOVER_COLOR = new Color(25, 103, 210);
    private final Color GRID_COLOR = new Color(230, 230, 230);
    private final Color TEXT_COLOR = new Color(100, 100, 100);

    public YearlyExpenseBarChart() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 300));
        // Initialize with current year
        selectedYear = LocalDate.now().getYear();
        for (int m = 1; m <= 12; m++) {
            monthlyTotals.put(m, 0.0);
        }
    }

    public void setData(List<Transaction> transactions, int year) {
        this.selectedYear = year;
        // Reset totals
        for (int m = 1; m <= 12; m++) {
            monthlyTotals.put(m, 0.0);
        }

        // Filter transactions for the given year and type EXPENSE
        for (Transaction t : transactions) {
            if (!"EXPENSE".equals(t.type)) continue;
            LocalDate transDate = null;
            if (t.transTime != null) {
                transDate = t.transTime.toLocalDateTime().toLocalDate();
            } else if (t.transDate != null) {
                transDate = t.transDate.toLocalDate();
            } else {
                continue;
            }
            if (transDate.getYear() == year) {
                int month = transDate.getMonthValue();
                monthlyTotals.merge(month, t.amount, Double::sum);
            }
        }

        // Calculate max for scaling
        maxTotal = monthlyTotals.values().stream().max(Double::compare).orElse(0.0);
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
        int paddingLeft = 60;
        int paddingRight = 30;
        int paddingTop = 30;
        int paddingBottom = 50;
        int chartWidth = width - paddingLeft - paddingRight;
        int chartHeight = height - paddingTop - paddingBottom;

        // Draw title
        g2.setColor(TEXT_COLOR);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        String title = "Monthly Expenses – " + selectedYear;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, paddingTop - 5);

        // Draw axes
        g2.setColor(GRID_COLOR);
        g2.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom); // Y-axis
        g2.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom); // X-axis

        // Draw Y-axis grid lines and labels
        int numGridLines = 5;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fm = g2.getFontMetrics();
        for (int i = 0; i <= numGridLines; i++) {
            double value = (maxTotal * i) / numGridLines;
            int y = height - paddingBottom - (int) ((value / maxTotal) * chartHeight);
            if (i > 0) {
                g2.setColor(GRID_COLOR);
                g2.drawLine(paddingLeft, y, width - paddingRight, y);
            }
            g2.setColor(TEXT_COLOR);
            String label = String.format("₹%.0f", value);
            g2.drawString(label, paddingLeft - fm.stringWidth(label) - 5, y + fm.getAscent() / 2);
        }

        // Draw bars and X-axis labels
        int barWidth = (chartWidth - 20) / 12; // 12 months
        int barSpacing = 5;
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");

        for (int month = 1; month <= 12; month++) {
            double total = monthlyTotals.get(month);
            int barHeight = (int) ((total / maxTotal) * chartHeight);
            int x = paddingLeft + (month - 1) * (barWidth + barSpacing) + barSpacing / 2;
            int y = height - paddingBottom - barHeight;

            // Bar
            g2.setColor(BAR_COLOR);
            g2.fill(new Rectangle2D.Double(x, y, barWidth, barHeight));

            // Month label
            g2.setColor(TEXT_COLOR);
            String monthLabel = YearMonth.of(selectedYear, month).format(monthFormatter);
            int labelX = x + (barWidth - fm.stringWidth(monthLabel)) / 2;
            g2.drawString(monthLabel, labelX, height - paddingBottom + 18);

            // Value label on top of bar (if bar is tall enough)
            if (barHeight > 20) {
                g2.setColor(Color.WHITE);
                String valStr = String.format("₹%.0f", total);
                int valX = x + (barWidth - fm.stringWidth(valStr)) / 2;
                g2.drawString(valStr, valX, y + fm.getAscent() + 5);
            }
        }

        g2.dispose();
    }
}
