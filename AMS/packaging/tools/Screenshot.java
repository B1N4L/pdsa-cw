import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;

// Renders the AMOffline window (and the New appointment form) to PNG files with sample data, for the website.
// Run with the compiled classes on the class path and -Dams.data pointing at an empty folder:
//   java -cp <classes> -Dams.data=<empty folder> packaging/tools/Screenshot.java <output folder>
public class Screenshot {

    // id, name, subject, description, priority, status, created, days from today, time
    private static final String[][] SAMPLE = {
            {"101", "Nimal Perera", "Contract review", "Bring the signed supplier contract and the two quotes.", "1", "1", "2026-10-14T03:10:00Z", "0", "16:30"},
            {"102", "Ayesha Fernando", "Q4 budget meeting", "Finance team, meeting room 2. Projector needed.", "2", "1", "2026-10-13T05:20:00Z", "1", "10:00"},
            {"103", "Kasun & Dilani", "Interview: front desk role", "Two candidates, 30 minutes each.", "2", "1", "2026-10-15T08:45:00Z", "2", "09:30"},
            {"104", "Mr. Jayasinghe", "Site visit follow-up", "", "3", "2", "2026-10-09T09:00:00Z", "-2", "11:00"},
            {"105", "Sarah Thomas", "Supplier call", "Confirm delivery dates for November.", "1", "1", "2026-10-16T02:30:00Z", "0", "17:15"},
            {"106", "IT support", "New laptop setup", "", "3", "1", "2026-10-12T04:15:00Z", "3", "13:00"},
            {"107", "R. Gunawardena", "Tax documents", "Rescheduled twice. Call before booking again.", "2", "3", "2026-10-08T06:00:00Z", "-1", "14:00"},
            {"108", "Board members", "Monthly board meeting", "", "1", "2", "2026-10-02T03:00:00Z", "0", "09:00"},
            {"109", "Priya Raman", "Onboarding session", "Prepare welcome pack and access card.", "3", "1", "2026-10-16T05:05:00Z", "-1", "10:00"},
    };

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : ".");
        out.mkdirs();
        File data = new File(System.getProperty("ams.data"));
        data.mkdirs();
        try (FileWriter writer = new FileWriter(new File(data, "appointment-list.txt"), StandardCharsets.UTF_8)) {
            for (String[] a : Boolean.getBoolean("shot.empty") ? new String[0][] : SAMPLE) {
                String scheduled = java.time.LocalDate.now().plusDays(Integer.parseInt(a[7])) + "T" + a[8];
                writer.write("ID: " + a[0] + "\nName: " + a[1] + "\nSubject: " + a[2] + "\nDescription: " + a[3]
                        + "\nPriority: " + a[4] + "\nStatus: " + a[5] + "\nScheduled: " + scheduled + "\nDate Created: " + a[6] + "\n\n");
            }
        }

        Class<?> kit = Class.forName("ams.UiKit");
        kit.getMethod("setupLookAndFeel").invoke(null);
        int width = Integer.getInteger("shot.width", 1220);
        int height = Integer.getInteger("shot.height", 780);

        SwingUtilities.invokeAndWait(() -> {
            try {
                JFrame frame = (JFrame) Class.forName("ams.AMSWindow").getConstructor().newInstance();
                frame.setSize(width, height);
                hidePath(frame.getContentPane());
                frame.addNotify();
                frame.validate();
                write(frame.getRootPane(), new File(out, "app-main.png"));

                // the New appointment form, filled in
                Class<?> dialogClass = Class.forName("ams.AppointmentDialog");
                Class<?> listClass = Class.forName("ams.LinkedList");
                Constructor<?> make = dialogClass.getDeclaredConstructor(Frame.class, listClass, Class.forName("ams.Appointment"), String.class);
                make.setAccessible(true);
                Object list = listClass.getConstructor().newInstance();
                JDialog dialog = (JDialog) make.newInstance(frame, list, null, "110");
                fill(dialog.getContentPane(), new String[]{"Dr. Mendis", "Quarterly health check"});
                java.lang.reflect.Field picker = dialogClass.getDeclaredField("datePicker");
                picker.setAccessible(true);
                picker.get(dialog).getClass().getMethod("setDate", java.time.LocalDate.class)
                        .invoke(picker.get(dialog), java.time.LocalDate.now().plusDays(1));
                java.lang.reflect.Field time = dialogClass.getDeclaredField("timeBox");
                time.setAccessible(true);
                ((JComboBox<?>) time.get(dialog)).setSelectedItem("10:30");
                dialog.addNotify();
                dialog.validate();
                write(dialog.getRootPane(), new File(out, "app-form.png"));
                dialog.dispose();
                frame.dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.exit(0);
    }

    // Show a generic data path in the status bar instead of this computer's real one
    private static void hidePath(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null && ((JLabel) c).getText().startsWith("Data file:")) {
                ((JLabel) c).setText("Data file: C:\\Users\\you\\AMS\\appointment-list.txt");
            } else if (c instanceof Container) {
                hidePath((Container) c);
            }
        }
    }

    // Put sample text into the empty text fields of the form, in order
    private static int fill(Container container, String[] values) {
        int used = 0;
        for (Component c : container.getComponents()) {
            if (c instanceof JTextField && ((JTextField) c).getText().isEmpty() && used < values.length) {
                ((JTextField) c).setText(values[used++]);
            } else if (c instanceof Container) {
                String[] rest = new String[values.length - used];
                System.arraycopy(values, used, rest, 0, rest.length);
                used += fill((Container) c, rest);
            }
        }
        return used;
    }

    private static void write(JComponent component, File file) throws Exception {
        double scale = Double.parseDouble(System.getProperty("shot.scale", "2"));
        BufferedImage image = new BufferedImage((int) (component.getWidth() * scale), (int) (component.getHeight() * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        component.paint(g);
        g.dispose();
        javax.imageio.ImageIO.write(image, "png", file);
        System.out.println("Wrote " + file);
    }
}
