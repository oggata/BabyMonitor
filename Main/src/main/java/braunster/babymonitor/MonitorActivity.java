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
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import TCP.ActionEventListener;
import TCP.AudioStreamController;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        app.terminateConnection();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public  static class PlaceholderFragment extends Fragment implements View.OnClickListener , ActionEventListener {

        // TODO wifi disabling closing only one connctin and the other connection doesn't reckon it.
        // TODO create ALERT notification only if app is not showing - Not Sure
        // TODO save a onSavedInstance obj to the bundle.
        // TODO check option for catching calls and messages received on the client(baby) device and forwarding them to the server(parents).
        // TODO create alert notification when connectionLost comes from the connection check?
        // TODO the server and client buttons need to have disabled mode when the wifi is disabled
        // TODO Explanation for first time user , setting connection
        // TODO only show available frequency for the device to the user

        private final String TAG = PlaceholderFragment.class.getSimpleName();

        private static final String PREFS_SERVER_IP = "prefs.server.ip";
        private static final String PREFS_SERVER_PORT = "prefs.server.port";
        private static final String PREFS_FIRST_LOGIN = "prefs.first_login";
        private static final String PREFS_FIRST_CONNECTION = "prefs.first_connection";
        private static final String PREFS_FIRST_SETTING = "prefs.first_setting";


        private static final int FADE_DURATION = 400, BACK_CHANGE_DURATION = 400;
        private static final int[] DATA_SERVER_PORTS = {9481, 4672};
        private static final int[] STREAM_SERVER_PORTS = {5489, 9714};

        /* Views*/
        private View rootView;
        private LinearLayout liServerClientBtn, liServerDataEt;
        private Button btnServer, btnClient, btnDisconnect, btnPlayStop, btnInfo, btnSetting;
        private TextView txtIp, txtBatterLevel;
        private EditText etIp, etServerPort;

        private PopupWindow settingPopUp, playStopPopUp, disconnectPopUp, serverDialog, clientDialog, infoDialog;

        //Keep the last level received of the battery
        int batteryPercentage = 0;

        private Point screenSize = new Point();

        // Animation
        private Animation animFadeIn, animFadeOut;
        private TransitionDrawable animTrans;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);

            rootView = inflater.inflate(R.layout.fragment_monitor, container, false);

            animFadeIn = AnimationUtils.loadAnimation(getActivity(), R.animator.fade_in);
            animFadeOut = AnimationUtils.loadAnimation(getActivity(), R.animator.fade_out);
            animTrans = (TransitionDrawable) rootView.getBackground();

            viewsInit();

            // For the wifi state listener
            initStreamConnection();

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

            setTxtIp();

            // Check for ip address and server port from the preferences
            etIp.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREFS_SERVER_IP, ""));
            etServerPort.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREFS_SERVER_PORT, ""));

            animTrans.setCrossFadeEnabled(true);

            getActivity().registerReceiver(this.mBatInfoReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();

            rootView.post(new Runnable() {
                @Override
                public void run() {
                    firstLogin();
                }
            });

            if (!app.getStreamConnection().isConnected())
            {
                btnSetting.setOnClickListener(this);
            }

            btnInfo.setOnClickListener(this);

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
                            if (etServerPort.getText().toString().isEmpty())// For now don use ports
                            {
                                if (app.getStreamConnection().getConnectionStatus().equals(TCPConnection.DISCONNECTED) || app.getStreamConnection().getConnectionType() == TCPConnection.CLIENT)
                                {
                                    initStreamConnection();

                                    initDataConnection();

//                                    app.getStreamConnection().start(Integer.parseInt(etServerPort.getText().toString()));
                                    app.getStreamConnection().start(STREAM_SERVER_PORTS[0]);
                                    // Making sure the picked port isnt the default data port
                                    app.getDataConnection().start( etServerPort.getText().toString().equals( String.valueOf(DATA_SERVER_PORTS[0])) ? DATA_SERVER_PORTS[1] : DATA_SERVER_PORTS[0]) ;

                                    v.setSelected(true);
                                }
                                else Toast.makeText(getActivity(), "Server is already open", Toast.LENGTH_LONG).show();

                                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREFS_SERVER_PORT,etServerPort.getText().toString()).commit();
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
                            if (!etIp.getText().toString().isEmpty() && etServerPort.getText().toString().isEmpty())// For now don't use ports
                            {
                                if (app.getStreamConnection().getConnectionStatus().equals(TCPConnection.DISCONNECTED) || app.getStreamConnection().getConnectionType() == TCPConnection.SERVER)
                                {
                                    initStreamConnection();

                                    initDataConnection();

//                                    app.getStreamConnection().start(etIp.getText().toString(), Integer.parseInt(etServerPort.getText().toString()));
                                    app.getStreamConnection().start(etIp.getText().toString(), STREAM_SERVER_PORTS[0]);
                                    app.getDataConnection().start(etIp.getText().toString(), etServerPort.getText().toString().equals( String.valueOf(DATA_SERVER_PORTS[0])) ? DATA_SERVER_PORTS[1] : DATA_SERVER_PORTS[0]) ;

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
            btnInfo = (Button ) rootView.findViewById(R.id.btn_info);
            btnSetting = (Button) rootView.findViewById(R.id.btn_setting);

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
                    Log.d(TAG, "Connection Failed, Issue: " + issue);
                    btnClient.setSelected(false);
                    btnServer.setSelected(false);

                    if (issue.equals(TCPConnection.ISSUE_NO_END_POINT))
                        Toast.makeText(getActivity(), "Please open select the parent phone first", Toast.LENGTH_LONG).show();
                    else if (issue.equals(TCPConnection.ISSUE_WIFI_TCP_SERVER_TIMEOUT))
                        Toast.makeText(getActivity(), "Timeout! You need to select the baby phone.", Toast.LENGTH_LONG).show();
                    else if (issue.equals(TCPConnection.ISSUE_OPENING_A_SERVER))
                    {
                        Toast.makeText(getActivity(), "Please select a different port. Preferred from 2000 and above.", Toast.LENGTH_LONG).show();
                        app.getStreamConnection().close();
                    }
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
                    setTxtIp();
                }

                @Override
                public void onDisabled() {
                    Log.d(TAG, "onDisabled");
                    setTxtIp();

                    app.closeConnections();
                }

                @Override
                public void onConnected(String networkName) {
                    Log.d(TAG, " Wifi Listener onConnected, Network Name: " + networkName);
                    setTxtIp();
                }

                @Override
                public void onDisconnected() {
                    Log.d(TAG, "onDisconnected");
                    setTxtIp();
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

                    if (issue.equals(TCPConnection.ISSUE_OPENING_A_SERVER))
                    {
                        Toast.makeText(getActivity(), "Connection have a problem please select a different port and try again." , Toast.LENGTH_LONG).show();
                    }
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
        public void onClick(final View v) {

            switch (v.getId())
            {
                case R.id.btn_stop_play:

                    v.post(new Runnable() {
                        @Override
                        public void run() {
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
                        }
                    });


                    break;

                case R.id.btn_disconnect:
                    if (app.getStreamConnection() != null)
                    {
                        Log.d(TAG, "Disconnect");

                        app.closeConnections();

                        setDisconnected();
                    }
                    break;

                case R.id.btn_info:

                    if (app.getStreamConnection().isConnected())
                    {
                        createWhenConnectedInfoPopup();
                    }
                    else
                    {
                        createHowToConnectPopup();
                    }
                    break;

                case R.id.btn_setting:

                    if (v.isSelected())
                    {
                        v.setSelected(false);

                        if (settingPopUp != null && settingPopUp.isShowing())
                            settingPopUp.dismiss();

                    }
                    else
                    {
                        v.setSelected(true);

                        createSettingsPopup();
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

        /*
        * --- PopUps ----
                           */
        private void createSettingsPopup(){

            settingPopUp = new PopupWindow(getActivity());

            View v = getActivity().getLayoutInflater().inflate(R.layout.setting_popup_layout, null);

            final RadioGroup radioGrp = (RadioGroup) v.findViewById(R.id.radio_grp_samples);
            RadioButton radio;

            // Showing the user all his available SampleRates
            for ( int rate : AudioStreamController.getSupportedSampleRates())
            {
                radio = new RadioButton(getActivity());

                if (rate == AudioStreamController.sampleRate)
                    radio.setChecked(true);

                radio.setText(String.valueOf(rate));
                radio.setId(rate);
                radioGrp.addView(radio);
            }

            radioGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, final int checkedId) {

                    AudioStreamController.sampleRate = checkedId;

                    // Small delay so the user will see what he picked.
                    rootView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            settingPopUp.dismiss();
                        }
                    }, 300);
                }
            });

            settingPopUp.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {

                    // A small delay for animation and making sure the if user pressed the btnsettign again it would not show again.
                    // The setSelected to false is the trick.
                    rootView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btnSetting.setSelected(false);

                            showServerData();

                            // If the info dialog is shown above the server data layout dont show the server and client buttons.
                            if (infoDialog != null && infoDialog.isShowing() && infoDialog.isAboveAnchor())
                                return;

                            showServerClientButtons();
                        }
                    }, FADE_DURATION - 100);
                }
            });

            settingPopUp.setContentView(v);
            settingPopUp.setOutsideTouchable(true);
            settingPopUp.setBackgroundDrawable(new BitmapDrawable());
            settingPopUp.setWidth(v.getLayoutParams().WRAP_CONTENT);
            settingPopUp.setHeight(v.getLayoutParams().WRAP_CONTENT);
            settingPopUp.setAnimationStyle(R.style.PopupAnimation);

            hideServerClientButtons();
            hideServerData();

            settingPopUp.showAsDropDown(btnSetting);

        }

        private void createWhenConnectedInfoPopup(){
            View v = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);

            ((TextView)v.findViewById(R.id.txt)).setText("This is the disconnect button. There's about 5 seconds delay till the other phone get notified that you disconnected.");
            ((TextView)v.findViewById(R.id.txt)).setGravity(Gravity.LEFT);
            ((TextView)v.findViewById(R.id.txt)).setTextColor(Color.WHITE);

            disconnectPopUp = new PopupWindow(getActivity());
            disconnectPopUp.setContentView(v);
            disconnectPopUp.setOutsideTouchable(true);
            disconnectPopUp.setBackgroundDrawable(new BitmapDrawable());
            disconnectPopUp.setWidth(screenSize.x);
            disconnectPopUp.setHeight(v.getLayoutParams().WRAP_CONTENT);
            disconnectPopUp.setAnimationStyle(R.style.PopupAnimation);
            disconnectPopUp.showAsDropDown(btnDisconnect);

            View v2 = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);

            ((TextView)v2.findViewById(R.id.txt)).setText("This is the Play/Stop Button.");
            ((TextView)v2.findViewById(R.id.txt)).setGravity(Gravity.LEFT);
            ((TextView)v2.findViewById(R.id.txt)).setTextColor(Color.WHITE);

            playStopPopUp = new PopupWindow(getActivity());
            playStopPopUp.setContentView(v2);
            playStopPopUp.setOutsideTouchable(true);
            playStopPopUp.setBackgroundDrawable(new BitmapDrawable());
            playStopPopUp.setWidth(screenSize.x);
            playStopPopUp.setHeight(v2.getLayoutParams().WRAP_CONTENT);
            playStopPopUp.setAnimationStyle(R.style.PopupAnimation);
            playStopPopUp.showAsDropDown(btnPlayStop);
        }

        private void createHowToConnectPopup(){
            View v = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);

            ((TextView)v.findViewById(R.id.txt)).setText("Enter the ip address of the phone you want to place in the baby room." +
                    " When the ip is set press on the 'Parent Button' in the phone you want to be near you, only then press on the 'Baby Button'. ");

            ((TextView)v.findViewById(R.id.txt)).setGravity(Gravity.LEFT);
            infoDialog = new PopupWindow(getActivity());
            infoDialog.setContentView(v);
            infoDialog.setOutsideTouchable(true);
            infoDialog.setBackgroundDrawable(new BitmapDrawable());
            infoDialog.setWidth(screenSize.x);
            infoDialog.setHeight(v.getLayoutParams().WRAP_CONTENT);
            infoDialog.setAnimationStyle(R.style.PopupAnimation);
            infoDialog.showAsDropDown(etIp);

            if (infoDialog.isAboveAnchor())
            {
                hideServerClientButtons();
                infoDialog.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        showServerClientButtons();
                    }
                });

                Log.d(TAG, "how to connect info is above the view");
            }
            else
                createButtonAndIpInfoPopup();

        }

        private void createButtonAndIpInfoPopup(){

            // Server popup
            View v = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);
            ((TextView)v.findViewById(R.id.txt)).setText("Parent Button");
            ((TextView)v.findViewById(R.id.txt)).setTextSize(15f);
            serverDialog = new PopupWindow(getActivity());
            serverDialog.setContentView(v);
            serverDialog.setOutsideTouchable(true);
            serverDialog.setBackgroundDrawable(new BitmapDrawable());
            serverDialog.setWidth(btnServer.getWidth());
            serverDialog.setHeight(v.getLayoutParams().WRAP_CONTENT);
            serverDialog.setAnimationStyle(R.style.PopupAnimation);
            serverDialog.showAsDropDown(btnServer);

            // Client popup
            View v2 = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);
            ((TextView)v2.findViewById(R.id.txt)).setText("Baby Button");
            ((TextView)v2.findViewById(R.id.txt)).setTextSize(15f);
            clientDialog = new PopupWindow(getActivity());
            clientDialog.setContentView(v2);
            clientDialog.setOutsideTouchable(true);
            clientDialog.setBackgroundDrawable(new BitmapDrawable());
            clientDialog.setWidth(btnClient.getWidth());
            clientDialog.setHeight(v2.getLayoutParams().WRAP_CONTENT);
            clientDialog.setAnimationStyle(R.style.PopupAnimation);
            clientDialog.showAsDropDown(btnClient);

            // Ip popup
            View v3 = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);
            ((TextView)v3.findViewById(R.id.txt)).setTextSize(11f);
            ((TextView)v3.findViewById(R.id.txt)).setText("Your phone Ip ->");
            PopupWindow popupIp = new PopupWindow(getActivity());
            popupIp.setContentView(v3);
            popupIp.setOutsideTouchable(true);
            popupIp.setBackgroundDrawable(new BitmapDrawable());
            popupIp.setWidth(v3.getLayoutParams().WRAP_CONTENT);
            popupIp.setHeight(v3.getLayoutParams().WRAP_CONTENT);
            popupIp.setAnimationStyle(R.style.PopupAnimation);
/*
            int pos[] = new int[2];
            txtIp.getLocationOnScreen(pos);
            popupIp.showAtLocation(rootView, Gravity.NO_GRAVITY, 5, pos[1]);*/

//            popupIp.showAsDropDown(txtIp);
        }

        private void setTxtIp(){
            if (txtIp != null)
                if (app.getStreamConnection().isConnectedToWifiNetwork())
                    txtIp.setText(app.getStreamConnection().getCurrentWifiIp());
                else
                    txtIp.setText("Not Connected To Wifi" );
        }

        /* Set the mode to of the app to disconnected.*/
        private void setDisconnected(){

            if (playStopPopUp != null && playStopPopUp.isShowing())
                playStopPopUp.dismiss();

            if (disconnectPopUp != null && disconnectPopUp.isShowing())
                disconnectPopUp.dismiss();

            setToDisconnectedLayout();
            cancelConnectionNotification();
        }

        /* ----- Notifications ----*/
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

        /*
        * ----- Layout ----
                             */
        /** Change the mode of the layout to connected.*/
        private void setToConnectedLayout(){

            animateBackground();

            hideTxtIp();
            hideServerClientButtons();
            hideServerData();
            hideSettingButton();

            setInfoBtnMode(true);

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showControlButtons();
                    showDisconnectButton();
                    showTxtBatteryLevel();
                    firstConnection();
                }
            }, FADE_DURATION - 100);
        }
        /** Change the mode of the layout to disconnected.*/
        private void setToDisconnectedLayout(){

            animateBackground();

            hideControlButtons();
            hideDisconnectButton();
            hideTxtBatteryLevel();

            setInfoBtnMode(false);

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showTxtIp();
                    showServerClientButtons();
                    showServerData();
                    showSettingButton();
                }
            }, FADE_DURATION - 100);

            btnPlayStop.setSelected(false);
        }

        /* ----- Animation ----*/
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

        /* ----- Show/Hide Views ----*/
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

            etIp.setEnabled(true);
            etServerPort.setEnabled(true);
        }
        private void hideServerData(){
            fadeViewOut(liServerDataEt);

            etIp.setEnabled(false);
            etServerPort.setEnabled(false);
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

            btnClient.setEnabled(true);
            btnServer.setEnabled(true);
        }
        private void hideServerClientButtons(){
            fadeViewOut(liServerClientBtn);

            btnClient.setEnabled(false);
            btnServer.setEnabled(false);
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

        /* Setting Button*/
        private void showSettingButton(){
            fadeViewIn(btnSetting);
            btnSetting.setOnClickListener(this);
        }

        private void hideSettingButton(){
            fadeViewOut(btnSetting);
            btnSetting.setOnClickListener(null);
        }

        /* Info Button*/
        private void setInfoBtnMode(final boolean connected){

            if (btnInfo.getTag().equals(R.string.connected) && connected || btnInfo.getTag().equals(R.string.disconnect) && !connected  )
                return;

            fadeViewOut(btnInfo);

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (connected)
                    {
                        btnInfo.setBackgroundResource(android.R.color.transparent);
                        btnInfo.setText("I");
                        btnInfo.setTextColor(Color.WHITE);
                        btnInfo.setTag(R.string.connected);
                    }
                    else
                    {
                        btnInfo.setBackgroundResource(R.drawable.info_btn_selector);
                        btnInfo.setText("");
                        btnInfo.setTag(R.string.disconnect);
                    }
                }
            }, FADE_DURATION - FADE_DURATION/2);

            rootView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fadeViewIn(btnInfo);
                }
            }, FADE_DURATION - 100);
        }


        /*--- First Cases ---*/
        private void firstLogin(){

            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREFS_FIRST_LOGIN, true))
            {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(PREFS_FIRST_LOGIN, false).commit();

                createHowToConnectPopup();
            }
        }

        private void firstConnection(){
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREFS_FIRST_CONNECTION, true))
            {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(PREFS_FIRST_CONNECTION, false).commit();

                createWhenConnectedInfoPopup();
            }
        }

        private void firstSetting(){
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREFS_FIRST_SETTING , true))
            {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(PREFS_FIRST_SETTING, false).commit();
            }
        }

        /* ----- Battery Change receiver ----*/
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
