
package ams;

import java.util.Scanner;

public class AMS {

    public static void main(String[] args) {

        LinkedList queue = new LinkedList();
        queue.restoreAppointments("src\\ams\\appointment-list.txt");
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
            System.out.println("|-----------------------------------Scheduling Tool-----------------------------------|");
            System.out.println("Type 'Enter' for navigation controls");
            choice = scn.nextLine();

            if(choice.equals("Enter")) {
                showMainMenu();
            }

            //view all appointments
            else if (choice.equals("1")) {
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
                System.out.print("Enter urgency[urgent(1) /medium(2) /low(3)]: ");
                int priority =Integer.parseInt(scn.nextLine());
                System.out.print("Enter meeting status[due(1) /completed(2) /missed(3)]: ");
                String status =scn.nextLine();
                
                Appointment app1= new Appointment(id, name, subject, description, priority, status);
                queue.insert(app1);
                System.out.println("\tAppointment scheduled!\n");
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
                        System.out.println("Press '1' view update fields for Id-" + temp_appointment.id + ":'");
                        System.out.println("Press '0' to exit");
                        choice = scn.nextLine();

                        //show update navigation buttons
                        if (choice.equals("1")) {
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
                            temp_appointment.priority = Integer.parseInt(scn.nextLine());
                            queue.updateById(temp_appointment);
                            System.out.println("\tUpdate success!");
                            break;
                        }
                        //update status
                        else if (choice.equals("5")) {
                            System.out.println("Enter new status[due(1) /completed(2) /missed(3)]");
                            temp_appointment.status = scn.nextLine();
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
                    choice = scn.nextLine();
                    if(choice.equals("y")){
                        queue.deleteById(temp_appointment.id);
                        System.out.println("Deletion success!");
                    }
                    else if(choice.equals("n")){
                        System.out.println("\tDeletion cancelled...");
                    }
                    else {
                        System.out.println("\tinvalid input, please try again...");
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
                    System.out.println("Appointment priority: "+ temp_appointment.priority);
                    System.out.println("Appointment status: "+ temp_appointment.status);
                    System.out.println("Appointment date created: "+ temp_appointment.dateCreated);
                }
            }
            else if(choice.equals("6")){
                System.out.println("Are you sure you want to delete all appointments?\nYes['y'] / No['n']");
                    choice = scn.nextLine();
                    if(choice.equals("y")){
                        queue.clearAll();
                        System.out.println("Deletion success!");
                    }
                    else if(choice.equals("n")){
                        System.out.println("\tDeletion cancelled...");
                    }
                    else {
                        System.out.println("\tinvalid input, please try again...");
                    }
            }

            else if(choice.equals("7")){
                System.out.println("[______Backup and Restore Options_____]");
                selectBackupRestore();
                choice = scn.nextLine();
                String filename;
                switch(choice){
                    case "1":
                        System.out.println("Enter the file name of the backup file:");
                        filename = scn.nextLine();
                        queue.storeAppointments("Backups/"+filename+".txt");
                        break;
                    case "2":
                        System.out.println("Enter the file name of the backup file:");
                        filename = scn.nextLine();
                        queue.restoreAppointments("Backups/"+filename+".txt");
                        break;
                    case "0":
                        break;
                    default:
                        break;
                }
            }
            
            //Exit from the tool
            else if(choice.equals("0")){
                queue.storeAppointments("src\\ams\\appointment-list.txt");
                break;
            }
        }
    }

    public static void showMainMenu(){

        System.out.println("Enter '1' to view Appointment List");//
        System.out.println("Enter '2' to place an Appointments");//
        System.out.println("Enter '3' to update an Appointment");//
        System.out.println("Enter '4' to delete an Appointment");//
        System.out.println("Enter '5' to view Appointment by Id");//
        System.out.println("Enter '6' to delete all Appointments"); //
        System.out.println("Enter '7' for backup & restore options");
        System.out.println("Enter '0' to Exit");
    }

    public static void showUpdateFields(){
        //you can't update id
        System.out.println("Enter '1' to update Name");
        System.out.println("Enter '2' to update Subject");
        System.out.println("Enter '3' to update Description");
        System.out.println("Enter '4' to update priority");
        System.out.println("Enter '5' to update status");
        System.out.println("Enter '0' to Exit");
    }
    public static void selectSortFields(){
        System.out.println("Sort by: Date:['1']   Name:['2']   Priority:['3']   Exit['0']");
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