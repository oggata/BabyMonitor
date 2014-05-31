package com.braunster.mymodule.app.connrction_and_threads;

import android.os.Handler;
import android.util.Log;

import com.braunster.mymodule.app.objects.InOutStreams;

import java.io.IOException;


/**
 * Created by itzik on 5/12/2014.
 */
public class TCPStringReader extends BaseThread {

    private static final String TAG = TCPXmlReader.class.getSimpleName();
    private static final boolean DEBUG = false;

    private StringBuilder sb;
    private char data;

    public TCPStringReader(InOutStreams inOutStreams, Handler handler){
        super(inOutStreams, handler);
        sb = new StringBuilder();
    }

    @Override
    public void run() {
        super.run();

        while (!isInterrupted()) {
             /*
             * To convert the InputStream to String we use the BufferedReader.readLine()
             * method. We iterate until the BufferedReader return null which means
             * there's no more data to read. Each line will appended to a StringBuilder
             * and returned as String.
             */

            try {
                currentThread().sleep(100);
            } catch (InterruptedException e) {
                interrupt();
                e.printStackTrace();
            }

            try {
                while (inOutStreams.getInputStream().available() > 0) {
                    data = (char) inOutStreams.getInputStream().read();
                    if (DEBUG)
                        Log.d(TAG, "Available while data: " + data + ", Available Bytes: " + inOutStreams.getInputStream().available());
                    sb.append(data);
                }
            } catch (IOException e) {
                if (DEBUG) Log.e(TAG, "Cane write to socket, socket is closed");
                close();
                reportToHandler(TCPConnection.ERROR_IN_STREAM);
            }

            if (!sb.toString().isEmpty()) {
                reportToHandler(TCPConnection.STRING_DATA_RECEIVED, sb.toString());

                // Clear the last message.
                sb.delete(0, sb.toString().length());
            }

        }
    }

    @Override
    public void close() {
        super.close();
        interrupt();
    }
}
