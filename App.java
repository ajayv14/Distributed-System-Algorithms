import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;



// Start independent socket server and client
public class App {

   /* static int expectedMaxConnections = 0;

    public static void main(String[] args) {
        
        int port = 0;
        String nodeId = "node01"; // Default node
        
        
        if (args.length > 0) {
            nodeId = args[0]; // Get node ID from command line
        }
        
        // Override hostname check with the provided nodeId
        System.setProperty("hostname", nodeId);
        
        // get portnumber based on nodeId
        port = readFile("serverData.txt", nodeId);
        
        try {

            System.out.println("Starting socket server on port  : " + port); 

            Thread t = new SocketServer(port,expectedMaxConnections);
            t.start();

            System.out.println("Starting  socket client thread");   
            
            Thread t2 = new SocketClient();
            t2.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Reads server data file and gets the post number for this current server.
    public static int readFile(String pathName, String nodeId) {
        
        int serverPort = 0;
        
        String line = null;
        
        try {
            // Read text file contents
            FileReader fR = new FileReader(pathName);
            BufferedReader bufferedReader = new BufferedReader(fR);
            
            while ((line = bufferedReader.readLine()) != null) {
                
                expectedMaxConnections++; // Total num of nodes

                String[] svr = line.split("-");
                
                // Use the provided nodeId instead of hostname
                if (svr[0].equals(nodeId)) {
                    serverPort = Integer.parseInt(svr[1]);
                }           
            
            }       
        
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }    

        System.out.println("Max connections " + expectedMaxConnections);    
    
        return serverPort;
    }*/

}
