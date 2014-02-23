package braunster.babymonitor;

import android.app.Application;

import TCP.TCPConnection;

/**
 * Created by itzik on 2/23/14.
 */
public class BabyMonitorAppObj extends Application {

    private TCPConnection connection;

    @Override
    public void onCreate() {
        super.onCreate();
        connection = new TCPConnection(getApplicationContext());
    }

    public TCPConnection getConnection() {
        return connection;
    }
}
