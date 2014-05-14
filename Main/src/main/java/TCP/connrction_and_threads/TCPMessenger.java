package TCP.connrction_and_threads;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import TCP.objects.InOutStreams;
import TCP.xml.objects.XmlMessage;

/**
 * Created by itzik on 5/11/2014.
 */
public class TCPMessenger extends BaseThread {

    private static final String TAG = TCPMessenger.class.getSimpleName();
    private static final boolean DEBUG = true;
    private boolean checkConnection = false;

    private long lastCommTime = -1;
    private static final int CHECK_INTERVALS = 5000;


    public TCPMessenger(InOutStreams inOutStreams, Handler handler, TCPConnection connection){
        super(inOutStreams, handler, connection);
    }

    private List<String> messages = new ArrayList<String>();

    @Override
    public void run() {
        super.run();

        while (!isInterrupted())
        {
            if (checkConnection && ( System.currentTimeMillis() - connection.lastCommunicationTime > CHECK_INTERVALS) )
            {
                messages.add(XmlMessage.getCheckMessage());
            }

            //There messages to send
            if (messages.size() > 0){
                if (DEBUG) Log.i(TAG, "Sending Messages, Amount: " + messages.size());

                List<String> messagesToSend = messages;

                messages = new ArrayList<String>();

                for (int i = 0; i < messagesToSend.size() ; i++)
                {
                    try {
                        if (DEBUG) Log.i(TAG, "Sending message: " + messagesToSend.get(i));
                        inOutStreams.getOutputStream().write(messagesToSend.get(i).getBytes());

                        // Set the last time the devices communicate successfully.
                        connection.lastCommunicationTime = System.currentTimeMillis();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                        reportToHandler(TCPConnection.ERROR_MSG_NOT_SENT);
                        interrupt();
                    }
                }
            }
        }
    }

    /** Write text to the socket*/
    public void write(String msg){
        messages.add(msg);
    }

    @Override
    public void close() {
        super.close();
        interrupt();
    }

    public void setCheckConnection(boolean checkConnection) {
        this.checkConnection = checkConnection;
    }
}
