
import java.util.*;
import java.net.*;
import java.io.*;

public class SocketServer extends Thread {

    private ServerSocket serverSocket; 


    public static int maxConn; // Max number of clients that are to be connected
    public static int numberOfSystems = 0;

    public static boolean isAllClientsConnected = false; // Chk if all clients have connected to this server

    public static boolean flag = false;

    public static Queue<Message> requestQueue = new LinkedList<>();


    public static boolean inCriticalSection = false;
    public static boolean isRequested = false;

    public static int numberCS = 0;

    public static int replies = 0;
    public static int activeCount = 0;

    
    public SocketServer(int portNum, int maxConnections) throws IOException {

        maxConn = maxConnections;

        serverSocket = new ServerSocket(portNum);
        serverSocket.setSoTimeout(100000);
    }

    public void run() {

        
        while (true) {

            try {

                Socket server = serverSocket.accept();
               
                ObjectInputStream inputStream = new ObjectInputStream(server.getInputStream());

                Message message = (Message) inputStream.readObject();
             

                // Establish connection
                if ("CONNECTION".equals(message.getMessage())) {
                     
                    System.out.println("Connection request recieved from : " + message.getFromServer());

                    maxConn--;

                    //set all clients conn to true
                    if (maxConn <= 0) {
                        isAllClientsConnected = true;
                        
                        System.out.println("All clients connected");
                    }

                    if (isAllClientsConnected && !flag) {
                       
                        SocketClient.socReqMsg();
                        flag = true;                        
                    }

                    System.out.println("Updated max connections : " + maxConn);
                
                }               


                //Request Message
                else if ("REQUEST".equals(message.getMessage())) {

                    System.out.println("Received REQUEST message : " + message.toString());

                    long messageTimeStamp = message.getTimeStamp(); // Timestamp from the message received
                    long machineTimeStamp = SocketClient.requestTimeStamp; // Current node's timestamp.
                    
                    int machineId = Integer.parseInt(SocketClient.machId);                    
                    int messageMachineId = Integer.parseInt(message.getFromServer()); // Machine id of the message sender

                    boolean replyNow;
                    
                    synchronized (SocketServer.class) {

                        replyNow = !isRequested 
                                || ( !inCriticalSection 
                                        && ( messageTimeStamp < machineTimeStamp || ( messageTimeStamp == machineTimeStamp && messageMachineId < machineId )));
                    

                        if(!replyNow) {

                            System.out.println("Request added to queue");
                            requestQueue.add(message);                                                        
                            continue;
                        }                      

                    }
                    
                    SocketClient.sendReply(message.getFromServer()); // Provide ack to sender approving access to CS
                    
                }

                
                // Reply message from sender providing ack/approval for enterning CS
                else if ("REPLY".equals(message.getMessage())) {
                    
                    synchronized (SocketServer.class) {
                        replies++;
                    }
                    
                    enterCriticalSectionIfPossible();                                          
                }


            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();

            }
        }
    }

   

    public static synchronized void enterCriticalSectionIfPossible() {

       if (replies == numberOfSystems - 1 && isRequested && !inCriticalSection) {
            
            inCriticalSection = true;
           
            criticalSection();
                           
            inCriticalSection = false;
                        
            isRequested = false;
            
            replies = 0;
            
            numberCS++;

            // Reply to deferred messages
            while (!requestQueue.isEmpty()) {

                Message deferredMessage = requestQueue.poll();
                SocketClient.sendReply(deferredMessage.getFromServer());
            }

            SocketClient.socReqMsg();

            System.out.println("Critical Section count so far: " + numberCS);
        }

    }


    // Simulated critical section
    private static void criticalSection() {

        long latency = new Date().getTime() - SocketClient.requestTimeStamp;

        System.out.println("Elapsed time: " + latency + " ms");
        System.out.println("Entering Critical Section");

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {}

        System.out.println("Exiting Critical Section");
    }


  


    



}
