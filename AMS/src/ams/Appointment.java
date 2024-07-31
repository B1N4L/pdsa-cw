
package ams;
import java.time.Clock;

public class Appointment {
    Clock systemClock = Clock.systemDefaultZone();

    String id;
    String name;
    String subject;
    String description;
    int duration;
    String contact;
    String eventType;
    String eventLink;
    int priority; // low/medium/urgent
    String status; // due/completed/cancelled
    String dateCreated;

    public Appointment(String id, String name, String subject, String description, int duration, String contact, int priority, String status, String eventType, String eventLink) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.duration = duration;
        this.contact = contact;
        this.priority = priority;
        this.status = status;
        this.eventType = eventType;
        this.eventLink = eventLink;
        this.dateCreated = String.valueOf(systemClock.instant());
    }
    
    
}
