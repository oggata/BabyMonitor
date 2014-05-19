package braunster.babymonitor.activities;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import TCP.connrction_and_threads.TCPConnection;
import TCP.interfaces.ActionEventListener;
import TCP.interfaces.ConnectionStateChangeListener;
import TCP.interfaces.IncomingDataListener;
import TCP.interfaces.onConnectionLostListener;
import TCP.objects.TList;
import TCP.xml.objects.XmlAttr;
import TCP.xml.objects.XmlMessage;
import TCP.xml.objects.XmlTag;
import braunster.babymonitor.R;
import braunster.babymonitor.fragements.BaseFragment;
import braunster.babymonitor.fragements.ConnectedFragment;
import braunster.babymonitor.fragements.SetupFragment;
import braunster.babymonitor.objects.Call;
import braunster.babymonitor.objects.NotUtil;
import braunster.babymonitor.objects.Prefs;
import braunster.babymonitor.receivers.IncomingCallReceiver;

public class MonitorActivity extends BaseActivity implements ActionEventListener, IncomingCallReceiver.CallsAndSMSListener{

    private static WeakReference<MonitorActivity> wrActivity = null;

    private final String TAG = MonitorActivity.class.getSimpleName();
    private static final boolean DEBUG = true;
    private SetupFragment setupFragment;
    private ConnectedFragment connectedFragment;

    private static final int FADE_DURATION = 400;

    private Bundle fragmentExtras;
    private Point screenSize = new Point();

    private View mainView;

    private IncomingCallReceiver incomingCallReceiver = new IncomingCallReceiver();

    /* Phone Battery*/
    public static final String XML_TAG_BATTERY = "battery";
    public static final String XML_ATTRIBUTE_BATTERY_STATUS = "status";
    public static final String XML_ATTRIBUTE_BATTERY_PERCENTAGE = "percentage";

    /* Incoming Data*/
    public static final String XML_TAG_PHONE_DATA = "phone_data";
    public static final String XML_TAG_SMS = "sms";
    public static final String XML_ATTRIBUTE_PHONE_NUMBER = "phone_number";
    public static final String XML_ATTRIBUTE_CALL_STATE = "phone_state";
    public static final String XML_ATTRIBUTE_TODO = "todo";
    public static final String XML_ATTRIBUTE_CALLER_CONTACT_NAME = "caller_contact_name";

    /* Call States*/
    private static final String CALL_STATE_HANG_UP = "hang_up";
    private static final String CALL_STATE_RINGING = "ringing";
    private static final String CALL_STATE_DIALING = "dailing";

    /*SMS Options*/
    public static final String READ = "read";
    public static final String SEND = "send";

    //Keep the last level received of the battery
    private int batteryPercentage = -1;
    private String batteryStatus = "";
    private String  phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wrActivity = new WeakReference<MonitorActivity>(this);

        mainView = this.getLayoutInflater().inflate(R.layout.activity_monitor, null);
        setContentView(mainView);

        TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tMgr.getLine1Number();

        fragmentExtras = new Bundle();

        if (DEBUG) Log.v(TAG, "OnCreate, isConnected: " + String.valueOf(app.getStreamConnection().isConnected()));

        if (savedInstanceState != null)
        {
            if (app.getDataConnection().isServer())
            {
                if (batteryPercentage == -1)
                {
                    batteryPercentage = savedInstanceState.getInt(XML_ATTRIBUTE_BATTERY_PERCENTAGE, -1);
                    batteryStatus = savedInstanceState.getString(XML_ATTRIBUTE_BATTERY_STATUS, "");
                }
            }
            if (DEBUG) Log.v(TAG, "savedInstanceState not null.");
        } else if (DEBUG) Log.v(TAG, "no savedInstanceState budle");
    }

    private void actionBarSetup(boolean isConnected) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActionBar();
            ab.setTitle("Monitor");
            ab.setSubtitle(isConnected ? "Connected" : "Disconnected");

            if (isConnected)
                getActionBar().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            else
                getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_dark_grey)));

        }
    }

    private void createConnectedFragment(Bundle extras){
        if (!app.isVisible())
            return;

        actionBarSetup(true);

        if (extras != null)
            fragmentExtras = extras;

        initBaseExtras();

        connectedFragment = new ConnectedFragment();

        if (app.getDataConnection().isServer())
            if (batteryPercentage != -1)
            {
                fragmentExtras.putInt(XML_ATTRIBUTE_BATTERY_PERCENTAGE, batteryPercentage);
                fragmentExtras.putString(XML_ATTRIBUTE_BATTERY_STATUS, batteryStatus);
            }



        createF(connectedFragment);
    }

    private void createSetupFragment(){
        if (!app.isVisible())
            return;

        actionBarSetup(false);

        setupFragment = new SetupFragment();

        initBaseExtras();

        createF(setupFragment);
    }

    private void createF(BaseFragment f){
        f.setArguments(fragmentExtras);

        FragmentTransaction ft = wrActivity.get().getFragmentManager().beginTransaction();

        ft.setCustomAnimations(R.anim.in_left, R.anim.out_right);

        ft.replace(R.id.container, f).commitAllowingStateLoss();

        initDataConnection();
        initStreamConnection();

        fragment = f;
    }

    /** Initiate the connection obj, Apply listeners etc.*/
    private void initStreamConnection(){

        if(DEBUG) Log.d(TAG, "initStreamConnection");

        app.getStreamConnection().setActionEventListener(MonitorActivity.this);

        app.getStreamConnection().setConnectionStateChangeListener(new ConnectionStateChangeListener() {
            @Override
            public void onConnected(int connectionType, Object obj, String sessionID) {
                if(DEBUG) Log.d(TAG, "Connected");

                createConnectedFragment(null);
            }

            @Override
            public void onConnectionChangeState(int connectionType, String state) {

            }

            @Override
            public void onConnectionFailed(String issue) {
                if(DEBUG) Log.d(TAG, "Connection Failed, Issue: " + issue);

                if (setupFragment != null)
                    setupFragment.onFailed();

                if (issue.equals(TCPConnection.ISSUE_NO_END_POINT))
                    Toast.makeText(MonitorActivity.this, "Please open select the parent phone first", Toast.LENGTH_LONG).show();
                else if (issue.equals(TCPConnection.ISSUE_WIFI_TCP_SERVER_TIMEOUT))
                    Toast.makeText(MonitorActivity.this, "Timeout! You need to press the baby phone.", Toast.LENGTH_LONG).show();
                else if (issue.equals(TCPConnection.ISSUE_OPENING_A_SERVER))
                {
                    Toast.makeText(MonitorActivity.this, "Please select a different port. Preferred from 2000 and above.", Toast.LENGTH_LONG).show();
                    app.getStreamConnection().close();
                }
            }
        });

        app.getStreamConnection().setOnConnectionLost(new onConnectionLostListener() {
            @Override
            public void onConnectionLost(int connectionType, String issue) {
               if(DEBUG) Log.d(TAG, "onConnection Lost");
                closeLogic(issue, app.getStreamConnection(), app.getDataConnection());
            }
        });
    }

    private void initDataConnection(){
        if(DEBUG) Log.d(TAG, "initDataConnection");

        // set some flags
        app.getDataConnection().addFlag(TCPConnection.FLAG_READ_XML);
        app.getDataConnection().addFlag(TCPConnection.FLAG_CHECK_CONNECTION);

        app.getDataConnection().setXmlTags(getXmlTags());

        app.getDataConnection().setConnectionStateChangeListener(new ConnectionStateChangeListener() {
            @Override
            public void onConnected(int connectionType, Object obj, String sessionId) {
                if(DEBUG) Log.d(TAG, "Data Connection  Connected");

                app.setDataSessionId(sessionId);

                // Register for battery data if this is the phone near th baby.
                if (!app.getDataConnection().isServer()) {
                    registerReceiver(mBatInfoReceiver,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                    incomingCallReceiver.setFilter("android.intent.action.PHONE_STATE", "android.intent.action.NEW_OUTGOING_CALL");

                    registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                }
                else
                {
                    // if user want to use call forwarding the server phone send his phone number to the client phone
                    if (app.prefs.getBoolean(Prefs.USE_CALL_FORWARDING, true))
                    {
                        TelephonyManager tMgr = (TelephonyManager)MonitorActivity.this.getSystemService(Context.TELEPHONY_SERVICE);
                        String mPhoneNumber = tMgr.getLine1Number();

                        if (mPhoneNumber != null)
                            sendDataXml(XML_TAG_PHONE_DATA, null, new TList<XmlAttr>(new XmlAttr(XML_ATTRIBUTE_TODO, Prefs.USE_CALL_FORWARDING), new XmlAttr(XML_ATTRIBUTE_PHONE_NUMBER, mPhoneNumber)) );
                        else
                        {
                            Toast.makeText(MonitorActivity.this, "Cant use call forwarding for this device", Toast.LENGTH_SHORT).show();
                            app.prefs.edit().putBoolean(Prefs.USE_CALL_FORWARDING, false).commit();
                        }
                    }

                    if (app.prefs.getBoolean(Prefs.USE_SMS_TUNNELING, true)) {
                        sendDataXml(XML_TAG_PHONE_DATA, null, new TList<XmlAttr>(new XmlAttr(XML_ATTRIBUTE_TODO, Prefs.USE_SMS_TUNNELING)));
                    }
                }
            }

            @Override
            public void onConnectionChangeState(int connectionType, String state) {

            }

            @Override
            public void onConnectionFailed(String issue) {
                if(DEBUG) Log.d(TAG, "Data Connection  Failed");

                if (issue.equals(TCPConnection.ISSUE_OPENING_A_SERVER))
                {
                    Toast.makeText(MonitorActivity.this, "Connection have a problem please select a different port and try again." , Toast.LENGTH_LONG).show();
                }
                // TODO notify setup problem of this problem
            }
        });

        app.getDataConnection().setOnConnectionLost(new onConnectionLostListener() {
            @Override
            public void onConnectionLost(int connectionType, final String issue) {
                if(DEBUG) Log.d(TAG, "Data Connection onConnection Lost, issue: " + issue);
                mainView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(DEBUG) Log.d(TAG, "OnConnectionLost post delay");
                        if (app.getStreamConnection().isConnected() || issue.equals(TCPConnection.ISSUE_CLOSED_BY_USER) || issue.equals(TCPConnection.ISSUE_END_POINT_CLOSED)) {
                            closeLogic(issue, app.getDataConnection(), app.getStreamConnection());
                        }
                    }
                }, (issue.equals(TCPConnection.ISSUE_CLOSED_BY_USER) || issue.equals(TCPConnection.ISSUE_END_POINT_CLOSED)) ? 0 : 3 * 1000);
            }
        });

        app.getDataConnection().setIncomingDataListener(new IncomingData());
    }

    private void closeLogic(String issue, TCPConnection connection, TCPConnection otherConnection){
        otherConnection.close();

        NotUtil.cancelNotification(this, NotUtil.NOTIFICATION_CONNECTION_ID);

        if (!connection.isServer())
        {
            if (fragment instanceof ConnectedFragment) {
                connectedFragment.restoreAudioModeIfWanted();
            }

            if (app.prefs.getBoolean(Prefs.USE_CALL_FORWARDING, true))
            {
                cancelCallForwarding();
            }

            unregisterSafely(incomingCallReceiver, "Incoming Call Receiver");
            unregisterSafely(mBatInfoReceiver, "Battery Info Receiver");
        }
        else
        {
            if (!issue.equals(TCPConnection.ISSUE_CLOSED_BY_USER))
                createAlertNotification("The connection was lost.");
        }

        createSetupFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume");

        if (fragment != null && fragment instanceof ConnectedFragment && getIntent() != null && getIntent().getExtras() != null)
            if (getIntent().getExtras().getString(XML_ATTRIBUTE_PHONE_NUMBER) != null)
            {
                fragment.setIncomingData(getIntent().getExtras());
                Log.d(TAG, "Incoming Data");
            }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        app.setVisible(true);

        if (DEBUG) Log.v(TAG, "onPostResume, isConnected: " + String.valueOf(app.getDataConnection().isConnected()));

        // Check if a change occur in the connection when the user returns to the app.
        // Fragment cannot be changed while the app is not showing so this a fix maybe will change it in future.
        if (app.getDataConnection().isConnected()){
            if(fragment == null || fragment instanceof SetupFragment) {
                createConnectedFragment(getIntent().getExtras());
            }
        }
        else
        {
            if(fragment == null || fragment instanceof ConnectedFragment) {
                createSetupFragment();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.v(TAG, "onPause");
        app.setVisible(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSaveInstanceState");
        if (app.getDataConnection().isServer()) {
            if (batteryPercentage != -1)
            {
                Log.d(TAG, "Saving battery percentage");
                outState.putInt(XML_ATTRIBUTE_BATTERY_PERCENTAGE, batteryPercentage);
                outState.putString(XML_ATTRIBUTE_BATTERY_STATUS, batteryStatus);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.v(TAG, "OnDestroy");

//        cancelConnectionNotification();
//
//        app.terminateConnection();
    }

    /** Create and alert notification that the connection has lost.*/
    private void createAlertNotification(String content){

        if (DEBUG) Log.d(TAG, "App is not visible");
        Intent resultIntent = new Intent(MonitorActivity.this, MonitorActivity.class);

        Bundle data = new Bundle();

        data.putString(NotUtil.TITLE, "Baby Monitor");
        data.putString(NotUtil.TICKER, "Baby Monitor - Connection was lost.");
        data.putString(NotUtil.CONTENT, content);

        NotUtil.createAlertNotification(MonitorActivity.this, NotUtil.NOTIFICATION_ALERT_ID, resultIntent, data);
    }

    /** ---{ Implemented Methods }---*/
    @Override
    public void onSmsReceived(String contactName, String contactNumber, String text) {
        if (!app.getStreamConnection().isServer())
            sendDataXml(XML_TAG_SMS, text, new TList<XmlAttr>(
                                                            new XmlAttr(XML_ATTRIBUTE_CALLER_CONTACT_NAME, contactName),
                                                            new XmlAttr(XML_ATTRIBUTE_PHONE_NUMBER, phoneNumber) ) );
    }

    @Override
    public void onStartRinging(String callerName, String phoneNumber) {
        if (DEBUG) Log.d(TAG, "onStartRinging");
        if (!app.getStreamConnection().isServer())
                sendDataXml( XML_TAG_PHONE_DATA, null,
                        new TList<XmlAttr>(
                                new XmlAttr(0, XML_ATTRIBUTE_CALLER_CONTACT_NAME, callerName),
                                new XmlAttr(1, XML_ATTRIBUTE_PHONE_NUMBER, phoneNumber),
                                new XmlAttr(2, XML_ATTRIBUTE_CALL_STATE, CALL_STATE_RINGING) ) );
    }

    @Override
    public void onStartDialing(String phoneNumber) {
        if (DEBUG) Log.d(TAG, "onStartDialing");
        if (!app.getStreamConnection().isServer())
            sendDataXml( XML_TAG_PHONE_DATA, null,
                    new TList<XmlAttr>( new XmlAttr(0, XML_ATTRIBUTE_PHONE_NUMBER, phoneNumber),
                            new XmlAttr(1, XML_ATTRIBUTE_CALL_STATE, CALL_STATE_DIALING) ) );
    }

    @Override
    public void onHangUp(String phoneNumber) {
        if (DEBUG) Log.d(TAG, "onHangUp");
        if (!app.getStreamConnection().isServer())
            sendDataXml( XML_TAG_PHONE_DATA, null,
                    new TList<XmlAttr>( new XmlAttr(0, XML_ATTRIBUTE_PHONE_NUMBER, phoneNumber),
                                        new XmlAttr(1, XML_ATTRIBUTE_CALL_STATE, CALL_STATE_HANG_UP) ) );
    }

    @Override
    public void onActionEvent(Intent intent){
        if(DEBUG) Log.d(TAG, "onActionEvent, Action: " + intent.getAction());

        String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(TCPConnection.ACTION_TOGGLE_CONTROLLER))
        {
            if (connectedFragment != null)
                NotUtil.createConnectedNotification(MonitorActivity.this, intent.getExtras().getBoolean(TCPConnection.CONTROLLER_ACTION_RESULT, false), app.getDataConnection().isServer());
        }
        else if (action.equals(TCPConnection.ACTION_CLOSE_CONNECTION))
        {
            createSetupFragment();
            NotUtil.cancelNotification(MonitorActivity.this, NotUtil.NOTIFICATION_CONNECTION_ID);
        }
    }

    public void sendSMS(String phoneNumber, String message){
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    public void startACall(String phoneNumber){
        if(app.hasTelephonyService())
        {
            Intent intent = new Intent(Intent.ACTION_CALL);

            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        }
        else
        {
            if (DEBUG) Log.e(TAG, "Cant Call");
            Toast.makeText(this, "The device you are using doesn't have call capabilities", Toast.LENGTH_SHORT).show();
        }
    }

    public void forwardCalls(String phoneNumber){
        if(app.hasTelephonyService()) {
            String callForwardString = "*21* " + phoneNumber + "#";
            Intent intentCallForward = new Intent(Intent.ACTION_CALL);
            Uri uri2 = Uri.fromParts("tel", callForwardString, "#");
            intentCallForward.setData(uri2);
            startActivity(intentCallForward);
        }
        else
        {
            if (DEBUG) Log.e(TAG, "Cant Forward Call");
            Toast.makeText(this, "The device you are using doesn't have call capabilities, So you cannot forward calls to it.", Toast.LENGTH_SHORT).show();
        }
    }

    public void cancelCallForwarding(){
        if(app.hasTelephonyService()) {
            Intent intentCallForward = new Intent(Intent.ACTION_CALL);
            Uri uri2 = Uri.fromParts("tel", "#21#", "#");
            intentCallForward.setData(uri2);
            startActivity(intentCallForward);
        }
    }

    private List<String > getXmlTags(){
        List<String> tags = new ArrayList<String>();
        tags.add(XML_TAG_BATTERY);
        tags.add(XML_TAG_PHONE_DATA);
        tags.add(XML_TAG_SMS);

        return tags;
    }

    /* ----- Battery Change receiver ----*/
    public BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // If this is the baby phone send battery data to the parent phone.
            if (!app.getStreamConnection().isServer())
                try{
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    String newStatus = "";

                    if (status == BatteryManager.BATTERY_STATUS_CHARGING){newStatus = "Charging";}
                    if (status == BatteryManager.BATTERY_STATUS_DISCHARGING){newStatus = "Discharging";}
                    if (status == BatteryManager.BATTERY_STATUS_FULL){newStatus = "Full";}
                    if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING){newStatus = "Not Charging";}
                    if (status == BatteryManager.BATTERY_STATUS_UNKNOWN){newStatus = "Unknown";}

                    int level = intent.getIntExtra("level", 0);
                    if(DEBUG) Log.i(TAG, "Level: " + level + ", Status: " + newStatus);

                    if (batteryPercentage != level || !newStatus.equals(batteryStatus))
                    {
                        batteryPercentage = level;
                        batteryStatus = newStatus;

                        sendDataXml( XML_TAG_BATTERY, null,
                                new TList<XmlAttr>( new XmlAttr(0, XML_ATTRIBUTE_BATTERY_PERCENTAGE, String.valueOf(level)),
                                        new XmlAttr(1, XML_ATTRIBUTE_BATTERY_STATUS, batteryStatus) ) );
                    }



    /*                    int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                        String BattPowerSource = "No Data";
                        if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC){BattPowerSource = "AC";}
                        if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB){BattPowerSource = "USB";}

                        String BattLevel = String.valueOf(level);

                        int BHealth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                        String BatteryHealth = "No Data";
                        if (BHealth == BatteryManager.BATTERY_HEALTH_COLD){BatteryHealth = "Cold";}
                        if (BHealth == BatteryManager.BATTERY_HEALTH_DEAD){BatteryHealth = "Dead";}
                        if (BHealth == BatteryManager.BATTERY_HEALTH_GOOD){BatteryHealth = "Good";}
                        if (BHealth == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE){BatteryHealth = "Over-Voltage";}
                        if (BHealth == BatteryManager.BATTERY_HEALTH_OVERHEAT){BatteryHealth = "Overheat";}
                        if (BHealth == BatteryManager.BATTERY_HEALTH_UNKNOWN){BatteryHealth = "Unknown";}
                        if (BHealth == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE){BatteryHealth = "Unspecified Failure";}

                        Log.i(TAG, "Level: " + level + " Status: "  + status + " Power Source: " + BattPowerSource + " Battery Health: " + BatteryHealth); */

                } catch (Exception e){
                    if(DEBUG) Log.v(TAG, "Battery Info Error");
                }
        }
    };

    private void unregisterSafely(BroadcastReceiver receiver, String name){
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            if (DEBUG) Log.e(TAG, name + " - un registering a receiver that never registered");
        }
    }

    private void initBaseExtras(){
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        fragmentExtras.putInt(BaseFragment.SCREEN_HEIGHT, screenSize.y);
        fragmentExtras.putInt(BaseFragment.SCREEN_WIDTH, screenSize.x);
    }

    public void sendDataXml(String name, String text, TList<XmlAttr> attrs){
        XmlTag xmlTag;

        if (text == null)
            xmlTag  = XmlTag.getTag(name, attrs);
        else xmlTag = XmlTag.getTag(name, text, attrs);

        Log.d(TAG, "Attr Amount: " + xmlTag.getAttributes().size() + ", Name: " + xmlTag.getAttributes().get(0).getName() + xmlTag.getAttributes().get(0).getIndex());
        if (app.getDataConnection().isConnected())
            app.getDataConnection().write(XmlMessage.writeMessage(xmlTag));
    }

    /** used to remove the phone number extra from the activity intent so the app will know it handled showing the incoming data popup in the connected fragment.*/
    public void removeExtra(String extra){
        getIntent().removeExtra(extra);
    }

    class IncomingData implements IncomingDataListener{
        XmlAttr attr;

        @Override
        public void onStringDateReceived(String data) {
            if (DEBUG) Log.d(TAG, "onStringDateReceived, Data: " + data);
        }

        @Override
        public void onParedXmlReady(XmlTag xmlTag) {
            if (DEBUG) Log.d(TAG, "onParedXmlReady, Size: " + xmlTag.getChildren().size());

            if (connectedFragment == null)
                return;
//                if (!(fragment instanceof ConnectedFragment))
//                 if (DEBUG) throw new IllegalStateException("Xml received when not on connected fragment");
            if (xmlTag.getAttributes().size() == 0) {
                if (DEBUG)
                    throw new IllegalArgumentException("Now attributes found with battery data");
                else return;
            }

            if (xmlTag.getName().equals(XML_TAG_BATTERY))
            {
                if (DEBUG) Log.d(TAG, "Battery Level Received: " + xmlTag.getAttrValue(XML_ATTRIBUTE_BATTERY_PERCENTAGE));
                batteryPercentage = xmlTag.getAttr(XML_ATTRIBUTE_BATTERY_PERCENTAGE).getValueAsInt();
                batteryStatus = xmlTag.getAttrValue(XML_ATTRIBUTE_BATTERY_STATUS);

                connectedFragment.setBatteryData(batteryPercentage, batteryStatus );
            }
            else if (xmlTag.getName().equals(XML_TAG_PHONE_DATA))
            {
                if (xmlTag.getAttr(XML_ATTRIBUTE_CALL_STATE) != null)
                {
                    attr = xmlTag.getAttr(XML_ATTRIBUTE_CALL_STATE);

                    if (DEBUG) Log.d(TAG, "Call State: " + attr.getValue());

                    if (attr.getValue().equals(CALL_STATE_RINGING))
                    {
                        connectedFragment.onIncomingData(
                                xmlTag.getAttrValue(XML_ATTRIBUTE_CALLER_CONTACT_NAME),
                                xmlTag.getAttrValue(XML_ATTRIBUTE_PHONE_NUMBER), null);

                        app.getCallsDataSource().addCall(app.getDataSessionId(),
                                new Call(xmlTag.getAttrValue(XML_ATTRIBUTE_CALLER_CONTACT_NAME), xmlTag.getAttrValue(XML_ATTRIBUTE_PHONE_NUMBER), String.valueOf(System.currentTimeMillis()))
                        );
                    }
                    else
                    if (attr.getValue().equals(CALL_STATE_HANG_UP))
                        connectedFragment.dismissIncomingDataPopup();
                    // TODO handle dialing
                }
                else if (!xmlTag.getAttrValue(XML_ATTRIBUTE_TODO).equals(""))
                {
                    if(xmlTag.getAttrValue(XML_ATTRIBUTE_TODO).equals(Prefs.USE_CALL_FORWARDING) )
                    {
                        forwardCalls(xmlTag.getAttrValue(XML_ATTRIBUTE_PHONE_NUMBER));
                    }
                    else if (xmlTag.getAttrValue(XML_ATTRIBUTE_TODO).equals(Prefs.USE_SMS_TUNNELING))
                    {
                        registerReceiver(incomingCallReceiver, incomingCallReceiver.getFilter());
                        incomingCallReceiver.setCallsAndSmsReceiver(MonitorActivity.this);
                    }
                }
            }
            else if (xmlTag.getName().equals(XML_TAG_SMS))
            {
                if (!xmlTag.hasText())
                {
                    if (DEBUG) throw new NullPointerException("Sms Tag has no text");
                    else return;
                    // TODO handle this fail.
                }

                String caller = xmlTag.getAttrValue(XML_ATTRIBUTE_CALLER_CONTACT_NAME);
                String phoneNumber = xmlTag.getAttrValue(XML_ATTRIBUTE_PHONE_NUMBER);

                if (xmlTag.getAttrValue(XML_ATTRIBUTE_TODO).equals(READ)) // Show the user xml received from the connected phone
                {
                    connectedFragment.onIncomingData(caller, phoneNumber, xmlTag.getText());

                    app.getCallsDataSource().addCall(app.getDataSessionId(),
                            new Call(caller, phoneNumber, String.valueOf(System.currentTimeMillis()), xmlTag.getText())
                    );
                }
                else if (xmlTag.getAttrValue(XML_ATTRIBUTE_TODO).equals(SEND))// Send sms using given data
                    sendSMS(phoneNumber, xmlTag.getText());
                else if (DEBUG) throw new IllegalArgumentException("Sms attribute todo has illegal value, Value: " + xmlTag.getAttrValue(XML_ATTRIBUTE_TODO));
                else return;
            }

            if (DEBUG) Log.i(TAG, "Parsed Messages: " + XmlMessage.writeMessage(xmlTag));
        }
    }
}

/*  // Animate the info button background when moving from connected to disconnected.
    private void setBtnMode(final boolean connected){

        // Making sure know unnecessary animation will occur.
        if ( (connected && btnInfo.getTag().equals(getResources().getString(R.string.connected))) || (!connected && btnInfo.getTag().equals(getResources().getString(R.string.disconnect))) )
            return;

        fadeViewOut(btnInfo, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (connected)
                {
                    btnInfo.setBackgroundResource(R.drawable.info_btn_connected_selector);
                    btnInfo.setTag(getResources().getString(R.string.connected));
                }
                else
                {
                    btnInfo.setBackgroundResource(R.drawable.info_btn_selector);
                    btnInfo.setTag(getResources().getString(R.string.disconnect));
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        fadeViewOut(btnSettings, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (connected)
                {
                    btnSettings.setBackgroundResource(R.drawable.settings_button_connected_selector);
                }
                else
                {
                    btnSettings.setBackgroundResource(R.drawable.settings_button_selector);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        fadeViewOut(btnShowCallLog, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (connected)
                {
                    btnShowCallLog.setBackgroundResource(R.drawable.calls_log_button_connected_selector);
                }
                else
                {
                    btnShowCallLog.setBackgroundResource(R.drawable.calls_log_button_selector);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mainView.postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeViewIn(btnInfo);
                fadeViewIn(btnSettings);
                fadeViewIn(btnShowCallLog);
            }
        }, FADE_DURATION);

    }*/