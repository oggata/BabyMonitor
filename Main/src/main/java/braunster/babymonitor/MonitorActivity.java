package braunster.babymonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.TransitionDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import TCP.ActionEventListener;
import TCP.ConnectionStateChangeListener;
import TCP.CreateXmlAsyncTask;
import TCP.IncomingDataListener;
import TCP.ParseXmlAsyncTask;
import TCP.TCPConnection;
import TCP.TaskFinishedListener;
import TCP.WifiStatesListener;
import TCP.XMLParser;
import TCP.XmlMessage;
import TCP.onConnectionLostListener;

public class MonitorActivity extends Activity {

    private final String TAG = MonitorActivity.class.getSimpleName();
    private static BabyMonitorAppObj app;
    private PlaceholderFragment placeholderFragment;

    private static final int NOTIFICATION_CONNECTION_ID = 1991;
    private static final int NOTIFICATION_ALERT_ID = 1990;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

//        Log.e(TAG, "TEST!");
        app = (BabyMonitorAppObj) getApplication();

        if (savedInstanceState == null) {

            placeholderFragment = new PlaceholderFragment();

            getFragmentManager().beginTransaction()
                    .add(R.id.container, placeholderFragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public  static class PlaceholderFragment extends Fragment implements View.OnClickListener , ActionEventListener {

        // TODO Handle WifiStatesListener
        // TODO check if wifi disabling is closing the connection
        // TODO create ALERT notification only if app is not showing
        // TODO save a onSavedInstance obj to the bundle.
        // TODO check option for catching calls and messages received on the client(baby) device and forwarding them to the server(parents).
        // TODO create alert notification when connectionLost comes from the connection check?
        // TODO fix port problem cant switch port after selectign one the prefs not changing
        // TODO some UI delay when pressing the play/stop buttons
        // TODO the edit text of the server data is still selectable when invisible need to be handled.
        // TODO the server and client buttons need to have disabled mode when the wifi is disabled

        private final String TAG = PlaceholderFragment.class.getSimpleName();

        private static final String PREFS_SERVER_IP = "prefs.server.ip";
        private static final String PREFS_SERVER_PORT = "prefs.server.port";

        private static final int FADE_DURATION = 400, BACK_CHANGE_DURATION = 400;

        /* Views*/
        private View rootView;
        private LinearLayout liServerClientBtn, liServerDataEt;
        private Button btnServer, btnClient, btnDisconnect, btnPlayStop;
        private TextView txtIp, txtBatterLevel;
        private EditText etIp, etServerPort;

        //Keep the last level received of the battery
        int batteryPercentage = 0;

        // Animation
        private Animation animFadeIn, animFadeOut;
        private TransitionDrawable animTrans;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_monitor, container, false);

            animFadeIn = AnimationUtils.loadAnimation(getActivity(), R.animator.fade_in);
            animFadeOut = AnimationUtils.loadAnimation(getActivity(), R.animator.fade_out);
            animTrans = (TransitionDrawable) rootView.getBackground();

            viewsInit();

            // If the application is connected set the activity layout in connection formation.
            if (app.getStreamConnection() != null && app.getStreamConnection().isConnected())
            {
                animTrans.startTransition(0);

                liServerDataEt.setVisibility(View.INVISIBLE);
                liServerClientBtn.setVisibility(View.INVISIBLE);
                txtIp.setVisibility(View.INVISIBLE);

                showControlButtons();
                showDisconnectButton();

                initStreamConnection();

                initDataConnection();
            }

            // Check for ip address and server port from the preferences
            etIp.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREFS_SERVER_IP, ""));
            etServerPort.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREFS_SERVER_PORT, ""));

            if (txtIp != null)
                if (app.getStreamConnection().isConnectedToWifiNetwork())
                    txtIp.setText(app.getStreamConnection().getCurrentWifiIp());
                else
                    txtIp.setText("Not Connected To Wifi" );

            animTrans.setCrossFadeEnabled(true);

            getActivity().registerReceiver(this.mBatInfoReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            btnServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (v.isSelected())
                    {
                        v.setSelected(false);

                        app.closeConnections();

                    }
                    else
                    {
                        if (app.getStreamConnection().isConnectedToWifiNetwork())
                        {
                            if (!etServerPort.getText().toString().isEmpty())
                            {
                                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREFS_SERVER_PORT,etServerPort.getText().toString()).commit();

                                if (app.getStreamConnection().getConnectionStatus().equals(TCPConnection.DISCONNECTED) || app.getStreamConnection().getConnectionType() == TCPConnection.CLIENT)
                                {
                                    initStreamConnection();

                                    initDataConnection();

                                    app.getStreamConnection().start(Integer.parseInt(etServerPort.getText().toString()));

                                    app.getDataConnection().start(Integer.parseInt(etServerPort.getText().toString()) + 1);

                                    v.setSelected(true);
                                }
                                else Toast.makeText(getActivity(), "Server is already open", Toast.LENGTH_LONG).show();
                            }
                            else
                                Toast.makeText(getActivity(), "Please select a a port", Toast.LENGTH_LONG).show();
                        }
                        else
                            Toast.makeText(getActivity(), "Not connected to WIFI", Toast.LENGTH_LONG).show();
                    }

                }
            });

            btnClient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (v.isSelected())
                    {
                        v.setSelected(false);

                        app.closeConnections();
                    }
                    else
                    {
                        if (app.getStreamConnection().isConnectedToWifiNetwork())
                        {
                            if (!etIp.getText().toString().isEmpty() && !etServerPort.getText().toString().isEmpty())
                            {
                                if (app.getStreamConnection().getConnectionStatus().equals(TCPConnection.DISCONNECTED) || app.getStreamConnection().getConnectionType() == TCPConnection.SERVER)
                                {
                                    initStreamConnection();

                                    initDataConnection();

                                    app.getStreamConnection().start(etIp.getText().toString(), Integer.parseInt(etServerPort.getText().toString()));
                                    app.getDataConnection().start(etIp.getText().toString(), Integer.parseInt(etServerPort.getText().toString()) + 1);

                                    v.setSelected(true);
                                }

                                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREFS_SERVER_IP,etIp.getText().toString()).commit();
                                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREFS_SERVER_PORT,etServerPort.getText().toString()).commit();
                            }
                            else
                                Toast.makeText(getActivity(), "Please enter the ip address and the selected port of the server", Toast.LENGTH_LONG).show();
                        }
                        else
                            Toast.makeText(getActivity(), "Not connected to WIFI", Toast.LENGTH_LONG).show();
                    }

                }
            });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            cancelConnectionNotification();

            getActivity().unregisterReceiver(mBatInfoReceiver);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }

        private void viewsInit(){

            // Linear Layout
            liServerClientBtn = (LinearLayout) rootView.findViewById(R.id.linear_client_server_select_buttons);
            liServerDataEt = (LinearLayout) rootView.findViewById(R.id.linear_server_data);

            // Buttons - Server & Client Connection - Disconnect - Control
            btnServer = (Button) rootView.findViewById(R.id.btn_start_server);
            btnClient = (Button) rootView.findViewById(R.id.btn_start_client);
            btnDisconnect = (Button) rootView.findViewById(R.id.btn_disconnect);
            btnPlayStop = (Button) rootView.findViewById(R.id.btn_stop_play);

            // EditText - Server Data
            etIp = (EditText) rootView.findViewById(R.id.et_server_ip);
            etServerPort = (EditText) rootView.findViewById(R.id.et_server_port);

            // TextView - Phone Ip
            txtIp = (TextView) rootView.findViewById(R.id.txt_phone_ip);
            txtBatterLevel = (TextView) rootView.findViewById(R.id.txt_battery_level);
        }

        /** Initiate the connection obj, Apply listeners etc.*/
        private void initStreamConnection(){

            Log.d(TAG, "initStreamConnection");

            app.getStreamConnection().setActionEventListener(this);

            app.getStreamConnection().setConnectionStateChangeListener(new ConnectionStateChangeListener() {
                @Override
                public void onConnected(int connectionType, Object obj) {
                    Log.d(TAG, "Connected");

                    setToConnectedLayout();

                    createConnectedNotification(false);
                }

                @Override
                public void onConnectionChangeState(int connectionType, String state) {

                }

                @Override
                public void onConnectionFailed(String issue) {
                    Log.d(TAG, "Connection Failed");
                    btnClient.setSelected(false);
                    btnServer.setSelected(false);

                    if (issue.equals(TCPConnection.ISSUE_NO_END_POINT))
                        Toast.makeText(getActivity(), "Please open select the parent phone first", Toast.LENGTH_LONG).show();
                    else if (issue.equals(TCPConnection.ISSUE_WIFI_TCP_SERVER_TIMEOUT))
                        Toast.makeText(getActivity(), "Timeout! You need to select the baby phone.", Toast.LENGTH_LONG).show();
                }
            });

            app.getStreamConnection().setOnConnectionLost(new onConnectionLostListener() {
                @Override
                public void onConnectionLost(int connectionType, String issue) {

                    Log.d(TAG, "onConnection Lost");

                    setDisconnected();

                    app.getDataConnection().close();

                    createAlertNotification();
                }
            });

            app.getStreamConnection().setWifiStatesListener(new WifiStatesListener() {
                @Override
                public void onEnabled() {
                    Log.d(TAG, "onEnabled");
                }

                @Override
                public void onDisabled() {
                    Log.d(TAG, "onDisabled");
                }

                @Override
                public void onConnected(String networkName) {
                    Log.d(TAG, "onConnected, Network Name: " + networkName);
                }

                @Override
                public void onDisconnected() {
                    Log.d(TAG, "onDisconnected");
                }
            });
        }

        private void initDataConnection(){
            Log.d(TAG, "initDataConnection");

            // set some flags
            app.getDataConnection().preformConnectionCheck(true);
            app.getDataConnection().setReadXml(false);

            app.getDataConnection().setConnectionStateChangeListener(new ConnectionStateChangeListener() {
                @Override
                public void onConnected(int connectionType, Object obj) {
                    Log.d(TAG, "Data Connection  Connected");
                }

                @Override
                public void onConnectionChangeState(int connectionType, String state) {

                }

                @Override
                public void onConnectionFailed(String issue) {
                    Log.d(TAG, "Data Connection  Failed");
//                    btnClient.setSelected(false);
//                    btnServer.setSelected(false);
                }
            });

            app.getDataConnection().setOnConnectionLost(new onConnectionLostListener() {
                @Override
                public void onConnectionLost(int connectionType, String issue) {

                    Log.d(TAG, "Data Connection onConnection Lost");
                //  TODO maybe need to handle some fails but generally the stream connection should handle this situations.

                    rootView.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Log.d(TAG, "OnConnectionLost post delay");

                            if (app.getStreamConnection().isConnected())
                            {
                                app.getStreamConnection().close();

                                setDisconnected();

                                createAlertNotification();
                            }
                        }
                    }, 3 * 1000);
                }
            });

            app.getDataConnection().setIncomingDataListener(new IncomingDataListener() {
                @Override
                public void onXmlReceived(XMLParser parser) {

                    List<String> tags = new ArrayList<String>();
                    tags.add("battery");

                    final ParseXmlAsyncTask parseXmlAsyncTask = new ParseXmlAsyncTask();
                    parseXmlAsyncTask.setTaskFinishedListener(new TaskFinishedListener() {
                        @Override
                        public void onFinished() {
                            final List<String> data;
                            try {
                                data = parseXmlAsyncTask.get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    parseXmlAsyncTask.setTags(tags);



                    parseXmlAsyncTask.execute(parser);


                }
            });

            /*app.getDataConnection().setWifiStatesListener(new WifiStatesListener() {
                @Override
                public void onEnabled() {
                    Log.d(TAG, "onEnabled");
                }

                @Override
                public void onDisabled() {
                    Log.d(TAG, "onDisabled");
                }

                @Override
                public void onConnected(String networkName) {
                    Log.d(TAG, "onConnected, Network Name: " + networkName);
                }

                @Override
                public void onDisconnected() {
                    Log.d(TAG, "onDisconnected");
                }
            });*/

        }

        @Override
        public void onClick(View v) {

            switch (v.getId())
            {
                case R.id.btn_stop_play:

                    if (app.getStreamConnection() != null && app.getStreamConnection().isConnected())
                    {
                        if (!v.isSelected())
                        {
                            if(app.getStreamConnection().getConnectionType() == TCPConnection.SERVER)
                                if (app.getStreamConnection().getAudioController().play())
                                {
                                    createConnectedNotification(true);
                                    v.setSelected(!v.isSelected());
                                }
                                else Toast.makeText(getActivity(), "Cant Play!", Toast.LENGTH_LONG).show();
                            else
                                if (app.getStreamConnection().getRecordController().record())
                                {
                                    createConnectedNotification(true);
                                    v.setSelected(!v.isSelected());
                                }
                                else Toast.makeText(getActivity(), "Cant Record!", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            if(app.getStreamConnection().getConnectionType() == TCPConnection.SERVER)
                                app.getStreamConnection().getAudioController().stop();
                            else
                                app.getStreamConnection().getRecordController().stop();

                            createConnectedNotification(false);

                            v.setSelected(!v.isSelected());

                        }
                    }

                    break;

                case R.id.btn_disconnect:
                    if (app.getStreamConnection() != null)
                    {
                        Log.d(TAG, "Disconnect");

                        app.closeConnections();

                        setDisconnected();
                    }
                    break;
            }
        }

        @Override
        public void onActionEvent(Intent intent) {

            Log.d(TAG, "onActionEvent, Action: " + intent.getAction());

            if (intent.getAction().equals(TCPConnection.ACTION_TOGGLE_CONTROLLER))
            {
                createConnectedNotification(intent.getExtras().getBoolean(TCPConnection.CONTROLLER_ACTION_RESULT, false));
            }
            else if (intent.getAction().equals(TCPConnection.ACTION_CLOSE_CONNECTION))
            {
                setDisconnected();
            }
        }

        /* Set the mode to of the app to disconnected.*/
        private void setDisconnected(){

            setToDisconnectedLayout();
            cancelConnectionNotification();
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        /** Create an ongoing notification that can terminate the connection or play/stop the sound directly from the notification drawer.*/
        private void createConnectedNotification(boolean isStreaming){

            Log.d(TAG, "createConnectedNotification, " + (isStreaming ? "Streaming" : "not streaming") );

            // Build the notification characteristic.
            Notification.Builder mBuilder;
            mBuilder = new Notification.Builder(getActivity())
                    .setSmallIcon(R.drawable.disconnect_btn);

            // The view for the notification
            RemoteViews contentView= new RemoteViews(getActivity().getPackageName(), R.layout.notification_running_layout);

            // Listener for disconnect button
            Intent disconnectIntent =new Intent(TCPConnection.ACTION_CLOSE_CONNECTION);
            PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(getActivity(), 1, disconnectIntent, 0);
            contentView.setOnClickPendingIntent(R.id.btn_disconnect, disconnectPendingIntent);

            // Listener for play/pause button
            Intent playStopIntent =new Intent(TCPConnection.ACTION_TOGGLE_CONTROLLER);
            // Extra which controller to use. Server use sound player client us recorder
            playStopIntent.putExtra(TCPConnection.CONTROLLER,
                    app.getStreamConnection().getConnectionType() == TCPConnection.SERVER ? TCPConnection.CONTROLLER_SOUND_PLAYER : TCPConnection.CONTROLLER_SOUND_RECORDER);
            PendingIntent playStopPendingIntent = PendingIntent.getBroadcast(getActivity(), 1, playStopIntent, 0);

            if (app.getStreamConnection().getConnectionType() == TCPConnection.SERVER)
            {
                if (isStreaming)
                    contentView.setImageViewResource(R.id.btn_controller, R.drawable.stop_btn);
                else
                    contentView.setImageViewResource(R.id.btn_controller, R.drawable.play_btn);
            }
            else
            {
                if (isStreaming)
                    contentView.setImageViewResource(R.id.btn_controller, R.drawable.stop_btn);
                else
                    contentView.setImageViewResource(R.id.btn_controller, R.drawable.play_btn);
            }

            contentView.setOnClickPendingIntent(R.id.btn_controller, playStopPendingIntent);

            // Listener for the text message
            Intent messageIntent =new Intent(getActivity(), MonitorActivity.class);
            PendingIntent messagePendingIntent = PendingIntent.getActivity(getActivity(), 1, messageIntent, 0);
            contentView.setOnClickPendingIntent(R.id.txt_message, messagePendingIntent);

            // Notification Object from Builder
            Notification notification = mBuilder.build();
            // Add flag of ongoing event
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            // Set the content view of the notification to the xml.
            notification.contentView = contentView;

            NotificationManager mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.

            mNotifyMgr.notify(NOTIFICATION_CONNECTION_ID, notification);

            // TODO add other phone battery power and more data fro notification
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        /** Create and alert notification that the connection has lost.*/
        private void createAlertNotification(){

            Intent resultIntent = new Intent(getActivity(), MonitorActivity.class);

//            resultIntent.putExtra(ArduinoSocket.SOCKET_ID, pendingIntentId);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            getActivity(),
                            NOTIFICATION_ALERT_ID,
                            resultIntent, 0
                    );

            //Define sound URI - adding sound to the notification.
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Notification.Builder mBuilder =
                    new Notification.Builder(getActivity())
                            .setSmallIcon(android.R.drawable.ic_notification_overlay)
                            .setContentTitle("Baby Monitor")
                            .setContentText("The connection was lost.")
                            .setLights(0xFF0000FF, 500, 3000)
                            .setTicker("Baby Monitor - Connection was lost.")
                            .setVibrate(new long[]{0, 250, 200, 250, 150, 150, 75, 150, 75, 150})
                            .setSound(soundUri)
                            .setContentIntent(resultPendingIntent);

            // Sets an ID for the notification
            int mNotificationId = NOTIFICATION_ALERT_ID;

            Notification notification = mBuilder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL ;

            NotificationManager mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(NOTIFICATION_ALERT_ID, notification);

            // TODO add other phone battery power and more data fro notification
        }

        /** Cancel the ongoing notification that controls the connection state and play/stop*/
        private void cancelConnectionNotification(){
            NotificationManager mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(NOTIFICATION_CONNECTION_ID);
        }

        /** Change the mode of the layout to connected.*/
        private void setToConnectedLayout(){

            animateBackground();

            hideTxtIp();
            hideServerClientButtons();
            hideServerData();

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showControlButtons();
                    showDisconnectButton();
                    showTxtBatteryLevel();
                }
            }, FADE_DURATION - 100);
        }
        /** Change the mode of the layout to disconnected.*/
        private void setToDisconnectedLayout(){

            animateBackground();

            hideControlButtons();
            hideDisconnectButton();
            hideTxtBatteryLevel();

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showTxtIp();
                    showServerClientButtons();
                    showServerData();
                }
            }, FADE_DURATION - 100);

            btnPlayStop.setSelected(false);
        }

        /* Fade in and Fade out given view*/
        private void  fadeViewIn(final View v){

            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);

            v.animate().alpha(1f).setDuration(FADE_DURATION).setListener(null);
        }
        private void fadeViewOut(final View v){

            v.animate()
                    .alpha(0f)
                    .setDuration(FADE_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(View.VISIBLE);
                        }
                    });
        }

        /** Animating the background color*/
        private void animateBackground(){
            animTrans.setCrossFadeEnabled(true);

            if (app.getStreamConnection() != null && app.getStreamConnection().isConnected())
            {
                animTrans.startTransition(BACK_CHANGE_DURATION);
                Log.d(TAG, "AnimateBackground Status Connected");
            }
            else
            {
                animTrans.reverseTransition(BACK_CHANGE_DURATION);
                Log.d(TAG, "AnimateBackground Status Not Connected");
            }
        }

        /* The control buttons */
        private void showControlButtons(){
            fadeViewIn(btnPlayStop);

            btnPlayStop.setOnClickListener(this);
        }
        private void hideControlButtons(){
            fadeViewOut(btnPlayStop);
            btnPlayStop.setOnClickListener(null);
        }

        /* The IP and PORT data*/
        private void showServerData(){
            btnClient.setSelected(false);
            btnServer.setSelected(false);
            fadeViewIn(liServerDataEt);
        }
        private void hideServerData(){
            fadeViewOut(liServerDataEt);
        }

        /* Disconnect Button*/
        private void showDisconnectButton(){
           fadeViewIn(btnDisconnect);

            btnDisconnect.setOnClickListener(this);
        }
        private void hideDisconnectButton(){
            fadeViewOut(btnDisconnect);

            btnDisconnect.setOnClickListener(null);
        }

        /*Server And Client Buttons*/
        private void showServerClientButtons(){
            fadeViewIn(liServerClientBtn);
        }
        private void hideServerClientButtons(){
            fadeViewOut(liServerClientBtn);
        }

        /* Ip address of the phone*/
        private void showTxtIp(){
            fadeViewIn(txtIp);

        }
        private void hideTxtIp(){
            fadeViewOut(txtIp);
        }

        /* battery level textview*/
        private void showTxtBatteryLevel(){
            fadeViewIn(txtBatterLevel);
            txtBatterLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "OnClick");
                    List<XmlMessage> xmlMessages = new ArrayList<XmlMessage>();
                    xmlMessages.add(new XmlMessage(String.valueOf(50)));

                    final CreateXmlAsyncTask task = new CreateXmlAsyncTask();
                    task.setTaskFinishedListener(new TaskFinishedListener() {
                        @Override
                        public void onFinished() {
                            if (app.getDataConnection().isConnected()){
                                try {
                                    String xml = task.get();
                                    Log.i(TAG, xml);
                                    app.getDataConnection().write(xml);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    task.execute(xmlMessages);
                }
            });
        }
        private void hideTxtBatteryLevel(){
            fadeViewOut(txtBatterLevel);
            txtBatterLevel.setOnClickListener(null);
        }

        private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                //this will give you battery current status

                try{
                    int level = intent.getIntExtra("level", 0);
                    Log.i(TAG, "Level: " + level);

                    if (true || batteryPercentage != level)
                    {
                        txtBatterLevel.setText(String.valueOf(level));
                        List<XmlMessage> xmlMessages = new ArrayList<XmlMessage>();
                        xmlMessages.add(new XmlMessage(String.valueOf(level)));

                        final CreateXmlAsyncTask task = new CreateXmlAsyncTask();
                        task.setTaskFinishedListener(new TaskFinishedListener() {
                            @Override
                            public void onFinished() {
                                if (app.getDataConnection().isConnected()){
                                    try {
                                        String xml = task.get();
                                        Log.i(TAG, xml);
                                        app.getDataConnection().write(xml);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                        task.execute(xmlMessages);
                    }

/*                    int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                    int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                    String BStatus = "No Data";
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING){BStatus = "Charging";}
                    if (status == BatteryManager.BATTERY_STATUS_DISCHARGING){BStatus = "Discharging";}
                    if (status == BatteryManager.BATTERY_STATUS_FULL){BStatus = "Full";}
                    if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING){BStatus = "Not Charging";}
                    if (status == BatteryManager.BATTERY_STATUS_UNKNOWN){BStatus = "Unknown";}

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
                    Log.v(TAG, "Battery Info Error");
                }
            }
        };

    }
}
//        private void colorChange(){
////            //animate from your current color to red
////            final ValueAnimator anim = ValueAnimator.ofInt(Color.parseColor("#FFFFFF"), Color.parseColor("#000000"));
////            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
////                @Override
////                public void onAnimationUpdate(ValueAnimator animation) {
////                    rootView.setBackgroundColor( () anim.getAnimatedValue());
////                }
////            });
////
////            anim.start();
//
//
//            ColorDrawable layers[] = new ColorDrawable[2];
//            layers[0] = new ColorDrawable(0xff0000ff);
//            layers[1] = new ColorDrawable(0xffff0000);
//            ColorTransaction colorTransaction = new ColorTransaction(layers);
//            rootView.setBackgroundDrawable(colorTransaction);
//
//            colorTransaction.changeColor(0xff00ff00);
//        }