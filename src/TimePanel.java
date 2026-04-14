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
    private TaskDAO taskDAO = new TaskDAO(); 
    
    // --- CHANGED: No longer hardcoded to 1 ---
    private int userId;
    private int selectedDay = -1;

    private YearMonth currentMonth = YearMonth.now();
    private Map<LocalDate, List<Task>> monthCache = new HashMap<>();
    private javax.swing.Timer refreshTimer; 

    private Color[] taskColors = new Color[] {
        new Color(66,133,244),   // 0: Default / Blue
        new Color(52,168,83),    // 1: Work / Green
        new Color(251,188,5),    // 2: Personal / Yellow
        new Color(234,67,53),    // 3: Important / Red
        new Color(120,94,240),   // 4: Study / Purple
        new Color(0,172,193)     // 5: Meeting / Teal
    };

    // --- CHANGED: Now uses the actual category string to determine color ---
    private Color getTaskColor(Task t) {
        if (t.category == null) return taskColors[0];
        
        switch (t.category) {
            case "Work":      return taskColors[1];
            case "Personal":  return taskColors[2];
            case "Important": return taskColors[3];
            case "Study":     return taskColors[4];
            case "Meeting":   return taskColors[5];
            default:          return taskColors[0];
        }
    }

    // --- CHANGED: Constructor accepts the user ID ---
    public TimePanel(int loggedInUserId) {
        this.userId = loggedInUserId;
        
        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
        loadCalendar();

        LocalDate today = LocalDate.now();
        selectedDay = today.getDayOfMonth();
        showEvents(selectedDay);

        refreshTimer = new javax.swing.Timer(30000, e -> {
            if (selectedDay != -1 && dayViewPanel != null) {
                dayViewPanel.repaint(); 
            }
        });
        refreshTimer.start();
    }

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

        dayViewPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g); 
                
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

                    g2.setColor(redColor);
                    g2.fillRect(leftOffset, globalY, safeWidth, 2);

                    g2.setColor(Color.WHITE);
                    g2.fillOval(leftOffset - 5, globalY - 5, 10, 10);
                    g2.setColor(redColor);
                    g2.fillOval(leftOffset - 4, globalY - 4, 8, 8);

                    String timeStr = String.format("%02d:%02d", now.getHour(), now.getMinute());
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(timeStr);
                    
                    g2.setColor(redColor);
                    int textX = 52 - textWidth; 
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
    top.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    JButton prev = new JButton("<");
    JButton next = new JButton(">");

    styleTopButton(prev);
    styleTopButton(next);

    monthLabel = new JLabel("", SwingConstants.CENTER);
    monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
    
    // --- KEY FIX: Set a fixed width so the buttons don't move ---
    // 250 pixels is usually wide enough for "September 2026"
    monthLabel.setPreferredSize(new Dimension(250, 40)); 

    // Use a JPanel with FlowLayout to keep the items grouped together
    JPanel navGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    navGroup.setBackground(Color.WHITE);
    navGroup.add(prev);
    navGroup.add(monthLabel);
    navGroup.add(next);

    // Keep the "Create" button on the far right
    JButton create = new JButton("+ Create");
    create.setBackground(new Color(66, 133, 244));
    create.setForeground(Color.WHITE);
    create.setFocusPainted(false);
    create.setOpaque(true); 
    create.setBorderPainted(false); 
    create.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    create.setCursor(new Cursor(Cursor.HAND_CURSOR));
    
    create.addActionListener(e -> openTaskDialog(null));

    JPanel right = new JPanel();
    right.setBackground(Color.WHITE);
    right.add(create);

    prev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); loadCalendar(); });
    next.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); loadCalendar(); });

    // Add the stable navigation group to the center
    top.add(navGroup, BorderLayout.CENTER);
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
                public void mouseEntered(MouseEvent e) { if (d != selectedDay) btn.setBackground(new Color(245,247,250)); }
                public void mouseExited(MouseEvent e) { if (d != selectedDay) btn.setBackground(Color.WHITE); }
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
                globalMaxSegment.put(t.taskId, Math.max(globalMaxSegment.getOrDefault(t.taskId, 0), endOffset - startOffset));
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
                    
                    g.setColor(new Color(230,230,230)); 
                    g.drawLine(leftCol, getHeight() - 1, getWidth(), getHeight() - 1); 
                }
            };
            slot.setPreferredSize(new Dimension(300, slotHeight));
            slot.setMaximumSize(new Dimension(Integer.MAX_VALUE, slotHeight));
            slot.setBackground(Color.WHITE);

            JLabel timeLabel = new JLabel(String.format("%02d:00", hour));
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            timeLabel.setForeground(new Color(120,120,120));
            timeLabel.setBounds(8, (slotHeight - 18) / 2, 52, 18);
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
                int availableWidth = Math.max(totalWidth - leftOffset, 300);
                int colWidth = availableWidth / Math.max(cols, 1);

                for (int i = 0; i < active.size(); i++) {
                    Task t = active.get(i);

                    int taskStart = t.startHour * 60 + t.startMin;
                    int taskEnd = t.endHour * 60 + t.endMin;
                    int slotStart = hour * 60;
                    int slotEnd = slotStart + 60;

                    int startOffset = Math.max(0, taskStart - slotStart);
                    int endOffset = Math.min(60, taskEnd - slotStart);

                    int y = (int) Math.round(startOffset * pixelsPerMinute);
                    int height = (int) Math.round((endOffset - startOffset) * pixelsPerMinute);

                    if (t.startHour == hour && t.endHour == hour) {
                        height = Math.max(height, 35);
                        if (y + height > slotHeight) {
                            y = slotHeight - height;
                        }
                    } else if (t.startHour == hour) {
                        height = slotHeight - y;
                    } else if (t.endHour == hour) {
                        y = 0;
                        height = (int) Math.round(endOffset * pixelsPerMinute);
                    } else {
                        y = 0;
                        height = slotHeight;
                    }

                    Color c = getTaskColor(t);

                    JPanel cell = new JPanel(new BorderLayout());
                    cell.setOpaque(true);
                    cell.setBackground(c);
                    cell.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
                    
                    int topBorder = (taskStart < slotStart) ? 0 : 1;
                    int bottomBorder = (taskEnd > slotEnd) ? 0 : 1;
                    
                    cell.setBorder(BorderFactory.createMatteBorder(topBorder, 1, bottomBorder, 1, new Color(255,255,255,80)));
                    
                    int seamlessOverlap = (taskEnd > slotEnd) ? 1 : 0;
                    cell.setBounds(leftOffset + i * colWidth + 6, y, colWidth - 12, height + seamlessOverlap);

                    if (endOffset - startOffset == globalMaxSegment.get(t.taskId) && !labelShown.contains(t.taskId)) {
                        labelShown.add(t.taskId);
                        JLabel titleLbl = new JLabel(t.title);
                        titleLbl.setForeground(Color.WHITE);
                        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

                        JLabel timeLbl = new JLabel(String.format("%02d:%02d-%02d:%02d", t.startHour, t.startMin, t.endHour, t.endMin));
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

                    cell.setToolTipText("<html><b>" + t.title + "</b><br>Click to View/Edit</html>");
                    
                    cell.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            openTaskDialog(t); 
                        }
                    });
                    layer.add(cell);
                }
                int width = slot.getWidth() == 0 ? slot.getPreferredSize().width : slot.getWidth();
                layer.setBounds(0, 0, width, slotHeight);
                slot.add(layer);
            }
            dayViewPanel.add(slot);
        }
        dayViewPanel.revalidate();
        dayViewPanel.repaint();
    }

    private void openTaskDialog(Task existingTask) {
        if (selectedDay == -1) {
            JOptionPane.showMessageDialog(this, "Select a day first", "Notice", JOptionPane.PLAIN_MESSAGE);
            return;
        }

        boolean isEditMode = (existingTask != null);
        String dialogTitle = isEditMode ? "Edit Task" : "Create Task";

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog((Frame) parentWindow, dialogTitle, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Title:"), gbc);
        JTextField titleField = new JTextField();
        if (isEditMode) titleField.setText(existingTask.title);
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Category:"), gbc);
        String[] categories = {"Default", "Work", "Personal", "Important", "Study", "Meeting"};
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        categoryBox.setBackground(Color.WHITE);
        if (isEditMode && existingTask.category != null) categoryBox.setSelectedItem(existingTask.category);
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(categoryBox, gbc);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); 

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Start Time:"), gbc);
        SpinnerDateModel startModel = new SpinnerDateModel();
        JSpinner startSpinner = new JSpinner(startModel);
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startSpinner, "HH:mm");
        startSpinner.setEditor(startEditor);
        
        if (isEditMode) {
            cal.set(Calendar.HOUR_OF_DAY, existingTask.startHour);
            cal.set(Calendar.MINUTE, existingTask.startMin);
            startSpinner.setValue(cal.getTime());
        } else {
            startSpinner.setValue(Date.from(LocalTime.of(9, 0).atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
        }
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(startSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.2;
        formPanel.add(new JLabel("End Time:"), gbc);
        SpinnerDateModel endModel = new SpinnerDateModel();
        JSpinner endSpinner = new JSpinner(endModel);
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endSpinner, "HH:mm");
        endSpinner.setEditor(endEditor);
        
        if (isEditMode) {
            cal.set(Calendar.HOUR_OF_DAY, existingTask.endHour);
            cal.set(Calendar.MINUTE, existingTask.endMin);
            endSpinner.setValue(cal.getTime());
        } else {
            endSpinner.setValue(Date.from(LocalTime.of(10, 0).atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
        }
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(endSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.2;
        gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(new JLabel("Notes:"), gbc);
        JTextArea descArea = new JTextArea(4, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        if (isEditMode && existingTask.description != null) descArea.setText(existingTask.description);
        
        JScrollPane descScroll = new JScrollPane(descArea);
        gbc.gridx = 1; gbc.weightx = 0.8;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; 
        formPanel.add(descScroll, gbc);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        JPanel leftActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftActionPanel.setBackground(Color.WHITE);
        if (isEditMode) {
            JButton deleteBtn = new JButton("Delete");
            deleteBtn.setForeground(new Color(234, 67, 53));
            deleteBtn.setFocusPainted(false);
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
            deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                    dialog, 
                    "Are you sure you want to delete this task?", 
                    "Delete Task", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.PLAIN_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    taskDAO.deleteTask(existingTask.taskId);
                    loadCalendar();
                    showEvents(selectedDay);
                    dialog.dispose();
                }
            });
            leftActionPanel.add(deleteBtn);
        }

        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActionPanel.setBackground(Color.WHITE);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEditMode ? "Save Changes" : "Create Task");
        saveBtn.setBackground(new Color(66,133,244));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        
        saveBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String description = descArea.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            
            Date startTime = (Date) startSpinner.getValue();
            Date endTime = (Date) endSpinner.getValue();

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startTime);
            int sh = startCal.get(Calendar.HOUR_OF_DAY);
            int sm = startCal.get(Calendar.MINUTE);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endTime);
            int eh = endCal.get(Calendar.HOUR_OF_DAY);
            int em = endCal.get(Calendar.MINUTE);

            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a title.", "Error", JOptionPane.PLAIN_MESSAGE);
                return;
            }
            if (eh < sh || (eh == sh && em <= sm)) {
                JOptionPane.showMessageDialog(dialog, "End time must be after Start time.", "Error", JOptionPane.PLAIN_MESSAGE);
                return;
            }

            LocalDate d = currentMonth.atDay(selectedDay);

            if (isEditMode) {
                taskDAO.updateTask(existingTask.taskId, title, category, description, java.sql.Date.valueOf(d), sh, sm, eh, em);
            } else {
                taskDAO.addTask(userId, title, category, description, java.sql.Date.valueOf(d), sh, sm, eh, em);
            }

            loadCalendar();
            showEvents(selectedDay);
            dialog.dispose();
        });

        rightActionPanel.add(cancelBtn);
        rightActionPanel.add(saveBtn);

        buttonPanel.add(leftActionPanel, BorderLayout.WEST);
        buttonPanel.add(rightActionPanel, BorderLayout.EAST);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}