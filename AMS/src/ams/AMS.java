
package ams;

import java.util.Scanner;

public class AMS {


    public static void main(String[] args) {



        PriorityQueue queue = new PriorityQueue();
        Scanner scn= new Scanner(System.in);
        String choice;




        while(true){
            //show navigation buttons
            System.out.println("|------------------Appointment Scheduling Tool-----------------|");
            System.out.println("Enter 'h' to show navigation controls");
            choice = scn.nextLine();

            if(choice.equals("h")) {
                showMainMenu();
            }

            //view all appointments
            else if (choice.equals("1")) {
                //show sort selection buttons[hey teddy sort this by priority by afterwards***]
                while(true) {
                    System.out.println("Enter 'h' to select sorting fields");
                    choice = scn.nextLine();
                    if(choice.equals("h")) {
                        selectSortFields();
                    }
                    else if (choice.equals("1")) {
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

                System.out.print("Enter meeting ID/entity: ");
                String id =scn.nextLine();
                System.out.print("Enter name of participant: ");
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
                        System.out.println("Press 'u' view update fields for Id-" + temp_appointment.id + ":'");
                        System.out.println("Press 0 to exit");
                        choice = scn.nextLine();

                        //show update navigation buttons
                        if (choice.equals("u")) {
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
                System.out.println("[Delete Appointments_____]");
                System.out.println("Enter id to select appointment:");
                Appointment temp_appointment= queue.searchById(scn.nextLine());
                if(temp_appointment == null){
                    System.out.println("\tNo matching id found, please try again...");
                }
                else{
                    queue.deleteById(temp_appointment.id);
                    System.out.println("Deletion success!");
                }
            }
            //view appointment by id
            else if(choice.equals("5")){
                System.out.println("[View Appointment Details_____]");
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


            //Exit from the tool
            else if(choice.equals("0")){
                break;
            }
            else {
                System.out.println("\tinvalid input. please try again...\n");
            }


        }




    }


    public static void showMainMenu(){

        System.out.println("Enter '1' to view all Appointments");//
        System.out.println("Enter '2' to place an Appointments");//
        System.out.println("Enter '3' to update an Appointment");//
        System.out.println("Enter '4' to delete an Appointment");//
        System.out.println("Enter '5' to view Appointment by Id");//
        //System.out.println("Enter '3' to delete latest Appointment");

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
        System.out.println("Enter '1' to sort by date created");
        System.out.println("Enter '2' to sort by participant name");
        System.out.println("Enter '3' to sort by appointment priority");
        System.out.println("Enter '0' to exit");
    }

}
