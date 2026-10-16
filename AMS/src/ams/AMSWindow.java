package ams;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Desktop window for the Appointment Management System (the console version is AMS.java)
public class AMSWindow extends JFrame {

    private static final String[] FILTERS = {"All appointments", "Today", "Upcoming", "Due", "Urgent (due)", "Overdue", "Completed", "Missed"};
    private static final String[] SORTS = {"Sort by priority", "Sort by date & time", "Sort by date created", "Sort by name"};
    private static final String[] COLUMNS = {"ID", "When", "Name(s)", "Subject", "Priority", "Status"};

    private final LinkedList queue = new LinkedList();
    private final String listFile = DataPaths.appointmentList();

    private final AppointmentTableModel model = new AppointmentTableModel();
    private final JTable table = new JTable(model);
    private final UiKit.RoundField searchField = new UiKit.RoundField("Search name, subject or ID", true);
    private final JComboBox<String> filterBox = new JComboBox<>(FILTERS);
    private final JComboBox<String> sortBox = new JComboBox<>(SORTS);
    private final StatCard[] statCards = {
            new StatCard("Today", 1, () -> UiKit.PURPLE),
            new StatCard("Due", 3, () -> UiKit.statusColors("1")[0]),
            new StatCard("Urgent (due)", 4, () -> UiKit.priorityColors(1)[0]),
            new StatCard("Overdue", 5, () -> UiKit.OVERDUE),
            new StatCard("Completed", 6, () -> UiKit.statusColors("2")[0])};
    private final JLabel subtitle = UiKit.label(" ", Font.PLAIN, 13, UiKit.MUTED);
    private final CardLayout tableCards = new CardLayout();
    private final JPanel tableHolder = new JPanel(tableCards);
    private final JLabel emptyTitle = UiKit.label("", Font.BOLD, 16, UiKit.TEXT);
    private final JLabel emptyText = UiKit.label("", Font.PLAIN, 13, UiKit.MUTED);
    private final UiKit.FlatButton emptyAction = new UiKit.FlatButton("", UiKit.FlatButton.Kind.SECONDARY);
    private final DetailsPanel details = new DetailsPanel();
    private final JLabel savedLabel = UiKit.label(" ", Font.PLAIN, 12, UiKit.MUTED);
    private final JLabel countLabel = UiKit.label(" ", Font.PLAIN, 12, UiKit.MUTED);

    public AMSWindow() {
        super("AMS - Appointment Manager");
        setIconImages(UiKit.appIcons());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                save();
                dispose();
                System.exit(0);
            }
        });

        if (new File(listFile).exists()) {
            queue.restoreAppointments(listFile);
        }
        queue.sortByPriority();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiKit.BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
        setJMenuBar(buildMenuBar());

        setSize(1220, 780);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
        refresh(null);
        savedLabel.setText("Data file: " + new File(listFile).getAbsolutePath());

        // once a minute, update "overdue", "today" and the counts as time passes
        Timer clock = new Timer(60_000, e -> refresh(selectedId()));
        clock.start();

        // redraw with the new colours when the theme changes, and follow the system theme if chosen
        UiKit.onThemeChange(() -> refresh(selectedId()));
        UiKit.startThemeWatcher();
    }

    public static void main(String[] args) {
        UiKit.setupLookAndFeel();
        SwingUtilities.invokeLater(() -> new AMSWindow().setVisible(true));
    }

    // ---------- layout ----------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiKit.SURFACE);
        header.setBorder(UiKit.bottomLine(14, 22, 14, 22));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(new ImageIcon(UiKit.appIcon(40))), BorderLayout.WEST);
        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 0));
        titles.setOpaque(false);
        titles.add(UiKit.label("Appointments", Font.BOLD, 20, UiKit.TEXT));
        titles.add(subtitle);
        brand.add(titles, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        UiKit.FlatButton backup = new UiKit.FlatButton("Back up", UiKit.FlatButton.Kind.SECONDARY);
        UiKit.FlatButton restore = new UiKit.FlatButton("Restore", UiKit.FlatButton.Kind.SECONDARY);
        UiKit.FlatButton add = new UiKit.FlatButton("+  New appointment", UiKit.FlatButton.Kind.PRIMARY);
        backup.setToolTipText("Save a copy of all appointments to a file (Ctrl+B)");
        restore.setToolTipText("Add appointments from a backup file");
        add.setToolTipText("Place a new appointment (Ctrl+N)");
        backup.addActionListener(e -> backup());
        restore.addActionListener(e -> restore());
        add.addActionListener(e -> newAppointment());
        actions.add(backup);
        actions.add(restore);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(add);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 22, 10, 22));

        JPanel stats = new JPanel(new GridLayout(1, statCards.length, 12, 0));
        stats.setOpaque(false);
        for (StatCard card : statCards) {
            stats.add(card);
        }

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        searchField.setPreferredSize(new Dimension(320, 36));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(selectedId()); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(selectedId()); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(selectedId()); }
        });
        toolbar.add(searchField, BorderLayout.WEST);
        JPanel pickers = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pickers.setOpaque(false);
        for (JComboBox<String> box : java.util.List.of(filterBox, sortBox)) {
            box.setFont(UiKit.font(Font.PLAIN, 13));
            box.setPreferredSize(new Dimension(190, 36));
            box.setFocusable(true);
            pickers.add(box);
        }
        filterBox.addActionListener(e -> refresh(selectedId()));
        sortBox.addActionListener(e -> {
            applySort();
            refresh(selectedId());
        });
        toolbar.add(pickers, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(stats, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(14, 0));
        main.setOpaque(false);
        main.add(buildTableCard(), BorderLayout.CENTER);
        details.setPreferredSize(new Dimension(340, 100));
        main.add(details, BorderLayout.EAST);
        body.add(main, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildTableCard() {
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(UiKit.GRID);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(UiKit.SURFACE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(UiKit.font(Font.PLAIN, 13));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        int[] widths = {64, 170, 170, 230, 104, 116};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        CellRenderer text = new CellRenderer();
        PillRenderer pills = new PillRenderer();
        for (int i = 0; i < COLUMNS.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(i == 4 || i == 5 ? pills : text);
        }
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(100, 38));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setToolTipText("Click When, Name or Priority to sort");
        // Clicking a column heading sorts the linked list by that field
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == 4) {
                    sortBox.setSelectedIndex(0);
                } else if (column == 1) {
                    sortBox.setSelectedIndex(1);
                } else if (column == 2) {
                    sortBox.setSelectedIndex(3);
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                details.show(selectedAppointment());
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    editSelected();
                }
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "edit");
        table.getActionMap().put("edit", action(this::editSelected));
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getActionMap().put("delete", action(this::deleteSelected));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(1, 1, 1, 1));
        scroll.getViewport().setBackground(UiKit.SURFACE);
        scroll.setBackground(UiKit.SURFACE);

        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JPanel emptyContent = new JPanel();
        emptyContent.setOpaque(false);
        emptyContent.setLayout(new BoxLayout(emptyContent, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel(new ImageIcon(UiKit.appIcon(56)));
        for (JComponent c : new JComponent[]{icon, emptyTitle, emptyText, emptyAction}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        emptyContent.add(icon);
        emptyContent.add(Box.createVerticalStrut(14));
        emptyContent.add(emptyTitle);
        emptyContent.add(Box.createVerticalStrut(6));
        emptyContent.add(emptyText);
        emptyContent.add(Box.createVerticalStrut(16));
        emptyContent.add(emptyAction);
        empty.add(emptyContent);
        emptyAction.addActionListener(e -> {
            if (queue.isEmpty()) {
                newAppointment();
            } else {
                searchField.setText("");
                filterBox.setSelectedIndex(0);
            }
        });

        tableHolder.setOpaque(false);
        tableHolder.add(scroll, "table");
        tableHolder.add(empty, "empty");

        UiKit.RoundPanel card = new UiKit.RoundPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.add(tableHolder, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 24, 10, 24));
        bar.add(savedLabel, BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);
        return bar;
    }

    private JMenuBar buildMenuBar() {
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        file.add(item("New appointment", KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcut), this::newAppointment));
        file.addSeparator();
        file.add(item("Back up appointments...", KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcut), this::backup));
        file.add(item("Restore from backup...", null, this::restore));
        file.add(item("Open data folder", null, this::openDataFolder));
        file.addSeparator();
        file.add(item("Exit", null, () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));

        JMenu edit = new JMenu("Edit");
        edit.setMnemonic('E');
        edit.add(item("Edit appointment", KeyStroke.getKeyStroke(KeyEvent.VK_E, shortcut), this::editSelected));
        edit.add(item("Mark as due", KeyStroke.getKeyStroke(KeyEvent.VK_1, shortcut), () -> setStatus("1")));
        edit.add(item("Mark as completed", KeyStroke.getKeyStroke(KeyEvent.VK_2, shortcut), () -> setStatus("2")));
        edit.add(item("Mark as missed", KeyStroke.getKeyStroke(KeyEvent.VK_3, shortcut), () -> setStatus("3")));
        edit.add(item("Delete appointment", null, this::deleteSelected));
        edit.addSeparator();
        edit.add(item("Find...", KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcut), () -> {
            searchField.requestFocusInWindow();
            searchField.selectAll();
        }));
        edit.add(item("Show today's appointments", KeyStroke.getKeyStroke(KeyEvent.VK_T, shortcut), () -> {
            sortBox.setSelectedIndex(1);
            filterBox.setSelectedIndex(1);
        }));
        edit.addSeparator();
        edit.add(item("Delete all appointments...", null, this::clearAll));

        // View > Theme: follow the computer's setting, or always light / always dark
        JMenu view = new JMenu("View");
        view.setMnemonic('V');
        JMenu theme = new JMenu("Theme");
        ButtonGroup themes = new ButtonGroup();
        String[][] modes = {{"system", "Same as system"}, {"light", "Light"}, {"dark", "Dark"}};
        for (String[] mode : modes) {
            JRadioButtonMenuItem option = new JRadioButtonMenuItem(mode[1], UiKit.themeMode().equals(mode[0]));
            option.addActionListener(e -> UiKit.setThemeMode(mode[0]));
            themes.add(option);
            theme.add(option);
        }
        view.add(theme);

        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        help.add(item("Keyboard shortcuts", null, this::showShortcuts));
        help.add(item("About AMS", null, this::showAbout));

        bar.add(file);
        bar.add(edit);
        bar.add(view);
        bar.add(help);
        return bar;
    }

    // ---------- actions ----------

    private void newAppointment() {
        Appointment created = AppointmentDialog.showNew(this, queue, suggestId());
        if (created != null) {
            queue.insert(created);
            // make sure the new appointment is visible
            searchField.setText("");
            filterBox.setSelectedIndex(0);
            afterChange(created.id, "Appointment " + created.id + " placed");
        }
    }

    private void editSelected() {
        Appointment selected = selectedAppointment();
        if (selected == null) {
            return;
        }
        Appointment updated = AppointmentDialog.showEdit(this, queue, selected);
        if (updated != null) {
            queue.updateById(updated);
            afterChange(updated.id, "Appointment " + updated.id + " updated");
        }
    }

    private void setStatus(String status) {
        Appointment selected = selectedAppointment();
        if (selected == null || selected.status.equals(status)) {
            return;
        }
        selected.status = status;
        queue.updateById(selected);
        afterChange(selected.id, "Appointment " + selected.id + " marked as " + selected.statusLabel());
    }

    private void deleteSelected() {
        Appointment selected = selectedAppointment();
        if (selected == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete appointment " + selected.id + " (" + selected.subject + " with " + selected.name + ")?\nThis can't be undone.",
                "Delete appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            int row = table.getSelectedRow();
            queue.deleteById(selected.id);
            String next = row < model.getRowCount() - 1 ? model.get(row + 1).id : row > 0 ? model.get(row - 1).id : null;
            afterChange(next, "Appointment " + selected.id + " deleted");
        }
    }

    private void clearAll() {
        if (queue.isEmpty()) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete all " + queue.getSize() + " appointments?\nTip: use File > Back up first if you might need them again.",
                "Delete all appointments", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            queue.clearAll();
            afterChange(null, "All appointments deleted");
        }
    }

    private void backup() {
        JFileChooser chooser = new JFileChooser(DataPaths.backupsFolder());
        chooser.setDialogTitle("Back up appointments");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setSelectedFile(new File(DataPaths.backupsFolder(), "appointments-" + LocalDate.now() + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".txt")) {
            target = new File(target.getParentFile(), target.getName() + ".txt");
        }
        if (target.exists() && JOptionPane.showConfirmDialog(this, target.getName() + " already exists. Replace it?",
                "Back up appointments", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        if (queue.storeAppointments(target.getPath())) {
            savedLabel.setText("Backed up " + queue.getSize() + " appointments to " + target.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Backed up " + queue.getSize() + " appointments to:\n" + target.getAbsolutePath(),
                    "Backup saved", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "The backup could not be written to:\n" + target.getAbsolutePath(),
                    "Backup failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restore() {
        JFileChooser chooser = new JFileChooser(DataPaths.backupsFolder());
        chooser.setDialogTitle("Restore from backup");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        int before = queue.getSize();
        int added = queue.restoreAppointments(chooser.getSelectedFile().getPath());
        if (added < 0) {
            JOptionPane.showMessageDialog(this, "That file could not be read.", "Restore failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        afterChange(selectedId(), "Restored " + added + " appointments from " + chooser.getSelectedFile().getName());
        JOptionPane.showMessageDialog(this, "Added " + added + " appointment(s).\n"
                        + "Appointments whose ID is already in the list were skipped.\nYou now have " + (before + added) + " appointments.",
                "Restore complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openDataFolder() {
        File folder = new File(listFile).getAbsoluteFile().getParentFile();
        try {
            Desktop.getDesktop().open(folder);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Your appointments are saved in:\n" + folder.getPath(), "Data folder",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showShortcuts() {
        String key = System.getProperty("os.name", "").toLowerCase().contains("mac") ? "Cmd" : "Ctrl";
        JOptionPane.showMessageDialog(this,
                key + "+N   New appointment\n"
                        + "Enter or double-click   Edit the selected appointment\n"
                        + "Delete   Delete the selected appointment\n"
                        + key + "+1 / 2 / 3   Mark as due / completed / missed\n"
                        + key + "+F   Search\n"
                        + key + "+T   Show today's appointments, in time order\n"
                        + key + "+B   Back up\n"
                        + "Ctrl+Enter (in a form)   Save",
                "Keyboard shortcuts", JOptionPane.PLAIN_MESSAGE);
    }

    private void showAbout() {
        JPanel about = new JPanel();
        about.setOpaque(false);
        about.setLayout(new BoxLayout(about, BoxLayout.Y_AXIS));
        about.add(UiKit.label("AMS - Appointment Manager", Font.BOLD, 15, UiKit.TEXT));
        about.add(Box.createVerticalStrut(4));
        about.add(UiKit.label("An offline tool for organising appointments.", Font.PLAIN, 13, UiKit.TEXT));
        about.add(Box.createVerticalStrut(12));
        about.add(UiKit.label("Appointments are kept in a priority-ordered doubly linked list", Font.PLAIN, 13, UiKit.MUTED));
        about.add(UiKit.label("and saved automatically to:", Font.PLAIN, 13, UiKit.MUTED));
        about.add(UiKit.label(new File(listFile).getAbsolutePath(), Font.PLAIN, 13, UiKit.MUTED));
        about.add(Box.createVerticalStrut(14));
        about.add(link("Powered by ", "Trio Branch", "https://triobranch.com"));
        JOptionPane.showMessageDialog(this, about, "About AMS", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(UiKit.appIcon(64)));
    }

    // Text with a clickable link that opens in the web browser
    private JLabel link(String before, String text, String url) {
        JLabel label = UiKit.label("<html>" + before + "<a href='" + url + "' style='color:" + UiKit.hex(UiKit.ACCENT_TEXT) + "'>" + text + "</a></html>",
                Font.PLAIN, 13, UiKit.TEXT);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setToolTipText(url);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(java.net.URI.create(url));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AMSWindow.this, "Visit " + url, text, JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        return label;
    }

    // ---------- state ----------

    // Re-sort, save to disk and redraw after any change
    private void afterChange(String selectId, String message) {
        applySort();
        if (save()) {
            savedLabel.setText(message + "  ·  saved " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        refresh(selectId);
    }

    private boolean save() {
        if (queue.storeAppointments(listFile)) {
            return true;
        }
        JOptionPane.showMessageDialog(this, "Your changes could not be saved to:\n" + new File(listFile).getAbsolutePath(),
                "Save failed", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    // Order the linked list using the chosen sorting method
    private void applySort() {
        switch (sortBox.getSelectedIndex()) {
            case 1: queue.sortByScheduled(); break;
            case 2: queue.sortByDateCreated(); break;
            case 3: queue.sortByName(); break;
            default: queue.sortByPriority(); break;
        }
        table.getTableHeader().repaint();
    }

    // Does an appointment belong to a filter (index into FILTERS)?
    private static boolean matches(Appointment a, int filter) {
        boolean due = a.status.equals("1");
        switch (filter) {
            case 1: return a.isToday();
            case 2: return due && a.scheduledTime() != null && !a.isOverdue();
            case 3: return due;
            case 4: return due && a.priority == 1;
            case 5: return a.isOverdue();
            case 6: return a.status.equals("2");
            case 7: return a.status.equals("3");
            default: return true;
        }
    }

    // Fill the table from the linked list, applying the search box and filter
    private void refresh(String selectId) {
        Appointment[] all = queue.toArray();
        String search = searchField.getText().trim().toLowerCase();
        int filter = filterBox.getSelectedIndex();
        int shown = 0;
        Appointment[] rows = new Appointment[all.length];
        int[] counts = new int[FILTERS.length];
        int dueToday = 0;
        for (Appointment a : all) {
            for (int f = 0; f < FILTERS.length; f++) {
                if (matches(a, f)) {
                    counts[f]++;
                }
            }
            if (a.isToday() && a.status.equals("1")) {
                dueToday++;
            }
            boolean matchesSearch = search.isEmpty() || a.id.toLowerCase().contains(search)
                    || a.name.toLowerCase().contains(search) || a.subject.toLowerCase().contains(search)
                    || a.description.toLowerCase().contains(search);
            if (matches(a, filter) && matchesSearch) {
                rows[shown++] = a;
            }
        }
        Appointment[] visible = new Appointment[shown];
        System.arraycopy(rows, 0, visible, 0, shown);
        model.setRows(visible);

        for (StatCard card : statCards) {
            card.setValue(counts[card.filterIndex], filter == card.filterIndex);
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"));
        subtitle.setText(today + "  ·  " + (counts[1] == 0 ? "nothing booked today"
                : counts[1] + (counts[1] == 1 ? " appointment" : " appointments") + " today, " + dueToday + " still due"));

        if (shown == 0) {
            if (all.length == 0) {
                emptyTitle.setText("No appointments yet");
                emptyText.setText("Place your first appointment to get started.");
                emptyAction.setText("+  New appointment");
            } else {
                emptyTitle.setText("No matching appointments");
                emptyText.setText("Try a different search or filter.");
                emptyAction.setText("Clear search and filter");
            }
            tableCards.show(tableHolder, "empty");
        } else {
            tableCards.show(tableHolder, "table");
        }

        int row = 0;
        if (selectId != null) {
            for (int i = 0; i < visible.length; i++) {
                if (visible[i].id.equals(selectId)) {
                    row = i;
                    break;
                }
            }
        }
        if (shown > 0) {
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        }
        details.show(selectedAppointment());
        countLabel.setText(shown == all.length ? all.length + " appointments"
                : "Showing " + shown + " of " + all.length + " appointments");
    }

    private Appointment selectedAppointment() {
        int row = table.getSelectedRow();
        return row >= 0 && row < model.getRowCount() ? model.get(row) : null;
    }

    private String selectedId() {
        Appointment selected = selectedAppointment();
        return selected == null ? null : selected.id;
    }

    // Suggest the next free number as the ID for a new appointment
    private String suggestId() {
        int max = 0;
        for (Appointment a : queue.toArray()) {
            try {
                max = Math.max(max, Integer.parseInt(a.id.trim()));
            } catch (NumberFormatException e) {
                // IDs that are not numbers are ignored
            }
        }
        String id = String.valueOf(max + 1);
        while (queue.searchById(id) != null) {
            id = String.valueOf(Integer.parseInt(id) + 1);
        }
        return id;
    }

    private static Action action(Runnable runnable) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { runnable.run(); }
        };
    }

    private static JMenuItem item(String text, KeyStroke key, Runnable runnable) {
        JMenuItem item = new JMenuItem(text);
        if (key != null) {
            item.setAccelerator(key);
        }
        item.addActionListener(e -> runnable.run());
        return item;
    }

    // ---------- table ----------

    private static class AppointmentTableModel extends AbstractTableModel {
        private Appointment[] rows = new Appointment[0];

        void setRows(Appointment[] rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Appointment get(int row) {
            return rows[row];
        }

        @Override public int getRowCount() { return rows.length; }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            Appointment a = rows[row];
            switch (column) {
                case 0: return a.id;
                case 1: return a.scheduledLabel();
                case 2: return a.name;
                case 3: return a.subject;
                case 4: return UiKit.cap(a.priorityLabel());
                default: return UiKit.cap(a.statusLabel());
            }
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(t, value, selected, false, row, column);
            Appointment a = model.get(row);
            boolean done = !a.status.equals("1");
            setBorder(new EmptyBorder(0, 14, 0, 10));
            setBackground(selected ? UiKit.ACCENT_SOFT : UiKit.SURFACE);
            Color color = column == 0 || done ? UiKit.MUTED : UiKit.TEXT;
            boolean bold = column == 3 && !done;
            String tip = column == 3 && !a.description.isEmpty() ? a.description : null;
            if (column == 1) {
                if (a.isOverdue()) {
                    color = UiKit.OVERDUE;
                    tip = "Overdue";
                } else if (a.scheduledTime() == null) {
                    color = UiKit.PLACEHOLDER;
                } else if (a.isToday() && !done) {
                    bold = true;
                }
            }
            setForeground(color);
            setFont(UiKit.font(bold ? Font.BOLD : Font.PLAIN, 13));
            setToolTipText(tip);
            return this;
        }
    }

    private class PillRenderer implements TableCellRenderer {
        private final UiKit.Pill pill = new UiKit.Pill();

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
            Appointment a = model.get(row);
            pill.set(String.valueOf(value), column == 4 ? UiKit.priorityColors(a.priority) : UiKit.statusColors(a.status));
            pill.setRow(selected ? UiKit.ACCENT_SOFT : UiKit.SURFACE, 12);
            return pill;
        }
    }

    // Column headings, with an arrow on the column the list is sorted by
    private class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
            int[] sortedColumns = {4, 1, -1, 2};
            int sortedColumn = sortedColumns[Math.max(0, sortBox.getSelectedIndex())];
            super.getTableCellRendererComponent(t, value + (column == sortedColumn ? "  ↑" : ""), false, false, row, column);
            setFont(UiKit.font(Font.BOLD, 12));
            setForeground(column == sortedColumn ? UiKit.TEXT : UiKit.MUTED);
            setBackground(UiKit.HEADER_BG);
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, UiKit.BORDER), new EmptyBorder(0, 14, 0, 10)));
            return this;
        }
    }

    // ---------- summary cards ----------

    // Count of appointments in one group; clicking it filters the table to that group
    private class StatCard extends UiKit.RoundPanel {
        final int filterIndex;
        private final java.util.function.Supplier<Color> color;
        private final JLabel number = UiKit.label("0", Font.BOLD, 24, UiKit.TEXT);
        private boolean active;

        // color is looked up each time the card is drawn, so it follows the theme
        StatCard(String title, int filterIndex, java.util.function.Supplier<Color> color) {
            super(new BorderLayout(0, 2));
            this.filterIndex = filterIndex;
            this.color = color;
            setBorder(new EmptyBorder(12, 20, 12, 16));
            add(UiKit.label(title, Font.PLAIN, 13, UiKit.MUTED), BorderLayout.NORTH);
            add(number, BorderLayout.CENTER);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Show " + title.toLowerCase() + " appointments");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    filterBox.setSelectedIndex(active ? 0 : filterIndex);
                }
            });
        }

        void setValue(int value, boolean active) {
            number.setText(String.valueOf(value));
            this.active = active;
            setBackground(active ? UiKit.STAT_ACTIVE : UiKit.SURFACE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = UiKit.smooth(g);
            g2.setColor(color.get());
            g2.fillRoundRect(8, 12, 4, getHeight() - 24, 4, 4);
            if (active) {
                g2.setColor(UiKit.ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
            }
            g2.dispose();
        }
    }

    // ---------- details ----------

    // Right-hand panel showing everything about the selected appointment
    private class DetailsPanel extends UiKit.RoundPanel {
        private final CardLayout cards = new CardLayout();
        private final JPanel holder = new JPanel(cards);
        private final JLabel idLabel = UiKit.label("", Font.BOLD, 12, UiKit.MUTED);
        private final JLabel subjectLabel = UiKit.label("", Font.BOLD, 18, UiKit.TEXT);
        private final JLabel nameLabel = UiKit.label("", Font.PLAIN, 14, UiKit.TEXT);
        private final UiKit.Pill priorityPill = new UiKit.Pill();
        private final UiKit.Pill statusPill = new UiKit.Pill();
        private final JLabel createdLabel = UiKit.label("", Font.PLAIN, 13, UiKit.TEXT);
        private final JLabel whenLabel = UiKit.label("", Font.BOLD, 14, UiKit.TEXT);
        private final JLabel whenHint = UiKit.label("", Font.PLAIN, 13, UiKit.MUTED);
        private final JTextArea description = new JTextArea();
        private final UiKit.FlatButton statusA = new UiKit.FlatButton("", UiKit.FlatButton.Kind.SECONDARY);
        private final UiKit.FlatButton statusB = new UiKit.FlatButton("", UiKit.FlatButton.Kind.SECONDARY);
        private String statusACode;
        private String statusBCode;

        DetailsPanel() {
            super(new BorderLayout());
            setBorder(new EmptyBorder(20, 20, 18, 20));
            holder.setOpaque(false);

            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            empty.add(UiKit.label("Select an appointment to see its details", Font.PLAIN, 13, UiKit.MUTED));
            holder.add(empty, "empty");

            JPanel content = new JPanel(new BorderLayout(0, 14));
            content.setOpaque(false);

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pills.setOpaque(false);
            pills.add(priorityPill);
            pills.add(Box.createHorizontalStrut(6));
            pills.add(statusPill);
            for (JComponent c : new JComponent[]{idLabel, subjectLabel, nameLabel, pills}) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            // let the labels use the full width so text is never clipped
            for (JLabel l : new JLabel[]{idLabel, subjectLabel, nameLabel}) {
                l.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            }
            top.add(idLabel);
            top.add(Box.createVerticalStrut(6));
            top.add(subjectLabel);
            top.add(Box.createVerticalStrut(4));
            top.add(nameLabel);
            top.add(Box.createVerticalStrut(12));
            top.add(pills);
            top.add(Box.createVerticalStrut(16));
            JPanel when = new JPanel(new GridLayout(0, 1, 0, 2));
            when.setOpaque(false);
            when.add(whenLabel);
            when.add(whenHint);
            top.add(section("WHEN", when));
            top.add(Box.createVerticalStrut(12));
            top.add(section("CREATED", createdLabel));
            content.add(top, BorderLayout.NORTH);

            description.setEditable(false);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setOpaque(false);
            description.setFont(UiKit.font(Font.PLAIN, 13));
            description.setForeground(UiKit.TEXT);
            description.setBorder(null);
            JScrollPane scroll = new JScrollPane(description);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            content.add(section("DESCRIPTION", scroll), BorderLayout.CENTER);

            UiKit.FlatButton edit = new UiKit.FlatButton("Edit", UiKit.FlatButton.Kind.SECONDARY);
            UiKit.FlatButton delete = new UiKit.FlatButton("Delete", UiKit.FlatButton.Kind.DANGER);
            edit.addActionListener(e -> editSelected());
            delete.addActionListener(e -> deleteSelected());
            statusA.addActionListener(e -> setStatus(statusACode));
            statusB.addActionListener(e -> setStatus(statusBCode));
            JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
            buttons.setOpaque(false);
            buttons.add(statusA);
            buttons.add(statusB);
            buttons.add(edit);
            buttons.add(delete);
            content.add(buttons, BorderLayout.SOUTH);

            holder.add(content, "content");
            add(holder, BorderLayout.CENTER);
        }

        private JPanel section(String title, JComponent body) {
            JPanel panel = new JPanel(new BorderLayout(0, 4));
            panel.setOpaque(false);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(UiKit.label(title, Font.BOLD, 11, UiKit.MUTED), BorderLayout.NORTH);
            panel.add(body, BorderLayout.CENTER);
            return panel;
        }

        void show(Appointment a) {
            if (a == null) {
                cards.show(holder, "empty");
                return;
            }
            idLabel.setText("APPOINTMENT #" + a.id.toUpperCase());
            subjectLabel.setText("<html><div style='width:270px'>" + UiKit.esc(a.subject) + "</div></html>");
            nameLabel.setText("<html><div style='width:270px'>with " + UiKit.esc(a.name) + "</div></html>");
            priorityPill.set(UiKit.cap(a.priorityLabel()) + " priority", UiKit.priorityColors(a.priority));
            statusPill.set(UiKit.cap(a.statusLabel()), UiKit.statusColors(a.status));
            createdLabel.setText(UiKit.formatCreated(a.dateCreated, "EEEE d MMMM yyyy, HH:mm"));
            LocalDateTime scheduled = a.scheduledTime();
            if (scheduled == null) {
                whenLabel.setText("Not scheduled yet");
                whenLabel.setForeground(UiKit.MUTED);
                whenHint.setText("Edit the appointment to add a date and time.");
                whenHint.setForeground(UiKit.MUTED);
            } else {
                whenLabel.setText(scheduled.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm")));
                whenLabel.setForeground(UiKit.TEXT);
                whenHint.setText(relative(scheduled, a.isOverdue()));
                whenHint.setForeground(a.isOverdue() ? UiKit.OVERDUE : UiKit.MUTED);
            }
            description.setText(a.description.isEmpty() ? "No description." : a.description);
            description.setForeground(a.description.isEmpty() ? UiKit.MUTED : UiKit.TEXT);
            description.setCaretPosition(0);
            // offer the two statuses the appointment is not in
            String[] codes = a.status.equals("1") ? new String[]{"2", "3"} : a.status.equals("2") ? new String[]{"1", "3"} : new String[]{"2", "1"};
            statusACode = codes[0];
            statusBCode = codes[1];
            statusA.setText(markText(codes[0]));
            statusB.setText(markText(codes[1]));
            cards.show(holder, "content");
            revalidate();
            repaint();
        }

        // "In 3 days", "In 2 hours", "Overdue by 40 minutes", "5 days ago"
        private String relative(LocalDateTime when, boolean overdue) {
            Duration gap = Duration.between(LocalDateTime.now(), when);
            boolean future = !gap.isNegative();
            Duration size = gap.abs();
            String amount;
            if (size.toDays() >= 1) {
                long days = Math.round(size.toHours() / 24.0);
                amount = days + (days == 1 ? " day" : " days");
            } else if (size.toHours() >= 1) {
                amount = size.toHours() + (size.toHours() == 1 ? " hour" : " hours");
            } else {
                long minutes = Math.max(1, size.toMinutes());
                amount = minutes + (minutes == 1 ? " minute" : " minutes");
            }
            if (future) {
                return "In " + amount;
            }
            return overdue ? "Overdue by " + amount : amount + " ago";
        }

        private String markText(String code) {
            return code.equals("1") ? "Mark as due" : code.equals("2") ? "Mark completed" : "Mark missed";
        }
    }
}
