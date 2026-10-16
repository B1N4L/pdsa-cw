import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Drives the AMS window's own code without showing it, and checks the results.
//   java -cp <classes> -Dams.data=<empty folder> packaging/tools/SmokeTest.java
public class SmokeTest {

    // column and option positions in the window
    static final int COL_ID = 0, COL_WHEN = 1, COL_NAME = 2;
    static final int SORT_PRIORITY = 0, SORT_SCHEDULED = 1, SORT_NAME = 3;
    static final int FILTER_ALL = 0, FILTER_TODAY = 1, FILTER_OVERDUE = 5, FILTER_COMPLETED = 6;

    static int failures = 0;

    public static void main(String[] args) throws Exception {
        File data = new File(System.getProperty("ams.data"));
        Class.forName("ams.UiKit").getMethod("setupLookAndFeel").invoke(null);
        SwingUtilities.invokeAndWait(() -> {
            try {
                run(data);
            } catch (Exception e) {
                e.printStackTrace();
                failures++;
            }
        });
        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void run(File data) throws Exception {
        Class<?> windowClass = Class.forName("ams.AMSWindow");
        JFrame window = (JFrame) windowClass.getConstructor().newInstance();
        Object queue = get(window, "queue");
        JTable table = (JTable) get(window, "table");
        File listFile = new File(data, "appointment-list.txt");
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        check("starts empty", table.getRowCount() == 0);
        check("empty message shown", ((JLabel) get(window, "emptyTitle")).getText().equals("No appointments yet"));

        // times typed in different ways
        Method parseTime = Class.forName("ams.UiKit").getMethod("parseTime", String.class);
        check("time 14:30", LocalTime.of(14, 30).equals(parseTime.invoke(null, "14:30")));
        check("time 9.15", LocalTime.of(9, 15).equals(parseTime.invoke(null, "9.15")));
        check("time 930", LocalTime.of(9, 30).equals(parseTime.invoke(null, "930")));
        check("time 2:30 pm", LocalTime.of(14, 30).equals(parseTime.invoke(null, "2:30 pm")));
        check("time 12am", LocalTime.of(0, 0).equals(parseTime.invoke(null, "12am")));
        check("time 25:00 rejected", parseTime.invoke(null, "25:00") == null);
        check("time 'soon' rejected", parseTime.invoke(null, "soon") == null);

        // New appointment through the form's own validation
        Object first = save(form(window, queue, null, "1", "Nimal Perera", "Contract review", "Room 2\nBring the quotes", 0, 0, tomorrow, "10:00"));
        check("form accepts valid input", first != null);
        check("scheduled time stored", get(first, "scheduledAt").equals(tomorrow + "T10:00"));
        call(queue, "insert", first);
        call(window, "afterChange", "1", "placed");
        check("one row after adding", table.getRowCount() == 1);
        check("saved to disk with time", Files.readString(listFile.toPath()).contains("Scheduled: " + tomorrow + "T10:00"));
        check("When column shows tomorrow", table.getValueAt(0, COL_WHEN).equals("Tomorrow 10:00"));

        check("duplicate ID rejected", save(form(window, queue, null, "1", "Someone", "Something", "", 1, 0, null, null)) == null);
        check("missing name rejected", save(form(window, queue, null, "2", "", "Something", "", 1, 0, null, null)) == null);
        check("time without date rejected", save(form(window, queue, null, "2", "A", "B", "", 1, 0, null, "10:00")) == null);
        check("bad time rejected", save(form(window, queue, null, "2", "A", "B", "", 1, 0, tomorrow, "99:99")) == null);

        // double booking: warned once, saved on the second press
        Object clashForm = form(window, queue, null, "2", "Ayesha Fernando", "Budget meeting", "", 2, 0, tomorrow, "10:00");
        check("double booking warns first", save(clashForm) == null
                && ((JLabel) get(clashForm, "errorLabel")).getText().contains("already booked"));
        Object second = saveAgain(clashForm);
        check("double booking saved on second press", second != null);
        call(queue, "insert", second);
        call(window, "afterChange", "2", "placed");

        // a due appointment in the past: warned once, then overdue
        Object pastForm = form(window, queue, null, "3", "Amal Silva", "Interview", "", 1, 0, today.minusDays(1), "09:00");
        check("past time warns first", save(pastForm) == null);
        Object third = saveAgain(pastForm);
        call(queue, "insert", third);
        call(window, "afterChange", "3", "placed");

        // no date at all is fine
        Object fourth = save(form(window, queue, null, "4", "IT support", "Laptop", "", 2, 0, null, null));
        check("no date is allowed", fourth != null && get(fourth, "scheduledAt").equals(""));
        call(queue, "insert", fourth);
        Object fifth = save(form(window, queue, null, "5", "Priya Raman", "Onboarding", "", 1, 0, today, "23:59"));
        call(queue, "insert", fifth);
        call(window, "afterChange", "5", "placed");

        check("five rows", table.getRowCount() == 5);
        check("priority order: urgent first", get(model(window, 0), "priority").equals(1) && get(model(window, 4), "priority").equals(3));
        check("suggested next ID is 6", call(window, "suggestId").equals("6"));

        // date & time sort: yesterday, today, tomorrow x2, then not scheduled
        JComboBox<?> sortBox = (JComboBox<?>) get(window, "sortBox");
        sortBox.setSelectedIndex(SORT_SCHEDULED);
        check("time order", table.getValueAt(0, COL_ID).equals("3") && table.getValueAt(1, COL_ID).equals("5")
                && table.getValueAt(4, COL_ID).equals("4"));
        check("unscheduled shows as Not scheduled", table.getValueAt(4, COL_WHEN).equals("Not scheduled"));

        // filters and summary counts
        JComboBox<?> filterBox = (JComboBox<?>) get(window, "filterBox");
        filterBox.setSelectedIndex(FILTER_TODAY);
        check("today filter", table.getRowCount() == 1 && table.getValueAt(0, COL_ID).equals("5"));
        filterBox.setSelectedIndex(FILTER_OVERDUE);
        check("overdue filter", table.getRowCount() == 1 && table.getValueAt(0, COL_ID).equals("3"));
        filterBox.setSelectedIndex(FILTER_ALL);
        check("header counts today", ((JLabel) get(window, "subtitle")).getText().contains("1 appointment today, 1 still due"));

        // Status change on the selected row
        sortBox.setSelectedIndex(SORT_PRIORITY);
        table.setRowSelectionInterval(0, 0);
        Object selected = model(window, 0);
        call(window, "setStatus", "2");
        check("status changed to completed", get(selected, "status").equals("2"));
        check("completed saved", Files.readString(listFile.toPath()).contains("Status: 2"));
        filterBox.setSelectedIndex(FILTER_COMPLETED);
        check("completed filter", table.getRowCount() == 1);
        filterBox.setSelectedIndex(FILTER_ALL);

        // Edit: moving appointment 4 to urgent keeps the list ordered
        Object edited = save(form(window, queue, call(queue, "searchById", "4"), "4", "IT support", "Laptop", "", 0, 0, tomorrow, "15:00"));
        call(queue, "updateById", edited);
        call(window, "afterChange", "4", "updated");
        check("edit keeps date", get(edited, "scheduledAt").equals(tomorrow + "T15:00"));
        check("urgent ones first after edit", get(model(window, 0), "priority").equals(1) && get(model(window, 1), "priority").equals(1));

        // Sorting by name and searching
        sortBox.setSelectedIndex(SORT_NAME);
        check("sort by name", table.getValueAt(0, COL_NAME).equals("Amal Silva") && table.getValueAt(4, COL_NAME).equals("Priya Raman"));
        sortBox.setSelectedIndex(SORT_PRIORITY);
        JTextField search = (JTextField) get(window, "searchField");
        search.setText("budget");
        check("search finds one", table.getRowCount() == 1 && table.getValueAt(0, COL_ID).equals("2"));
        search.setText("zzz");
        check("no-match message", ((JLabel) get(window, "emptyTitle")).getText().equals("No matching appointments"));
        search.setText("");

        // Reopen: everything survives the save file
        JFrame reopened = (JFrame) windowClass.getConstructor().newInstance();
        Object reloadedQueue = get(reopened, "queue");
        check("reload keeps 5 appointments", (int) call(reloadedQueue, "getSize") == 5);
        check("multi-line description kept", get(call(reloadedQueue, "searchById", "1"), "description").equals("Room 2\nBring the quotes"));
        check("scheduled time kept", get(call(reloadedQueue, "searchById", "2"), "scheduledAt").equals(tomorrow + "T10:00"));
        reopened.dispose();

        // An old file (no Scheduled line, written by the first version) still loads
        File old = new File(data, "old-format.txt");
        Files.writeString(old.toPath(), "ID: 345\nName: Binal\nSubject: home\nDescription: homework\nPriority: 2\nStatus: 1\n"
                + "Date Created: 2024-08-11T06:22:24.472675300Z\n\nID: 346\nName: Teddy\nSubject: viva\nDescription: \nPriority: 1\n"
                + "Status: 1\nDate Created: 2024-08-12T06:22:24Z\n\n", StandardCharsets.UTF_8);
        Object oldQueue = Class.forName("ams.LinkedList").getConstructor().newInstance();
        check("old format loads both", (int) call(oldQueue, "restoreAppointments", old.getPath()) == 2);
        check("old format has no time", get(call(oldQueue, "searchById", "345"), "scheduledAt").equals(""));
        check("old format empty description", get(call(oldQueue, "searchById", "346"), "description").equals(""));

        // Backup and restore
        File backup = new File((String) Class.forName("ams.DataPaths").getMethod("backupsFolder").invoke(null), "test-backup.txt");
        check("backup written", (boolean) call(queue, "storeAppointments", backup.getPath()));
        check("restore skips duplicates", (int) call(queue, "restoreAppointments", backup.getPath()) == 0);
        call(queue, "clearAll");
        check("restore after clear adds 5", (int) call(queue, "restoreAppointments", backup.getPath()) == 5);

        // Themes: View > Theme switches live and is remembered
        Class<?> kit = Class.forName("ams.UiKit");
        boolean systemDark = (boolean) kit.getMethod("systemPrefersDark").invoke(null);
        System.out.println("  (this computer's theme: " + (systemDark ? "dark" : "light") + ")");
        check("starts following the system", kit.getMethod("themeMode").invoke(null).equals("system")
                && (boolean) kit.getMethod("isDark").invoke(null) == systemDark);
        JMenu theme = (JMenu) window.getJMenuBar().getMenu(2).getMenuComponent(0);
        JLabel subtitle = (JLabel) get(window, "subtitle");
        ((JMenuItem) theme.getMenuComponent(2)).doClick();
        check("Dark switches to dark", (boolean) kit.getMethod("isDark").invoke(null));
        check("table turns dark", table.getBackground().getRGB() == new java.awt.Color(0x161B22).getRGB());
        check("labels turn light-on-dark", subtitle.getForeground().getRGB() == new java.awt.Color(0x9AA4B2).getRGB());
        check("dark choice saved", Files.readString(new File(data, "settings.properties").toPath()).contains("theme=dark"));
        ((JMenuItem) theme.getMenuComponent(1)).doClick();
        check("Light switches back", !(boolean) kit.getMethod("isDark").invoke(null)
                && table.getBackground().getRGB() == java.awt.Color.WHITE.getRGB()
                && subtitle.getForeground().getRGB() == new java.awt.Color(0x667085).getRGB());
        ((JMenuItem) theme.getMenuComponent(0)).doClick();
        check("Same as system follows the computer", (boolean) kit.getMethod("isDark").invoke(null) == systemDark
                && Files.readString(new File(data, "settings.properties").toPath()).contains("theme=system"));

        window.dispose();
    }

    // Open the appointment form (not shown) and fill it in
    static Object form(JFrame owner, Object queue, Object editing, String id, String name, String subject, String description,
                       int priorityIndex, int statusIndex, LocalDate date, String time) throws Exception {
        Class<?> dialogClass = Class.forName("ams.AppointmentDialog");
        var make = dialogClass.getDeclaredConstructor(java.awt.Frame.class, Class.forName("ams.LinkedList"), Class.forName("ams.Appointment"), String.class);
        make.setAccessible(true);
        Object dialog = make.newInstance(owner, queue, editing, id);
        ((JTextField) get(dialog, "idField")).setText(id);
        ((JTextField) get(dialog, "nameField")).setText(name);
        ((JTextField) get(dialog, "subjectField")).setText(subject);
        ((JTextArea) get(dialog, "descriptionArea")).setText(description);
        call(get(dialog, "datePicker"), "setDate", date);
        ((JComboBox<?>) get(dialog, "timeBox")).setSelectedItem(time == null ? "" : time);
        call(get(dialog, "prioritySelect"), "setSelectedIndex", priorityIndex);
        call(get(dialog, "statusSelect"), "setSelectedIndex", statusIndex);
        return dialog;
    }

    // Press Save; returns the appointment, or null if the form refused it or showed a warning
    static Object save(Object dialog) throws Exception {
        call(dialog, "save");
        return get(dialog, "result");
    }

    static Object saveAgain(Object dialog) throws Exception {
        Object result = save(dialog);
        ((JDialog) dialog).dispose();
        return result;
    }

    static Object model(JFrame window, int row) throws Exception {
        return call(get(window, "model"), "get", row);
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok    " : "  FAIL  ") + name);
        if (!ok) failures++;
    }

    static Object get(Object target, String name) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    static Object call(Object target, String name, Object... args) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    return m.invoke(target, args);
                }
            }
        }
        throw new NoSuchMethodException(name);
    }
}
