
package ams;
import java.time.Clock;

public class Appointment {
    Clock systemClock = Clock.systemDefaultZone();

    String id;
    String name;
    String subject;
    String description;
    int priority; // low/medium/urgent
    String status; // due/completed/cancelled
    String dateCreated;

    public Appointment(String id, String name, String subject, String description, int priority, String status) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dateCreated = String.valueOf(systemClock.instant());
    }
    
    
}
