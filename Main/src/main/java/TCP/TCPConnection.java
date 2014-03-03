package TCP;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
    //TODO work on connection check
    private final String TAG = Connection.class.getSimpleName();

    // The time passes between each check inside the communication thread.
    public static int TIME_BETWEEN_CHECKS = 1000 * 5;

    /*Flags*/
    public static final int FLAG_CHECK_CONNECTION = 5000;
    public static final int FLAG_READ_XML = 5001;

    /*Key's*/
    public static final String CONNECTION_STATUS = "connection.status";
    public static final String CONNECTION_TYPE = "connection.type";
    public static final String CONNECTION_DEVICE_NAME = "connection.device_name";
    public static final String CONNECTION_ISSUE = "connection.issue";
    public static final String CONNECTION_MESSAGE = "connection.message";
    public static final String CONNECTION_MESSAGE_TYPE = "connection.message.type";

    public static final String CONTROLLER = "connection.controller";
    public static final String CONTROLLER_ACTION_RESULT = "connection.controller.action_result";

    /* ACTION*/
    public static final String ACTION_CLOSE_CONNECTION = "connection.action.close_connection";
    public static final String ACTION_TOGGLE_CONTROLLER = "connection.action.toggle_controller";

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
    public static final int XML_DATA_IS_RECEIVED = 6009;

    /*Connection Statuses*/
    public static final String CONNECTED = "connection.connected";
    public static final String DISCONNECTED = "connection.disconnected";
    public static final String CONNECTING = "connection.connecting";

    /*Connection Issues*/
    public static final String ISSUE_TIMEOUT_WHEN_CHECKING = "connection.issue.timeout_when_checking";
    public static final String ISSUE_AlREADY_CONNECTED = "connection.issue.already_connected";
    public static final String ISSUE_WIFI_DISABLED = "connection.issue.wifi_disabled";
    public static final String ISSUE_WIFI_TCP_SERVER_TIMEOUT = "connection.issue.wifi_tcp_server_timeout";
    public static final String ISSUE_NO_NETWORK_FOUND = "connection.issue.no_network_found";
    public static final String ISSUE_CONNECTED_TO_WRONG_NETWORK = "connection.issue.connected_to_wrong_network";
    public static final String ISSUE_NO_END_POINT = "connection.issue.no_server_end_point";// No server has been found for the given ip and port
    public static final String ISSUE_STREAM_FAILED = "connection.issue.stream_failed";
    public static final String ISSUE_CLOSED_VIA_ACTION = "connection.issue.connection_was_closed_via_action";

    /*Connection Types*/
    public static final int CLIENT = 9000;
    public static final int SERVER = 9001;

    /*Controller Types*/
    public static final int CONTROLLER_SOUND_RECORDER = 3000;
    public static final int CONTROLLER_SOUND_PLAYER = 3001;

    private static final String TYPE_WIFI_TCP_STRING = "Wifi TCP";

    private Context context;
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

    /* Wifi Info and Obj*/
    private WifiManager wifiManager;
    private ConnectivityManager connManager;
    private NetworkInfo wifi;

    /* Handler */
    private Handler handler;

    private int serverPort = 2000;// Default for 2000 TODO make dynamic
    private String serverIp;

    /* Listeners*/
    private ConnectionStateChangeListener connectionStateChangeListener; // Connection State Change listener
    private onConnectionLostListener onConnectionLost;
    private WifiStatesListener wifiStatesListener;
    private ActionEventListener actionEventListener;
    private IncomingDataListener incomingDataListener;

    private boolean preformConnectionCheck = false;// Flag that passes on to the communication thread and tell him to preform check or not.
    private boolean readXml = false;// Flag that passes on to the communication thread and tell him to format data received to xml file.

    /*-----Public Methods ------*/

    /*Constructor*/
    public TCPConnection(Context context){

        this.context = context;

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

                        closeConnectionThread();

                        commThread = new TCPCommThread( (Socket) msg.obj );
                        commThread.setHandler(this);
                        commThread.setCheckConnection(preformConnectionCheck);
                        commThread.setReadXml(readXml);
                        commThread.start();

                        connectionStatus = CONNECTED;
                        if ( connectionStateChangeListener != null)
                        {
                            connectionStateChangeListener.onConnected(TCPConnection.this.connectionType, tag);
                            connectionStateChangeListener.onConnectionChangeState(TCPConnection.this.connectionType, connectionStatus);
                        }
                        else  { Log.e(TAG, "Connection has no connection state change listener"); }

                        break;

                    case XML_DATA_IS_RECEIVED:

                        if (incomingDataListener != null)
                            incomingDataListener.onXmlReceived( (XMLParser) msg.obj);
                        else
                            Log.e(TAG, "No incoming data listener");

                        break;

                    case ERROR_ACCEPTATION_TIMEOUT:

                        Log.e(TAG, "TCP Acceptation Timeout");

                        connectionStatus = DISCONNECTED;

                        closeConnectionThread();

                        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionFailed(ISSUE_WIFI_TCP_SERVER_TIMEOUT); }
                        else  { Log.e(TAG, "No connection state change listener"); }

                        break;

                    case ERROR_CONNECTING_AS_CLIENT:

                        Log.e(TAG, "TCP Error connecting as client");

                        connectionStatus = DISCONNECTED;

                        closeConnectionThread();

                        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionFailed(ISSUE_NO_END_POINT); }
                        else  { Log.e(TAG, "No connection state change listener"); }


                        break;

                    case ERROR_MSG_NOT_SENT:

                        Log.e(TAG, "TCP Error message was not sent");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_CANT_OPEN_IN_OUT_STREAM:
                        Log.e(TAG, "TCP Error opening input and output stream");

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

        Log.i(TAG, "start, Server!. Current Connection Status = " + connectionStatus);

        // Start the tcp server thread if there isn't any other thread alive.
        if (connectionStatus.equals(DISCONNECTED))
        {
            registerActionReceiver();

            if (connectionType == CLIENT)
                tcpServerConnectionThread.interrupt();

            this.connectionType = SERVER;

            this.serverPort = serverPort;

            startConnectionThread();

            return true;
        }

        return false;
    }

    public boolean start(String ip, int serverPort){
        Log.i(TAG, "start, Client!. Current Connection Status = " + connectionStatus);

        if (connectionStatus.equals(DISCONNECTED))
        {
            registerActionReceiver();

            if (connectionType == SERVER)
                tcpServerConnectionThread.interrupt();

            this.connectionType = CLIENT;

            this.serverPort = serverPort;
            serverIp = ip;

            startConnectionThread();
        }


        return false;
    }

    public boolean write(String message){

        if (isConnected())
            commThread.write(message);

        return isConnected();
    }

    /* Stop running thread and set ConnectionStatus to Disconnected and call the start() method. */
    /**
     * @deprecated  this method is detracted till Library will be more stable */
    public void restart(){

        close();

        start(connectionType);

    }

    /** Close the connection and all his belonging, Stop communication, record, playing sound etc...*/
    public void close(){
        Log.i(TAG, "Closing Connection, Connection Type: " + connectionType );

        closeConnectionThread();

        stopCommunicationThread();

        unregisterWifiReceiver();

        unregisterActionReceiver();
    }

    /** @deprecated this method is detracted till Library will be more stable */
    public void Terminate(){
        close();

        if (tcpServerConnectionThread != null)
            tcpServerConnectionThread.close();
    }

    /** Return true if the state of the connection is CONNECTED any other state will return false(i.e DISCONNECTED, SCANNING, CONNECTING etc.*/
    public synchronized boolean isConnected(){
        return connectionStatus.equals(CONNECTED);
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

    /*-----Private Methods ------*/

    /** Close the connection when an issue occur and notify the onConnectionLost listener so the disconnection could be handled.*/
    private void close(String issue){

        String prevStatus = connectionStatus;

        close();

        // Only if was connected before close with issue notify user.
        if (prevStatus == CONNECTED)
        {
            if (onConnectionLost!= null)
                onConnectionLost.onConnectionLost(connectionType, issue );
            else
                Log.e(TAG, "No connection Lost Listener");
        }
        else if (prevStatus == CONNECTING)
        {
            if (connectionStateChangeListener != null)
                connectionStateChangeListener.onConnectionFailed(issue);
            else
                Log.e(TAG, "No connection change state listener");
        }
    }

    /** Start the connection Thread, Handle Server or Client options.*/
    private void startConnectionThread(){

        Log.i(TAG, "Start Connection Thread");

        connectionStatus = CONNECTING;

        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus); }
        else  { Log.d(TAG, "Connection has no connection state change listener"); }

        switch (connectionType)
        {
            case SERVER:
                if (serverSocket != null && !tcpServerConnectionThread.isClosed() &&!tcpServerConnectionThread.isAccepting())
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

    private void closeConnectionThread(){
        if (tcpServerConnectionThread != null)
            tcpServerConnectionThread.interrupt();
    }

    /** Stop the communication thread*/
    private void stopCommunicationThread(){
        Log.d(TAG, "stopCommunicationThread");
        connectionStatus = DISCONNECTED;

        if (commThread != null)
        {
            commThread.close();
        }
    }

    /* Getters and Setters */

    public String getCurrentWifiIp(){
        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);

        if (wifi.isConnected())
        {
            int ip =wifiManager.getConnectionInfo().getIpAddress();

            String ipString = String.format(
                    "%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));

            return ipString;
        }
        return null;
    }

    public boolean isConnectedToWifiNetwork(){

        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        wifiManager = (WifiManager) context.getSystemService(Activity.WIFI_SERVICE);

        return wifi.isConnected();
    }

    public synchronized String getConnectionStatus() {
        return connectionStatus;
    }

    public void setOnConnectionLost(onConnectionLostListener onConnectionLost) {
        this.onConnectionLost = onConnectionLost;
    }

    public void setConnectionStateChangeListener(ConnectionStateChangeListener listener){
        connectionStateChangeListener = listener;
    }

    public void setIncomingDataListener(IncomingDataListener incomingDataListener) {
        this.incomingDataListener = incomingDataListener;
    }

    public void setWifiStatesListener(WifiStatesListener wifiStatesListener) {
        registerWifiReceiver();
        this.wifiStatesListener = wifiStatesListener;
    }

    public void setActionEventListener(ActionEventListener actionEventListener) {
        this.actionEventListener = actionEventListener;
    }

    public void setReadXml(boolean readXml) {
        this.readXml = readXml;
    }

    public void setTimeBetweenChecks(int timeBetweenChecks) {
        this.TIME_BETWEEN_CHECKS = timeBetweenChecks;
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

    /* Broadcast Receiver - Wifi State Receiver - Action Receiver*/
    private void registerActionReceiver(){

        IntentFilter actionFilter = new IntentFilter(ACTION_CLOSE_CONNECTION);
        actionFilter.addAction(ACTION_TOGGLE_CONTROLLER);

        context.registerReceiver(actionReceiver, actionFilter);
    }

    private void unregisterActionReceiver(){
        try {
            context.unregisterReceiver(actionReceiver);
        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
        }
    }

    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "ActionReceiver, Action: " + intent.getAction());

            if (intent.getAction().equals(ACTION_CLOSE_CONNECTION))
                close();
            else if (intent.getAction().equals(ACTION_TOGGLE_CONTROLLER))
            {
                if (intent.getExtras() != null)
                {
                    if (isConnected())
                        switch (intent.getExtras().getInt(CONTROLLER))
                        {
                            case CONTROLLER_SOUND_RECORDER:
                                intent.putExtra(CONTROLLER_ACTION_RESULT, getRecordController().toggle());
                                break;

                            case CONTROLLER_SOUND_PLAYER:
                                intent.putExtra(CONTROLLER_ACTION_RESULT, getAudioController().toggle());
                                break;
                        }
                }
            }

            if (actionEventListener != null)
                actionEventListener.onActionEvent(intent);
            else
                Log.e(TAG, "No Action Event Listener");

        }
    };

    private void registerWifiReceiver(){

        IntentFilter wifiFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiReceiver, wifiFilter);
    }

    private void unregisterWifiReceiver(){
        try {
            context.unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
        }
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_DISABLED)
                {
                    if (wifiStatesListener != null)
                        wifiStatesListener.onDisabled();
                    else
                        Log.e(TAG, "no wifi states change listener");

                    close(ISSUE_WIFI_DISABLED);
                }
                else if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_ENABLED)
                {
                    if (wifiStatesListener != null)
                            wifiStatesListener.onEnabled();
                    else
                        Log.e(TAG, "no wifi states change listener");
                }
            }
            else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
            {
                NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (netInfo.isConnected())
                {
                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                    if (wifiStatesListener != null)
                        wifiStatesListener.onConnected(wifiInfo.getSSID());
                    else
                        Log.e(TAG, "no wifi states change listener");
                }
                else
                {
                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                    if (wifiStatesListener != null)
                        wifiStatesListener.onDisconnected();
                    else
                        Log.e(TAG, "no wifi states change listener");
                }
            }

        }
    };
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