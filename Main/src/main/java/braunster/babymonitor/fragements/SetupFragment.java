package braunster.babymonitor.fragements;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import TCP.connrction_and_threads.AudioStreamController;
import TCP.connrction_and_threads.TCPConnection;
import TCP.interfaces.WifiStatesListener;
import braunster.babymonitor.R;

/**
 * Created by itzik on 5/9/2014.
 */
/**
 * A placeholder fragment containing a simple view.
 */
public  class SetupFragment extends BaseFragment implements View.OnClickListener {

    // TODO wifi disabling closing only one connctin and the other connection doesn't reckon it.
    // TODO create ALERT notification only if app is not showing - Not Sure
    // TODO save a onSavedInstance obj to the bundle.
    // TODO check option for catching calls and messages received on the client(baby) device and forwarding them to the server(parents).
    // TODO create alert notification when connectionLost comes from the connection check?
    // TODO the server and client buttons need to have disabled mode when the wifi is disabled
    // TODO Explanation for first time user , setting connection
    // TODO only show available frequency for the device to the user

    private final String TAG = SetupFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String PREFS_SERVER_IP = "prefs.server.ip";
    private static final String PREFS_SERVER_PORT = "prefs.server.port";
    private static final String PREFS_FIRST_LOGIN = "prefs.first_login";
    private static final String PREFS_FIRST_SETTING = "prefs.first_setting";
    private static final String PREFS_SAMPLE_RATE = "prefs_sample_rate";// The sample rate used by the user.

    private static final int FADE_DURATION = 400, BACK_CHANGE_DURATION = 400;
    private static final int[] DATA_SERVER_PORTS = {9481, 4672};
    private static final int[] STREAM_SERVER_PORTS = {5489, 9714};

    /* Views*/
    private LinearLayout liServerClientBtn, liServerDataEt;
    private Button btnServer, btnClient, btnSetting;
    private TextView txtIp, txtBatterLevel;
    private EditText etIp, etServerPort;

    private PopupWindow settingPopUp, serverDialog, clientDialog, infoDialog;

    public SetupFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_monitor, container, false);

        viewsInit();

        setTxtIp();

        // Set the sample rate to the one saved on the preference manager
        AudioStreamController.sampleRate = app.prefs.getInt(PREFS_SAMPLE_RATE, 8000);

        // Check for ip address and server port from the preferences
        etIp.setText(app.prefs.getString(PREFS_SERVER_IP, ""));
        etServerPort.setText(app.prefs.getString(PREFS_SERVER_PORT, ""));

        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Listen to wifi network events
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

//                app.closeConnections();
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

        mainView.post(new Runnable() {
            @Override
            public void run() {
                firstLogin();
            }
        });

        btnSetting.setOnClickListener(this);

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
                                app.getStreamConnection().start(STREAM_SERVER_PORTS[0]);
                                // Making sure the picked port isnt the default data port
                                app.getDataConnection().start( etServerPort.getText().toString().equals( String.valueOf(DATA_SERVER_PORTS[0])) ? DATA_SERVER_PORTS[1] : DATA_SERVER_PORTS[0]) ;

                                v.setSelected(true);
                            }
                            else Toast.makeText(getActivity(), "Server is already open", Toast.LENGTH_LONG).show();

                            app.prefs.edit().putString(PREFS_SERVER_PORT,etServerPort.getText().toString()).commit();
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
                                app.getStreamConnection().start(etIp.getText().toString(), STREAM_SERVER_PORTS[0]);
                                app.getDataConnection().start(etIp.getText().toString(), etServerPort.getText().toString().equals( String.valueOf(DATA_SERVER_PORTS[0])) ? DATA_SERVER_PORTS[1] : DATA_SERVER_PORTS[0]) ;

                                v.setSelected(true);
                            }

                            app.prefs.edit().putString(PREFS_SERVER_IP,etIp.getText().toString()).commit();
                            app.prefs.edit().putString(PREFS_SERVER_PORT,etServerPort.getText().toString()).commit();
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

    private void viewsInit(){

        // Linear Layout
        liServerClientBtn = (LinearLayout) mainView.findViewById(R.id.linear_client_server_select_buttons);
        liServerDataEt = (LinearLayout) mainView.findViewById(R.id.linear_server_data);

        // Buttons - Server & Client Connection - Disconnect - Control
        btnServer = (Button) mainView.findViewById(R.id.btn_start_server);
        btnClient = (Button) mainView.findViewById(R.id.btn_start_client);
        btnSetting = (Button) mainView.findViewById(R.id.btn_setting);

        // EditText - Server Data
        etIp = (EditText) mainView.findViewById(R.id.et_server_ip);
        etServerPort = (EditText) mainView.findViewById(R.id.et_server_port);

        // TextView - Phone Ip
        txtIp = (TextView) mainView.findViewById(R.id.txt_phone_ip);
        txtBatterLevel = (TextView) mainView.findViewById(R.id.txt_battery_level);
    }

    @Override
    public void onClick(final View v) {

        switch (v.getId())
        {

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
    public void onInfoPressed() {
        super.onInfoPressed();
        createHowToConnectPopup();
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

                app.prefs.edit().putInt(PREFS_SAMPLE_RATE, checkedId).commit();

                // Small delay so the user will see what he picked.
                mainView.postDelayed(new Runnable() {
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
                mainView.postDelayed(new Runnable() {
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
            popupIp.showAtLocation(mainView, Gravity.NO_GRAVITY, 5, pos[1]);*/

//            popupIp.showAsDropDown(txtIp);
    }

    private void setTxtIp(){
        if (txtIp != null)
            if (app.getStreamConnection().isConnectedToWifiNetwork())
                txtIp.setText(app.getStreamConnection().getCurrentWifiIp());
            else
                txtIp.setText("Not Connected To Wifi" );
    }

    public void onFailed(){
        btnClient.setSelected(false);
        btnServer.setSelected(false);
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

        /* ----- Show/Hide Views ----*/

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

    /*--- First Cases ---*/
    private void firstLogin(){

        if (app.prefs.getBoolean(PREFS_FIRST_LOGIN, true))
        {
            app.prefs.edit().putBoolean(PREFS_FIRST_LOGIN, false).commit();

            createHowToConnectPopup();
        }
    }

    private void firstSetting(){
        if (app.prefs.getBoolean(PREFS_FIRST_SETTING, true))
        {
            app.prefs.edit().putBoolean(PREFS_FIRST_SETTING, false).commit();
        }
    }


}
