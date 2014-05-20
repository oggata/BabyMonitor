package braunster.babymonitor.fragements;

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
import android.widget.TextView;
import android.widget.Toast;

import TCP.connrction_and_threads.AudioStreamController;
import TCP.connrction_and_threads.TCPConnection;
import TCP.interfaces.WifiStatesListener;
import braunster.babymonitor.R;
import braunster.babymonitor.objects.Prefs;

/**
 * Created by itzik on 5/9/2014.
 */
/**
 * A placeholder fragment containing a simple view.
 */
public  class SetupFragment extends BaseFragment {

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

    private static final int FADE_DURATION = 400, BACK_CHANGE_DURATION = 400;
    private static final int[] DATA_SERVER_PORTS = {9481, 4672};
    private static final int[] STREAM_SERVER_PORTS = {5489, 9714};

    /* Views*/
    private LinearLayout liServerClientBtn;
    private Button btnServer, btnClient;
    private TextView txtIp, txtBatterLevel;
    private EditText etIp, etServerPort;

    private PopupWindow serverDialog, clientDialog, infoDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_setup, container, false);

        viewsInit();

        setTxtIp();

        // Set the sample rate to the one saved on the preference manager
        AudioStreamController.sampleRate = app.prefs.getInt(Prefs.SAMPLE_RATE, 8000);

        // Check for ip address and server port from the preferences
        etIp.setText(app.prefs.getString(Prefs.SERVER_IP, ""));
        etServerPort.setText(app.prefs.getString(Prefs.SERVER_PORT, ""));

        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Listen to wifi network events
        app.getStreamConnection().setWifiStatesListener(new WifiStatesListener() {
            @Override
            public void onEnabled() {
                if (DEBUG) Log.v(TAG, "onEnabled");
                setTxtIp();
            }

            @Override
            public void onDisabled() {
                if (DEBUG) Log.v(TAG, "onDisabled");
                setTxtIp();

//                app.closeConnections();
            }

            @Override
            public void onConnected(String networkName) {
                if (DEBUG) Log.v(TAG, " Wifi Listener onConnected, Network Name: " + networkName);
                setTxtIp();
            }

            @Override
            public void onDisconnected() {
                if (DEBUG) Log.v(TAG, "onDisconnected");
                setTxtIp();
            }
        });

        mainView.post(new Runnable() {
            @Override
            public void run() {
                firstLogin();
            }
        });

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

                            app.prefs.edit().putString(Prefs.SERVER_PORT,etServerPort.getText().toString()).commit();
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

                            app.prefs.edit().putString(Prefs.SERVER_IP,etIp.getText().toString()).commit();
                            app.prefs.edit().putString(Prefs.SERVER_PORT,etServerPort.getText().toString()).commit();
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
    }

    private void viewsInit(){

        // Linear Layout
        liServerClientBtn = (LinearLayout) mainView.findViewById(R.id.linear_client_server_select_buttons);

        // Buttons - Server & Client Connection - Disconnect - Control
        btnServer = (Button) mainView.findViewById(R.id.btn_start_server);
        btnClient = (Button) mainView.findViewById(R.id.btn_start_client);

        // EditText - Server Data
        etIp = (EditText) mainView.findViewById(R.id.et_server_ip);
        etServerPort = (EditText) mainView.findViewById(R.id.et_server_port);

        // TextView - Phone Ip
        txtIp = (TextView) mainView.findViewById(R.id.txt_phone_ip);
        txtBatterLevel = (TextView) mainView.findViewById(R.id.txt_battery_level);
    }
    /*
        * --- PopUps ----
                           */
    @Override
    public void createInfoPopup(boolean connected) {
        super.createInfoPopup(connected);
        createHowToConnectPopup();
    }

    public void createHowToConnectPopup(){
        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_info_layout, null);

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
        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_info_layout, null);
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
        View v2 = getActivity().getLayoutInflater().inflate(R.layout.popup_info_layout, null);
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
        View v3 = getActivity().getLayoutInflater().inflate(R.layout.popup_info_layout, null);
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

    /* ----- Show/Hide Views ----*/
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
    /*All content*/
    @Override
    public void showContent(){
        super.showContent();
        fadeViewIn(mainView.findViewById(R.id.linear_content));

        btnClient.setEnabled(true);
        btnServer.setEnabled(true);
    }
    @Override
    public void hideContent(){
        super.hideContent();
        fadeViewOut(mainView.findViewById(R.id.linear_content));

        btnClient.setEnabled(false);
        btnServer.setEnabled(false);
    }

    /*--- First Cases ---*/
    private void firstLogin(){

        if (app.prefs.getBoolean(Prefs.FIRST_LOGIN, true))
        {
            app.prefs.edit().putBoolean(Prefs.FIRST_LOGIN, false).commit();

            createHowToConnectPopup();
        }
    }

    private void firstSetting(){
        if (app.prefs.getBoolean(Prefs.FIRST_SETTING, true))
        {
            app.prefs.edit().putBoolean(Prefs.FIRST_SETTING, false).commit();
        }
    }


}
