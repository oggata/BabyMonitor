package TCP.connrction_and_threads;

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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import TCP.interfaces.ActionEventListener;
import TCP.interfaces.ConnectionStateChangeListener;
import TCP.interfaces.IncomingDataListener;
import TCP.interfaces.WifiStatesListener;
import TCP.interfaces.onConnectionLostListener;
import TCP.objects.InOutStreams;
import TCP.xml.objects.XmlMessage;
import TCP.xml.objects.XmlTag;

/**
 * Created by itzik on 11/18/13.
 */
public class TCPConnection {
    //TODO work on connection check
    private String TAG = Connection.class.getSimpleName();
    private static final boolean DEBUG = true;

    // The time passes between each check inside the communication thread.
    public static int TIME_BETWEEN_CHECKS = 1000 * 5;

    /*Flags*/
    public static final int FLAG_CHECK_CONNECTION = 5000;
    public static final int FLAG_READ_XML = 5001; // Flag that passes on to the communication thread and tell him to format data received to xml file.
    public static final int FLAG_READ_STRING = 5002;

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
    public static final int ERROR_RECORD_STREAM_STOPPED = 6008;
    public static final int XML_DATA_IS_RECEIVED = 6009;
    public static final int ERROR_CANT_OPEN_SERVER = 6010;
    public static final int IN_OUT_STREAMS_OPEN = 6011;
    public static final int ERROR_IN_STREAM = 6012;
    public static final int STRING_DATA_RECEIVED = 6013;

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
    public static final String ISSUE_CLOSED_BY_USER = "connection.issue.connection_was_closed_by_user";
    public static final String ISSUE_OPENING_A_SERVER = "connection.issue.opening_a_serer";

    /*Connection Types*/
    public static final int CLIENT = 9000;
    public static final int SERVER = 9001;

    /*Controller Types*/
    public static final int CONTROLLER_SOUND_RECORDER = 3000;
    public static final int CONTROLLER_SOUND_PLAYER = 3001;

    private static final String TYPE_WIFI_TCP_STRING = "Wifi TCP";

    private Context context;
    private int connectionType; // Contain the type of the connection. Wifi TCP, Bluetooth. (In future Server).
    long lastCommunicationTime, currentCheckStartTime, lastCheckTime, startedTime, currentTime; // Time variables.

    private String connectionStatus = DISCONNECTED;
    private Object tag; // The name of the device that the connection is bound to.
    private ServerSocket serverSocket;
    private Socket socket;
    private Map<Integer, Boolean> flags = new HashMap<Integer, Boolean>();
    private List<String> xmlTags;

    private InOutStreams inOutStreams;
    private AudioStreamController audioStreamController;
    private TCPMessenger messenger;
    private TCPXmlReader xmlReader;
    private TCPStringReader stringReader;

    /* Threads*/
//    private TCPCommThread commThread;
    private TCPServerConnectionThread tcpServerConnectionThread;
    private TCPStreamsOpener tcpStreamsOpener;

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

    private static int id_count = 1;

    /*-----Public Methods ------*/

    /*Constructor*/
    public TCPConnection(Context context){

        if (DEBUG)
        {
            TAG += " - " + id_count;
            id_count++;
        }

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

                        if (DEBUG) Log.i(TAG, "TCP Connected");

                        startedTime = currentTime;

                        stopConnectionThread();

                        socket = (Socket) msg.obj;
                        tcpStreamsOpener = new TCPStreamsOpener(socket, this);
                        tcpStreamsOpener.start();

                        break;

                    case IN_OUT_STREAMS_OPEN :

                        if (DEBUG) Log.i(TAG, "In and Out streams are open.");

                        inOutStreams = (InOutStreams) msg.obj;

                        if (flags.containsKey(FLAG_READ_XML)) initXmlReader();
                        else if (flags.containsKey(FLAG_READ_STRING)) initStringReader();

                        if (flags.containsKey(FLAG_CHECK_CONNECTION))
                        {
                            initMessenger();
                            messenger.setCheckConnection(true);
                        }

                        connectionStatus = CONNECTED;

                        dispatchConnectionChangedStateEvent(ConnectionStateChangeListener.TYPE_CONNECTED, null);
                        dispatchConnectionChangedStateEvent(ConnectionStateChangeListener.TYPE_STATE, null);

                        break;

                    case XML_DATA_IS_RECEIVED:
                        // Set the last time the devices communicate successfully.
                        lastCommunicationTime = System.currentTimeMillis();

                        XmlTag xmlTag = ((XmlTag) msg.obj);

                        if (xmlTag == null) return;

                        // Catch connection checks
                        if (xmlTag.getChildren().size() == 0 && xmlTag.getName().equals(XmlMessage.XML_TAG_CHECK))
                        { if (DEBUG) Log.d(TAG, "Check Message Received: " + xmlTag.getName()); return; }

                        if (DEBUG) Log.d(TAG, "Xml Data Received: " + xmlTag.getChildren().size());

                        if (incomingDataListener != null)
                            incomingDataListener.onParedXmlReady((XmlTag) msg.obj);
                        else
                            if (DEBUG) Log.e(TAG, "No incoming data listener");

                        break;

                    case STRING_DATA_RECEIVED:
                        // Set the last time the devices communicate successfully.
                        lastCommunicationTime = System.currentTimeMillis();

                        if (DEBUG) Log.d(TAG, "String Data Received: " + msg.obj);

                        if (incomingDataListener != null)
                            incomingDataListener.onStringDateReceived((String) msg.obj);
                        else
                        if (DEBUG) Log.e(TAG, "No incoming data listener");

                        break;

                    case ERROR_ACCEPTATION_TIMEOUT:

                        if (DEBUG) Log.e(TAG, "TCP Acceptation Timeout");

                    connectionStatus = DISCONNECTED;

                    tcpServerConnectionThread.close();

                    dispatchConnectionChangedStateEvent(ConnectionStateChangeListener.TYPE_FAILED, ISSUE_WIFI_TCP_SERVER_TIMEOUT);

                    break;

                    case ERROR_CANT_OPEN_SERVER:

                        if (DEBUG) Log.e(TAG, "TCP Error Opening a server");

                        connectionStatus = DISCONNECTED;

                        tcpServerConnectionThread.close();

                        dispatchConnectionChangedStateEvent(ConnectionStateChangeListener.TYPE_FAILED, ISSUE_OPENING_A_SERVER);

                        break;

                    case ERROR_CONNECTING_AS_CLIENT:

                        if (DEBUG) Log.e(TAG, "TCP Error connecting as client");

                        connectionStatus = DISCONNECTED;

                        stopConnectionThread();

                        dispatchConnectionChangedStateEvent(ConnectionStateChangeListener.TYPE_FAILED, ISSUE_NO_END_POINT);

                        break;

                    case ERROR_MSG_NOT_SENT:

                        if (DEBUG) Log.e(TAG, "TCP Error message was not sent");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_CANT_OPEN_IN_OUT_STREAM:
                        if (DEBUG) Log.e(TAG, "TCP Error opening input and output stream");

                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_AUDIO_STREAM_FAIL:

                        if (DEBUG) Log.e(TAG, "TCP Error Audio stream fail");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_RECORD_STREAM_STOPPED:
                        if (DEBUG) Log.e(TAG, "TCP Error Record stream fail");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;

                    case ERROR_IN_STREAM:
                        if (DEBUG) Log.e(TAG, "TCP Error stream fail, maybe socket is closed.");
                        // TODO Find best solution for this problem

                        close(ISSUE_STREAM_FAILED);

                        break;
                }
            }
        };
    }

   /*Start the connection object.*/
    public boolean start(int serverPort){

        if (DEBUG) Log.i(TAG, "start, Server!. Current Connection Status = " + connectionStatus);

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
        if (DEBUG) Log.i(TAG, "start, Client!. Current Connection Status = " + connectionStatus);

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
        validate("write");

        if (messenger == null || messenger.isClosed()) { initMessenger(); }

        messenger.write(message);

        /*if (isConnected())
            commThread.write(message);*/

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
        if (DEBUG) Log.i(TAG, "Closing Connection, Connection Type: " + connectionType );

        connectionStatus = DISCONNECTED;

        stopConnectionThread();

        stopCommunicationThreads();

        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        //        unregisterWifiReceiver();
//
//        unregisterActionReceiver();
    }

    /** Close the connection and all his belonging, Stop communication, record, playing sound etc...*/
    public void closeAndTriggerConnectionLost(){
        close(ISSUE_CLOSED_BY_USER);
    }

    /** @deprecated this method is detracted till Library will be more stable */
    public void Terminate(){
        close();

        unregisterActionReceiver();

        unregisterWifiReceiver();
    }

    /** Return true if the state of the connection is CONNECTED any other state will return false(i.e DISCONNECTED, SCANNING, CONNECTING etc.*/
    public synchronized boolean isConnected(){
        return connectionStatus.equals(CONNECTED);
    }

    public void addFlag(int flag){
        flags.put(flag, true);
    }

    /** Return AudioController interface to control the audio stream*/
    public AudioStreamController.AudioController getAudioController(){
        validate("getAudioController");

        if (audioStreamController == null || audioStreamController.isClosed()) initAudioController();

        return audioStreamController.getAudioController();
        /*if (connectionStatus.equals(CONNECTED) && commThread != null)
            return commThread.getAudioController();
        else
            return null;*/
    }

    public AudioStreamController.RecordController getRecordController(){
        validate("getRecordController");

        if (audioStreamController == null || audioStreamController.isClosed()) initAudioController();

        return audioStreamController.getRecordController();

        /*if (connectionStatus.equals(CONNECTED) && commThread != null)
            return commThread.getRecordController();
        else return null;*/
    }

    /*--- { Private Methods } ---*/

    /** Close the connection when an issue occur and notify the onConnectionLost listener so the disconnection could be handled.*/
    private void close(String issue){

        String prevStatus = connectionStatus;

        close();

        // Only if was connected before close with issue notify user.
        if (prevStatus.equals(CONNECTED))
        {
            dispatchConnectionLost(issue);
        }
        else if (prevStatus.equals(CONNECTING))
        {
            dispatchConnectionChangedStateEvent(ConnectionStateChangeListener.TYPE_FAILED, issue);
        }
    }

    /** Start the connection Thread, Handle Server or Client options.*/
    private void startConnectionThread(){

        if (DEBUG) Log.i(TAG, "Start Connection Thread");

        connectionStatus = CONNECTING;

        if ( connectionStateChangeListener != null) { connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus); }
        else  { if (DEBUG) Log.d(TAG, "Connection has no connection state change listener"); }

        switch (connectionType)
        {
            case SERVER:
                if (serverSocket != null && !tcpServerConnectionThread.isClosed() &&!tcpServerConnectionThread.isAccepting())
                    tcpServerConnectionThread.startAccept(true);
//                    tcpServerConnectionThread.close();

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

    private void stopConnectionThread(){
        if (tcpServerConnectionThread != null)
        {
            tcpServerConnectionThread.close();
            tcpServerConnectionThread.interrupt();
        }
    }

    /** Stop the communication thread*/
    private void stopCommunicationThreads(){
        if (DEBUG) Log.d(TAG, "stopCommunicationThread");

        if (audioStreamController != null) audioStreamController.close();

        if (messenger != null) messenger.close();

        if (xmlReader != null) xmlReader.close();

        if (tcpStreamsOpener != null) tcpStreamsOpener.close();

//        if (commThread != null)
//        {
//            commThread.close();
//        }
    }

    private void dispatchConnectionLost(String issue){
        if (onConnectionLost!= null)
            onConnectionLost.onConnectionLost(connectionType, issue );
        else
            if (DEBUG) Log.e(TAG, "No connection Lost Listener");
    }

    private void dispatchConnectionChangedStateEvent(int type, String issue){
        if (connectionStateChangeListener != null)
            switch (type)
            {
                case ConnectionStateChangeListener.TYPE_STATE:
                    connectionStateChangeListener.onConnectionChangeState(connectionType, connectionStatus);
                    break;

                case ConnectionStateChangeListener.TYPE_CONNECTED:
                    connectionStateChangeListener.onConnected(connectionType, tag);
                    break;

                case ConnectionStateChangeListener.TYPE_FAILED:
                    connectionStateChangeListener.onConnectionFailed(issue);
                    break;
            }
        else
        if (DEBUG) Log.e(TAG, "No connection change state listener");
    }

    /** Validate that the connection is connected and that the inOutStreams are initialized.*/
    private void validate(String funcName){
        if (!isConnected()) if (DEBUG) throw new IllegalStateException(funcName + ", No connection! The connection isn't connected to any client/server");

        if (inOutStreams == null) throw new IllegalStateException("in and out streams are closed.");
    }

    private void initAudioController(){
        if (DEBUG) Log.i(TAG, "initAudioController.");
        audioStreamController = new AudioStreamController(inOutStreams, handler);
        audioStreamController.start();
    }

    private void initMessenger(){
        if (DEBUG) Log.i(TAG, "initMessenger.");
        messenger = new TCPMessenger(inOutStreams, handler, this);
        messenger.start();
    }

    private void initXmlReader(){
        if (DEBUG) Log.i(TAG, "initXmlReader.");
        xmlReader = new TCPXmlReader(inOutStreams, handler, this);
        xmlReader.setTags(xmlTags);
        xmlReader.start();
    }

    private void initStringReader(){
        if (DEBUG) Log.i(TAG, "initXmlReader.");
        stringReader = new TCPStringReader(inOutStreams, handler);
        stringReader.start();
    }

    /* --- {  Getters and Setters } ---*/
    public Socket getSocket() {
        return socket;
    }

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

    public int getConnectionType() {
        return connectionType;
    }

    public String getConnectionTypeInString() {
            return TYPE_WIFI_TCP_STRING;
    }

    public void setXmlTags(List<String> xmlTags) {
        this.xmlTags = xmlTags;
    }

    public void setTextTag(String tag){
        TAG += " - " + tag;
    }

    /*--- {  Broadcast Receiver - Wifi State Receiver - Action Receiver } ---*/
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

            if (DEBUG) Log.d(TAG, "ActionReceiver, Action: " + intent.getAction());

            String action = intent.getAction();
            if (action == null)
                    return;

            if (action.equals(ACTION_CLOSE_CONNECTION))
                close();
            else if (action.equals(ACTION_TOGGLE_CONTROLLER))
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
                if (DEBUG) Log.e(TAG, "No Action Event Listener");

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

            String action = intent.getAction();

            if (action == null)
                return;

            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_DISABLED)
                {
                    if (wifiStatesListener != null)
                        wifiStatesListener.onDisabled();
                    else
                    {
                        if (DEBUG) Log.e(TAG, "no wifi states change listener");
                        close(ISSUE_WIFI_DISABLED);
                    }
                }
                else if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_ENABLED)
                {
                    if (wifiStatesListener != null)
                            wifiStatesListener.onEnabled();
                    else
                        if (DEBUG) Log.e(TAG, "no wifi states change listener");
                }
            }
            else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
            {
                NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (netInfo == null)
                    return;

                if (netInfo.isConnected())
                {
                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                    if (wifiInfo == null)
                        return;

                    if (wifiStatesListener != null)
                        wifiStatesListener.onConnected(wifiInfo.getSSID());
                    else
                        if (DEBUG) Log.e(TAG, "no wifi states change listener");
                }
                else
                {
                    if (wifiStatesListener != null)
                        wifiStatesListener.onDisconnected();
                    else
                        if (DEBUG) Log.e(TAG, "no wifi states change listener");
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