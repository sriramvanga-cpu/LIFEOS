import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Calendar;


public class HealthPanel extends JPanel {

    private int userId;
    private HealthDAO dao = new HealthDAO();
    private double userHeight;
    
    private JLabel syncStatusLabel;
    private JLabel kjValueLabel;
    private javax.swing.Timer autoSaveTimer;
    private HealthDAO.HealthRecord currentRecord;

    private BarChartPanel stepsChart;
    private BarChartPanel sleepChart;

    private javax.swing.Timer sessionTimer;
    private long sessionStartTime;
    private boolean isRecording = false;
    private JLabel liveTimerLabel;
    private JButton liveBtn;

    public HealthPanel(int userId) {
        this.userId = userId;
        this.userHeight = dao.getUserHeight(userId);
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setFocusable(false);

        tabbedPane.addTab(" Overview ", buildOverviewPanel());
        tabbedPane.addTab(" Activity Journal ", buildActivityJournalPanel());
        tabbedPane.addTab(" Medications ", buildMedicationsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(250, 251, 253));

        java.sql.Date sqlToday = java.sql.Date.valueOf(LocalDate.now());
        currentRecord = dao.getDailyRecord(userId, sqlToday);

        //   HEADER SECTION  
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);
        topSection.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Today's Progress");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleBox.add(title);
        
        syncStatusLabel = new JLabel("All changes saved");
        syncStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        syncStatusLabel.setForeground(new Color(150, 150, 150));
        titleBox.add(syncStatusLabel);
        topSection.add(titleBox, BorderLayout.WEST);

        //   QUICK STATS  
        kjValueLabel = new JLabel(String.format("%.0f", calculateTotalKJ(currentRecord)));
        kjValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 0));
        statsRow.setOpaque(false);
        statsRow.add(createQuickStat(String.format("%.2f", (currentRecord.steps * 0.75) / 1000.0), "km"));
        
        JPanel kjBox = new JPanel(new GridLayout(2, 1));
        kjBox.setOpaque(false);
        kjBox.add(kjValueLabel);
        JLabel kjSub = new JLabel("kJ", SwingConstants.CENTER);
        kjSub.setForeground(Color.GRAY);
        kjBox.add(kjSub);
        statsRow.add(kjBox);

        topSection.add(statsRow, BorderLayout.EAST);
        panel.add(topSection, BorderLayout.NORTH);

        //   DASHBOARD CONTENT  
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.add(buildWeeklyGoalRow());

        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        cardsRow.setOpaque(false);

        // Metrics with Live-Updates
        JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(currentRecord.steps, 0, 100000, 500));
        stepSpinner.addChangeListener(e -> { 
            currentRecord.steps = (Integer) stepSpinner.getValue(); 
            stepsChart.updateTodayValue(currentRecord.steps);
            triggerAutoSave(); 
        });
        cardsRow.add(buildRingCard("Steps", stepSpinner, 10000, new Color(66, 133, 244)));
        
        JSpinner sleepSpinner = new JSpinner(new SpinnerNumberModel(currentRecord.sleep, 0.0, 24.0, 0.5));
        sleepSpinner.addChangeListener(e -> { 
            currentRecord.sleep = (Double) sleepSpinner.getValue(); 
            sleepChart.updateTodayValue(currentRecord.sleep);
            triggerAutoSave(); 
        });
        cardsRow.add(buildMetricCard("Sleep", sleepSpinner, 8.0, "hrs"));

        double startW = currentRecord.weight == 0 ? 70.00 : currentRecord.weight;
        JSpinner weightSpinner = new JSpinner(new SpinnerNumberModel(startW, 1.00, 500.00, 0.10));
        weightSpinner.setEditor(new JSpinner.NumberEditor(weightSpinner, "0.00"));
        weightSpinner.addChangeListener(e -> { 
            currentRecord.weight = ((Number) weightSpinner.getValue()).doubleValue(); 
            triggerAutoSave(); 
        });
        cardsRow.add(buildBMICard(weightSpinner));

        mainContent.add(cardsRow);
        
        //   CHARTS ROW  
        JPanel chartsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        chartsRow.setOpaque(false);
        stepsChart = new BarChartPanel(dao.getWeeklyMetric(userId, "STEPS"), new Color(66, 133, 244));
        sleepChart = new BarChartPanel(dao.getWeeklyMetric(userId, "SLEEP"), new Color(120, 94, 240));
        chartsRow.add(wrapInCard("Weekly Steps", stepsChart));
        chartsRow.add(wrapInCard("Weekly Sleep", sleepChart));
        mainContent.add(chartsRow);
        
        mainContent.add(buildInsightBar());

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void triggerAutoSave() {
        syncStatusLabel.setText("Syncing...");
        syncStatusLabel.setForeground(new Color(66, 133, 244));
        kjValueLabel.setText(String.format("%.0f", calculateTotalKJ(currentRecord)));

        if (autoSaveTimer != null && autoSaveTimer.isRunning()) {
            autoSaveTimer.restart();
        } else {
            autoSaveTimer = new javax.swing.Timer(1000, e -> {
                dao.saveDailyRecord(userId, java.sql.Date.valueOf(LocalDate.now()), currentRecord);
                syncStatusLabel.setText("All changes saved");
                syncStatusLabel.setForeground(new Color(150, 150, 150));
            });
            autoSaveTimer.setRepeats(false);
            autoSaveTimer.start();
        }
    }

    private class BarChartPanel extends JPanel {
        private Map<LocalDate, Double> data;
        private Color barColor;

        public BarChartPanel(Map<LocalDate, Double> data, Color barColor) { 
            this.data = data; this.barColor = barColor; setBackground(Color.WHITE); 
        }

        public void updateTodayValue(double value) {
            data.put(LocalDate.now(), value);
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); 
            Graphics2D g2 = (Graphics2D) g; 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth(), height = getHeight(), padding = 30;
            double max = 0; for (Double v : data.values()) if (v > max) max = v; if (max == 0) max = 10; 
            g2.setColor(new Color(240, 240, 240));
            for (int i = 0; i <= 4; i++) { int y = padding + (i * (height - 2 * padding) / 4); g2.drawLine(padding, y, width - padding, y); }
            g2.setColor(new Color(220, 220, 220)); g2.drawLine(padding, height - padding, width - padding, height - padding);
            int bw = (width - 2 * padding) / (data.size() * 2); int xo = padding + bw / 2;
            DateTimeFormatter df = DateTimeFormatter.ofPattern("E");
            for (Map.Entry<LocalDate, Double> entry : data.entrySet()) {
                int bh = (int) ((entry.getValue() / max) * (height - 2 * padding));
                g2.setColor(barColor); g2.fillRoundRect(xo, h() - padding - bh, bw, bh, 8, 8);
                g2.setColor(Color.DARK_GRAY); g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String v = String.format("%.0f", entry.getValue());
                g2.drawString(v, xo + (bw - g2.getFontMetrics().stringWidth(v)) / 2, h() - padding - bh - 5);
                g2.setColor(Color.GRAY); g2.drawString(entry.getKey().format(df), xo + (bw - g2.getFontMetrics().stringWidth(entry.getKey().format(df))) / 2, h() - padding + 20);
                xo += bw * 2;
            }
        }
        private int h() { return getHeight(); }
    }

    //   ACTIVITY JOURNAL TAB  
    private JPanel buildActivityJournalPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);

    //   LEFT SIDE 
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    listPanel.setBackground(new Color(250, 250, 252));
    JScrollPane scroll = new JScrollPane(listPanel);
    scroll.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    refreshActivityList(listPanel); 
    panel.add(scroll, BorderLayout.CENTER);

    //   RIGHT SIDE 
    JPanel sidePanel = new JPanel();
    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
    sidePanel.setBackground(Color.WHITE);
    sidePanel.setPreferredSize(new Dimension(350, 0));
    sidePanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)));

    // LIVE SESSION RECORDER
    sidePanel.add(buildLiveRecorderSection(listPanel));
    
    JSeparator sep = new JSeparator();
    sep.setMaximumSize(new Dimension(300, 1));
    sidePanel.add(Box.createRigidArea(new Dimension(0, 20)));
    sidePanel.add(sep);
    sidePanel.add(Box.createRigidArea(new Dimension(0, 20)));

    // MANUAL ENTRY FORM
    sidePanel.add(buildManualEntrySection(listPanel));

    panel.add(sidePanel, BorderLayout.EAST);
    return panel;
}

private JPanel buildLiveRecorderSection(JPanel listPanel) {
    JPanel livePanel = new JPanel();
    livePanel.setLayout(new BoxLayout(livePanel, BoxLayout.Y_AXIS));
    livePanel.setOpaque(false);
    livePanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));

    JLabel sectionTitle = new JLabel("Live Session");
    sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
    sectionTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

    liveTimerLabel = new JLabel("00:00:00");
    liveTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 42));
    liveTimerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    liveTimerLabel.setForeground(new Color(180, 180, 180));

    String[] types = {"Walking", "Running", "Cycling", "Hiking", "Weightlifting", "Yoga"};
    JComboBox<String> liveTypeBox = new JComboBox<>(types);
    liveTypeBox.setMaximumSize(new Dimension(250, 35));

    liveBtn = new JButton("Start Tracking");
    liveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
    liveBtn.setBackground(new Color(66, 133, 244));
    liveBtn.setForeground(Color.WHITE);
    liveBtn.setOpaque(true);
    liveBtn.setBorderPainted(false);
    liveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    liveBtn.setPreferredSize(new Dimension(200, 45));
    liveBtn.setMaximumSize(new Dimension(200, 45));

    liveBtn.addActionListener(e -> {
        if (!isRecording) {
            isRecording = true;
            sessionStartTime = System.currentTimeMillis();
            liveBtn.setText("Stop & Save");
            liveBtn.setBackground(new Color(234, 67, 53));
            liveTimerLabel.setForeground(new Color(52, 168, 83));
            
            sessionTimer = new javax.swing.Timer(1000, evt -> {
                long elapsed = System.currentTimeMillis() - sessionStartTime;
                long hrs = (elapsed / 3600000);
                long mins = (elapsed / 60000) % 60;
                long secs = (elapsed / 1000) % 60;
                liveTimerLabel.setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
            });
            sessionTimer.start();
        } else {
        isRecording = false;
        sessionTimer.stop();
        long sessionEndTime = System.currentTimeMillis();
        
        long diff = sessionEndTime - sessionStartTime;
        
        if (diff < 60000) {
            JOptionPane.showMessageDialog(this, "Activity too short to store (minimum 1 minute).", "Session Discarded", JOptionPane.PLAIN_MESSAGE);
        } else {
            int duration = (int) (diff / 60000);
            dao.addActivity(userId, (String)liveTypeBox.getSelectedItem(), duration, 0.0, 
                            new java.sql.Timestamp(sessionStartTime), new java.sql.Timestamp(sessionEndTime));
            refreshActivityList(listPanel);
            kjValueLabel.setText(String.format("%.0f", calculateTotalKJ(currentRecord)));
        }
        
        liveBtn.setText("Start Tracking");
        liveBtn.setBackground(new Color(66, 133, 244));
        liveTimerLabel.setText("00:00:00");
        liveTimerLabel.setForeground(new Color(180, 180, 180));
        }
    });

    livePanel.add(sectionTitle);
    livePanel.add(Box.createRigidArea(new Dimension(0, 15)));
    livePanel.add(liveTypeBox);
    livePanel.add(Box.createRigidArea(new Dimension(0, 15)));
    livePanel.add(liveTimerLabel);
    livePanel.add(Box.createRigidArea(new Dimension(0, 15)));
    livePanel.add(liveBtn);

    return livePanel;
}

private JPanel buildManualEntrySection(JPanel listPanel) {
    JPanel manualPanel = new JPanel(new GridBagLayout());
    manualPanel.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridx = 0; gbc.weightx = 1.0; 
    gbc.insets = new Insets(5, 25, 5, 25);

    gbc.gridy = 0; JLabel manualTitle = new JLabel("Manual Entry");
    manualTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
    manualPanel.add(manualTitle, gbc);

    String[] types = {"Walking", "Running", "Cycling", "Hiking", "Swimming", "Weightlifting", "Yoga"};
    JComboBox<String> typeBox = new JComboBox<>(types);
    gbc.gridy++; manualPanel.add(new JLabel("Activity:"), gbc);
    gbc.gridy++; manualPanel.add(typeBox, gbc);

    JTextField distField = new JTextField();
    gbc.gridy++; manualPanel.add(new JLabel("Distance (km):"), gbc);
    gbc.gridy++; manualPanel.add(distField, gbc);

    JSpinner startSpin = new JSpinner(new SpinnerDateModel());
    startSpin.setEditor(new JSpinner.DateEditor(startSpin, "HH:mm"));
    gbc.gridy++; manualPanel.add(new JLabel("Start Time:"), gbc);
    gbc.gridy++; manualPanel.add(startSpin, gbc);

    JSpinner endSpin = new JSpinner(new SpinnerDateModel());
    endSpin.setEditor(new JSpinner.DateEditor(endSpin, "HH:mm"));
    gbc.gridy++; manualPanel.add(new JLabel("End Time:"), gbc);
    gbc.gridy++; manualPanel.add(endSpin, gbc);

    JButton addBtn = new JButton("Add Record");
    addBtn.setBackground(new Color(52, 168, 83)); addBtn.setForeground(Color.WHITE);
    addBtn.setOpaque(true); addBtn.setBorderPainted(false);
    
    addBtn.addActionListener(e -> {
    Date sTime = (Date)startSpin.getValue();
    Date eTime = (Date)endSpin.getValue();
    
    Calendar cal = Calendar.getInstance();
    Calendar calS = Calendar.getInstance(); calS.setTime(sTime);
    calS.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    
    Calendar calE = Calendar.getInstance(); calE.setTime(eTime);
    calE.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

    long diff = calE.getTimeInMillis() - calS.getTimeInMillis();
    
    if (diff < 60000) {
        JOptionPane.showMessageDialog(this, "Activity too short to store (minimum 1 minute).", "Invalid Duration", JOptionPane.PLAIN_MESSAGE);
        return;
    }

    int duration = (int) (diff / 60000);
    double dist = 0; try { dist = Double.parseDouble(distField.getText()); } catch(Exception ex){}
    
    dao.addActivity(userId, (String)typeBox.getSelectedItem(), duration, dist, 
                    new java.sql.Timestamp(calS.getTimeInMillis()), new java.sql.Timestamp(calE.getTimeInMillis()));
    refreshActivityList(listPanel);
    kjValueLabel.setText(String.format("%.0f", calculateTotalKJ(currentRecord)));
    distField.setText("");
});

    gbc.gridy++; gbc.insets = new Insets(15, 25, 0, 25);
    manualPanel.add(addBtn, gbc);

    return manualPanel;
}

    private void refreshActivityList(JPanel listPanel) {
    listPanel.removeAll();
    java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
    double w = dao.getDailyRecord(userId, today).weight; if (w <= 0) w = 70.0;
    
    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("dd MMM, HH:mm:ss");

    for (HealthDAO.ActivityLog log : dao.getActivities(userId)) {
        JPanel card = new JPanel(new BorderLayout()); 
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true), 
            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        
        double kcal = calculateCalories(log, w);
        JLabel title = new JLabel(log.activity + " (" + log.duration + " mins" + (log.distance > 0 ? " | " + log.distance + "km" : "") + ")");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JLabel info = new JLabel("🔥 ~" + (int)kcal + " kcal  |  🕒 " + timeFormat.format(log.startTime));
        info.setForeground(Color.GRAY);
        
        JPanel txt = new JPanel(new GridLayout(2, 1)); txt.setOpaque(false); txt.add(title); txt.add(info);
        JButton del = new JButton("X"); del.setForeground(Color.RED); del.setContentAreaFilled(false); del.setBorderPainted(false);
        del.addActionListener(e -> { 
            dao.deleteActivity(log.logId); 
            refreshActivityList(listPanel); 
            kjValueLabel.setText(String.format("%.0f", calculateTotalKJ(currentRecord))); 
        });
        
        card.add(txt, BorderLayout.CENTER); card.add(del, BorderLayout.EAST);
        listPanel.add(card); listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    }
    listPanel.revalidate(); listPanel.repaint();
}

    //   MEDICATIONS TAB  
    private JPanel buildMedicationsPanel() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(Color.WHITE);
        JPanel listPanel = new JPanel(); listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS)); listPanel.setBackground(new Color(250, 250, 252));
        JScrollPane scroll = new JScrollPane(listPanel); scroll.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        refreshMedicationList(listPanel); panel.add(scroll, BorderLayout.CENTER);
        JPanel form = new JPanel(new GridBagLayout()); form.setBackground(Color.WHITE); form.setPreferredSize(new Dimension(350, 0));
        form.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = 2; gbc.gridx = 0; gbc.weightx = 1.0; gbc.insets = new Insets(8, 20, 8, 20);
        gbc.gridy = 0; JLabel t = new JLabel("Add Medication"); t.setFont(new Font("Segoe UI", 1, 20)); form.add(t, gbc);
        JTextField n = new JTextField(); JTextField d = new JTextField(); JSpinner g = new JSpinner(new SpinnerNumberModel(24, 1, 168, 1));
        gbc.gridy++; form.add(new JLabel("Name:"), gbc); gbc.gridy++; form.add(n, gbc);
        gbc.gridy++; form.add(new JLabel("Dosage:"), gbc); gbc.gridy++; form.add(d, gbc);
        gbc.gridy++; form.add(new JLabel("Interval (Hours):"), gbc); gbc.gridy++; form.add(g, gbc);
        JButton addBtn = new JButton("Add to Regimen"); addBtn.setBackground(new Color(66, 133, 244)); addBtn.setForeground(Color.WHITE); addBtn.setOpaque(true); addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> { if (!n.getText().isEmpty()) dao.addMedication(userId, n.getText(), d.getText(), (Integer)g.getValue()); refreshMedicationList(listPanel); });
        gbc.gridy++; gbc.insets = new Insets(20, 20, 10, 20); form.add(addBtn, gbc); panel.add(form, BorderLayout.EAST); return panel;
    }

    private void refreshMedicationList(JPanel listPanel) {
    listPanel.removeAll();
    
    // Fetch the list of medications for the current user
    for (HealthDAO.Medication med : dao.getMedications(userId)) {
        
        JPanel card = new JPanel(new BorderLayout()); 
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true), 
            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // MEDICATION INFO  
        JLabel title = new JLabel(med.name + " (" + med.dosage + ")"); 
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        
        JLabel detail = new JLabel("Interval: Every " + med.gap + " hours"); 
        detail.setForeground(Color.GRAY);
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        mainContent.add(title);
        mainContent.add(detail);
        mainContent.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

        // DYNAMIC DOSE TRACKER (DB-BACKED)  
        // Fetch which doses were already taken today from Neon
        Set<Integer> takenDoses = dao.getTakenDosesToday(userId, med.medId);

        int dosesPerDay = Math.max(1, 24 / med.gap); 
        
        JPanel doseBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        doseBoxPanel.setOpaque(false);
        
        for (int i = 1; i <= dosesPerDay; i++) {
            final int doseIndex = i;
            JCheckBox doseCheck = new JCheckBox("Dose " + i);
            doseCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            doseCheck.setOpaque(false);

            // Restore the saved state from the database
            if (takenDoses.contains(doseIndex)) {
                doseCheck.setSelected(true);
            }

            // Save state to the database instantly when toggled
            doseCheck.addActionListener(e -> {
                dao.toggleMedicationDose(userId, med.medId, doseIndex, doseCheck.isSelected());
            });

            doseBoxPanel.add(doseCheck);
        }
        mainContent.add(doseBoxPanel);

        // DELETE BUTTON  
        JButton del = new JButton("✕"); 
        del.setForeground(Color.RED); 
        del.setContentAreaFilled(false); 
        del.setBorderPainted(false);
        del.setCursor(new Cursor(Cursor.HAND_CURSOR));
        del.setToolTipText("Remove medication");
        
        del.addActionListener(e -> { 
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Remove " + med.name + " from your regimen?", "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dao.deleteMedication(med.medId); //
                refreshMedicationList(listPanel); 
            }
        });
        
        card.add(mainContent, BorderLayout.CENTER); 
        card.add(del, BorderLayout.EAST); 
        
        listPanel.add(card); 
        listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    }
    
    listPanel.revalidate(); 
    listPanel.repaint();
}

    //   HELPERS  
    private double calculateTotalKJ(HealthDAO.HealthRecord record) {
        double kj = record.steps * 0.15; double w = record.weight > 0 ? record.weight : 70.0;
        List<HealthDAO.ActivityLog> logs = dao.getActivities(userId); LocalDate today = LocalDate.now();
        for (HealthDAO.ActivityLog log : logs) { if (log.startTime != null && log.startTime.toLocalDateTime().toLocalDate().equals(today)) kj += (calculateCalories(log, w) * 4.184); }
        return kj;
    }

    private double calculateCalories(HealthDAO.ActivityLog log, double weight) {
        if (log.distance > 0) { switch(log.activity) { case "Running": return log.distance * weight * 1.036; case "Walking": return log.distance * weight * 0.75; case "Cycling": return log.distance * weight * 0.40; case "Hiking": return log.distance * weight * 0.85; } }
        double met = 4.0;
        switch(log.activity) { case "Walking": met = 3.5; break; case "Running": met = 8.0; break; case "Swimming": met = 7.0; break; case "Weightlifting": met = 3.5; break; case "Yoga": met = 2.5; break; }
        return (met * weight * 3.5) / 200 * log.duration;
    }

    private JPanel createQuickStat(String v, String l) {
        JPanel p = new JPanel(new GridLayout(2, 1)); p.setOpaque(false);
        JLabel vl = new JLabel(v, 0); vl.setFont(new Font("Segoe UI", 1, 20));
        JLabel tl = new JLabel(l, 0); tl.setForeground(Color.GRAY);
        p.add(vl); p.add(tl); return p;
    }

    private JPanel buildWeeklyGoalRow() {
        JPanel row = new JPanel(new FlowLayout(1, 15, 20)); row.setOpaque(false);
        Map<LocalDate, Double> steps = dao.getWeeklyMetric(userId, "STEPS"); boolean[] hit = new boolean[8];
        for (Map.Entry<LocalDate, Double> entry : steps.entrySet()) if (entry.getValue() >= 10000) hit[entry.getKey().getDayOfWeek().getValue()] = true;
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            JLabel l = new JLabel(days[i], 0); l.setPreferredSize(new Dimension(35, 35)); l.setOpaque(true); l.setFont(new Font("Segoe UI", 1, 12));
            if (hit[i+1]) { l.setBackground(new Color(66, 133, 244)); l.setForeground(Color.WHITE); }
            else { l.setBackground(Color.WHITE); l.setForeground(Color.LIGHT_GRAY); l.setBorder(BorderFactory.createLineBorder(new Color(235, 237, 240))); }
            row.add(l);
        }
        return row;
    }

    private JPanel buildRingCard(String t, JSpinner s, int g, Color c) {
        JPanel card = createBaseCard(t);
        JPanel ring = new JPanel() {
            @Override protected void paintComponent(Graphics gr) {
                super.paintComponent(gr); Graphics2D g2 = (Graphics2D) gr; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 110, x = (getWidth()-size)/2, y = (getHeight()-size)/2;
                g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(245, 245, 245)); g2.drawOval(x, y, size, size);
                g2.setColor(c); double angle = (((Number)s.getValue()).doubleValue()/g)*360;
                g2.draw(new Arc2D.Double(x, y, size, size, 90, -angle, Arc2D.OPEN));
            }
        };
        ring.setLayout(new GridBagLayout()); ring.setOpaque(false); s.setFont(new Font("Segoe UI", 1, 22)); ring.add(s);
        s.addChangeListener(e -> ring.repaint()); card.add(ring, BorderLayout.CENTER); return card;
    }

    private JPanel buildMetricCard(String t, JSpinner s, double g, String u) {
        JPanel card = createBaseCard(t); JPanel cPanel = new JPanel(new GridBagLayout()); cPanel.setOpaque(false);
        s.setFont(new Font("Segoe UI", 1, 28)); cPanel.add(s); card.add(cPanel, BorderLayout.CENTER);
        JLabel l = new JLabel("Target: "+g+" "+u, 0); l.setForeground(Color.GRAY); card.add(l, BorderLayout.SOUTH); return card;
    }

    private JPanel buildBMICard(JSpinner s) {
        JPanel card = createBaseCard("Weight & BMI"); JPanel p = new JPanel(new GridLayout(3, 1)); p.setOpaque(false);
        JLabel bl = new JLabel("", 0); bl.setFont(new Font("Segoe UI", 1, 38)); JLabel cl = new JLabel(); cl.setHorizontalAlignment(0); cl.setFont(new Font("Segoe UI", 1, 14));
        Runnable update = () -> { double weight = ((Number)s.getValue()).doubleValue(); double bmi = weight/Math.pow(userHeight/100, 2); bl.setText(String.format("%.1f", bmi));
            if (bmi<18.5) { cl.setText("Underweight"); cl.setForeground(Color.ORANGE); } else if (bmi<25) { cl.setText("Normal"); cl.setForeground(new Color(52, 168, 83)); } else { cl.setText("Overweight"); cl.setForeground(Color.RED); } };
        update.run(); s.addChangeListener(e -> update.run()); p.add(s); p.add(bl); p.add(cl); card.add(p, BorderLayout.CENTER); return card;
    }

    private JPanel createBaseCard(String t) {
        JPanel c = new JPanel(new BorderLayout()); c.setBackground(Color.WHITE); c.setPreferredSize(new Dimension(280, 230));
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        JLabel tl = new JLabel(t); tl.setFont(new Font("Segoe UI", 1, 16)); tl.setForeground(Color.DARK_GRAY); c.add(tl, BorderLayout.NORTH); return c;
    }

    private JPanel wrapInCard(String t, BarChartPanel chart) {
        JPanel w = new JPanel(new BorderLayout()); w.setBackground(Color.WHITE); w.setPreferredSize(new Dimension(430, 240));
        w.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        JLabel tl = new JLabel(t); tl.setFont(new Font("Segoe UI", 1, 16)); w.add(tl, BorderLayout.NORTH); w.add(chart, BorderLayout.CENTER); return w;
    }

    private JPanel buildInsightBar() {
        JPanel b = new JPanel(new BorderLayout()); b.setBackground(Color.WHITE); b.setMaximumSize(new Dimension(900, 80));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(235, 237, 240), 1, true), BorderFactory.createEmptyBorder(15, 25, 15, 25)));
        int s = dao.getStepStreak(userId); JLabel m = new JLabel(s>0 ? "🔥 You're on a "+s+"-day streak!" : "💡 Reach 10k steps to start a streak!");
        m.setFont(new Font("Segoe UI", 2, 16)); m.setForeground(new Color(66, 133, 244)); b.add(new JLabel("💡 "), BorderLayout.WEST); b.add(m, BorderLayout.CENTER); return b;
    }
}