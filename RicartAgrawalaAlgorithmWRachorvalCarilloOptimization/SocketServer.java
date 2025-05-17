
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

    private Map<String, Socket> connections = new HashMap<>();
    
    // Map to store output streams for each connection
    private Map<String, ObjectOutputStream> outputStreams = new HashMap<>();

    public  volatile boolean inCriticalSection = false;

    public  volatile boolean wantingCS = false;
                 
    private AtomicInteger logicalClockTS = new AtomicInteger(0);
       
    private static String node;
    
    private volatile boolean running = true;
    
    private Thread listenerThread;

    private final Object stateLock = new Object();



    public SocketServer(String server) throws IOException {
       
        readFileAndPopulateServerMap("nodes.txt");
        
        System.out.println("node : " + node);
       
        serverSocket = new ServerSocket(serverMap.get(node));
        serverSocket.setSoTimeout(5000);  // Reduced timeout for more responsive checking
     
        // Start listener in a controlled thread
        listenerThread = new Thread(this::listener);
        listenerThread.setName("SocketServer-Listener");
        listenerThread.start();

        connectToOtherServers(serverMap); // Connect to nodes with lower id than the current node
    }


    
    public void listener() {
        
        System.out.println("Listener thread started");
        
        while (running) {
          
            try {
                // Non-blocking accept with timeout
                Socket server = serverSocket.accept();
                            
                // Process the connection in a separate thread to avoid blocking the listener
                new Thread(() -> {
                    try {

                        ObjectInputStream inputStream = new ObjectInputStream(server.getInputStream());
                        while (true) {
                            Message message = (Message) inputStream.readObject();
                            //System.out.println("Message received : " + message);
                            handleClientMessages(message);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("Error processing client connection: " + e.getMessage());
                    }
                }).start();
                
            } catch (SocketTimeoutException e) {
                // This is expected due to the timeout, just continue the loop
                //System.out.println("Socket accept timed out, checking if we should continue running");
            } catch (IOException e) {
               System.out.println("error occured");
            }
        }
        System.out.println("Listener thread exited");
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
       
       
        synchronized (stateLock) {

            boolean defer = inCriticalSection ||
                        (wantingCS &&
                         ((senderTS > currentTS) ||
                         (senderTS == currentTS && senderMachineId > machineId)));

            if (defer) {
                System.out.println("REQUEST added to queue");
                requestQueue.add(message);
            } else {
                sendReply(message.getSenderMachineId());
            }
        }      

    }


    private void handleReplyMessage(Message message) {

        System.out.println("Received REPLY message : " + message.toString());

        synchronized (stateLock) {
            repliesReceived.add(message.getSenderMachineId());
            stateLock.notifyAll(); //
        }    
    }

    

    private  void requestCS() throws InterruptedException{

     repliesReceived.clear(); // clear prior replies

     synchronized (this) { 
        
        wantingCS = true; 
  
        // Send requests to others 
        for (String server : otherServersMap.keySet()) {
            sendRequest(server);
        }

    }  
        // Wait untill all replies are received
       
        synchronized (stateLock) {

            while(repliesReceived.size() < otherServersMap.size()){

                System.out.println(String.format("Waiting for %s more REPLY events", otherServersMap.size() - repliesReceived.size()));

                stateLock.wait();       
            }
        }


        criticalSection();

        synchronized (this) { 
            wantingCS = false;
        }    
        
        
        
        System.out.println("Number of deferred msgs : " + requestQueue.size());

        // Send deferred replies
        while (!requestQueue.isEmpty()) {

            Message deferredMessage = requestQueue.poll();

            sendReply(deferredMessage.getSenderMachineId());
        }
    }

    // Simulated critical section
    private void criticalSection() {

        System.out.println(String.format("Node %s : Entering Critical Section", node));

        inCriticalSection = true;

        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignored) {
        }

        inCriticalSection = false;

        System.out.println(String.format("Node %s Exited Critical Section", node));
    }

  
    
    
    private void sendEventToServer(String server, Message m){

         /*try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }*/
       
        Socket client = connections.get(server);

        if(client != null){
           
            try {
                // Get or create the output stream for this connection
                ObjectOutputStream outToServer = outputStreams.get(server);
           
                if (outToServer == null) {
                    outToServer = new ObjectOutputStream(client.getOutputStream());
                    outputStreams.put(server, outToServer);
                }
                
                // Reset the stream before writing to avoid header issues
                outToServer.reset();
                outToServer.writeObject(m);
                outToServer.flush();
                            
            } catch (IOException e) {
                System.out.println("Failed to send message to server " + server);
                e.printStackTrace();               
            }         
        } else {
            System.out.println(String.format("Connections to server : %s returned null", server));
        }
    }
  

   
    // Request to enter CS
    public void sendRequest(String server) {
               
        System.out.println("Sending REQUEST to : " + server);              
                
        Message m = new Message(logicalClockTS.get(), node, "REQUEST");   

        sendEventToServer(server, m);
    }  



    // Reply to any REQUEST received
    public void sendReply(String server) {

        System.out.println("Sending REPLY to : " + server);             
          
        Message m = new Message(logicalClockTS.get(), node, "REPLY");  

        sendEventToServer(server, m);        
    }


    


    private void connectToOtherServers(Map<String,Integer> serverMap){             
        

        for(String server : serverMap.keySet()){
           
             if (!server.equals(node)) {
            
                otherServersMap.put(server, serverMap.get(server));               
                
             }
        }

        // Initial delay to allow for other nodes to start    
        try {
            Thread.sleep(20000); //20 secs
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        for(String server : otherServersMap.keySet()){

            // Add port nos and machine ids map
            int port = otherServersMap.get(server);

            System.out.println(String.format("Connecting to server %s at port %s", server, port));
            
            try{
                
                Socket client = new Socket("localhost", port);
                connections.put(server,client); // add all bi-directional connections                    
                
            } catch (IOException e) {
                System.out.println(String.format("Failed to connect to server : %s and port %s ",server,port));
                e.printStackTrace();
            }          
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
        try {
            
            while (true) {                
                Thread.sleep(15000);
                System.out.println("Requesting critical section");
                server.requestCS();
            }

        } catch (InterruptedException e) {
            
            System.out.println("Main thread interrupted");
        } 
    }    
             
    

}
