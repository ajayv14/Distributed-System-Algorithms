
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.*;
import java.io.*;

public class SocketServer {

    private final ServerSocket serverSocket;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public  Queue<Message> requestQueue = new LinkedList<>();

    private Set<String> repliesReceived = new HashSet<>();
    
    private  Map<String, Integer> serverMap = new HashMap<>();
    private  Map<String, Integer> OtherServersMap = new HashMap<>();

    public int maxConnections; // Max number of clients that are to be connected

    public  boolean inCriticalSection = false;

    public  boolean wantingCS = false;
   
   

    private int machineId;
    
    
    private List<Socket> socketConnections = new ArrayList<>();


    private AtomicInteger logicalClockTS = new AtomicInteger(0);

    public  boolean flag = false;
   
 




    public  int numberCS = 0;

    private static String hostname;



    public SocketServer(String server) throws IOException {

        readFileAndPopulateServerMap("nodes.txt");
        connectToOtherServers(serverMap); // Connect to nodes with lower id

        System.out.println("hostname : " + hostname);
       
        serverSocket = new ServerSocket(serverMap.get(hostname));
        serverSocket.setSoTimeout(100000);    
        
        
        machineId = Integer.parseInt(hostname.substring(4));

        System.out.println("machineId : " + machineId);

    }


    // Lamports logical clock 

    private long getLogicalClockTS(){
        return logicalClockTS.incrementAndGet();
    }



    public void listener() {

        while (true) {

            try {

                Socket server = serverSocket.accept();

                ObjectInputStream inputStream = new ObjectInputStream(server.getInputStream());

                Message message = (Message) inputStream.readObject();

                executorService.submit(() -> handleClientMessages(message));
               

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
       
        long currentTS = getLogicalClockTS();

        logicalClockTS.set((int)Math.max(currentTS, senderTS));
       
        int senderMachineId = Integer.parseInt(message.getSenderMachineId()); // Machine id of the message sender

       
        synchronized (this) {

            if( !wantingCS
                    || (!inCriticalSection
                            && (senderTS < currentTS
                                    || (senderTS == currentTS && senderMachineId < machineId)))){


                                        sendReply(message.getSenderMachineId()); // Provide ack to sender approving access to CS

                                    }

           else {

                System.out.println("Request added to queue");
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

    

    private synchronized void requestCS() {

        wantingCS = true; 

        repliesReceived.clear(); // clear prior replies

        // Send requests to others 
        for (String server : OtherServersMap.keySet()) {
            sendRequest(server);
        }



        // Wait untill all replies are received
        while(repliesReceived.size() < OtherServersMap.size()){

            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }            
        }

           
        
        criticalSection();

        wantingCS = false;

      
        // Send deferred replies
        while (!requestQueue.isEmpty()) {

            Message deferredMessage = requestQueue.poll();
            sendReply(deferredMessage.getSenderMachineId());
        }     

    }

    // Simulated critical section
    private void criticalSection() {

        System.out.println("Entering Critical Section");

        inCriticalSection = true;

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }

        inCriticalSection = false;

        System.out.println("Exited Critical Section");
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



    
    public void connectToServer(String server, int port) {
      
        Message m = new Message(logicalClockTS.incrementAndGet(), machineId+"", "CONNECTION");

        sendEventToServer(server, port, m);
    }

    

    // Socket to send request message
    public void sendRequestMsg(String server, int port, long reqTimeStamp) {
                
        Message m = new Message(logicalClockTS.incrementAndGet(), machineId+"", "REQUEST");     
        sendEventToServer("localhost", port, m);
    }


    public void sendRequest(String sysNum) {
       
        String server = "node" + sysNum;
        Integer port = serverMap.get(server);
                
        Message m = new Message(logicalClockTS.incrementAndGet(), machineId+"", "REQUEST");   

        sendEventToServer("localhost", port, m);
    }



     // Reply message to any REQUEST received
    public void sendReply(String sysNum) {

        System.out.println("Sending REPLY to : " + sysNum);
       
        String server = "node" + sysNum;
        Integer port = serverMap.get(server);      
        Message m = new Message(logicalClockTS.incrementAndGet(), machineId+"", "REPLY");  

        sendEventToServer("localhost", port, m);        
    }


    /*
     *  Process server map and connect to all servers with lower number in hostname
     */
    private void connectToOtherServers(Map<String,Integer> serverMap){      
        
        Map<String,Integer> filteredMap =  new HashMap<>();

        for(String server : serverMap.keySet()){
           
             if (!server.equals(hostname)) {

                // Connect to all servers with lower number in hostname
                int id = Integer.parseInt(server.substring(4)); 
                    
                if (id < machineId) {                    
                
                    filteredMap.put(server, serverMap.get(server));                  
                }                       
             }
        }

        maxConnections = filteredMap.size();

        for(String server : filteredMap.keySet()){

            // Add port nos and machine ids map
            int port = filteredMap.get(server);
            connectToServer(server, port);  
        }
               
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
            hostname = args[0]; // Get node ID from command line
            System.out.println("hostname : " + hostname);
        }  


        

        SocketServer server = new SocketServer(hostname);
        
        // Simulate servers periodically trying to enter the critical section
        Random random = new Random();
        while (true) {
            Thread.sleep(random.nextInt(5000) + 10000); // Wait between 10 to 15 seconds
            server.requestCS();
        }        
    }


}
