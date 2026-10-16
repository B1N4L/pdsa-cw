package ams;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// Small settings file (settings.properties) kept next to the appointment list, e.g. the chosen theme
public class Settings {

    private static Properties values;

    private static File file() {
        return new File(DataPaths.dataFolder(), "settings.properties");
    }

    private static Properties load() {
        if (values == null) {
            values = new Properties();
            File file = file();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    values.load(reader);
                } catch (IOException e) {
                    // start with defaults if the file can't be read
                }
            }
        }
        return values;
    }

    public static String get(String key, String fallback) {
        return load().getProperty(key, fallback);
    }

    public static void set(String key, String value) {
        load().setProperty(key, value);
        try (FileWriter writer = new FileWriter(file(), StandardCharsets.UTF_8)) {
            values.store(writer, "AMS settings");
        } catch (IOException e) {
            // the setting still applies until the app is closed
        }
    }
}
