package braunster.babymonitor.objects;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.braunster.mymodule.app.connrction_and_threads.TCPConnection;
import com.bugsense.trace.BugSenseHandler;

import braunster.babymonitor.database.CallsDataSource;

/**
 * Created by itzik on 2/23/14.
 */
public class BabyMonitorAppObj extends Application {

    private TCPConnection streamConnection, dataConnection;
    public  SharedPreferences prefs;
    private final static String APIKEY = "a5522b00";
    private boolean visible = true;
    private CallsDataSource callsDataSource;

    private static boolean hasTelephonyService;
    private static BabyMonitorAppObj instance;
    private String dataSessionId = "";


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PackageManager pm = getBaseContext().getPackageManager();
        hasTelephonyService = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        callsDataSource = new CallsDataSource(getApplicationContext());
        BugSenseHandler.initAndStartSession(getApplicationContext(), APIKEY);

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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void terminateConnection(){
        streamConnection.Terminate();
        dataConnection.Terminate();
    }

    public CallsDataSource getCallsDataSource() {
        return callsDataSource;
    }

    public boolean hasTelephonyService() {
        return hasTelephonyService;
    }

    public String getDataSessionId() {
        return dataSessionId;
    }

    public void setDataSessionId(String dataSessionId) {
        this.dataSessionId = dataSessionId;
    }
}
