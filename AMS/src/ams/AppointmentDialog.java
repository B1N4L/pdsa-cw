package ams;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Form for placing a new appointment or editing an existing one
public class AppointmentDialog extends JDialog {


    private final LinkedList queue;
    private final Appointment editing;

    private final UiKit.RoundField idField = new UiKit.RoundField("e.g. 101", false);
    private final UiKit.RoundField nameField = new UiKit.RoundField("Who is the appointment with?", false);
    private final UiKit.RoundField subjectField = new UiKit.RoundField("What is it about?", false);
    private final UiKit.DatePicker datePicker = new UiKit.DatePicker("Pick a date");
    private final JComboBox<String> timeBox = new JComboBox<>(timeSlots());
    private final JTextArea descriptionArea = new JTextArea();
    private final UiKit.Segmented prioritySelect = new UiKit.Segmented(new String[]{"Urgent", "Medium", "Low"},
            i -> UiKit.priorityColors(i + 1));
    private final UiKit.Segmented statusSelect = new UiKit.Segmented(new String[]{"Due", "Completed", "Missed"},
            i -> UiKit.statusColors(String.valueOf(i + 1)));
    private final JLabel errorLabel = UiKit.label(" ", Font.PLAIN, 13, UiKit.DANGER);
    private final UiKit.FlatButton saveButton;

    private Appointment result;
    // a warning the user has already seen; pressing Save again with the same details saves anyway
    private String confirmedWarning;

    // editing is null when placing a new appointment
    private AppointmentDialog(Frame owner, LinkedList queue, Appointment editing, String suggestedId) {
        super(owner, editing == null ? "New appointment" : "Edit appointment", true);
        this.queue = queue;
        this.editing = editing;

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiKit.SURFACE);
        root.setBorder(new EmptyBorder(22, 24, 18, 24));

        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 2));
        heading.setOpaque(false);
        heading.add(UiKit.label(editing == null ? "New appointment" : "Edit appointment", Font.BOLD, 19, UiKit.TEXT));
        heading.add(UiKit.label(editing == null ? "Fill in the details and press Save." : "The ID can't be changed.",
                Font.PLAIN, 13, UiKit.MUTED));
        heading.setBorder(new EmptyBorder(0, 0, 14, 0));
        root.add(heading, BorderLayout.NORTH);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UiKit.font(Font.PLAIN, 13));
        descriptionArea.setForeground(UiKit.TEXT);
        descriptionArea.setCaretColor(UiKit.TEXT);
        descriptionArea.setSelectionColor(UiKit.SELECTION);
        descriptionArea.setSelectedTextColor(UiKit.TEXT);
        descriptionArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionScroll.setPreferredSize(new Dimension(100, 80));
        descriptionScroll.setBorder(new UiKit.RoundBorder(new Insets(1, 1, 1, 1)));
        descriptionScroll.getViewport().setBackground(UiKit.SURFACE);
        descriptionArea.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { descriptionScroll.repaint(); }
            @Override public void focusLost(FocusEvent e) { descriptionScroll.repaint(); }
        });
        // Tab moves to the next field instead of typing a tab character
        descriptionArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        descriptionArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        // time: pick a 15-minute slot or type any time ("9.30", "2:30 pm")
        timeBox.setEditable(true);
        timeBox.setSelectedItem("");
        timeBox.setFont(UiKit.font(Font.PLAIN, 13));
        timeBox.setPreferredSize(new Dimension(130, 38));
        timeBox.setMaximumRowCount(12);
        timeBox.setToolTipText("Pick a time or type one, e.g. 9:30 or 2:30 pm");
        datePicker.setPreferredSize(new Dimension(200, 38));
        // picking a date for a new appointment suggests a time, so it can be saved straight away
        datePicker.addPropertyChangeListener("date", e -> {
            if (datePicker.getDate() != null && timeText().isEmpty()) {
                timeBox.setSelectedItem("09:00");
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        // ID and name side by side, the ID box narrower
        JPanel idRow = new JPanel(new BorderLayout(12, 0));
        idRow.setOpaque(false);
        JPanel idPanel = field("ID", idField);
        idPanel.setPreferredSize(new Dimension(130, idPanel.getPreferredSize().height));
        idRow.add(idPanel, BorderLayout.WEST);
        idRow.add(field("Name(s)", nameField), BorderLayout.CENTER);
        addRow(form, c, idRow);
        addRow(form, c, field("Subject", subjectField));

        // date and time side by side
        JPanel whenRow = new JPanel(new BorderLayout(12, 0));
        whenRow.setOpaque(false);
        whenRow.add(field("Date", datePicker), BorderLayout.CENTER);
        whenRow.add(field("Time", timeBox), BorderLayout.EAST);
        addRow(form, c, whenRow);

        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        addRow(form, c, field("Description (optional)", descriptionScroll));
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        addRow(form, c, field("Priority", prioritySelect));
        addRow(form, c, field("Status", statusSelect));
        addRow(form, c, errorLabel);
        root.add(form, BorderLayout.CENTER);

        UiKit.FlatButton cancel = new UiKit.FlatButton("Cancel", UiKit.FlatButton.Kind.SECONDARY);
        saveButton = new UiKit.FlatButton(saveText(), UiKit.FlatButton.Kind.PRIMARY);
        cancel.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> save());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(8, 0, 0, 0));
        buttons.add(cancel);
        buttons.add(saveButton);
        root.add(buttons, BorderLayout.SOUTH);

        // Enter saves (outside the description box), Esc closes
        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        descriptionArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "save");
        descriptionArea.getActionMap().put("save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { save(); }
        });

        if (editing == null) {
            idField.setText(suggestedId);
            prioritySelect.setSelectedIndex(1);
            statusSelect.setSelectedIndex(0);
        } else {
            idField.setText(editing.id);
            idField.setEnabled(false);
            nameField.setText(editing.name);
            subjectField.setText(editing.subject);
            descriptionArea.setText(editing.description);
            LocalDateTime when = editing.scheduledTime();
            if (when != null) {
                timeBox.setSelectedItem(when.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                datePicker.setDate(when.toLocalDate());
            }
            prioritySelect.setSelectedIndex(Math.max(0, Math.min(2, editing.priority - 1)));
            int status = "2".equals(editing.status) ? 1 : "3".equals(editing.status) ? 2 : 0;
            statusSelect.setSelectedIndex(status);
        }

        setContentPane(root);
        setSize(580, 700);
        setMinimumSize(new Dimension(500, 660));
        setLocationRelativeTo(owner);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                (editing == null ? nameField : subjectField).requestFocusInWindow();
            }
        });
    }

    // Show the form for a new appointment; returns the appointment, or null if cancelled
    public static Appointment showNew(Frame owner, LinkedList queue, String suggestedId) {
        AppointmentDialog dialog = new AppointmentDialog(owner, queue, null, suggestedId);
        dialog.setVisible(true);
        return dialog.result;
    }

    // Show the form for an existing appointment; returns it with the changes applied, or null if cancelled
    public static Appointment showEdit(Frame owner, LinkedList queue, Appointment appointment) {
        AppointmentDialog dialog = new AppointmentDialog(owner, queue, appointment, appointment.id);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void save() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String subject = subjectField.getText().trim();
        if (id.isEmpty()) {
            showError("Please enter an appointment ID.", idField);
            return;
        }
        if (editing == null && queue.searchById(id) != null) {
            showError("ID '" + id + "' is already used by another appointment.", idField);
            return;
        }
        if (name.isEmpty()) {
            showError("Please enter who the appointment is with.", nameField);
            return;
        }
        if (subject.isEmpty()) {
            showError("Please enter a subject.", subjectField);
            return;
        }

        // date and time are optional, but go together
        LocalDate date = datePicker.getDate();
        String timeText = timeText();
        LocalTime time = UiKit.parseTime(timeText);
        if (date == null && !timeText.isEmpty()) {
            showError("Please pick a date for this time, or clear the time.", datePicker);
            return;
        }
        if (date != null && time == null) {
            showError(timeText.isEmpty() ? "Please choose a time." : "'" + timeText + "' isn't a time. Try 14:30 or 2:30 pm.", timeBox);
            return;
        }
        String scheduledAt = date == null ? "" : LocalDateTime.of(date, time).toString();
        int priority = prioritySelect.getSelectedIndex() + 1;
        String status = String.valueOf(statusSelect.getSelectedIndex() + 1);

        // warnings: shown once, pressing Save again keeps the appointment as it is
        String warning = warningFor(scheduledAt, status, id);
        if (warning != null && !warning.equals(confirmedWarning)) {
            confirmedWarning = warning;
            errorLabel.setForeground(UiKit.WARNING);
            errorLabel.setText("<html><div style='width:440px'>" + UiKit.esc(warning) + " Press <b>Save anyway</b> to keep it.</div></html>");
            saveButton.setText("Save anyway");
            return;
        }

        String description = descriptionArea.getText().trim();
        if (editing == null) {
            result = new Appointment(id, name, subject, description, priority, status);
            result.scheduledAt = scheduledAt;
        } else {
            editing.name = name;
            editing.subject = subject;
            editing.description = description;
            editing.priority = priority;
            editing.status = status;
            editing.scheduledAt = scheduledAt;
            result = editing;
        }
        dispose();
    }

    // A double booking, or a due appointment set in the past (null if there's nothing to warn about)
    private String warningFor(String scheduledAt, String status, String id) {
        if (scheduledAt.isEmpty() || !status.equals("1")) {
            return null;
        }
        Appointment clash = queue.findClash(scheduledAt, id);
        LocalDateTime when = LocalDateTime.parse(scheduledAt);
        String at = when.format(DateTimeFormatter.ofPattern("HH:mm 'on' EEE d MMM"));
        if (clash != null) {
            return clash.name + " is already booked at " + at + " (" + clash.subject + ").";
        }
        boolean changed = editing == null || !scheduledAt.equals(editing.scheduledAt);
        if (changed && when.isBefore(LocalDateTime.now())) {
            return at + " has already passed.";
        }
        return null;
    }

    private String timeText() {
        Object item = timeBox.isEditable() ? timeBox.getEditor().getItem() : timeBox.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private String saveText() {
        return editing == null ? "Save appointment" : "Save changes";
    }

    private void showError(String message, JComponent focus) {
        confirmedWarning = null;
        saveButton.setText(saveText());
        errorLabel.setForeground(UiKit.DANGER);
        errorLabel.setText(message);
        focus.requestFocusInWindow();
    }

    // Times every 15 minutes through a working day
    private static String[] timeSlots() {
        String[] slots = new String[(21 - 7) * 4];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = String.format("%02d:%02d", 7 + i / 4, (i % 4) * 15);
        }
        return slots;
    }

    // A small caption above an input
    private static JPanel field(String caption, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(UiKit.label(caption, Font.BOLD, 12, UiKit.MUTED), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private static void addRow(JPanel form, GridBagConstraints c, JComponent row) {
        c.gridy = form.getComponentCount();
        c.insets = new Insets(0, 0, 12, 0);
        form.add(row, c);
    }
}
