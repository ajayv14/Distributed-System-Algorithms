import java.util.*;
import java.net.*;
import java.io.*;

public class SocketClient extends Thread {

   
    public static String machId;
   
    static {
        
        // Try to get node ID from system property, fallback to hostname if not set
        String hostname = System.getProperty("hostname");
        
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


    //ArrayList to store machine name and port numbers in a single String
    public static ArrayList<String> l1 = new ArrayList<String>();


    public static long requestTimeStamp = 0;


    public void run() {

        readFile("../aos/serverData.txt");
    }

    public static void connectClient(String svrName, int portNum) {
       
        try {
           
            Socket client = new Socket(svrName, portNum);
           
            //DataStreams
            OutputStream outToServer = client.getOutputStream();
            ObjectOutputStream oSs = new ObjectOutputStream(outToServer);

            Message m = new Message(new Date().getTime(),machId,"Connection");
    
            oSs.writeObject(m);
               
            client.close();
        } catch (IOException e) {
            System.out.println("Failed to connect to server");
            e.printStackTrace();
        }
    }

    // Read ArrayList and find servers to be pinged with REQ msg
    public static void socReqMsg() {
        
        System.out.println("Request about to be placed");
        
        if (socServer.numberCS < 40) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {

            }

            socServer.isReq = true;
            requestTimeStamp = new Date().getTime();
            System.out.println("numberCS " + socServer.numberCS);


            if (socServer.numberCS == 0) {
                System.out.println("Inside number CS 0 ");
                for (String itm : l1) {
                    String[] c = itm.split("-");
                    String svrNameLocal = c[0];
                    String machineId = svrNameLocal.substring(2, 4);
                    if (!machId.equals(machineId)) {
                        socReqMsgConn(c[0], Integer.parseInt(c[1]), requestTimeStamp);
                    }
                }
            } else {

                socServer.localArray.clear();
                socServer.localArray.addAll(socServer.currentlyRequestingNodes);
                socServer.activeCount = socServer.localArray.size();

                if (socServer.activeCount == 0) {
                    socServer.inCS = true;
                    long currentTm = new Date().getTime();
                    long latency = currentTm - requestTimeStamp;
                    System.out.println("Elapsed time: " + latency);
                    System.out.println("Entered :" + new Date());

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException ie) {

                    }

                    System.out.println("Exiting");
                    socServer.inCS = false;
                    socServer.isReq = false;
                    socServer.numberCS++;
                    socServer.replies = 0;
                    System.out.println("Critical Sections till now " + socServer.numberCS);
                    replyToAll();
                    socReqMsg();

                } else {

                    for (String item : socServer.localArray) {
                        System.out.println("Sending req to " + item);
                        sendReq(item);
                    }

                }

            }
        }
    }


    public static void replyToAll() {
        int size = socServer.qReq.size();
        for (int i = 0; i < size; i++) {
            msg m = socServer.qReq.remove();
            reply(m.getfromSvr());
            socServer.currentlyRequestingNodes.add(m.getfromSvr());
        }
    }


    // Socket to send request messgae
    public static void socReqMsgConn(String svrName, int portNum, long reqTimeStamp) {

        try {

            Socket clientMsg = new Socket(svrName, portNum);
            //DataStreams
            OutputStream ost = clientMsg.getOutputStream();
            ObjectOutputStream oSsm = new ObjectOutputStream(ost);
            msg m1 = new msg();
            m1.settimeStmp(reqTimeStamp);
            m1.setfromSvr(machId);
            m1.setmsgVal("Req");
            oSsm.writeObject(m1);
            clientMsg.close();
            // Each process should only place one request at a time
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //File Reader

    public static void readFile(String pathName) {
        int clientPartSvr = 0;
        String lineTxt = null;
        String currentNodeName = "node" + machId;

        try {
            //Read text file contents
            FileReader fR = new FileReader(pathName);
            // Wrapper
            BufferedReader bufferedReader = new BufferedReader(fR);

            while ((lineTxt = bufferedReader.readLine()) != null) {
                //System.out.println(lineTxt);
                l1.add(lineTxt);
                //connect client code
                String[] svr = lineTxt.split("-");
                
                // Check if this is our node based on the node ID
                if (svr[0].equals(currentNodeName)) {
                    int port1 = Integer.parseInt(svr[1]);
                    clientPartSvr = Integer.parseInt(machId);
                    //System.out.println("Connected to my own server");
                    //connectClient(svr[0],port1 );	// connect to its own server
                } else {
                    // connect to all servers with lower number in hostname
                    int intPartSvr = Integer.parseInt(svr[0].substring(4)); // Extract ID from nodeXX
                    //System.out.println("System that may be connected now to: "+intPartSvr);

                    if (intPartSvr < Integer.parseInt(machId)) {
                        // add port nos and machine ids to list l1 and l2
                        int port1 = Integer.parseInt(svr[1]);
                        connectClient(svr[0], port1);
                    }
                }
                // set server static variable canReq to true, all servers to which client is supposed to connect are connected
                socServer.canReq = true;
            }
            // Close the file
            bufferedReader.close();


        } catch (Exception e) {
            // TODO: handle exception
        }


    }


    //Method to send a Reply Rep message for ant Request Req

    public static void reply(String sysNum) {

        int portN = 0;
        String mName = null;
        String nodeName = "node" + sysNum;
        for (String itms : l1) {
            String[] arVal = itms.split("-");

            if (arVal[0].equals(nodeName)) {
                // find port num
                portN = Integer.parseInt(arVal[1]);
                mName = arVal[0];

                //System.out.println("found"+mName+portN);
            }
        }

        try {
            Socket soClient = new Socket(mName, portN);
            //DataStreams
            OutputStream opS = soClient.getOutputStream();
            ObjectOutputStream oOS = new ObjectOutputStream(opS);


            msg m2 = new msg();
            m2.settimeStmp(new Date().getTime());
            m2.setfromSvr(machId);
            m2.setmsgVal("Rep");
            oOS.writeObject(m2);
            oOS.flush();
            soClient.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void sendReq(String sysNum) {
        int portN = 0;
        String mName = null;
        String nodeName = "node" + sysNum;
        for (String itms : l1) {
            String[] arVal = itms.split("-");
            if (arVal[0].equals(nodeName)) {
                // find port num
                portN = Integer.parseInt(arVal[1]);
                mName = arVal[0];

                //System.out.println("found"+mName+portN);
            }
        }

        try {
            Socket soClient = new Socket(mName, portN);
            //DataStreams
            OutputStream opS = soClient.getOutputStream();
            ObjectOutputStream oOS = new ObjectOutputStream(opS);
            msg m2 = new msg();
            m2.settimeStmp(requestTimeStamp);
            m2.setfromSvr(machId);
            m2.setmsgVal("Req");
            oOS.writeObject(m2);
            oOS.flush();
            soClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
