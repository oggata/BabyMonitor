package TCP.connrction_and_threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import TCP.interfaces.BaseThreadInterface;
import TCP.objects.InOutStreams;

/**
 * Created by itzik on 5/11/2014.
 */
public class BaseThread extends Thread implements BaseThreadInterface{

    private static final String TAG = BaseThread.class.getSimpleName();

    private static boolean DEBUG = false;
    private Handler handler;
    InOutStreams inOutStreams;
    private boolean close = false;
    TCPConnection connection;

    public BaseThread(){}

    public BaseThread(InOutStreams inOutStreams){
        this.inOutStreams = inOutStreams;

    }

    public BaseThread(Handler handler){
        this.handler = handler;
    }

    public BaseThread(InOutStreams inOutStreams, Handler handler){
        this.handler = handler;
        this.inOutStreams = inOutStreams;
    }

    public BaseThread (InOutStreams inOutStreams, Handler handler, TCPConnection connection){
        this.handler = handler;
        this.inOutStreams = inOutStreams;
        this.connection = connection;
    }

    @Override
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void close() {
        close = true;
    }

    // Send message to the handler
    void reportToHandler(int code){
        if (DEBUG) Log.d(TAG, "Report to handler, Code: " + code);
        Message msg = new Message();
        msg.what = code;
        handler.sendMessage(msg);
    }

    // Send message to the handler
    void reportToHandler(int code, Object obj){
        if (DEBUG) Log.d(TAG, "Report to handler, Code: " + code);
        Message msg = new Message();
        msg.what = code;
        msg.obj = obj;
        handler.sendMessage(msg);
    }

    public boolean isClosed() {
        return close;
    }
}
