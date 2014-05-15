package braunster.babymonitor;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import TCP.connrction_and_threads.TCPConnection;

/**
 * Created by itzik on 2/23/14.
 */
public class BabyMonitorAppObj extends Application {

    private TCPConnection streamConnection, dataConnection;
    public  SharedPreferences prefs;
    private final static String APIKEY = "a5522b00";

    private static BabyMonitorAppObj instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        BugSenseHandler.initAndStartSession(getApplicationContext(), APIKEY);

        streamConnection = new TCPConnection(getApplicationContext());
        dataConnection = new TCPConnection(getApplicationContext());

        dataConnection.setTextTag("DATA");
        streamConnection.setTextTag("STREAM");


    }

    public static BabyMonitorAppObj getInstance() {
        return instance;
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
