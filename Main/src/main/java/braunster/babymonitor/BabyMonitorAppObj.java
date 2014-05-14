package braunster.babymonitor;

import android.app.Application;

import TCP.connrction_and_threads.TCPConnection;

/**
 * Created by itzik on 2/23/14.
 */
public class BabyMonitorAppObj extends Application {

    private TCPConnection streamConnection, dataConnection;

    private final static String APIKEY = "a5522b00";

    @Override
    public void onCreate() {
        super.onCreate();

//        BugSenseHandler.initAndStartSession(getApplicationContext(), APIKEY);

        streamConnection = new TCPConnection(getApplicationContext());
        dataConnection = new TCPConnection(getApplicationContext());

        dataConnection.setTextTag("DATA");
        streamConnection.setTextTag("STREAM");

    }

    public TCPConnection getStreamConnection() {
        return streamConnection;
    }

    public TCPConnection getDataConnection() {
        return dataConnection;
    }

    public void closeConnections(){
        streamConnection.close();
        dataConnection.close();
    }

    public void terminateConnection(){
        streamConnection.Terminate();
        dataConnection.Terminate();
    }
}
