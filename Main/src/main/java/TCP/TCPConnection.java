package TCP;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;

/**
 * Created by itzik on 11/18/13.
 */
public class TCPConnection {
    private final String TAG = Connection.class.getSimpleName();

    /*Key's*/
    public static final String CONNECTION_STATUS = "connection.status";
    public static final String CONNECTION_TYPE = "connection.type";
    public static final String CONNECTION_DEVICE_NAME = "connection.device_name";
    public static final String CONNECTION_ISSUE = "connection.issue";
    public static final String CONNECTION_MESSAGE = "connection.message";
    public static final String CONNECTION_MESSAGE_TYPE = "connection.message.type";


    /* Handler Messages*/
    public static final int SERVER_SOCKET = 6000;
    public static final int SUCCESS_CONNECTION = 6001;
    public static final int ERROR_ACCEPTATION_TIMEOUT = 6002;
    public static final int ERROR_CONNECTING_AS_CLIENT = 6003;
    public static final int ERROR_CANT_OPEN_IN_OUT_STREAM = 6004; // CommThread can open input and output stream. Socket must be restarted.
    public static final int ERROR_AUDIO_STREAM_FAIL = 6005;
    public static final int ERROR_MSG_NOT_SENT = 6006;
    public static final int ERROR_OPENING_SERVER = 6007;
    public static final int ERROR_RECORD_STREAM_STOPPED = 6008;

    /*Connection Statuses*/
    public static final String CONNECTED = "connection.connected";
    public static final String DISCONNECTED = "connection.disconnected";
    public static final String CONNECTING = "connection.connecting";

    /*Connection Issues*/
    // General
    public static final String ISSUE_TIMEOUT_WHEN_CHECKING = "connection.issue.timeout_when_checking";
    public static final String ISSUE_AlREADY_CONNECTED = "connection.issue.already_connected";
    //Wifi
    public static final String ISSUE_WIFI_DISABLED = "connection.issue.wifi_disabled";
    public static final String ISSUE_WIFI_TCP_SERVER_TIMEOUT = "connection.issue.wifi_tcp_server_timeout";
    public static final String ISSUE_NO_NETWORK_FOUND = "connection.issue.no_network_found";
    public static final String ISSUE_CONNECTED_TO_WRONG_NETWORK = "connection.issue.connected_to_wrong_network";
    public static final String ISSUE_NO_END_POINT = "connection.issue.no_server_end_point";// No server has been found for the given ip and port
    public static final String ISSUE_STREAM_FAILED = "connection.issue.stream_failed";

    /*Connection Types*/
    public static final int CLIENT = 9000;
    public static final int SERVER = 9001;

    private static final String TYPE_WIFI_TCP_STRING = "Wifi TCP";

    private Activity activity;
    private int connectionType; // Contain the type of the connection. Wifi TCP, Bluetooth. (In future Server).
    private long lastCommunicationTime, currentCheckStartTime, lastCheckTime, startedTime, currentTime; // Time variables.
    private Check connectionCheck = new Check(); // The object that contain all the details of the connection check.
    private boolean isHandleLastCheck = true; // Flag indicating if the last connection check results was handel.
                                              // Set to true because false will make the connection try to handle an empty check object.

    private String connectionStatus = DISCONNECTED, connectionIssue;
    private Object tag; // The name of the device that the connection is bound to.
    private ServerSocket serverSocket;

    /* Threads*/
    private TCPCommThread commThread;
    private TCPServerConnectionThread tcpServerConnectionThread;

    /* Handler */
    private Handler handler;

    private int serverPort = 2000;// Default for 2000 TODO make dynamic
    private String serverIp;

    /* Listeners*/
    private ConnectionStateChangeListener connectionStateChangeListener; // Connection State Change listener
    private onConnectionLostListener onConnectionLost;
    private WifiStatesListener wifiStatesListener;

    private int timeBetweenChecks = 1 * (60* 1000); //  The time that pass between each connection check.

    private boolean preformConnectionCheck = true;

    /*Constructor*/
    public TCPConnection(){

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what)
                {
                    case SERVER_SOCKET:
                        serverSocket = (ServerSocket) msg.obj;
                        break;

                    case SUCCESS_CONNECTION :

                        Log.i(TAG, "TCP Connected");

                        startedTime = currentTime;

                        if (commThread != null) { commThread.interrupt(); }

                        commThread = new TCPCommThread( (Socket) msg.obj );
                        commThread.setHandler(this);
                        commThread.start();

                        connectionStatus = CONNECTED;
                        if ( connectionStateChangeListener != null)
                        {
                            connectionStateChangeListener.onConnected(TCPConnection.this.connectionType, tag);
                            connectionStateChangeListener.onConnectionChangeState(TCPConnection.this.connectionType, connectionStatus);
                        }
                        else  { Log.d(TAG, "Connection has no connection state change listener"); }

                        break;

                    case ERROR_ACCEPTATION_TIMEOUT:

                        Log.e(TAG, "TCP Acceptation Timeout");

                        connectionStatus = DISCONNECTED;

                        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionFailed(ISSUE_WIFI_TCP_SERVER_TIMEOUT); }
                        else  { Log.d(TAG, "No connection state change listener"); }

                        break;

                    case ERROR_CONNECTING_AS_CLIENT:

                        Log.e(TAG, "TCP Error connecting as client");

                        connectionStatus = DISCONNECTED;

                        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionFailed(ISSUE_NO_END_POINT); }
                        else  { Log.d(TAG, "No connection state change listener"); }


                        break;

                    case ERROR_MSG_NOT_SENT:

                        Log.e(TAG, "TCP Error opening input and output stream");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_CANT_OPEN_IN_OUT_STREAM:

                        Log.e(TAG, "TCP Error message was not sent");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);


                        break;

                    case ERROR_AUDIO_STREAM_FAIL:

                        Log.e(TAG, "TCP Error Audio stream fail");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_RECORD_STREAM_STOPPED:
                        Log.e(TAG, "TCP Error Record stream fail");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;
                }
            }
        };
    }

   /*Start the connection object.*/
    public boolean start(int serverPort){

        this.connectionType = SERVER;

        this.serverPort = serverPort;

        // Start the tcp server thread if there isn't any other thread alive.
        if (connectionStatus.equals(DISCONNECTED))
        {
            startConnectionThread();
        }

        return false;
    }

    public boolean start(String ip, int serverPort){

        this.connectionType = CLIENT;

        this.serverPort = serverPort;
        serverIp = ip;

        // Start the tcp server thread if there isn't any other thread alive.
        if (connectionStatus.equals(DISCONNECTED))
        {
            startConnectionThread();
        }


        return false;
    }

    /* Stop running thread and set ConnectionStatus to Disconnected and call the start() method. */
    public void restart(){

        close();

        start(connectionType);

    }

    /** Return AudioController interface to control the audio stream*/
    public TCPCommThread.AudioController getAudioController(){
        if (connectionStatus.equals(CONNECTED) && commThread != null)
            return commThread.getAudioController();
        else
            return null;
    }

    public TCPCommThread.RecordController getRecordController(){
        if (connectionStatus.equals(CONNECTED) && commThread != null)
            return commThread.getRecordController();
        else return null;
    }

    public synchronized boolean isConnected(){
        return connectionStatus.equals(CONNECTED);
    }

    public void close(){
        Log.i(TAG, "Closing Connection, Connection Type: " + connectionType );

        stopCommunicationThread();
    }

    public void Terminate(){
        close();

        if (tcpServerConnectionThread != null)
            tcpServerConnectionThread.close();
    }

    private void close(String issue){
        Log.i(TAG, "Closing Connection, Connection Type: " + connectionType );

        if (onConnectionLost!= null)
            onConnectionLost.onConnectionLost(connectionType, issue );

        stopCommunicationThread();
    }
    
    private void startConnectionThread(){

        connectionStatus = CONNECTING;

        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus); }
        else  { Log.d(TAG, "Connection has no connection state change listener"); }

        switch (connectionType)
        {
            case SERVER:
                if (serverSocket != null)
                    tcpServerConnectionThread.startAccept(true);
                else
                {
                    tcpServerConnectionThread = new TCPServerConnectionThread(serverPort);
                    tcpServerConnectionThread.setHandler(handler);
                    tcpServerConnectionThread.start();
                }

                break;

            case CLIENT:
                tcpServerConnectionThread = new TCPServerConnectionThread(serverIp, serverPort);
                tcpServerConnectionThread.setHandler(handler);
                tcpServerConnectionThread.start();

                break;
        }

    }

    private void stopCommunicationThread(){
        Log.d(TAG, "stopCommunicationThread");
        connectionStatus = DISCONNECTED;

        if (commThread != null)
        {
            commThread.close();
        }
    }

    /* Getters and Setters */

    public synchronized String getConnectionStatus() {
        return connectionStatus;
    }

    public void setOnConnectionLost(onConnectionLostListener onConnectionLost) {
        this.onConnectionLost = onConnectionLost;
    }

    public void setConnectionStateChangeListener(ConnectionStateChangeListener listener){
        connectionStateChangeListener = listener;
    }

    public void setWifiStatesListener(WifiStatesListener wifiStatesListener) {
        this.wifiStatesListener = wifiStatesListener;
    }

    public void setTimeBetweenChecks(int timeBetweenChecks) {
        this.timeBetweenChecks = timeBetweenChecks;
    }

    public long getLastCommunicationTime() {
        return lastCommunicationTime;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    public long getStartedTime() {
        return startedTime;
    }

    public int getConnectionType() {
        return connectionType;
    }

    public String getConnectionTypeInString() {
            return TYPE_WIFI_TCP_STRING;
    }

    public void preformConnectionCheck(boolean preformConnectionCheck) {
        this.preformConnectionCheck = preformConnectionCheck;
    }

    public boolean isPreformConnectionCheck() {
        return preformConnectionCheck;
    }

    private BroadcastReceiver wifiS
}

/*    public void write(Msg msg){
        //TODO write method

        // Check the connection status and connection type
        if (connectionStatus.equals(CONNECTED))
        {
            if ( connectionType == Connection.TYPE_WIFI_TCP && commThread != null)
            {
                commThread.write(msg);
            }
            else if (connectionType == TYPE_BLUETOOTH && connectedThread != null)
            {
                connectedThread.write(msg);
            }
        } else { Log.d(TAG, "trying to send a message to a connection that isn't connected."); if (arduinoDevice!= null) Log.d(TAG, "Arduino Device Name: " + arduinoDevice.getDeviceName()); }

        // TODO Handle messages that want to get notified when received

    }*/

    /*Initialize the check*/
/*    public void startCheck() {
        Log.i(TAG, " Check is started, Current Time: " + currentTime);
        this.currentCheckStartTime = currentTime;
        connectionCheck.setStartedTime(currentTime);
    }*/

    /*Set the current time for the connection obj.(called from the running thread). The method will save the time given to currentTime and pass the time to the connection check.
    * The connection check will take the time and do a timeout check. The method then also check to see if there's unhandled check if so it will handle it.*/
/*    public void setTime(long time){

        currentTime = time;

        if (connectionStatus.equals(CONNECTED))
        {
            // Check if the last connection check is finished.
            if (preformConnectionCheck)
                if ( (connectionCheck.getStatus() == Check.DONE || connectionCheck.getStatus() == Check.NOT_STARTED) )
                {
                    // The finished check is un handled.
                    if (!isHandleLastCheck)
                    {
                        handleCheck();
                    }
                    else if ( currentTime - lastCommunicationTime > timeBetweenChecks && currentTime - lastCheckTime > timeBetweenChecks)
                    {
//                    Log.i(TAG, timeBetweenChecks + " from last communication...");

                        // Set the started time of the connection check.
                        startCheck();

                        isHandleLastCheck = false;

                        // To much time passed since the last communication and the connection check so the system is performing a connection check.
                        write(Check.getCheckMessage());
                    }
                }
                // Check for timeout in the connectionCheck. and update his time.
                else connectionCheck.checkForTimeout(time);
        }
        else if ( connectionStatus.equals(CONNECTING))
        {
            // TODO handle connecting timeout
        }
        else if ( connectionStatus.equals(DISCONNECTED))
        {
            // TODO handle connection status DISconnected
        }

    }*/

    /* Handle incoming message */
/*    public void handleMessage(String message){

        lastCommunicationTime = currentTime;

        // Any message from the arduino will terminate the check progress.
        if (connectionCheck.getStatus() == Check.ON_PROGRESS)
        {
            connectionCheck.setResult(Check.SUCCESS);
            connectionCheck.done();
            Log.i(TAG, "Connection check is done, The check was successful");
        }

        String type = message;
        // TODO extract the type out of the message

        // The type is check command
        if (type.equals(String.valueOf(Command.CHECK)))
        {
            write(Msg.getCheckOkMessage(connectionType));
        }
        else if (type.equals(String.valueOf(Command.CHECK_OK)))
        {

        }
        else
        {
            if (type.equals(String.valueOf(Command.RECEIVED)))
            {
                // TODO get the message id from the receive
            }
            else
            {
                // TODO handle more messages type

                write(Msg.getReceivedMessage(connectionType));

                // Check to see if theres a listeners assigned if so use it.
                if (messagesListener != null)  messagesListener.onMessageReceived(message); else Log.d(TAG, "Connection dosen't have message listener");
            }
        }
    }*/

    /* Handle check result */
//    private void handleCheck(){
////        Log.d(TAG, "Handling check");
//
//        isHandleLastCheck = true;
//        lastCheckTime = currentCheckStartTime;
//
//        switch (connectionCheck.getResult())
//        {
//            case Check.FAILED:
//                Log.i(TAG, " Connection has lost... Failed");
//                connectionStatus = DISCONNECTED;
//                if (connectionStateChangeListener != null) { onConnectionLost.onConnectionLost(connectionType, connectionIssue); connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus); }
//                else { Log.d(TAG, "Connection has no connection lost listener"); }
//                break;
//
//            case Check.TIMEOUT:
//                Log.i(TAG, " Connection has lost... Timeout...");
//                connectionStatus = DISCONNECTED;
//                if (connectionStateChangeListener != null)
//                {
//                    connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus);
//                }
//                else { Log.d(TAG, "Connection has connection changed listener"); }
//
//                if (onConnectionLost != null)
//                {
//                    onConnectionLost.onConnectionLost(connectionType, connectionIssue);
//                }
//                else { Log.d(TAG, "Connection has no connection lost listener"); }
//
//                break;
//            case Check.SUCCESS:
//
//                if ( connectionStatus.equals(DISCONNECTED) || connectionStatus.equals(CONNECTING)  )
//                {
//                    Log.i(TAG, " The connection has been made...");
//                    connectionStatus = CONNECTED;
//                    if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnected(connectionType, tag); connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus); }
//                    else  { Log.d(TAG, "Connection has no connection state change listener"); }
//                }
//                break;
//        }
//    }