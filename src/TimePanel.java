import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.*;
import java.util.List;

public class TimePanel extends JPanel {

    private JPanel calendarPanel;
    private JPanel dayViewPanel;
    private JLabel monthLabel;
    private TaskDAO taskDAO = new TaskDAO(); // Assumes your TaskDAO class exists
    private int userId = 1;
    private int selectedDay = -1;

    private YearMonth currentMonth = YearMonth.now();
    private Map<LocalDate, List<Task>> monthCache = new HashMap<>();
    private javax.swing.Timer refreshTimer; 

    // --- Task color palette and helper ---
    private Color[] taskColors = new Color[] {
        new Color(66,133,244),
        new Color(52,168,83),
        new Color(251,188,5),
        new Color(234,67,53),
        new Color(120,94,240),
        new Color(0,172,193)
    };

    private Color getTaskColor(Task t) {
        int idx = Math.abs(t.taskId) % taskColors.length;
        return taskColors[idx];
    }

    public TimePanel() {
        setLayout(new BorderLayout());
        
        // Add content directly
        add(buildContent(), BorderLayout.CENTER);

        loadCalendar();

        LocalDate today = LocalDate.now();
        selectedDay = today.getDayOfMonth();
        showEvents(selectedDay);

        // Auto refresh current time line every 30 seconds
        refreshTimer = new javax.swing.Timer(30000, e -> {
            if (selectedDay != -1 && dayViewPanel != null) {
                dayViewPanel.repaint(); // Smoothly redraw the red line
            }
        });
        refreshTimer.start();
    }

    // Prevent memory leak when panel is removed from screen
    @Override
    public void removeNotify() {
        super.removeNotify();
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }

    private JPanel buildContent() {
        JPanel container = new JPanel(new BorderLayout());
        container.add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.65);

        calendarPanel = new JPanel();
        calendarPanel.setBackground(new Color(248,249,250));

        // --- OVERLAY FIX: Draw current time directly on the panel ---
        dayViewPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g); // Draw standard hours and tasks first
                
                LocalDate today = LocalDate.now();
                if (selectedDay != -1 && currentMonth.atDay(selectedDay).equals(today)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int slotHeight = 70;
                    double ppm = slotHeight / 60.0;
                    LocalTime now = LocalTime.now();
                    int globalY = (int) (now.getHour() * slotHeight + now.getMinute() * ppm);

                    int leftOffset = 64;
                    int width = getWidth();
                    int safeWidth = Math.max(0, width - leftOffset - 10);
                    Color redColor = new Color(234, 67, 53);

                    // 1. Line
                    g2.setColor(redColor);
                    g2.fillRect(leftOffset, globalY, safeWidth, 2);

                    // 2. Dot
                    g2.setColor(Color.WHITE);
                    g2.fillOval(leftOffset - 5, globalY - 5, 10, 10);
                    g2.setColor(redColor);
                    g2.fillOval(leftOffset - 4, globalY - 4, 8, 8);

                    // 3. Floating Time Text
                    String timeStr = String.format("%02d:%02d", now.getHour(), now.getMinute());
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(timeStr);
                    
                    g2.setColor(redColor);
                    
                    // Nudge the text right-aligned, just before the dot
                    int textX = 52 - textWidth; 
                    // Center the text vertically perfectly on the red line
                    int textY = globalY + (fm.getAscent() / 2) - 1; 

                    g2.drawString(timeStr, textX, textY);

                    g2.dispose();
                }
            }
        };
        dayViewPanel.setLayout(new BoxLayout(dayViewPanel, BoxLayout.Y_AXIS));
        dayViewPanel.setBackground(new Color(250,250,252));

        JScrollPane scroll = new JScrollPane(dayViewPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // --- BUTTON VISIBILITY FIX: Prevent panels from stretching infinitely ---
        calendarPanel.setMinimumSize(new Dimension(300, 300));
        scroll.setMinimumSize(new Dimension(300, 300));

        split.setLeftComponent(calendarPanel);
        split.setRightComponent(scroll);

        container.add(split, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));

        JButton prev = new JButton("<");
        JButton next = new JButton(">");

        styleTopButton(prev);
        styleTopButton(next);

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
        center.setBackground(Color.WHITE);
        center.add(prev);
        center.add(monthLabel);
        center.add(next);

        JButton create = new JButton("+ Create");
        create.setBackground(new Color(66,133,244));
        create.setForeground(Color.WHITE);
        create.setFocusPainted(false);
        
        // --- MAC/OS THEME FIX: Force button colors to render ---
        create.setOpaque(true); 
        create.setBorderPainted(false); 
        
        create.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        create.setCursor(new Cursor(Cursor.HAND_CURSOR));
        create.addActionListener(e -> addTask());

        JPanel right = new JPanel();
        right.setBackground(Color.WHITE);
        right.add(create);

        prev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadCalendar();
        });

        next.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadCalendar();
        });

        top.add(center, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private void styleTopButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(245,245,245));
        btn.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void loadCalendar() {
        calendarPanel.removeAll();
        
        // --- 7-DAY FIX: Use 0 rows so Swing forces exactly 7 columns ---
        calendarPanel.setLayout(new GridLayout(0, 7, 12, 12));

        monthLabel.setText(currentMonth.getMonth() + " " + currentMonth.getYear());

        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        for (String d : days) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setForeground(Color.GRAY);
            calendarPanel.add(lbl);
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < startDay; i++) {
            calendarPanel.add(new JLabel());
        }

        monthCache.clear();

        List<Task> tasks = taskDAO.getTasksByMonth(userId, java.sql.Date.valueOf(firstDay));
        for (Task t : tasks) {
            LocalDate d = t.startDate.toLocalDate();
            monthCache.computeIfAbsent(d, k -> new ArrayList<>()).add(t);
        }

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            JButton btn = new JButton(String.valueOf(day));
            btn.setFocusPainted(false);
            btn.setBackground(Color.WHITE);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
                BorderFactory.createEmptyBorder(12,12,12,12)
            ));

            int d = day;
            btn.addActionListener(e -> {
                selectedDay = d;
                showEvents(d);
                loadCalendar();
            });

            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (d != selectedDay) btn.setBackground(new Color(245,247,250));
                }
                public void mouseExited(MouseEvent e) {
                    if (d != selectedDay) btn.setBackground(Color.WHITE);
                }
            });

            LocalDate today = LocalDate.now();
            if (day == today.getDayOfMonth() && currentMonth.equals(YearMonth.now())) {
                btn.setBorder(BorderFactory.createLineBorder(new Color(66,133,244), 1, true));
            }

            if (day == selectedDay) {
                btn.setBackground(new Color(232,240,254));
                btn.setForeground(Color.BLACK);
                btn.setBorder(BorderFactory.createLineBorder(new Color(66,133,244), 2, true));
            }

            calendarPanel.add(btn);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private void showEvents(int day) {
        selectedDay = day;
        dayViewPanel.removeAll();

        LocalDate date = currentMonth.atDay(day);
        List<Task> tasks = monthCache.getOrDefault(date, new ArrayList<>());

        Map<Integer, List<Task>> hourMap = new HashMap<>();
        for (Task t : tasks) {
            int start = t.startHour * 60 + t.startMin;
            int end = t.endHour * 60 + t.endMin;
            for (int h = start / 60; h <= end / 60; h++) {
                hourMap.computeIfAbsent(h, k -> new ArrayList<>()).add(t);
            }
        }

        Map<Integer, Integer> globalMaxSegment = new HashMap<>();
        for (Task t : tasks) {
            int taskStart = t.startHour * 60 + t.startMin;
            int taskEnd = t.endHour * 60 + t.endMin;

            for (int h = taskStart / 60; h <= taskEnd / 60; h++) {
                int slotStart = h * 60;
                int startOffset = Math.max(0, taskStart - slotStart);
                int endOffset = Math.min(60, taskEnd - slotStart);
                int segmentDuration = endOffset - startOffset;

                globalMaxSegment.put(t.taskId,
                    Math.max(globalMaxSegment.getOrDefault(t.taskId, 0), segmentDuration)
                );
            }
        }

        Set<Integer> labelShown = new HashSet<>();
        int slotHeight = 70;
        
        for (int hour = 0; hour < 24; hour++) {
            JPanel slot = new JPanel(null) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int leftCol = 64;
                    g.setColor(new Color(245,247,250));
                    g.fillRect(0, 0, leftCol, getHeight());
                    g.setColor(new Color(220,220,220));
                    g.drawLine(leftCol, 0, leftCol, getHeight());
                    g.setColor(new Color(210,210,210));
                    g.drawLine(leftCol, getHeight()-2, getWidth(), getHeight()-2);
                }
            };
            slot.setPreferredSize(new Dimension(300, slotHeight));
            slot.setMaximumSize(new Dimension(Integer.MAX_VALUE, slotHeight));
            slot.setBackground(Color.WHITE);
            slot.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(230,230,230)));

            JLabel timeLabel = new JLabel(String.format("%02d:00", hour));
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            timeLabel.setForeground(new Color(120,120,120));
            int labelHeight = 18;
            int yCenter = (slotHeight - labelHeight) / 2;
            timeLabel.setBounds(8, yCenter, 52, labelHeight);
            timeLabel.setOpaque(false);
            slot.add(timeLabel);

            double pixelsPerMinute = slotHeight / 60.0;
            List<Task> active = hourMap.getOrDefault(hour, new ArrayList<>());

            if (!active.isEmpty()) {
                active.sort(Comparator.comparingInt(a -> a.startHour * 60 + a.startMin));

                int cols = active.size();
                JPanel layer = new JPanel(null);
                layer.setOpaque(false);

                int totalWidth = dayViewPanel.getWidth();
                if (totalWidth <= 0) totalWidth = dayViewPanel.getPreferredSize().width;

                int leftOffset = 64;
                int availableWidth = totalWidth - leftOffset;
                if (availableWidth <= 0) availableWidth = 300;

                int colWidth = availableWidth / Math.max(cols, 1);

                for (int i = 0; i < active.size(); i++) {
                    Task t = active.get(i);

                    int taskStart = t.startHour * 60 + t.startMin;
                    int taskEnd = t.endHour * 60 + t.endMin;
                    int slotStart = hour * 60;

                    int startOffset = Math.max(0, taskStart - slotStart);
                    int endOffset = Math.min(60, taskEnd - slotStart);

                    int y = (int)(startOffset * pixelsPerMinute);
                    if (t.startHour == hour) y = Math.max(0, y - Math.min(6, startOffset/2));

                    int height = (int)((endOffset - startOffset) * pixelsPerMinute);
                    if (t.startHour == hour) height = Math.max(height, 35);

                    Color c = getTaskColor(t);

                    JPanel cell = new JPanel(new BorderLayout());
                    cell.setOpaque(true);
                    cell.setLayout(new BorderLayout());
                    cell.setBackground(c);
                    cell.setBorder(BorderFactory.createMatteBorder(1,1,1,1,new Color(255,255,255,80)));
                    leftOffset = 64; 
                    cell.setBounds(leftOffset + i * colWidth + 6, y, colWidth - 12, Math.max(height, 12));

                    int segmentDuration = endOffset - startOffset;
                    boolean showLabel = (segmentDuration == globalMaxSegment.get(t.taskId))
                                        && !labelShown.contains(t.taskId);

                    if (showLabel) {
                        labelShown.add(t.taskId);
                        JLabel titleLbl = new JLabel(t.title);
                        titleLbl.setForeground(Color.WHITE);
                        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

                        JLabel timeLbl = new JLabel(
                            String.format("%02d:%02d-%02d:%02d",
                            t.startHour, t.startMin, t.endHour, t.endMin)
                        );
                        timeLbl.setForeground(new Color(255,255,255,200));
                        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));

                        JPanel textPanel = new JPanel();
                        textPanel.setOpaque(false);
                        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                        textPanel.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));

                        textPanel.add(titleLbl);
                        textPanel.add(timeLbl);

                        cell.add(textPanel, BorderLayout.CENTER);
                    }

                    cell.setToolTipText("Right-click to delete");
                    cell.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                int confirm = JOptionPane.showConfirmDialog(
                                    TimePanel.this,
                                    "Delete \"" + t.title + "\" (" +
                                    String.format("%02d:%02d-%02d:%02d", t.startHour, t.startMin, t.endHour, t.endMin) + ")?",
                                    "Confirm Delete",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.PLAIN_MESSAGE
                                );
                                if (confirm == JOptionPane.YES_OPTION) {
                                    taskDAO.deleteTask(t.taskId);
                                    loadCalendar();
                                    showEvents(selectedDay);
                                    revalidate();
                                }
                            }
                        }
                    });

                    layer.add(cell);
                }

                int width = slot.getWidth();
                if (width == 0) width = slot.getPreferredSize().width;
                layer.setBounds(0, 0, width, slotHeight);
                slot.add(layer);
            }

            dayViewPanel.add(slot);
        }

        dayViewPanel.revalidate();
        dayViewPanel.repaint();
    }

    private void addTask() {
        if (selectedDay == -1) {
            JOptionPane.showMessageDialog(this, "Select a day first");
            return;
        }

        JTextField titleField = new JTextField();

        String[] times = new String[96];
        for (int i = 0; i < 96; i++) {
            int h = i / 4;
            int m = (i % 4) * 15;
            times[i] = String.format("%02d:%02d", h, m);
        }

        JComboBox<String> startBox = new JComboBox<>(times);
        JComboBox<String> endBox = new JComboBox<>(times);

        JPanel panel = new JPanel(new GridLayout(3,2,10,10));
        panel.add(new JLabel("Title:")); panel.add(titleField);
        panel.add(new JLabel("Start:")); panel.add(startBox);
        panel.add(new JLabel("End:")); panel.add(endBox);

        int res = JOptionPane.showConfirmDialog(this, panel, "Create Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String title = titleField.getText().trim();
        String s = (String) startBox.getSelectedItem();
        String e = (String) endBox.getSelectedItem();

        int sh = Integer.parseInt(s.split(":")[0]);
        int sm = Integer.parseInt(s.split(":")[1]);
        int eh = Integer.parseInt(e.split(":")[0]);
        int em = Integer.parseInt(e.split(":")[1]);

        if (title.isEmpty() || (eh < sh || (eh == sh && em <= sm))) {
            JOptionPane.showMessageDialog(this, "Invalid input");
            return;
        }

        LocalDate d = currentMonth.atDay(selectedDay);
        taskDAO.addTask(userId, title, java.sql.Date.valueOf(d), sh, sm, eh, em);

        loadCalendar();
        showEvents(selectedDay);
    }
}