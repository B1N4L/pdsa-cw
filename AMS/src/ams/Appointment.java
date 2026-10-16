
package ams;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment {
    Clock systemClock = Clock.systemDefaultZone();

    String id;
    String name;
    String subject;
    String description;
    int priority; // low/medium/urgent
    String status; // due/completed/missed
    String dateCreated;
    String scheduledAt = ""; // when the appointment takes place, e.g. "2026-10-20T14:30" (empty if not scheduled yet)

    public Appointment(String id, String name, String subject, String description, int priority, String status) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dateCreated = String.valueOf(systemClock.instant());
    }
    public Appointment(String id, String name, String subject, String description, int priority, String status, String dateCreated) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dateCreated = dateCreated;
    }
    public Appointment(String id, String name, String subject, String description, int priority, String status, String dateCreated, String scheduledAt) {
        this(id, name, subject, description, priority, status, dateCreated);
        this.scheduledAt = scheduledAt;
    }

    // Readable name for the priority code
    public String priorityLabel() {
        switch (priority) {
            case 1: return "urgent";
            case 2: return "medium";
            case 3: return "low";
            default: return "unknown";
        }
    }

    // Readable name for the status code
    public String statusLabel() {
        switch (status) {
            case "1": return "due";
            case "2": return "completed";
            case "3": return "missed";
            default: return "unknown";
        }
    }

    // Date and time created, e.g. "2024-08-11 06:22"
    public String createdLabel() {
        if (dateCreated.length() >= 16) {
            return dateCreated.substring(0, 10) + " " + dateCreated.substring(11, 16);
        }
        return dateCreated;
    }

    // When the appointment takes place, or null if it has no (valid) date and time
    public LocalDateTime scheduledTime() {
        try {
            return scheduledAt.isEmpty() ? null : LocalDateTime.parse(scheduledAt);
        } catch (Exception e) {
            return null;
        }
    }

    // Short date and time, e.g. "Today 14:30", "Tomorrow 09:00" or "Tue 20 Oct 2026, 14:30"
    public String scheduledLabel() {
        LocalDateTime when = scheduledTime();
        if (when == null) {
            return "Not scheduled";
        }
        LocalDate today = LocalDate.now();
        String time = when.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (when.toLocalDate().equals(today)) {
            return "Today " + time;
        }
        if (when.toLocalDate().equals(today.plusDays(1))) {
            return "Tomorrow " + time;
        }
        if (when.toLocalDate().equals(today.minusDays(1))) {
            return "Yesterday " + time;
        }
        return when.format(DateTimeFormatter.ofPattern(when.getYear() == today.getYear() ? "EEE d MMM, HH:mm" : "EEE d MMM yyyy, HH:mm"));
    }

    // True for a due appointment whose time has already passed
    public boolean isOverdue() {
        LocalDateTime when = scheduledTime();
        return "1".equals(status) && when != null && when.isBefore(LocalDateTime.now());
    }

    // True if the appointment is scheduled for today
    public boolean isToday() {
        LocalDateTime when = scheduledTime();
        return when != null && when.toLocalDate().equals(LocalDate.now());
    }
}
