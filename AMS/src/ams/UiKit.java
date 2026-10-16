package ams;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Colours, fonts, light/dark themes and small custom components shared by the window and the dialogs
public class UiKit {

    // ---------- colours ----------

    // Every colour has a light and a dark value: {light, dark}. The order matches usePalette() below.
    private static final int[][] TONES = {
            {0xF4F5F7, 0x0E1116}, // BG            window background
            {0xFFFFFF, 0x161B22}, // SURFACE       cards, table, dialogs
            {0xFAFBFC, 0x1B212A}, // HEADER_BG     table heading
            {0xE2E5EA, 0x2A313C}, // BORDER
            {0xEEF0F3, 0x20262F}, // GRID          lines between table rows
            {0x1D2433, 0xE6E9EF}, // TEXT
            {0x667085, 0x9AA4B2}, // MUTED         secondary text
            {0x98A2B3, 0x6E7887}, // PLACEHOLDER
            {0xB0B7C3, 0x4A5361}, // FAINT         days outside the month
            {0x2563EB, 0x2563EB}, // ACCENT        primary buttons, focus
            {0x1D4ED8, 0x3B74F0}, // ACCENT_HOVER
            {0x1E40AF, 0x1D4ED8}, // ACCENT_PRESSED
            {0xA9C1F5, 0x2A3A5C}, // ACCENT_DISABLED
            {0xE8F0FE, 0x1C2B45}, // ACCENT_SOFT   selected row
            {0x1D4ED8, 0x8FB4FF}, // ACCENT_TEXT   links
            {0xBFD5FF, 0x264F8F}, // SELECTION     selected text
            {0xF5F8FF, 0x172338}, // STAT_ACTIVE   chosen summary card
            {0xFFFFFF, 0xFFFFFF}, // ON_ACCENT     text on primary buttons
            {0xC8322B, 0xF97066}, // DANGER
            {0xFDECEA, 0x3A1D1D}, // DANGER_SOFT
            {0xF0B8B3, 0x6E2B26}, // DANGER_LINE
            {0xF9D3CF, 0x4A2220}, // DANGER_PRESSED
            {0xF2F4F7, 0x222934}, // HOVER
            {0xE4E7EC, 0x2A3240}, // PRESSED
            {0xB54708, 0xFDB022}, // WARNING
            {0xC4320A, 0xFF8A4C}, // OVERDUE
            {0x6941C6, 0xB692F6}, // PURPLE        "Today" card
            {0xB42318, 0xFDA29B}, {0xFEE4E2, 0x4A1D1A}, // urgent badge: text, background
            {0xB54708, 0xFEC84B}, {0xFEF0C7, 0x43300A}, // medium
            {0x344054, 0xD0D5DD}, {0xEAECF0, 0x2E3540}, // low
            {0x175CD3, 0x84CAFF}, {0xD1E9FF, 0x14304F}, // due
            {0x067647, 0x75E0A7}, {0xDCFAE6, 0x0F3624}, // completed
            {0x5D6B82, 0xA4ADBA}, {0xF2F4F7, 0x262C36}, // missed
    };
    private static final Color[] LIGHT = palette(0);
    private static final Color[] DARK = palette(1);

    // The current theme's colours (swapped by applyTheme)
    public static Color BG, SURFACE, HEADER_BG, BORDER, GRID, TEXT, MUTED, PLACEHOLDER, FAINT,
            ACCENT, ACCENT_HOVER, ACCENT_PRESSED, ACCENT_DISABLED, ACCENT_SOFT, ACCENT_TEXT, SELECTION, STAT_ACTIVE, ON_ACCENT,
            DANGER, DANGER_SOFT, DANGER_LINE, DANGER_PRESSED, HOVER, PRESSED, WARNING, OVERDUE, PURPLE;
    private static Color[] urgent, medium, low, due, completed, missed;

    private static boolean dark = false;
    private static boolean installed = false;
    private static final List<Runnable> themeListeners = new ArrayList<>();
    private static Thread themeWatcher;

    static {
        usePalette(LIGHT);
    }

    private static Color[] palette(int column) {
        Color[] colors = new Color[TONES.length];
        for (int i = 0; i < TONES.length; i++) {
            colors[i] = new Color(TONES[i][column]);
        }
        return colors;
    }

    private static void usePalette(Color[] p) {
        int i = 0;
        BG = p[i++]; SURFACE = p[i++]; HEADER_BG = p[i++]; BORDER = p[i++]; GRID = p[i++]; TEXT = p[i++];
        MUTED = p[i++]; PLACEHOLDER = p[i++]; FAINT = p[i++]; ACCENT = p[i++]; ACCENT_HOVER = p[i++];
        ACCENT_PRESSED = p[i++]; ACCENT_DISABLED = p[i++]; ACCENT_SOFT = p[i++]; ACCENT_TEXT = p[i++];
        SELECTION = p[i++]; STAT_ACTIVE = p[i++]; ON_ACCENT = p[i++]; DANGER = p[i++]; DANGER_SOFT = p[i++];
        DANGER_LINE = p[i++]; DANGER_PRESSED = p[i++]; HOVER = p[i++]; PRESSED = p[i++]; WARNING = p[i++];
        OVERDUE = p[i++]; PURPLE = p[i++];
        urgent = new Color[]{p[i++], p[i++]};
        medium = new Color[]{p[i++], p[i++]};
        low = new Color[]{p[i++], p[i++]};
        due = new Color[]{p[i++], p[i++]};
        completed = new Color[]{p[i++], p[i++]};
        missed = new Color[]{p[i++], p[i]};
    }

    // Text and background colours for a priority code (1 urgent, 2 medium, 3 low)
    public static Color[] priorityColors(int priority) {
        return priority == 1 ? urgent : priority == 2 ? medium : low;
    }

    // Text and background colours for a status code (1 due, 2 completed, 3 missed)
    public static Color[] statusColors(String status) {
        return status.equals("1") ? due : status.equals("2") ? completed : missed;
    }

    // Outline drawn around the focused control
    public static Color focusRing() {
        return dark ? new Color(0x8F, 0xB4, 0xFF, 170) : new Color(37, 99, 235, 140);
    }

    // CSS colour for HTML text, e.g. "#1D4ED8"
    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---------- themes ----------

    public static boolean isDark() {
        return dark;
    }

    // "system", "light" or "dark": -Dams.theme=... wins, otherwise the saved choice (default: follow the system)
    public static String themeMode() {
        String forced = System.getProperty("ams.theme");
        String mode = forced != null ? forced : Settings.get("theme", "system");
        return mode.equals("light") || mode.equals("dark") ? mode : "system";
    }

    // Save the user's choice and switch to it
    public static void setThemeMode(String mode) {
        Settings.set("theme", mode);
        applyTheme(resolveDark(mode));
    }

    private static boolean resolveDark(String mode) {
        return mode.equals("dark") || (mode.equals("system") && systemPrefersDark());
    }

    // Call once before any window is made
    public static void setupLookAndFeel() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        applyTheme(resolveDark(themeMode()));
    }

    // Run something after every theme switch (e.g. redraw the table)
    public static void onThemeChange(Runnable listener) {
        themeListeners.add(listener);
    }

    // Switch every open window to the light or dark colours
    public static void applyTheme(boolean toDark) {
        if (installed && toDark == dark) {
            return;
        }
        // map each old colour object to its new one, to recolour components that were given palette colours
        Color[] from = dark ? DARK : LIGHT;
        Color[] to = toDark ? DARK : LIGHT;
        IdentityHashMap<Color, Color> swap = new IdentityHashMap<>();
        for (int i = 0; i < from.length; i++) {
            swap.put(from[i], to[i]);
        }
        dark = toDark;
        usePalette(to);
        installLookAndFeel();
        installed = true;
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            recolor(window, swap);
            window.repaint();
        }
        for (Runnable listener : themeListeners) {
            listener.run();
        }
    }

    // Swing's cross-platform look, coloured with the current palette (the same on Windows, Linux and macOS)
    private static void installLookAndFeel() {
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            MetalLookAndFeel.setCurrentTheme(new AmsMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            // keep whatever look is installed
        }
        UIManager.put("ComboBoxUI", FlatComboBoxUI.class.getName());
        UIManager.put("ScrollBarUI", FlatScrollBarUI.class.getName());
        Font base = font(Font.PLAIN, 13);
        for (String key : new String[]{"Label.font", "TextField.font", "TextArea.font", "ComboBox.font", "Table.font",
                "Menu.font", "MenuItem.font", "MenuBar.font", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "Button.font", "ToggleButton.font", "List.font",
                "ToolTip.font", "FileChooser.listFont"}) {
            UIManager.put(key, new FontUIResource(base));
        }
        UIManager.put("Panel.background", new ColorUIResource(SURFACE));
        UIManager.put("OptionPane.background", new ColorUIResource(SURFACE));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("ToolTip.background", new ColorUIResource(SURFACE));
        UIManager.put("ToolTip.foreground", new ColorUIResource(TEXT));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(4, 8, 4, 8)));
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("MenuBar.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.background", new ColorUIResource(SURFACE));
        UIManager.put("Viewport.background", new ColorUIResource(SURFACE));
    }

    // Give components the new colour wherever they were set to an old palette colour
    private static void recolor(Component c, IdentityHashMap<Color, Color> swap) {
        if (c.isBackgroundSet() && swap.containsKey(c.getBackground())) {
            c.setBackground(swap.get(c.getBackground()));
        }
        if (c.isForegroundSet() && swap.containsKey(c.getForeground())) {
            c.setForeground(swap.get(c.getForeground()));
        }
        if (c instanceof JTextComponent) {
            JTextComponent t = (JTextComponent) c;
            if (swap.containsKey(t.getCaretColor())) t.setCaretColor(swap.get(t.getCaretColor()));
            if (swap.containsKey(t.getSelectionColor())) t.setSelectionColor(swap.get(t.getSelectionColor()));
            if (swap.containsKey(t.getSelectedTextColor())) t.setSelectedTextColor(swap.get(t.getSelectedTextColor()));
            if (swap.containsKey(t.getDisabledTextColor())) t.setDisabledTextColor(swap.get(t.getDisabledTextColor()));
        }
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            if (swap.containsKey(table.getGridColor())) table.setGridColor(swap.get(table.getGridColor()));
        }
        if (c instanceof DatePicker) {
            JPopupMenu popup = ((DatePicker) c).popup;
            SwingUtilities.updateComponentTreeUI(popup);
            recolor(popup, swap);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                recolor(child, swap);
            }
        }
    }

    // Check the operating system's light/dark setting every few seconds while "System" is chosen
    public static void startThemeWatcher() {
        if (themeWatcher != null) {
            return;
        }
        themeWatcher = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
                if (themeMode().equals("system")) {
                    boolean wantDark = systemPrefersDark();
                    if (wantDark != dark) {
                        SwingUtilities.invokeLater(() -> {
                            if (themeMode().equals("system")) {
                                applyTheme(wantDark);
                            }
                        });
                    }
                }
            }
        }, "ams-theme-watcher");
        themeWatcher.setDaemon(true);
        themeWatcher.start();
    }

    // Is the computer set to dark mode? Reads Windows' app mode, macOS' appearance, or GNOME/KDE settings.
    public static boolean systemPrefersDark() {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                String out = run("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme");
                return Pattern.compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x0\\b").matcher(out).find();
            }
            if (os.contains("mac")) {
                return run("defaults", "read", "-g", "AppleInterfaceStyle").trim().equalsIgnoreCase("dark");
            }
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                return true;
            }
            String desktop = String.valueOf(System.getenv("XDG_CURRENT_DESKTOP")).toLowerCase();
            if (desktop.contains("kde")) {
                return kdePrefersDark();
            }
            String scheme = run("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
            if (scheme.contains("prefer-dark")) {
                return true;
            }
            if (scheme.contains("prefer-light")) {
                return false;
            }
            return run("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme").toLowerCase().contains("dark");
        } catch (Exception e) {
            return false;
        }
    }

    // KDE keeps its colour scheme in ~/.config/kdeglobals
    private static boolean kdePrefersDark() throws Exception {
        File file = new File(System.getProperty("user.home"), ".config/kdeglobals");
        if (!file.exists()) {
            return false;
        }
        String section = "";
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.startsWith("[")) {
                section = line;
            } else if (section.equals("[General]") && line.startsWith("ColorScheme=")) {
                if (line.toLowerCase().contains("dark")) {
                    return true;
                }
            } else if (section.equals("[Colors:Window]") && line.startsWith("BackgroundNormal=")) {
                String[] rgb = line.substring(line.indexOf('=') + 1).split(",");
                if (rgb.length >= 3) {
                    double brightness = 0.299 * Integer.parseInt(rgb[0].trim()) + 0.587 * Integer.parseInt(rgb[1].trim())
                            + 0.114 * Integer.parseInt(rgb[2].trim());
                    return brightness < 128;
                }
            }
        }
        return false;
    }

    // Run a small command and return what it printed ("" if it did not finish in 3 seconds)
    private static String run(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return "";
        }
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // Colours for Swing's own parts (menus, message boxes, file chooser) in the current theme
    private static class AmsMetalTheme extends DefaultMetalTheme {
        private static ColorUIResource res(Color c) { return new ColorUIResource(c); }
        private static FontUIResource fontRes(int style) { return new FontUIResource(font(style, 13)); }

        @Override public String getName() { return "AMS"; }
        @Override protected ColorUIResource getPrimary1() { return res(ACCENT_HOVER); }
        @Override protected ColorUIResource getPrimary2() { return res(ACCENT); }
        @Override protected ColorUIResource getPrimary3() { return res(SELECTION); }
        @Override protected ColorUIResource getSecondary1() { return res(MUTED); }
        @Override protected ColorUIResource getSecondary2() { return res(BORDER); }
        @Override protected ColorUIResource getSecondary3() { return res(SURFACE); }
        @Override protected ColorUIResource getBlack() { return res(TEXT); }
        @Override protected ColorUIResource getWhite() { return res(SURFACE); }
        @Override public ColorUIResource getFocusColor() { return res(ACCENT); }
        @Override public ColorUIResource getMenuBackground() { return res(SURFACE); }
        @Override public ColorUIResource getMenuSelectedBackground() { return res(ACCENT_SOFT); }
        @Override public ColorUIResource getMenuSelectedForeground() { return res(TEXT); }
        @Override public ColorUIResource getMenuDisabledForeground() { return res(PLACEHOLDER); }
        @Override public ColorUIResource getInactiveControlTextColor() { return res(PLACEHOLDER); }
        @Override public ColorUIResource getInactiveSystemTextColor() { return res(PLACEHOLDER); }
        @Override public ColorUIResource getAcceleratorForeground() { return res(MUTED); }
        @Override public ColorUIResource getAcceleratorSelectedForeground() { return res(MUTED); }
        @Override public ColorUIResource getSeparatorForeground() { return res(BORDER); }
        @Override public ColorUIResource getSeparatorBackground() { return res(SURFACE); }
        @Override public ColorUIResource getHighlightedTextColor() { return res(TEXT); }
        @Override public ColorUIResource getTextHighlightColor() { return res(SELECTION); }
        @Override public FontUIResource getControlTextFont() { return fontRes(Font.PLAIN); }
        @Override public FontUIResource getSystemTextFont() { return fontRes(Font.PLAIN); }
        @Override public FontUIResource getUserTextFont() { return fontRes(Font.PLAIN); }
        @Override public FontUIResource getMenuTextFont() { return fontRes(Font.PLAIN); }
        @Override public FontUIResource getWindowTitleFont() { return fontRes(Font.BOLD); }
        @Override public FontUIResource getSubTextFont() { return new FontUIResource(font(Font.PLAIN, 12)); }
    }

    // ---------- fonts and text ----------

    private static String fontFamily;

    // Pick a clean font that exists on this computer (Windows, Linux desktops, macOS)
    public static Font font(int style, float size) {
        if (fontFamily == null) {
            List<String> installedFonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            fontFamily = Font.SANS_SERIF;
            for (String name : new String[]{"Segoe UI", "Inter", "SF Pro Text", "Cantarell", "Ubuntu", "Noto Sans", "DejaVu Sans"}) {
                if (installedFonts.contains(name)) {
                    fontFamily = name;
                    break;
                }
            }
        }
        return new Font(fontFamily, style, 1).deriveFont(size);
    }

    // "urgent" -> "Urgent"
    public static String cap(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // Make user text safe to show inside an HTML label
    public static String esc(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Show the stored UTC creation time in the computer's own time zone
    public static String formatCreated(String dateCreated, String pattern) {
        try {
            return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(Instant.parse(dateCreated));
        } catch (Exception e) {
            return dateCreated;
        }
    }

    // Read a time typed by the user: "14:30", "9.15", "930", "2:30 pm", "9am" (null if it isn't a valid time)
    public static LocalTime parseTime(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile("^(\\d{1,2})(?:[:.]?(\\d{2}))?\\s*([ap])?\\.?\\s*(?:m\\.?)?$", Pattern.CASE_INSENSITIVE)
                .matcher(text.trim());
        if (!m.matches()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        if (m.group(3) != null) {
            if (hour < 1 || hour > 12) {
                return null;
            }
            boolean pm = m.group(3).equalsIgnoreCase("p");
            hour = hour % 12 + (pm ? 12 : 0);
        }
        if (hour > 23 || minute > 59) {
            return null;
        }
        return LocalTime.of(hour, minute);
    }

    // ---------- icon ----------

    // The app icon: a blue rounded square with a white calendar page (the same in both themes)
    public static BufferedImage appIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float s = size;
        g.setPaint(new GradientPaint(0, 0, new Color(0x3B82F6), 0, s, new Color(0x1D4ED8)));
        g.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.44f, s * 0.44f));
        // calendar page
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Float(s * 0.2f, s * 0.26f, s * 0.6f, s * 0.54f, s * 0.12f, s * 0.12f));
        g.setColor(new Color(0xBFD5FF));
        g.fill(new RoundRectangle2D.Float(s * 0.2f, s * 0.26f, s * 0.6f, s * 0.14f, s * 0.12f, s * 0.12f));
        g.fillRect(Math.round(s * 0.2f), Math.round(s * 0.33f), Math.round(s * 0.6f), Math.round(s * 0.07f));
        // binder rings
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Float(s * 0.33f, s * 0.17f, s * 0.07f, s * 0.16f, s * 0.07f, s * 0.07f));
        g.fill(new RoundRectangle2D.Float(s * 0.6f, s * 0.17f, s * 0.07f, s * 0.16f, s * 0.07f, s * 0.07f));
        // tick
        g.setColor(new Color(0x2563EB));
        g.setStroke(new BasicStroke(Math.max(1.5f, s * 0.075f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[]{Math.round(s * 0.36f), Math.round(s * 0.46f), Math.round(s * 0.65f)},
                new int[]{Math.round(s * 0.58f), Math.round(s * 0.68f), Math.round(s * 0.49f)}, 3);
        g.dispose();
        return image;
    }

    public static List<Image> appIcons() {
        return Arrays.asList(appIcon(16), appIcon(24), appIcon(32), appIcon(48), appIcon(64), appIcon(128), appIcon(256));
    }

    // ---------- components ----------

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    // A label with a set font and colour
    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    // A one-pixel line under a panel, in the current border colour
    public static AbstractBorder bottomLine(int top, int left, int bottom, int right) {
        return new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(BORDER);
                g.fillRect(x, y + h - 1, w, 1);
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(top, left, bottom + 1, right);
            }
        };
    }

    // Flat rounded button in a few styles
    public static class FlatButton extends JButton {
        public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST }

        private final Kind kind;

        public FlatButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(font(kind == Kind.PRIMARY ? Font.BOLD : Font.PLAIN, 13));
            setForeground(kind == Kind.PRIMARY ? ON_ACCENT : kind == Kind.DANGER ? DANGER : TEXT);
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            ButtonModel model = getModel();
            boolean hover = model.isRollover() && isEnabled();
            boolean down = model.isPressed();
            Color fill = null;
            Color line = null;
            switch (kind) {
                case PRIMARY:
                    fill = !isEnabled() ? ACCENT_DISABLED : down ? ACCENT_PRESSED : hover ? ACCENT_HOVER : ACCENT;
                    break;
                case SECONDARY:
                    fill = down ? PRESSED : hover ? HOVER : SURFACE;
                    line = BORDER;
                    break;
                case DANGER:
                    fill = down ? DANGER_PRESSED : hover ? DANGER_SOFT : SURFACE;
                    line = DANGER_LINE;
                    break;
                case GHOST:
                    fill = down ? PRESSED : hover ? HOVER : null;
                    break;
            }
            int w = getWidth();
            int h = getHeight();
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            }
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            }
            if (isFocusOwner()) {
                g2.setColor(focusRing());
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Text field with rounded corners, a placeholder and an optional search icon
    public static class RoundField extends JTextField {
        private final String placeholder;
        private final boolean searchIcon;

        public RoundField(String placeholder, boolean searchIcon) {
            this.placeholder = placeholder;
            this.searchIcon = searchIcon;
            setOpaque(false);
            setFont(font(Font.PLAIN, 13));
            setForeground(TEXT);
            setBorder(new EmptyBorder(8, searchIcon ? 34 : 10, 8, 10));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        // these follow the current theme
        @Override public Color getCaretColor() { return TEXT; }
        @Override public Color getSelectionColor() { return SELECTION; }
        @Override public Color getSelectedTextColor() { return TEXT; }
        @Override public Color getDisabledTextColor() { return MUTED; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(isEnabled() ? SURFACE : HOVER);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2.setColor(isFocusOwner() ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            if (searchIcon) {
                g2.setColor(MUTED);
                g2.setStroke(new BasicStroke(1.6f));
                int cy = h / 2;
                g2.drawOval(13, cy - 7, 11, 11);
                g2.drawLine(22, cy + 2, 26, cy + 6);
            }
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null) {
                Graphics2D g3 = smooth(g);
                g3.setColor(PLACEHOLDER);
                g3.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g3.getFontMetrics();
                g3.drawString(placeholder, in.left, (h - fm.getHeight()) / 2 + fm.getAscent());
                g3.dispose();
            }
        }
    }

    // Panel with rounded corners and a thin border
    public static class RoundPanel extends JPanel {
        public RoundPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBackground(SURFACE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
        }
    }

    // Small coloured badge, e.g. "Urgent" or "Due"
    public static class Pill extends JComponent {
        private String text = "";
        private Color fg = TEXT;
        private Color bg = HOVER;
        private Color rowBackground;
        private int x = 0;

        public Pill() {
            setFont(font(Font.BOLD, 12));
        }

        public void set(String text, Color[] colors) {
            this.text = text;
            this.fg = colors[0];
            this.bg = colors[1];
            repaint();
        }

        // Used inside table cells: fill the row colour and indent the badge
        public void setRow(Color rowBackground, int x) {
            this.rowBackground = rowBackground;
            this.x = x;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(x + fm.stringWidth(text) + 20, 24);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            if (rowBackground != null) {
                g2.setColor(rowBackground);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            FontMetrics fm = g2.getFontMetrics(getFont());
            int w = fm.stringWidth(text) + 20;
            int h = 22;
            int y = (getHeight() - h) / 2;
            g2.setColor(bg);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(fg);
            g2.setFont(getFont());
            g2.drawString(text, x + 10, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // A row of toggle buttons where one option is chosen (like a segmented control).
    // colors gives the {text, background} colours for option i, looked up each time it is drawn.
    public static class Segmented extends JPanel {
        private final JToggleButton[] buttons;

        public Segmented(String[] options, IntFunction<Color[]> colors) {
            super(new GridLayout(1, options.length, 6, 0));
            setOpaque(false);
            ButtonGroup group = new ButtonGroup();
            buttons = new JToggleButton[options.length];
            for (int i = 0; i < options.length; i++) {
                int index = i;
                JToggleButton button = new JToggleButton(options[i]) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Color[] c = colors.apply(index);
                        Graphics2D g2 = smooth(g);
                        boolean on = isSelected();
                        g2.setColor(on ? c[1] : getModel().isRollover() ? HOVER : SURFACE);
                        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                        g2.setColor(on ? c[0] : BORDER);
                        g2.setStroke(new BasicStroke(on ? 1.6f : 1f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                        if (isFocusOwner()) {
                            g2.setColor(focusRing());
                            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
                        }
                        g2.dispose();
                        Color fg = on ? c[0] : TEXT;
                        if (!fg.equals(getForeground())) {
                            setForeground(fg);
                        }
                        super.paintComponent(g);
                    }
                };
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setOpaque(false);
                button.setRolloverEnabled(true);
                button.setFont(font(Font.BOLD, 13));
                button.setBorder(new EmptyBorder(8, 10, 8, 10));
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                group.add(button);
                buttons[i] = button;
                add(button);
            }
        }

        public int getSelectedIndex() {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].isSelected()) {
                    return i;
                }
            }
            return 0;
        }

        public void setSelectedIndex(int index) {
            buttons[index].setSelected(true);
        }
    }

    // A date field that opens a month calendar when clicked; fires a "date" property change
    public static class DatePicker extends JButton {
        private final String placeholder;
        private final JPopupMenu popup = new JPopupMenu();
        private final JLabel monthLabel = label("", Font.BOLD, 14, TEXT);
        private final DayButton[] days = new DayButton[42];
        private LocalDate date;
        private YearMonth shown = YearMonth.now();

        public DatePicker(String placeholder) {
            this.placeholder = placeholder;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(font(Font.PLAIN, 13));
            setBorder(new EmptyBorder(8, 10, 8, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Pick a date (Delete clears it)");
            addActionListener(e -> openCalendar());
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "clear");
            getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"), "clear");
            getActionMap().put("clear", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { setDate(null); }
            });
            buildCalendar();
            updateText();
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate newDate) {
            LocalDate old = date;
            date = newDate;
            updateText();
            firePropertyChange("date", old, newDate);
        }

        private void updateText() {
            setText(date == null ? placeholder : date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")));
        }

        // placeholder or date colour, following the theme
        @Override
        public Color getForeground() {
            return date == null ? PLACEHOLDER : TEXT;
        }

        private void buildCalendar() {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.setBackground(SURFACE);
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));

            FlatButton prev = new FlatButton("‹", FlatButton.Kind.GHOST);
            FlatButton next = new FlatButton("›", FlatButton.Kind.GHOST);
            prev.setFont(font(Font.BOLD, 18));
            next.setFont(font(Font.BOLD, 18));
            prev.setToolTipText("Previous month");
            next.setToolTipText("Next month");
            prev.addActionListener(e -> { shown = shown.minusMonths(1); updateDays(); });
            next.addActionListener(e -> { shown = shown.plusMonths(1); updateDays(); });
            monthLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(prev, BorderLayout.WEST);
            head.add(monthLabel, BorderLayout.CENTER);
            head.add(next, BorderLayout.EAST);
            panel.add(head, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
            grid.setOpaque(false);
            for (String name : new String[]{"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"}) {
                JLabel l = label(name, Font.BOLD, 11, MUTED);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                grid.add(l);
            }
            for (int i = 0; i < days.length; i++) {
                days[i] = new DayButton();
                grid.add(days[i]);
            }
            panel.add(grid, BorderLayout.CENTER);

            FlatButton today = new FlatButton("Today", FlatButton.Kind.SECONDARY);
            FlatButton tomorrow = new FlatButton("Tomorrow", FlatButton.Kind.SECONDARY);
            FlatButton clear = new FlatButton("Clear", FlatButton.Kind.GHOST);
            today.addActionListener(e -> choose(LocalDate.now()));
            tomorrow.addActionListener(e -> choose(LocalDate.now().plusDays(1)));
            clear.addActionListener(e -> choose(null));
            JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            foot.setOpaque(false);
            foot.add(today);
            foot.add(tomorrow);
            foot.add(clear);
            panel.add(foot, BorderLayout.SOUTH);

            popup.setBorder(new AbstractBorder() {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                    g.setColor(BORDER);
                    g.drawRect(x, y, w - 1, h - 1);
                }

                @Override
                public Insets getBorderInsets(Component c) {
                    return new Insets(1, 1, 1, 1);
                }
            });
            popup.add(panel);
        }

        private void openCalendar() {
            shown = YearMonth.from(date != null ? date : LocalDate.now());
            updateDays();
            popup.show(this, 0, getHeight() + 4);
            for (DayButton day : days) {
                if (day.day.equals(date != null ? date : LocalDate.now())) {
                    day.requestFocusInWindow();
                }
            }
        }

        private void choose(LocalDate chosen) {
            popup.setVisible(false);
            setDate(chosen);
            requestFocusInWindow();
        }

        private void updateDays() {
            monthLabel.setText(shown.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            LocalDate first = shown.atDay(1);
            LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1);
            for (int i = 0; i < days.length; i++) {
                days[i].setDay(start.plusDays(i));
            }
            popup.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(SURFACE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2.setColor(isFocusOwner() ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            // calendar icon on the right
            int x = w - 26;
            int y = h / 2 - 7;
            g2.setColor(MUTED);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(x, y + 1, 14, 13, 3, 3);
            g2.drawLine(x, y + 5, x + 14, y + 5);
            g2.drawLine(x + 4, y - 1, x + 4, y + 2);
            g2.drawLine(x + 10, y - 1, x + 10, y + 2);
            g2.dispose();
            super.paintComponent(g);
        }

        // One day in the calendar grid
        private class DayButton extends JButton {
            LocalDate day = LocalDate.now();

            DayButton() {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setRolloverEnabled(true);
                setMargin(new Insets(0, 0, 0, 0));
                setBorder(new EmptyBorder(0, 0, 0, 0));
                setPreferredSize(new Dimension(38, 32));
                setFont(font(Font.PLAIN, 13));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addActionListener(e -> choose(day));
            }

            void setDay(LocalDate day) {
                this.day = day;
                setText(String.valueOf(day.getDayOfMonth()));
                getAccessibleContext().setAccessibleName(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")));
                repaint();
            }

            // selected, other-month and normal days, following the theme
            @Override
            public Color getForeground() {
                if (day == null) {
                    return TEXT;
                }
                return day.equals(date) ? ON_ACCENT : !YearMonth.from(day).equals(shown) ? FAINT : TEXT;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                boolean selected = day.equals(date);
                boolean today = day.equals(LocalDate.now());
                int w = getWidth();
                int h = getHeight();
                if (selected) {
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
                } else if (getModel().isRollover() || isFocusOwner()) {
                    g2.setColor(HOVER);
                    g2.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
                }
                if (today && !selected) {
                    g2.setColor(ACCENT);
                    g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
                }
                g2.dispose();
                int style = today || selected ? Font.BOLD : Font.PLAIN;
                if (getFont().getStyle() != style) {
                    setFont(font(style, 13));
                }
                super.paintComponent(g);
            }
        }
    }

    // Thin rounded outline, used around the description box in the form
    public static class RoundBorder extends AbstractBorder {
        private final Insets padding;

        public RoundBorder(Insets padding) {
            this.padding = padding;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = smooth(g);
            boolean focused = c instanceof JScrollPane && ((JScrollPane) c).getViewport().getView() != null
                    && ((JScrollPane) c).getViewport().getView().isFocusOwner();
            g2.setColor(focused ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return padding;
        }
    }

    // ---------- Swing parts restyled for both themes ----------

    // Drop-down lists: rounded box, chevron, themed list (used for every JComboBox in the app)
    public static class FlatComboBoxUI extends BasicComboBoxUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatComboBoxUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            comboBox.setOpaque(false);
            comboBox.setBackground(new ColorUIResource(SURFACE));
            comboBox.setForeground(new ColorUIResource(TEXT));
            comboBox.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(0, 6, 0, 2));
        }

        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    g2.setColor(MUTED);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    g2.dispose();
                }
            };
            arrow.setName("ComboBox.arrowButton");
            arrow.setOpaque(false);
            arrow.setContentAreaFilled(false);
            arrow.setBorder(new EmptyBorder(0, 0, 0, 0));
            arrow.setFocusable(false);
            arrow.setPreferredSize(new Dimension(28, 28));
            return arrow;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = smooth(g);
            boolean focused = comboBox.isFocusOwner() || (comboBox.isEditable() && comboBox.getEditor().getEditorComponent().isFocusOwner());
            g2.setColor(comboBox.isEnabled() ? SURFACE : HOVER);
            g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
            g2.setColor(focused ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
            g2.dispose();
            if (!comboBox.isEditable()) {
                paintCurrentValue(g, rectangleForCurrentValue(), false);
            }
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            ListCellRenderer<? super Object> renderer = comboBox.getRenderer();
            Component value = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
            value.setFont(comboBox.getFont());
            value.setForeground(comboBox.isEnabled() ? TEXT : PLACEHOLDER);
            if (value instanceof JComponent) {
                ((JComponent) value).setOpaque(false);
            }
            currentValuePane.paintComponent(g, value, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
        }

        @Override
        protected ListCellRenderer<Object> createRenderer() {
            return new FlatListRenderer();
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override
                protected void configurePopup() {
                    super.configurePopup();
                    setBorder(BorderFactory.createLineBorder(BORDER));
                }

                @Override
                protected void configureList() {
                    super.configureList();
                    list.setBackground(SURFACE);
                    list.setForeground(TEXT);
                    list.setSelectionBackground(ACCENT_SOFT);
                    list.setSelectionForeground(TEXT);
                }
            };
            return popup;
        }

        @Override
        protected ComboBoxEditor createEditor() {
            return new BasicComboBoxEditor.UIResource() {
                @Override
                protected JTextField createEditorComponent() {
                    JTextField field = new JTextField("", 9) {
                        @Override public Color getForeground() { return TEXT; }
                        @Override public Color getCaretColor() { return TEXT; }
                        @Override public Color getSelectionColor() { return SELECTION; }
                        @Override public Color getSelectedTextColor() { return TEXT; }
                    };
                    field.setOpaque(false);
                    field.setBorder(new EmptyBorder(0, 4, 0, 0));
                    field.setFont(font(Font.PLAIN, 13));
                    field.addFocusListener(new FocusAdapter() {
                        @Override public void focusGained(FocusEvent e) { comboBox.repaint(); }
                        @Override public void focusLost(FocusEvent e) { comboBox.repaint(); }
                    });
                    return field;
                }
            };
        }

        @Override
        protected FocusListener createFocusListener() {
            FocusListener base = super.createFocusListener();
            return new FocusListener() {
                @Override public void focusGained(FocusEvent e) { base.focusGained(e); comboBox.repaint(); }
                @Override public void focusLost(FocusEvent e) { base.focusLost(e); comboBox.repaint(); }
            };
        }
    }

    // Rows in drop-down lists
    private static class FlatListRenderer extends DefaultListCellRenderer implements UIResource {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, false);
            setBorder(new EmptyBorder(6, 8, 6, 8));
            setBackground(selected ? ACCENT_SOFT : SURFACE);
            setForeground(TEXT);
            setOpaque(index >= 0);
            return this;
        }
    }

    // Slim rounded scroll bars without arrow buttons
    public static class FlatScrollBarUI extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new FlatScrollBarUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollbar.setOpaque(false);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return noButton();
        }

        private static JButton noButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(12, 48) : new Dimension(48, 12);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // transparent track
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            int alpha = isDragging ? 200 : isThumbRollover() ? 160 : 110;
            g2.setColor(new Color(MUTED.getRed(), MUTED.getGreen(), MUTED.getBlue(), alpha));
            g2.fillRoundRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6, 6, 6);
            g2.dispose();
        }
    }
}
