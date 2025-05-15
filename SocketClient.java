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

        System.out.println("hostname : " + hostname);
    }

    public void run() {

        System.out.println("Inside run method");        
        readFileAndPopulateServerMap("serverData.txt");
        processServerMap(serverMap);

    }

     
     
    private static void sendEventToServer(String server, int port, Message m){

        System.out.println(String.format("Sending event to server %s , port %s, message %s ", server, port, m));

        try(Socket client = new Socket("localhost", port);
            
            ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());){                        

            outToServer.writeObject(m);        
            
        } catch (IOException e) {
            System.out.println("Failed to connect to server : " + server);
            e.printStackTrace();
        }
    }



    // Send CONNECTION message
    public static void connectToServer(String server, int port) {
      
        Message m = new Message(new Date().getTime(), machId, "CONNECTION");

        sendEventToServer(server, port, m);
    }




    // Read ArrayList and find servers to be pinged with REQ msg
    public static void socReqMsg() {
       
       
        System.out.println("Request about to be placed");
        
        
        try {
                Thread.sleep(6000);
        } catch (InterruptedException ie) {
                // Ignore interruption
        }

        SocketServer.isRequested = true;
            
        requestTimeStamp = new Date().getTime();
            
        System.out.println("numberCS " + SocketServer.numberCS);

 
                
       // Check if there are any deferred requests
       
       if (SocketServer.requestQueue.isEmpty()) {

            SocketServer.inCriticalSection = true;

            long currentTm = new Date().getTime();
            long latency = currentTm - requestTimeStamp;
                    
            System.out.println("Elapsed time: " + latency);
            System.out.println("Entered :" + new Date());

                    try {
                        Thread.sleep(5000);
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
                               
                                        
                    //socReqMsg();

        } else {

             // Send requests to all nodes that have deferred requests
               for (Message msg : SocketServer.requestQueue) {
                        String sysNum = msg.getFromServer();
                        System.out.println("Sending req to " + sysNum);
                        sendRequest(sysNum);
               }
        }
          
    }

    // Socket to send request message
    public static void sendRequestMsg(String server, int port, long reqTimeStamp) {
                
        Message m = new Message(reqTimeStamp, machId, "REQUEST");     
        sendEventToServer("localhost", port, m);
    }


    public static void sendRequest(String sysNum) {
       
        String server = "node" + sysNum;
        Integer port = serverMap.get(server);
                
        Message m = new Message(requestTimeStamp, machId, "REQUEST");   

        sendEventToServer("localhost", port, m);
    }

     // Reply message to any REQUEST received
    public static void sendReply(String sysNum) {

        System.out.println("Sending REPLY to : " + sysNum);
       
        String server = "node" + sysNum;
        Integer port = serverMap.get(server);      
        Message m = new Message(new Date().getTime(), machId, "REPLY");  

        sendEventToServer("localhost", port, m);        
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

    /*
     *  Process server map and connect to all servers with lower number in hostname
     */
    private static void processServerMap(Map<String,Integer> serverMap){


       
        
        Map<String,Integer> filteredMap =  new HashMap<>();


        for(String server : serverMap.keySet()){
               
            
             if (!server.equals(hostname)) {

                // Connect to all servers with lower number in hostname
                int id = Integer.parseInt(server.substring(4)); 
                    
                if (id < Integer.parseInt(machId)) {                    
                
                    filteredMap.put(server, serverMap.get(server));                  
                }              
                  
             }
        }


        SocketServer.maxConn = filteredMap.size();


        for(String server : filteredMap.keySet()){

            // Add port nos and machine ids map
            int port = filteredMap.get(server);
            connectToServer(server, port);  
        }


               
    }

    /**
     *  Read file and populate server map
     * @param pathName
     */
    public static void readFileAndPopulateServerMap(String pathName) {

        System.out.println("Reading file : " + pathName); 

        String line = null;
      
        try {
            FileReader fR = new FileReader(pathName);           
            BufferedReader bufferedReader = new BufferedReader(fR);

            while ((line = bufferedReader.readLine()) != null) {
                                           
                String[] serverInfo = line.split("-");                
                
                serverMap.put(serverInfo[0], Integer.parseInt(serverInfo[1]));
                
            }
            
            // Set number of systems for SocketServer
            SocketServer.numberOfSystems = serverMap.size();
            
            // Close the file
            bufferedReader.close();

            System.out.println("Server map : " + serverMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}