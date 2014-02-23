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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        app = (BabyMonitorAppObj) getApplication();

        if (savedInstanceState == null) {

            placeholderFragment = new PlaceholderFragment();

            getFragmentManager().beginTransaction()
                    .add(R.id.container, placeholderFragment)
                    .commit();
        }
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

        private final String TAG = PlaceholderFragment.class.getSimpleName();

        private static final String PREFS_SERVER_IP = "prefs.server.ip";

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
            if (app.getConnection() != null && app.getConnection().isConnected())
            {
                animTrans.startTransition(0);

                liServerDataEt.setVisibility(View.INVISIBLE);
                liServerClientBtn.setVisibility(View.INVISIBLE);
                txtIp.setVisibility(View.INVISIBLE);

                showControlButtons();
                showDisconnectButton();

            }

            // Check for ip address from the preferences
            etIp.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREFS_SERVER_IP, ""));

            if (txtIp != null)
                if (app.getConnection().isConnectedToWifiNetwork())
                    txtIp.setText(app.getConnection().getCurrentWifiIp());
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

                    if (app.getConnection().isConnectedToWifiNetwork()) {

                        if (app.getConnection().getConnectionStatus().equals(TCPConnection.DISCONNECTED))
                        {
                            initConnection();
                        }

                        app.getConnection().start(2000);
                    }
                    else
                        Toast.makeText(getActivity(), "Not connected to WIFI", Toast.LENGTH_LONG).show();
                }
            });

            btnClient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (app.getConnection().isConnectedToWifiNetwork())
                    {
                        if (!etIp.getText().toString().isEmpty())
                        {
                            if (app.getConnection().getConnectionStatus().equals(TCPConnection.DISCONNECTED))
                            {
                                initConnection();
                            }

                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREFS_SERVER_IP,etIp.getText().toString()).commit();

                            app.getConnection().start(etIp.getText().toString(), 2000);
                        }
                        else
                            Toast.makeText(getActivity(), "Please enter the ip address of the server", Toast.LENGTH_LONG).show();
                    }
                    else
                        Toast.makeText(getActivity(), "Not connected to WIFI", Toast.LENGTH_LONG).show();
                }
            });

            btnPlayStop.setOnClickListener(this);
            btnDisconnect.setOnClickListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            cancelConnectionNotification();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // TODO save a data obj to the bundle.
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
        private void initConnection(){

            Log.d(TAG, "initConnection");

            app.getConnection().setActionEventListener(this);

            app.getConnection().setConnectionStateChangeListener(new ConnectionStateChangeListener() {
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
                }
            });

            app.getConnection().setOnConnectionLost(new onConnectionLostListener() {
                @Override
                public void onConnectionLost(int connectionType, String issue) {

                    Log.d(TAG, "onConnection Lost");
                    animateBackground();
                    setToDisconnectedLayout();
                    cancelConnectionNotification();
                }
            });

            app.getConnection().setWifiStatesListener(new WifiStatesListener() {
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

        @Override
        public void onClick(View v) {

            switch (v.getId())
            {
                case R.id.btn_stop_play:

                    if (app.getConnection() != null && app.getConnection().isConnected())
                    {
                        if (!v.isSelected())
                        {
                            if(app.getConnection().getConnectionType() == TCPConnection.SERVER)
                                app.getConnection().getAudioController().play();
                            else
                                app.getConnection().getRecordController().record();

                            createConnectedNotification(true);
                        }
                        else
                        {
                            if(app.getConnection().getConnectionType() == TCPConnection.SERVER)
                                app.getConnection().getAudioController().stop();
                            else
                                app.getConnection().getRecordController().stop();

                            createConnectedNotification(false);
                        }

                        v.setSelected(!v.isSelected());
                    }

                    break;

                case R.id.btn_disconnect:
                    if (app.getConnection() != null)
                    {
                        Log.d(TAG, "Disconnect");

                        app.getConnection().close();

                        animateBackground();

                        setToDisconnectedLayout();

                        cancelConnectionNotification();
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
                animateBackground();
                setToDisconnectedLayout();
                cancelConnectionNotification();
            }
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
                    app.getConnection().getConnectionType() == TCPConnection.SERVER ? TCPConnection.CONTROLLER_SOUND_PLAYER : TCPConnection.CONTROLLER_SOUND_RECORDER);
            PendingIntent playStopPendingIntent = PendingIntent.getBroadcast(getActivity(), 1, playStopIntent, 0);

            if (app.getConnection().getConnectionType() == TCPConnection.SERVER)
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

//            Log.d(TAG, "sendNotification, Title: " + title + " Content: " + content + " PendingIntentId: " + pendingIntentId);
            Intent resultIntent = new Intent(getActivity(), MonitorActivity.class);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            getActivity(),
                            1,
                            resultIntent, 0
                    );

            //Define sound URI - adding sound to the notification.
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // Build the notification characteristic.
            Notification.Builder mBuilder;
            mBuilder = new Notification.Builder(getActivity())
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setLights(0xFF0000FF, 500, 3000)
                    .setVibrate(new long[]{0, 250, 200, 250, 150, 150, 75, 150, 75, 150})
                    .setSound(soundUri);

            // The view for the notification
            RemoteViews contentView=new RemoteViews(getActivity().getPackageName(), R.layout.notification_running_layout);

            Intent intent =new Intent(getActivity(), MonitorActivity.class);
            intent.putExtra("SHUT_DOWN_CONNECTION", true);

            PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 1, intent, 0);
            contentView.setOnClickPendingIntent(R.id.btn_disconnect, pendingIntent);

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

        private void cancelConnectionNotification(){
            NotificationManager mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(NOTIFICATION_CONNECTION_ID);
        }

        private void setToConnectedLayout(){
            hideServerData();
            hideTxtIp();
            hideServerClientButtons();

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showControlButtons();
                    showDisconnectButton();
                }
            }, 1000);
        }

        private void setToDisconnectedLayout(){
            hideControlButtons();
            hideDisconnectButton();

            showServerClientButtons();
            showServerData();
            showTxtIp();

            btnPlayStop.setSelected(false);
        }

        // Animate
        private void  fadeViewIn(final View v){

            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);

            v.animate().alpha(1f).setDuration(1000).setListener(null);
        }

        private void fadeViewOut(final View v){

            v.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(View.VISIBLE);
                        }
                    });
        }

        private void animateBackground(){
            animTrans.setCrossFadeEnabled(true);

            if (app.getConnection() != null && app.getConnection().isConnected())
            {
                animTrans.startTransition(1000);
                Log.d(TAG, "AnimateBackground Status Connected");
            }
            else
            {
                animTrans.reverseTransition(1000);
                Log.d(TAG, "AnimateBackground Status Not Connected");
            }
        }

        private void showControlButtons(){
            fadeViewIn(btnPlayStop);
        }

        private void hideControlButtons(){
            fadeViewOut(btnPlayStop);
        }

        private void showServerData(){
            fadeViewIn(liServerDataEt);
        }

        private void hideServerData(){
            fadeViewOut(liServerDataEt);
        }

        private void showDisconnectButton(){
           fadeViewIn(btnDisconnect);
        }

        private void hideDisconnectButton(){
            fadeViewOut(btnDisconnect);
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