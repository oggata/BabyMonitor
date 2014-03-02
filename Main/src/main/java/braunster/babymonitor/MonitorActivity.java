package braunster.babymonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
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

import TCP.ActionEventListener;
import TCP.ConnectionStateChangeListener;
import TCP.TCPConnection;
import TCP.WifiStatesListener;
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
        // TODO Waze like notification that the TCP connection is running.
        // TODO background change sometime fails.
        // TODO check if wifi disabling is closing the connection
        // TODO create ALERT notification only if app is not showing
        // TODO save a onsavedinstance obj to the bundle.

        private final String TAG = PlaceholderFragment.class.getSimpleName();

        private static final String PREFS_SERVER_IP = "prefs.server.ip";
        private static final String PREFS_SERVER_PORT = "prefs.server.port";

        private static final int FADE_DURATION = 400, BACK_CHANGE_DURATION = 400;

        private View rootView;
        private LinearLayout liServerClientBtn, liServerDataEt;
        private Button btnServer, btnClient, btnDisconnect, btnPlayStop;
        private TextView txtIp;
        private EditText etIp, etServerPort;


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
        }

        /** Initiate the connection obj, Apply listeners etc.*/
        private void initStreamConnection(){

            Log.d(TAG, "initStreamConnection");

            app.getStreamConnection().setActionEventListener(this);

            app.getStreamConnection().setConnectionStateChangeListener(new ConnectionStateChangeListener() {
                @Override
                public void onConnected(int connectionType, Object obj) {
                    Log.d(TAG, "Connected");

                    animateBackground();

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

//            app.getDataConnection().setActionEventListener(this);

            //
            app.getDataConnection().preformConnectionCheck(true);

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

        private void setDisconnected(){
            animateBackground();
            setToDisconnectedLayout();
            cancelConnectionNotification();
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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

        private void cancelConnectionNotification(){
            NotificationManager mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(NOTIFICATION_CONNECTION_ID);
        }

        private void setToConnectedLayout(){
            hideTxtIp();
            hideServerClientButtons();
            hideServerData();

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showControlButtons();
                    showDisconnectButton();
                }
            }, FADE_DURATION);
        }

        private void setToDisconnectedLayout(){
            hideControlButtons();
            hideDisconnectButton();

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showTxtIp();
                    showServerClientButtons();
                    showServerData();
                }
            }, FADE_DURATION);

            btnPlayStop.setSelected(false);
        }

        // Animate
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

        private void showControlButtons(){
            fadeViewIn(btnPlayStop);

            btnPlayStop.setOnClickListener(this);
        }

        private void hideControlButtons(){
            fadeViewOut(btnPlayStop);
            btnPlayStop.setOnClickListener(null);
        }

        private void showServerData(){
            btnClient.setSelected(false);
            btnServer.setSelected(false);
            fadeViewIn(liServerDataEt);
        }

        private void hideServerData(){
            fadeViewOut(liServerDataEt);
        }

        private void showDisconnectButton(){
           fadeViewIn(btnDisconnect);

            btnDisconnect.setOnClickListener(this);
        }

        private void hideDisconnectButton(){
            fadeViewOut(btnDisconnect);

            btnDisconnect.setOnClickListener(null);
        }

        private void showServerClientButtons(){
            fadeViewIn(liServerClientBtn);
        }

        private void hideServerClientButtons(){
            fadeViewOut(liServerClientBtn);
        }

        private void showTxtIp(){
            fadeViewIn(txtIp);
        }

        private void hideTxtIp(){
            fadeViewOut(txtIp);
        }

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