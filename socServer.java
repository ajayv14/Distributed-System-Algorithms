
import java.util.*;
import java.net.*;
import java.io.*;
public class socServer extends Thread {
private ServerSocket serverSocket;

public static int svrTimeStmp=0 ; // Time stamp of server
public static int connectCount=0; // Number of client connections established to the server
public static int maxConn; // Count of number of clients that are to be connected
public static int numberOfSystems=0;
public static boolean canReq = true; // chk if client has conn to all servers
public static boolean connAllClient = false; // Chk if all clients ahve connected to this swerver

public static boolean flag = false;
public static Queue<msg> qReq = new LinkedList<>();
public static ArrayList<String> currentlyRequestingNodes = new ArrayList<>();
public static ArrayList<String> localArray = new ArrayList<>(); 
public static boolean inCS = false;
public static boolean isReq = false;
public static int numberCS =0;
public static int replies = 0; 
public static int activeCount=0;
public socServer(int portNum)throws IOException{	
	
	serverSocket = new ServerSocket(portNum);
	serverSocket.setSoTimeout(100000);
}
	
public void run(){
	
	while(true){
			
		try{


                     
        Socket server = serverSocket.accept();
                   
	    //System.out.println("Connected to "+server.getRemoteSocketAddress());
                         ObjectInputStream oIs = new ObjectInputStream(server.getInputStream());
 		
                          msg m = (msg)oIs.readObject();
                                                                 
		
                     //msg type 1
              		if(m.getmsgVal().equals("Connection")){
        		            connectCount++;
			                maxConn--;
			        }

                   //set all clients conn to true
                   if(maxConn - Integer.parseInt(socClient.machId)==0){
	                   connAllClient = true;
                    }
              					
                   if(connAllClient==true && canReq==true)
                   {
                     if(!flag){
	                   socClient.socReqMsg();
	                   flag = true;
                   }
                   }



                     //msg type 2
                     if(m.getmsgVal().equals("Req"))
                      {

	                      if(!inCS && ((!isReq)||(isReq && socClient.reqTimeStamp > m.gettimeStmp()) ||
						            (isReq && socClient.reqTimeStamp==m.gettimeStmp() && 
									Integer.parseInt(socClient.machId)<Integer.parseInt(m.getfromSvr())))){

		                 if(isReq && !inCS && numberCS!=0 && !(currentlyRequestingNodes.contains(m.getfromSvr()))){
				                   socClient.sendReq(m.getfromSvr());	
		                    		++activeCount;		
		                       }
		
		                 socClient.reply(m.getfromSvr());
		                 currentlyRequestingNodes.add(m.getfromSvr());
	                 }else{
		                     qReq.add(m);

	            }	

} 

// msg type 3-Reply message recieve 

if(m.getmsgVal().equals("Rep")){
	replyUpdate();
	currentlyRequestingNodes.remove(m.getfromSvr());
	checkForCS();
//System.out.println("Quite wokimng fine, REPLY recieved");

}


		}catch(IOException e){
			e.printStackTrace();
			break;
		}catch(ClassNotFoundException e){
                                 e.printStackTrace();
                        
		}
        }
}

public static synchronized void replyUpdate(){
	replies++;
	
}


//read text file and get corresponding port numbers
public static int readFile(String pathName){
	
	String lineTxt=null;
	int svrPort=0;
    int cnN=0;
	try {
              
		//Read text file contents
		FileReader fR = new FileReader(pathName);
		// Wrapper			
		BufferedReader bufferedReader = new BufferedReader(fR);
		
		while((lineTxt = bufferedReader.readLine()) != null){
			
			//System.out.println("Read Server name"+lineTxt);
		
			String[] svr = lineTxt.split("-");
                 

			if(svr[0].equals(java.net.InetAddress.getLocalHost().getHostName())){
			
				svrPort = Integer.parseInt(svr[1]);
                        			                    
            }          
                        	

			
     // fill maxConn value to find number of clients to be connected

     cnN =Integer.parseInt(svr[0].substring(2,4));

      if(cnN>maxConn)
        {
              maxConn = cnN; 
        }
  }

numberOfSystems = maxConn;
//maxConn = maxConn-Integer.parseInt(java.net.InetAddress.getLocalHost().getHostName().substring(2,4));
//System.out.println("Total numre of machines :" + maxConn);
//close file
bufferedReader.close();
}
catch(IOException e){
e.printStackTrace();
}
	System.out.println("Server port assigned is ="+svrPort);
	return svrPort;
}


public static void main(String [] args){
	
	int port=0;
	
	//get portnumber based on localhostname
	
		port=readFile("../aos/serverData.txt");		
try{
		Thread t = new socServer(port);
		t.start();
			
		Thread t2 = new socClient();
		t2.start();
		
}
catch(IOException e)
{
e.printStackTrace();
}
	
	
}
	

	
public static synchronized void checkForCS(){

	if((numberCS==0 && replies==numberOfSystems-1) || replies == activeCount){
		inCS = true;
		long latency= new Date().getTime() - socClient.reqTimeStamp;
		System.out.println("Elapsed time " + latency);
		System.out.println("Entering critical section");
		try{
			Thread.sleep(30);
		}catch(InterruptedException ie){
			
		}	
		System.out.println("Exiting");
		inCS=false;
		isReq=false;
		replies=0;
		numberCS++;
		socClient.replyToAll();
		System.out.println("Critical Sections till now " + numberCS);
		socClient.socReqMsg();
		//System.out.println("New request order issued to socReqMsg");
	}	

}	
	
	
}
