import java.io.*;

public class Message implements Serializable {

    private long timeStamp;
    private String fromServer;
    private String messageString;

    public Message(long tStamp, String frmServer, String msgString) {

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

    @Override
    public String toString() {

        return "Message [timeStamp=" + timeStamp + ", fromServer=" + fromServer + ", message=" + messageString + "]";
    }




   

}
