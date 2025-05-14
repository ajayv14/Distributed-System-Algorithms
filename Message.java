import java.io.*;

public class Message implements Serializable {

    private long timeStamp;
    private String fromServer;
    private String messageString;

    public void message(long tStamp, String frmServer, String msgString) {

        timeStamp = tStamp;
        fromServer = frmServer;
        messageString = msgString;

    }

    public String getMessage(){
        return messageString;
    }

    public long getTimeStamp() {
       
        return timeStamp;
    }

    public String getFromServer(){
        return fromServer;
    }

   

}
