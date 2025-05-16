package RicartAgrawalaAlgorithmWRachorvalCarilloOptimization;
import java.io.*;

public class Message implements Serializable {

    private long timeStamp;
    private String senderMachineId;
    private String messageString;

    public Message(long tStamp, String senderMachineId, String msgString) {

        timeStamp = tStamp;
        this.senderMachineId = senderMachineId;
        messageString = msgString;

    }

    public String getMessage(){
        return messageString;
    }

    public long getTimeStamp() {
       
        return timeStamp;
    }

    public String getSenderMachineId(){
        return senderMachineId;
    }

    @Override
    public String toString() {

        return "Message [timeStamp=" + timeStamp + ", SenderId=" + senderMachineId + ", message=" + messageString + "]";
    }




   

}
