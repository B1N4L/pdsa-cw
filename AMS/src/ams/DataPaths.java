package ams;

import java.io.File;

// Works out where the appointment list and backups are kept, on Windows, Linux and macOS
public class DataPaths {

    // When run from the project folder (IDE or console) the project's own list file is used
    private static final File PROJECT_LIST = new File("src" + File.separator + "ams" + File.separator + "appointment-list.txt");

    // Folder that holds the data: the project folder, or an "AMS" folder in the user's home
    // (can be changed with -Dams.data=<folder>)
    public static File dataFolder() {
        String override = System.getProperty("ams.data");
        File folder;
        if (override != null && !override.isEmpty()) {
            folder = new File(override);
        } else if (PROJECT_LIST.exists()) {
            return new File(".");
        } else {
            folder = new File(System.getProperty("user.home"), "AMS");
        }
        folder.mkdirs();
        return folder;
    }

    // File the appointment list is saved to
    public static String appointmentList() {
        File folder = dataFolder();
        if (new File(".").equals(folder)) {
            return PROJECT_LIST.getPath();
        }
        return new File(folder, "appointment-list.txt").getPath();
    }

    // Folder that backups are saved to by default
    public static String backupsFolder() {
        File backups = new File(dataFolder(), "Backups");
        backups.mkdirs();
        return backups.getPath();
    }
}
