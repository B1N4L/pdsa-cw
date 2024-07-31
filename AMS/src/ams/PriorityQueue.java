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
        if (head == null) {
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

    //[INCOMPLETE] Update appointment by id
    public void updateById(Appointment app) {
        Node current = head;
        while (current != null) {
            if (current.appointment.id.equals(app.id)) {
                current.appointment = app;
            }
            current = current.next;
        }
    }


    // Delete appointment node with the highest priority (head of the list)
    public void deleteHighestPriority() {
        if (head == null) {
            return;
        }
        if (head.next == null) {
            head = null;
            tail = null;
        } else {
            head = head.next;
            head.prev = null;
        }
    }


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

    // Display all appointments with name, sbj & priority(use some sorting methods to this)
    public void displayAll() {
        Node current = head;
        while (current != null) {
            System.out.println( current.appointment.name + " " + current.appointment.subject + " " + current.appointment.priority);
            current = current.next;
        }
        System.out.println();
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
