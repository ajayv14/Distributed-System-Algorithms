import java.io.*;

public class msg implements Serializable {

    private long timeStmp;
    private String fromSvr;
    private String msgVal;

    public void msg(long tS, String fSvr, String mVal) {

        timeStmp = tS;
        fromSvr = fSvr;
        msgVal = mVal;

    }

    // set  values
    public void settimeStmp(long tS) {

        timeStmp = tS;

    }


    public void setfromSvr(String fSvr) {

        fromSvr = fSvr;
    }

    public void setmsgVal(String mVal) {

        msgVal = mVal;
    }

// get values

    public long gettimeStmp() {


        return timeStmp;


    }

    public String getfromSvr() {

        return fromSvr;
    }

    public String getmsgVal() {

        return msgVal;
    }


}
