
package ams;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class AMS {

    public static void main(String[] args) {

        LinkedList queue = new LinkedList();
        String listFile = DataPaths.appointmentList();
        if (new File(listFile).exists()) {
            queue.restoreAppointments(listFile);
        }
        Scanner scn= new Scanner(System.in);
        String choice;

        System.out.println( "                                     _           _                                _   \n" +
                            "     /\\                             (_)         | |                              | |  \n" +
                            "    /  \\     _ __    _ __     ___    _   _ __   | |_   _ __ ___     ___   _ __   | |_ \n" +
                            "   / /\\ \\   | '_ \\  | '_ \\   / _ \\  | | | '_ \\  | __| | '_ ` _ \\   / _ \\ | '_ \\  | __|\n" +
                            "  / ____ \\  | |_) | | |_) | | (_) | | | | | | | | |_  | | | | | | |  __/ | | | | | |_ \n" +
                            " /_/    \\_\\ | .__/  | .__/   \\___/  |_| |_| |_|  \\__| |_| |_| |_|  \\___| |_| |_|  \\__|\n" +
                            "            | |     | |                                                               \n" +
                            "            |_|     |_|                                                               ");
        
        while(true){
            //show navigation buttons
            System.out.println("\n|-----------------------------------Scheduling Tool-----------------------------------|\n");
            System.out.println("\t****[Current Due Appointments: '"+queue.getDueCount()+"']****\n");
            showMainMenu();
            System.out.print("Choose an option: ");
            choice = scn.nextLine().trim();

            //view all appointments
            if (choice.equals("1")) {
                //show sort selection buttons[hey teddy sort this by priority by afterward***]
                System.out.println("Select sorting option:");
                while(true) {
                    selectSortFields();
                    choice = scn.nextLine();
                    System.out.println();
                    if (choice.equals("1")) {
                        queue.sortByDateCreated();
                        queue.displayAll();
                    } else if (choice.equals("2")) {
                        queue.sortByName();
                        queue.displayAll();
                    } else if (choice.equals("3")) {
                        queue.sortByPriority();
                        queue.displayAll();
                    } else if (choice.equals("4")) {
                        queue.sortByScheduled();
                        queue.displayAll();
                    } else if (choice.equals("0")) {
                        queue.sortByPriority();
                        break;
                    } else {
                        System.out.println("\tinvalid input, please try again...");
                    }
                }
            }

            //insert appointment
            else if (choice.equals("2")){

                System.out.print("Enter meeting ID: ");
                String id =scn.nextLine();
                System.out.print("Enter participant name(s): ");
                String name =scn.nextLine();
                System.out.print("Enter Appointment Subject: ");
                String subject =scn.nextLine();
                System.out.print("Enter description: ");
                String description =scn.nextLine();
                String scheduledAt = readScheduled(scn);
                Appointment clash = queue.findClash(scheduledAt, id);
                if (clash != null) {
                    System.out.println("\tNote: " + clash.name + " is already booked at that time (" + clash.subject + ").");
                }
                System.out.print("Enter urgency[urgent(1) /medium(2) /low(3)]: ");
                int priority =readOption(scn, 1, 3);
                System.out.print("Enter meeting status[due(1) /completed(2) /missed(3)]: ");
                String status =String.valueOf(readOption(scn, 1, 3));

                Appointment app1= new Appointment(id, name, subject, description, priority, status);
                app1.scheduledAt = scheduledAt;
                if(queue.insert(app1)) {
                    System.out.println("\tAppointment scheduled!\n");
                }
                else {
                    System.out.println("\tAn appointment with id '" + id + "' already exists, please try again...\n");
                }
            }

            //update appointment
            else if(choice.equals("3")){
                System.out.println("[______Update Appointments_____]");
                System.out.println("Enter id to select appointment:");
                Appointment temp_appointment= queue.searchById(scn.nextLine());
                if(temp_appointment == null){
                    System.out.println("\tNo matching id found, please try again...");
                }
                else{
                    while(true) {
                        //show update navigation buttons
                        System.out.println("Press 'u' to view update fields for Id-" + temp_appointment.id);
                        System.out.println("Press '0' to exit");
                        choice = scn.nextLine().trim();

                        //show update navigation buttons
                        if (choice.equalsIgnoreCase("u")) {
                            showUpdateFields();
                        }
                        //update name
                        else if (choice.equals("1")) {
                            System.out.println("Enter new name");
                            temp_appointment.name = scn.nextLine();
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //update subject
                        else if (choice.equals("2")) {
                            System.out.println("Enter new subject");
                            temp_appointment.subject = scn.nextLine();
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //update description
                        else if (choice.equals("3")) {
                            System.out.println("Enter new description");
                            temp_appointment.description = scn.nextLine();
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //update priority
                        else if (choice.equals("4")) {
                            System.out.println("Enter new priority[urgent(1) /medium(2) /low(3)]");
                            temp_appointment.priority = readOption(scn, 1, 3);
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //update status
                        else if (choice.equals("5")) {
                            System.out.println("Enter new status[due(1) /completed(2) /missed(3)]");
                            temp_appointment.status = String.valueOf(readOption(scn, 1, 3));
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //update date and time
                        else if (choice.equals("6")) {
                            System.out.println("Currently: " + temp_appointment.scheduledLabel());
                            temp_appointment.scheduledAt = readScheduled(scn);
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //exit update interface
                        else if(choice.equals("0")){
                            break;
                        }
                        //invalid input(home interface)
                        else{
                            System.out.println("\tinvalid input, please try again...");
                        }
                    }
                }
            }
            //Delete appointment by id
            else if(choice.equals("4")){
                System.out.println("[__________Delete Appointments_______]");
                System.out.println("Enter id to select appointment:");
                Appointment temp_appointment= queue.searchById(scn.nextLine());
                if(temp_appointment == null){
                    System.out.println("\tNo matching id found, please try again...");
                }
                else{
                    System.out.println("Are you sure you want to delete this appointment?\nYes['y'] / No['n']");
                    if(readYesNo(scn)){
                        queue.deleteById(temp_appointment.id);
                        System.out.println("Deletion success!");
                    }
                    else{
                        System.out.println("\tDeletion cancelled...");
                    }
                }
            }
            //view appointment by id
            else if(choice.equals("5")){
                System.out.println("[______View Appointment Details_____]");
                System.out.println("Enter appointment id to select appointment:");
                Appointment temp_appointment= queue.searchById(scn.nextLine());
                if(temp_appointment == null){
                    System.out.println("\tNo matching id found, please try again...");
                }
                else{
                    System.out.println("Appointment Id: "+ temp_appointment.id);
                    System.out.println("Appointment name: "+ temp_appointment.name);
                    System.out.println("Appointment subject: "+ temp_appointment.subject);
                    System.out.println("Appointment description: "+ temp_appointment.description);
                    System.out.println("Appointment date & time: "+ temp_appointment.scheduledLabel()
                            + (temp_appointment.isOverdue() ? " (overdue)" : ""));
                    System.out.println("Appointment priority: "+ temp_appointment.priorityLabel());
                    System.out.println("Appointment status: "+ temp_appointment.statusLabel());
                    System.out.println("Appointment date created: "+ temp_appointment.createdLabel() + " (UTC)");
                }
            }
            else if(choice.equals("6")){
                System.out.println("Are you sure you want to delete all appointments?\nYes['y'] / No['n']");
                    if(readYesNo(scn)){
                        queue.clearAll();
                        System.out.println("Deletion success!");
                    }
                    else{
                        System.out.println("\tDeletion cancelled...");
                    }
            }

            else if(choice.equals("7")){
                System.out.println("[______Backup and Restore Options_____]");
                selectBackupRestore();
                choice = scn.nextLine().trim();
                String filename;
                switch(choice){
                    case "1":
                        System.out.println("Enter the file name of the backup file for Backup:");
                        filename = scn.nextLine();
                        queue.storeAppointments(DataPaths.backupsFolder() + File.separator + filename + ".txt");
                        break;
                    case "2":
                        System.out.println("Enter the file name of the backup file for Restore:");
                        filename = scn.nextLine();
                        queue.restoreAppointments(DataPaths.backupsFolder() + File.separator + filename + ".txt");
                        break;
                    case "0":
                        break;
                    default:
                        System.out.println("\tinvalid input, please try again...");
                        break;
                }
            }

            //Exit from the tool
            else if(choice.equalsIgnoreCase("E")){
                queue.storeAppointments(listFile);
                break;
            }
            //invalid input(home interface)
            else{
                System.out.println("\tinvalid input '" + choice + "', please choose an option from the menu...");
            }
        }
    }

    public static void showMainMenu(){

        System.out.println("Enter '1' to view Appointment List");//
        System.out.println("Enter '2' to place an Appointment");//
        System.out.println("Enter '3' to update an Appointment");//
        System.out.println("Enter '4' to delete an Appointment");//
        System.out.println("Enter '5' to view Appointment by Id");//
        System.out.println("Enter '6' to delete all Appointments"); //
        System.out.println("Enter '7' for backup & restore options");
        System.out.println("Enter 'E' to Exit");
    }

    public static void showUpdateFields(){
        //you can't update id
        System.out.println("Enter '1' to update Name");
        System.out.println("Enter '2' to update Subject");
        System.out.println("Enter '3' to update Description");
        System.out.println("Enter '4' to update priority");
        System.out.println("Enter '5' to update status");
        System.out.println("Enter '6' to update date & time");
        System.out.println("Enter '0' to Exit");
    }
    public static void selectSortFields(){
        System.out.println("Sort by: Date created:['1']   Name:['2']   Priority:['3']   Date & time:['4']   Exit['0']");
    }

    //read a whole number between min and max, asking again until the input is valid
    //read when the appointment takes place; returns "" if left blank, otherwise e.g. "2026-10-20T14:30"
    public static String readScheduled(Scanner scn){
        LocalDate date;
        while(true){
            System.out.print("Enter date [YYYY-MM-DD, 'today', 'tomorrow', or blank for none]: ");
            String text = scn.nextLine().trim().toLowerCase();
            if(text.isEmpty()){
                return "";
            }
            try{
                date = text.equals("today") ? LocalDate.now() : text.equals("tomorrow") ? LocalDate.now().plusDays(1) : LocalDate.parse(text);
                break;
            }
            catch(DateTimeParseException e){
                System.out.println("\tinvalid date, please use the form 2026-10-20...");
            }
        }
        while(true){
            System.out.print("Enter time [e.g. 14:30 or 2:30 pm]: ");
            LocalTime time = UiKit.parseTime(scn.nextLine());
            if(time != null){
                return LocalDateTime.of(date, time).toString();
            }
            System.out.println("\tinvalid time, please try again...");
        }
    }

    public static int readOption(Scanner scn, int min, int max){
        while(true){
            try{
                int value = Integer.parseInt(scn.nextLine().trim());
                if(value >= min && value <= max){
                    return value;
                }
            }
            catch(NumberFormatException e){
                //falls through to the retry message
            }
            System.out.print("\tinvalid input, please enter a number from " + min + " to " + max + ": ");
        }
    }

    //read a y/n answer (any letter case), asking again until the input is valid
    public static boolean readYesNo(Scanner scn){
        while(true){
            String answer = scn.nextLine().trim();
            if(answer.equalsIgnoreCase("y")){
                return true;
            }
            if(answer.equalsIgnoreCase("n")){
                return false;
            }
            System.out.print("\tinvalid input, please enter 'y' or 'n': ");
        }
    }

    public static void selectBackupRestore(){
        System.out.println("Enter '1' to Backup appointment details");
        System.out.println("Enter '2' to Restore appointment details");
        System.out.println("Enter '0' to exit");
    }

    //don't use this method(it's not intended to work)
    public static void loading(){
        System.out.print("(|)");
        for(double i=0; i<1000000; i=+0.0001);
        System.out.print("(/)");
        for(double i=0; i<1000000; i=+0.0001);
        System.out.print("(-)");
        for(double i=0; i<1000000; i=+0.0001);
        System.out.print("(|)");
        for(double i=0; i<1000000; i=+0.0001);
    }
}