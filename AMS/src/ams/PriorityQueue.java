package ams;

public class PriorityQueue {

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
    public PriorityQueue() {
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
            System.out.println( current.appointment.id + " " +current.appointment.name + " " + current.appointment.subject + " " + current.appointment.dateCreated);
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
