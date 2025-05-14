
import java.util.*;
import java.net.*;
import java.io.*;

public class SocketServer extends Thread {

    private ServerSocket serverSocket;

    public static int serverTimeStamp = 0; // Time stamp of server
    public static int connectionCount = 0; // Number of client connections established to the server
    public static int maxConn; // Max number of clients that are to be connected
    public static int numberOfSystems = 0;

    public static boolean isAllClientsConnected = false; // Chk if all clients have connected to this server

    public static boolean flag = false;

    public static Queue<Message> requestQueue = new LinkedList<>();

    public static ArrayList<String> currentlyRequestingNodes = new ArrayList<>();
    public static ArrayList<String> localArray = new ArrayList<>();

    public static boolean inCriticalSection = false;
    public static boolean isRequest = false;

    public static int numberCS = 0;
    public static int replies = 0;
    public static int activeCount = 0;

    public SocketServer(int portNum) throws IOException {

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
                if ("Connection".equals(message.getMessage())) {
                     
                    maxConn--;

                    //set all clients conn to true
                    if (maxConn == 0) {
                        isAllClientsConnected = true;
                    }

                    if (isAllClientsConnected && !flag) {
                       
                        socClient.socReqMsg();
                        flag = true;                        
                    }
                
                }               


                //Request Message
                else if ("Request".equals(message.getMessage())) {

                    long messageTimeStamp = message.getTimeStamp();
                    long currentTimeStamp = socClient.requestTimeStamp;
                    
                    int machineId = Integer.parseInt(socClient.machId);
                    int fromMachineId = Integer.parseInt(message.getFromServer());

                    boolean replyNow;
                    
                    synchronized (SocketServer.class) {

                        replyNow = !isRequest 
                                || ( !inCriticalSection 
                                        && ( messageTimeStamp < currentTimeStamp || messageTimeStamp == currentTimeStamp && fromMachineId < machineId ));
                    

                        if(!replyNow) {
                            requestQueue.add(message);                                                        
                            continue;
                        }
                       

                    }

                    socClient.reply(message.getFromServer());
                    
                }

                // msg type 3-Reply message recieve 

                else if ("Rep".equals(message.getMessage())) {
                    
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

        if ((numberCS == 0 && replies == numberOfSystems - 1) || replies == activeCount) {

            inCriticalSection = true;

            long latency = new Date().getTime() - socClient.requestTimeStamp;

            System.out.println("Elapsed time " + latency);
            System.out.println("Entering critical section");

            try {
                Thread.sleep(30);
            } catch (InterruptedException ie) {

            }
            System.out.println("Exiting");

            inCriticalSection = false;
            isRequest = false;

            replies = 0;
            numberCS++;

            socClient.replyToAll();
            System.out.println("Critical Sections till now " + numberCS);
            socClient.socReqMsg();
            // System.out.println("New request order issued to socReqMsg");
        }

    }

    // read text file and get corresponding port numbers
    public static int readFile(String pathName) {

        String lineTxt = null;
        int svrPort = 0;
        int cnN = 0;

        try {

            // Read text file contents
            FileReader fR = new FileReader(pathName);
            // Wrapper
            BufferedReader bufferedReader = new BufferedReader(fR);

            while ((lineTxt = bufferedReader.readLine()) != null) {

                // System.out.println("Read Server name"+lineTxt);

                String[] svr = lineTxt.split("-");

                if (svr[0].equals(java.net.InetAddress.getLocalHost().getHostName())) {

                    svrPort = Integer.parseInt(svr[1]);

                }

                // fill maxConn value to find number of clients to be connected

                cnN = Integer.parseInt(svr[0].substring(2, 4));

                if (cnN > maxConn) {
                    maxConn = cnN;
                }
            }

            numberOfSystems = maxConn;
            // maxConn =
            // maxConn-Integer.parseInt(java.net.InetAddress.getLocalHost().getHostName().substring(2,4));
            // System.out.println("Total numre of machines :" + maxConn);
            // close file
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server port assigned is =" + svrPort);
        return svrPort;
    }

    public static void main(String[] args) {

        int port = 0;

        // get portnumber based on localhostname

        port = readFile("../aos/serverData.txt");

        try {
            Thread t = new SocketServer(port);
            t.start();

            Thread t2 = new socClient();
            t2.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
