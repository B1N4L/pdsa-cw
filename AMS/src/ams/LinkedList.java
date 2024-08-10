package ams;

import java.io.*;


public class LinkedList {

        private static class Node {
            Appointment appointment;
            int priority;
            Node prev;
            Node next;

            // Constructor for creating a new node
            Node(Appointment appointment) {
                this.appointment = appointment;
                this.prev = null;
                this.next = null;
                this.priority = appointment.priority;
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


    // Add a new appointment based on priority
    public void insert(Appointment app) {
        Node newNode = new Node(app);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            Node current = head;
            while (current != null && current.priority <= app.priority) {
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
    }

    //Update appointment by id
    public void updateById(Appointment app) {
        Node current = head;
        while (current != null) {
            if (current.appointment.id.equals(app.id)) {
                current.appointment = app;
            }
            current = current.next;
        }
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
        Node current = head;
        while (current != null) {
            System.out.println( current.appointment.id + "\t" +current.appointment.name + "\t" + current.appointment.subject + "\t" + current.appointment.dateCreated.substring(0, 10));
            current = current.next;
        }
        System.out.println();
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

//backup/store appointment list
public void storeAppointments(String filename) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
        Node current = head;
        while (current != null) {
            Appointment appointment = current.appointment;
            writer.write("ID: " + appointment.id);
            writer.newLine();
            writer.write("Name: " + appointment.name);
            writer.newLine();
            writer.write("Subject: " + appointment.subject);
            writer.newLine();
            writer.write("Description: " + appointment.description);
            writer.newLine();
            writer.write("Priority: " + appointment.priority);
            writer.newLine();
            writer.write("Status: " + appointment.status);
            writer.newLine();
            writer.write("Date Created: " + appointment.dateCreated);
            writer.newLine();
            writer.newLine(); // Add a blank line between appointments

            current = current.next;
        }
        System.out.println("Appointments successfully stored in file: " + filename);
    } catch (IOException e) {
        System.out.println("An error occurred while writing to the file: " + e.getMessage());
    }
}

    public void restoreAppointments(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.split(": ")[1].trim();
                String name = reader.readLine().split(": ")[1].trim();
                String subject = reader.readLine().split(": ")[1].trim();
                String description = reader.readLine().split(": ")[1].trim();

                int priority = Integer.parseInt(reader.readLine().split(": ")[1].trim());
                String status = reader.readLine().split(": ")[1].trim();
                String dateCreated = reader.readLine().split(": ")[1].trim();

                // Skip the blank line between appointments
                reader.readLine();

                // Create a new Appointment object with the parsed data
                Appointment appointment = new Appointment(id, name, subject, description, priority, status);
                appointment.dateCreated = dateCreated; // Restore the original dateCreated value

                // Insert the restored appointment into the priority queue
                this.insert(appointment);
            }
            System.out.println("Appointments successfully restored from file: " + filename);
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
        }
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
}
