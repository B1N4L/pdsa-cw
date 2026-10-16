package ams;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LinkedList {

    private static class Node {

        Appointment appointment;
        Node prev;
        Node next;

        // Constructor for creating a new node
        Node(Appointment appointment) {
            this.appointment = appointment;
            this.prev = null;
            this.next = null;
        }
    }
    // Head and tail pointers of the doubly linked list
    private Node head;
    private Node tail;

    // Constructor for creating an empty priority queue
    public LinkedList() {
        this.head = null;
        this.tail = null;
    }

    // Add a new appointment based on priority (returns false if the id already exists)
    public boolean insert(Appointment app) {
        if (searchById(app.id) != null) {
            return false;
        }
        Node newNode = new Node(app);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            Node current = head;
            while (current != null && current.appointment.priority <= app.priority) {
                current = current.next;
            }
            if (current == head) {
                newNode.next = head;
                head.prev = newNode;
                head = newNode;
            } else if (current == null) {
                tail.next = newNode;
                newNode.prev = tail;
                tail = newNode;
            } else {
                newNode.next = current;
                newNode.prev = current.prev;
                current.prev.next = newNode;
                current.prev = newNode;
            }
        }
        return true;
    }

    //Update appointment by id (re-inserted so the list stays in priority order)
    public void updateById(Appointment app) {
        if (searchById(app.id) == null) {
            return;
        }
        deleteById(app.id);
        insert(app);
    }

    /*
    // Delete appointment node with the highest priority (head of the list)
    public void deleteHighestPriority() {
        if (isEmpty()) {
            return;
        }
        if (head.next == null) {
            head = null;
            tail = null;
        } else {
            head = head.next;
            head.prev = null;
        }
    }*/
    // Delete an appointment node by id
    public void deleteById(String id) {
        Node current = head;
        while (current != null && !current.appointment.id.equals(id)) {
            current = current.next;
        }
        if (current == null) {
            return;
        }
        if (current.prev != null) {
            current.prev.next = current.next;
        } else {
            head = current.next;
        }
        if (current.next != null) {
            current.next.prev = current.prev;
        } else {
            tail = current.prev;
        }
    }

    // Search for an appointment by id
    public Appointment searchById(String id) {
        Node current = head;
        while (current != null) {
            if (current.appointment.id.equals(id)) {
                return current.appointment;
            }
            current = current.next;
        }
        return null;
    }

    // Display all appointments with id, name, sbj, creation date & priority(use some sorting methods to this)
    public void displayAll() {
        if (isEmpty()) {
            System.out.println("\tNo appointments yet.\n");
            return;
        }
        String format = "%-6s %-24s %-16s %-20s %-8s %-10s%n";
        System.out.printf(format, "ID", "When", "Name", "Subject", "Priority", "Status");
        System.out.println("-".repeat(89));
        Node current = head;
        while (current != null) {
            Appointment app = current.appointment;
            String when = app.scheduledLabel() + (app.isOverdue() ? " !" : "");
            System.out.printf(format, fit(app.id, 6), when, fit(app.name, 16), fit(app.subject, 20),
                    app.priorityLabel(), app.statusLabel());
            current = current.next;
        }
        System.out.println("(! = overdue)");
        System.out.println();
    }

    // Shorten text that is too long for its table column
    private static String fit(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }

    //sort by creation date of appointments[watch out for conflicts]
    public void sortByDateCreated() {
        if (isEmpty()) {
            return;
        }

        boolean swapped;
        do {
            swapped = false;
            Node current = head;
            while (current.next != null) {
                if (current.appointment.dateCreated.compareTo(current.next.appointment.dateCreated) > 0) {
                    // Swap appointments
                    Appointment temp_appointment = current.appointment;
                    current.appointment = current.next.appointment;
                    current.next.appointment = temp_appointment;
                    swapped = true;
                }
                current = current.next;
            }
        } while (swapped);
    }

    //sort appointments by the date and time they take place (appointments without one go last)
    public void sortByScheduled() {
        if (isEmpty()) {
            return;
        }

        boolean swapped;
        do {
            swapped = false;
            Node current = head;
            while (current.next != null) {
                if (scheduledAfter(current.appointment, current.next.appointment)) {
                    // Swap appointments
                    Appointment temp = current.appointment;
                    current.appointment = current.next.appointment;
                    current.next.appointment = temp;
                    swapped = true;
                }
                current = current.next;
            }
        } while (swapped);
    }

    // True if a should come after b when sorting by date and time
    private static boolean scheduledAfter(Appointment a, Appointment b) {
        if (a.scheduledAt.isEmpty()) {
            return !b.scheduledAt.isEmpty();
        }
        if (b.scheduledAt.isEmpty()) {
            return false;
        }
        // "yyyy-MM-ddTHH:mm" text sorts in date order
        return a.scheduledAt.compareTo(b.scheduledAt) > 0;
    }

    // Find another due appointment booked at exactly the same date and time (null if none)
    public Appointment findClash(String scheduledAt, String ignoreId) {
        if (scheduledAt == null || scheduledAt.isEmpty()) {
            return null;
        }
        Node current = head;
        while (current != null) {
            Appointment other = current.appointment;
            if (!other.id.equals(ignoreId) && other.status.equals("1") && other.scheduledAt.equals(scheduledAt)) {
                return other;
            }
            current = current.next;
        }
        return null;
    }

    //sort appointments by priority
    public void sortByPriority() {
        if (isEmpty()) {
            return;
        }

        boolean swapped;
        do {
            swapped = false;
            Node current = head;
            while (current.next != null) {
                if (current.appointment.priority > current.next.appointment.priority) {
                    // Swap appointments
                    Appointment temp = current.appointment;
                    current.appointment = current.next.appointment;
                    current.next.appointment = temp;
                    swapped = true;
                }
                current = current.next;
            }
        } while (swapped);
    }

    //sort appointments by name
    public void sortByName() {
        if (head == null) {
            return;
        }

        boolean swapped;
        do {
            swapped = false;
            Node current = head;
            while (current.next != null) {
                if (current.appointment.name.compareTo(current.next.appointment.name) > 0) {
                    // Swap appointments
                    Appointment temp = current.appointment;
                    current.appointment = current.next.appointment;
                    current.next.appointment = temp;
                    swapped = true;
                }
                current = current.next;
            }
        } while (swapped);
    }

    // Check if the list is empty
    public boolean isEmpty() {
        return head == null;
    }

//backup/store appointment list (returns false if the file could not be written)
    public boolean storeAppointments(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8))) {
            Node current = head;
            while (current != null) {
                Appointment appointment = current.appointment;
                writer.write("ID: " + escape(appointment.id));
                writer.newLine();
                writer.write("Name: " + escape(appointment.name));
                writer.newLine();
                writer.write("Subject: " + escape(appointment.subject));
                writer.newLine();
                writer.write("Description: " + escape(appointment.description));
                writer.newLine();
                writer.write("Priority: " + appointment.priority);
                writer.newLine();
                writer.write("Status: " + appointment.status);
                writer.newLine();
                writer.write("Scheduled: " + appointment.scheduledAt);
                writer.newLine();
                writer.write("Date Created: " + appointment.dateCreated);
                writer.newLine();
                writer.newLine(); // Add a blank line between appointments

                current = current.next;
            }
            System.out.println("Appointments successfully stored in file: " + filename);
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
            return false;
        }
    }

    // Restore appointments from a file (returns how many were added, or -1 if the file could not be read)
    public int restoreAppointments(String filename) {
        int restored = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename, StandardCharsets.UTF_8))) {
            // Each appointment is a block of "Label: value" lines ending with a blank line.
            // Fields are matched by label, so older files without a "Scheduled" line still load.
            String id = null, name = "", subject = "", description = "", priority = "", status = "1", scheduled = "", dateCreated = "";
            String line;
            do {
                line = reader.readLine();
                boolean endOfBlock = line == null || line.trim().isEmpty();
                if (!endOfBlock) {
                    String value = fieldValue(line);
                    switch (fieldLabel(line)) {
                        case "ID": id = value; break;
                        case "Name": name = value; break;
                        case "Subject": subject = value; break;
                        case "Description": description = value; break;
                        case "Priority": priority = value; break;
                        case "Status": status = value.isEmpty() ? "1" : value; break;
                        case "Scheduled": scheduled = value; break;
                        case "Date Created": dateCreated = value; break;
                        default: break;
                    }
                } else if (id != null) {
                    try {
                        if (dateCreated.isEmpty()) {
                            dateCreated = java.time.Instant.now().toString();
                        }
                        Appointment appointment = new Appointment(id, name, subject, description,
                                Integer.parseInt(priority), status, dateCreated, scheduled);
                        // Insert the restored appointment into the priority queue (duplicate ids are skipped)
                        if (this.insert(appointment)) {
                            restored++;
                        } else {
                            skipped++;
                        }
                    } catch (NumberFormatException e) {
                        skipped++;
                    }
                    id = null;
                    name = subject = description = priority = scheduled = dateCreated = "";
                    status = "1";
                }
            } while (line != null);
            System.out.println(restored + " appointment(s) restored from file: " + filename);
            if (skipped > 0) {
                System.out.println(skipped + " appointment(s) skipped (duplicate id or invalid data)");
            }
            return restored;
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
            return -1;
        }
    }

    // Get the label before the first ":" in a "Label: value" line
    private static String fieldLabel(String line) {
        int index = line.indexOf(':');
        return index < 0 ? "" : line.substring(0, index).trim();
    }

    // Get the value after the first ":" in a "Label: value" line (empty if missing)
    private static String fieldValue(String line) {
        if (line == null) {
            return "";
        }
        int index = line.indexOf(':');
        if (index < 0) {
            return "";
        }
        return unescape(line.substring(index + 1).trim());
    }

    // Keep multi-line text on one line in the file ("\n" is written as \n, "\" as \\)
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n");
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == 'n') {
                    result.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    result.append('\\');
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    // Copy the appointments into an array, in list order (used by the window to fill its table)
    public Appointment[] toArray() {
        Appointment[] appointments = new Appointment[getSize()];
        Node current = head;
        int i = 0;
        while (current != null) {
            appointments[i++] = current.appointment;
            current = current.next;
        }
        return appointments;
    }

    // Empty the appointment list
    public void clearAll() {
        head = null;
        tail = null;
    }

    // Get the size of the priority queue
    public int getSize() {
        int size = 0;
        Node current = head;
        while (current != null) {
            size++;
            current = current.next;
        }
        return size;
    }

    public int getDueCount() {
        int count = 0;
        Node current = head;
        while (current != null) {
            if (current.appointment.status.equals("1")) {
                count++;
            }
            current = current.next;
        }
        return count;
    }
}
