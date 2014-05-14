package TCP.connrction_and_threads;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import TCP.objects.InOutStreams;

/**
 * Created by itzik on 5/11/2014.
 */
public class TCPStreamsOpener extends BaseThread {

    private static final String TAG = TCPStreamsOpener.class.getSimpleName();
    private static final boolean DEBUG = true;

    private OutputStream tmpOut ;
    private InputStream tmpIn;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    private Socket tcpSocket;

    public TCPStreamsOpener(Socket tcpSocket, Handler handler){
        super(handler);
        this.tcpSocket = tcpSocket;
    }

    @Override
    public void run() {
        super.run();

        if (DEBUG) Log.i(TAG, "Opening Stream");
        // Getting the input string.
        try {

            tmpIn = tcpSocket.getInputStream();
            tmpOut = tcpSocket.getOutputStream();

        } catch (IOException e) {
            e.printStackTrace();
            reportToHandler(TCPConnection.ERROR_CANT_OPEN_IN_OUT_STREAM);
            interrupt();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        reportToHandler(TCPConnection.IN_OUT_STREAMS_OPEN, new InOutStreams(mmInStream, mmOutStream));
    }

    public InputStream getInputStream(){return mmInStream; }
    public OutputStream getOutputStream(){return mmOutStream; }
}
