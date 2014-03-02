package braunster.babymonitor;

import android.app.Application;

import com.bugsense.trace.BugSenseHandler;

import TCP.TCPConnection;

/**
 * Created by itzik on 2/23/14.
 */
public class BabyMonitorAppObj extends Application {

    private TCPConnection streamConnection, dataConnection;

    private final static String APIKEY = "7d589131";

    @Override
    public void onCreate() {
        super.onCreate();

        BugSenseHandler.initAndStartSession(getApplicationContext(), APIKEY);

        streamConnection = new TCPConnection(getApplicationContext());
        dataConnection = new TCPConnection(getApplicationContext());
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
}
