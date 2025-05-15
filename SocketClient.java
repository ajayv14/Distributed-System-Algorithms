import java.util.*;
import java.net.*;
import java.io.*;

public class SocketClient extends Thread {

    // ArrayList to store machine name and port numbers in a single String
    public static Map<String, Integer> serverMap = new HashMap<>();

    public static long requestTimeStamp = 0;
   
    public static String machId;

    static String hostname;
   
    static {
        // Try to get node ID from system property, fallback to hostname if not set
        hostname = System.getProperty("hostname");
        
        if (hostname != null) {
            machId = hostname.substring(4); // Extract ID from nodeXX
        } else {
            try {
                machId = InetAddress.getLocalHost().getHostName().substring(2, 4);
            } catch (UnknownHostException e) {
                machId = "01"; // Default to 01 if hostname can't be determined
                e.printStackTrace();
            }
        }
    }

    public void run() {
        readFile("serverData.txt");
    }

    // Send CONNECTION message
    public static void connectToServer(String server, int port) {
        try {
            Socket client = new Socket(server, port);
           
            // DataStreams
            OutputStream outToServer = client.getOutputStream();
            ObjectOutputStream oSs = new ObjectOutputStream(outToServer);

            Message m = new Message(new Date().getTime(), machId, "CONNECTION");
    
            oSs.writeObject(m);               
            client.close();

        } catch (IOException e) {
            System.out.println("Failed to connect to server : " + server);
            e.printStackTrace();
        }
    }




    // Read ArrayList and find servers to be pinged with REQ msg
    public static void socReqMsg() {
       
       
        System.out.println("Request about to be placed");
        
        if (SocketServer.numberCS < 40) 
        {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                // Ignore interruption
            }

            SocketServer.isRequested = true;
            
            requestTimeStamp = new Date().getTime();
            
            System.out.println("numberCS " + SocketServer.numberCS);

            if (SocketServer.numberCS == 0) {
              
                System.out.println("Inside number CS 0 ");


                for(String server : serverMap.keySet()){
                    
                    String machineId = server.substring(4); // Extract ID from nodeXX

                    if (!machId.equals(machineId)) {
                        sendRequestMsg(server, serverMap.get(server), requestTimeStamp);
                    }
                }
                              
                
            } else {
                
                // Check if there are any deferred requests
                if (SocketServer.requestQueue.isEmpty()) {

                    SocketServer.inCriticalSection = true;
                    long currentTm = new Date().getTime();
                    long latency = currentTm - requestTimeStamp;
                    System.out.println("Elapsed time: " + latency);
                    System.out.println("Entered :" + new Date());

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException ie) {
                        // Ignore interruption
                    }

                    System.out.println("Exiting");
                   
                    SocketServer.inCriticalSection = false;
                    SocketServer.isRequested = false;
                    SocketServer.numberCS++;
                    SocketServer.replies = 0;
                    System.out.println("Critical Sections till now " + SocketServer.numberCS);
                   
                   
                    replyToAll();
                                     
                    socReqMsg();
                } else {
                    // Send requests to all nodes that have deferred requests
                    for (Message msg : SocketServer.requestQueue) {
                        String sysNum = msg.getFromServer();
                        System.out.println("Sending req to " + sysNum);
                        sendRequest(sysNum);
                    }
                }
            }
        }
    }

    // Socket to send request message
    public static void sendRequestMsg(String server, int port, long reqTimeStamp) {
        try {
            Socket clientMsg = new Socket(server, port);

            // DataStreams
            OutputStream ost = clientMsg.getOutputStream();
            ObjectOutputStream oSsm = new ObjectOutputStream(ost);

            Message m = new Message(reqTimeStamp, machId, "Request");                  
            oSsm.writeObject(m);
            clientMsg.close();
            // Each process should only place one request at a time
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void readFile(String pathName) {
       
        String line = null;
      
        try {
            FileReader fR = new FileReader(pathName);           
            BufferedReader bufferedReader = new BufferedReader(fR);

            while ((line = bufferedReader.readLine()) != null) {
             
                              
                String[] serverInfo = line.split("-");                
                serverMap.put(serverInfo[0], Integer.parseInt(serverInfo[1]));

                // Check if this is our node based on hostname
                if (serverInfo[0].equals(hostname)) {
                    // This is our node
                } else {
                    // Connect to all servers with lower number in hostname
                    int id = Integer.parseInt(serverInfo[0].substring(4)); 
                    
                    if (id < Integer.parseInt(machId)) {
                        // Add port nos and machine ids map
                        int port = Integer.parseInt(serverInfo[1]);
                        connectToServer(serverInfo[0], port);                    
                    }
                }
            }
            
            // Set number of systems for SocketServer
            SocketServer.numberOfSystems = serverMap.size();
            
            // Close the file
            bufferedReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }





    public static void replyToAll() {
        
        int size = SocketServer.requestQueue.size();
        
        for (int i = 0; i < size; i++) {
            
            Message m = SocketServer.requestQueue.poll();
            
            if (m != null) {
                sendReply(m.getFromServer());
            }
        }
    }


    // Reply message to any REQUEST received
    public static void sendReply(String sysNum) {
        String nodeName = "node" + sysNum;
        Integer port = serverMap.get(nodeName);
              
        try {
            Socket soClient = new Socket(nodeName, port);
            
            OutputStream opS = soClient.getOutputStream();
            ObjectOutputStream oOS = new ObjectOutputStream(opS);

            Message m = new Message(new Date().getTime(), machId, "Reply");

            oOS.writeObject(m);
            oOS.flush();
            soClient.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public static void sendRequest(String sysNum) {
       
        String server = "node" + sysNum;
        Integer port = serverMap.get(server);
       
        try {
            Socket soClient = new Socket(server, port);
            // DataStreams
            OutputStream opS = soClient.getOutputStream();
            ObjectOutputStream oOS = new ObjectOutputStream(opS);
          
            Message m2 = new Message(requestTimeStamp, machId, "Request");
            
            oOS.writeObject(m2);
            oOS.flush();
            soClient.close();
        } catch (IOException e) {

            System.out.println("Failed to connect to server : " + server);
            e.printStackTrace();
        }
    }
}