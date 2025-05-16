package RicartAgrawalaAlgorithmWRachorvalCarilloOptimization;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.*;
import java.io.*;

public class SocketServer {

    private final ServerSocket serverSocket;    

    public  Queue<Message> requestQueue = new LinkedList<>();

    private Set<String> repliesReceived = new HashSet<>();
    
    private  Map<String, Integer> serverMap = new HashMap<>();

    private  Map<String, Integer> otherServersMap = new HashMap<>();

    public  volatile boolean inCriticalSection = false;

    public  volatile boolean wantingCS = false;
                 
    private AtomicInteger logicalClockTS = new AtomicInteger(0);
       
    private static String node;



    public SocketServer(String server) throws IOException {

       
        readFileAndPopulateServerMap("nodes.txt");
        
        System.out.println("node : " + node);
       
        serverSocket = new ServerSocket(serverMap.get(node));
        serverSocket.setSoTimeout(100000);    
     
        // Start listener
        new Thread(this::listener).start();


        connectToOtherServers(serverMap); // Connect to nodes with lower id than the current node

    }


    // thread
    public void listener() {

        while (true) {

            try {

                Socket server = serverSocket.accept();

                ObjectInputStream inputStream = new ObjectInputStream(server.getInputStream());

                Message message = (Message) inputStream.readObject();

                handleClientMessages(message);
               

            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();

            }
        }
    }


    private void handleClientMessages(Message message) {

        switch (message.getMessage()) {
                       
            case "REQUEST":
                handleRequestMesssage(message);
                break;

            case "REPLY":
                handleReplyMessage(message);
                break;

            default:
                System.out.println("Invalid message received");
        }
    }

  
    private void handleRequestMesssage(Message message) {

        System.out.println("Received REQUEST message : " + message.toString());

       
        long senderTS = message.getTimeStamp(); // Timestamp from the message received
       
        int currentTS = logicalClockTS.get(); 
        
        logicalClockTS.set(Math.max(logicalClockTS.get(), (int) senderTS) + 1);
       
        int senderMachineId = Integer.parseInt(message.getSenderMachineId().substring(4)); // Machine id of the message sender

        int machineId = Integer.parseInt(node.substring(4));
       
       
        synchronized (this) {

            if(!wantingCS
                    || (!inCriticalSection
                            && (senderTS < currentTS
                                    || (senderTS == currentTS && senderMachineId < machineId)))){

                                        sendReply(message.getSenderMachineId()); // Provide ack to sender approving access to CS

                                    }

           else {

                System.out.println("REQUEST added to queue");
                requestQueue.add(message);
            }
        }      

    }


    private void handleReplyMessage(Message message) {

        System.out.println("Received REPLY message : " + message.toString());

        synchronized (this) {
            repliesReceived.add(message.getSenderMachineId());
            notifyAll(); //
        }    
    }

    

    private synchronized void requestCS() throws InterruptedException{

        wantingCS = true; 

        repliesReceived.clear(); // clear prior replies

        // Send requests to others 
        for (String server : otherServersMap.keySet()) {
            sendRequest(server);
        }


        // Wait untill all replies are received
        synchronized (this) {
            while(repliesReceived.size() < otherServersMap.size()){

                System.out.println(String.format("Waiting for %s more REPLY events", otherServersMap.size() - repliesReceived.size()));

                wait();       
            }
        }  
        
        criticalSection();

        wantingCS = false;

      
        // Send deferred replies
        while (!requestQueue.isEmpty()) {

            Message deferredMessage = requestQueue.poll();
            sendReply(deferredMessage.getSenderMachineId());
        }
        
        
        // Introduce a delay before the next request
        try {
            Thread.sleep(new Random().nextInt(4000) + 10000); // Wait 10 to 14 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    // Simulated critical section
    private synchronized void criticalSection() {

        System.out.println(String.format("Node %s : Entering Critical Section", node));

        inCriticalSection = true;

        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignored) {
        }

        inCriticalSection = false;

        System.out.println(String.format("Node %s Exited Critical Section", node));
    }

  


    private void sendEventToServer(String server, int port, Message m){

       // System.out.println(String.format("Sending event to server %s , port %s, message %s ", server, port, m));

        try(Socket client = new Socket("localhost", port);
            
            ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());){                        

            outToServer.writeObject(m);        
            
        } catch (IOException e) {
            System.out.println(String.format("Failed to connect to server : %s and port %s ",server,port));
            e.printStackTrace();
        }
    }

  


    

    public void sendRequest(String server) {
       
        
        System.out.println("Sending REQUEST to : " + server);
        
        Integer port = serverMap.get(server);
                
        Message m = new Message(logicalClockTS.get(), node, "REQUEST");   

        sendEventToServer("localhost", port, m);
    }



     // Reply message to any REQUEST received
    public void sendReply(String server) {

        System.out.println("Sending REPLY to : " + server);
             
        Integer port = serverMap.get(server);      
        Message m = new Message(logicalClockTS.get(), node, "REPLY");  

        sendEventToServer("localhost", port, m);        
    }


    /*
     *  Process server map and connect to all servers with lower number in hostname
     */
    private void connectToOtherServers(Map<String,Integer> serverMap){             
        

        for(String server : serverMap.keySet()){
           
             if (!server.equals(node)) {

                // Connect to all servers with lower number in hostname
                int id = Integer.parseInt(server.substring(4));                                 

                int machineId = Integer.parseInt(node.substring(4));

                otherServersMap.put(server, serverMap.get(server));                              
                
             }
        }

        try {
            Thread.sleep(20000); //20 secs
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        for(String server : otherServersMap.keySet()){

            // Add port nos and machine ids map
            int port = otherServersMap.get(server);

            System.out.println(String.format("Connecting to server %s at port %s", server, port));
             sendRequest(server); // Connect and send REQUEST  
        }

        System.out.println("Other servers map : " + otherServersMap);
               
    }

   
       
    public void readFileAndPopulateServerMap(String path) {

        System.out.println("Reading file : " + path); 
        
      
        try(BufferedReader br = new BufferedReader(new FileReader(path))) {
            
            String line = null;

            while ((line = br.readLine()) != null) {
                                           
                String[] serverInfo = line.split("-");                
                
                serverMap.put(serverInfo[0], Integer.parseInt(serverInfo[1]));
                
            }            
           
            System.out.println("Server map : " + serverMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


   
    public static void main(String[] args) throws IOException, InterruptedException {
        
        
        if (args.length > 0) {
            node = args[0]; // Get node ID from command line
            System.out.println("hostname : " + node);
        }  
        else {
            System.out.println("Please provide node id as command line argument");
            System.exit(0);
        }

        SocketServer server = new SocketServer(node);
        
        // Simulate servers periodically trying to enter the critical section
        Random random = new Random();
        while (true) {
            Thread.sleep(random.nextInt(5000) + 10000); // Wait between 10 to 15 seconds
            server.requestCS();
        }        
    }

}
