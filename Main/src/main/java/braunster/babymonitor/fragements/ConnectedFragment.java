package braunster.babymonitor.fragements;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import TCP.connrction_and_threads.TCPConnection;
import braunster.babymonitor.BabyMonitorAppObj;
import braunster.babymonitor.ConnectedPhoneData;
import braunster.babymonitor.MonitorActivity;
import braunster.babymonitor.R;

/**
 * Created by itzik on 5/9/2014.
 */
public class ConnectedFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = ConnectedFragment.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final String PREFS_FIRST_CONNECTION = "prefs.first_connection";

    /* Views*/
    private Button btnPlayStop, btnDisconnect;
    private PopupWindow disconnectPopUp, playStopPopUp, incomingDataPopup;
    private TextView txtBatteryLevel, txtBatteryStatus;

    private MonitorActivity monitor;
    private ConnectedPhoneData connectedPhoneData = new ConnectedPhoneData();

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            connectedPhoneData.setBatteryLevel(args.getInt(MonitorActivity.XML_ATTRIBUTE_BATTERY_PERCENTAGE, -1));
            connectedPhoneData.setBatteryStatus(args.getString(MonitorActivity.XML_ATTRIBUTE_BATTERY_STATUS, null));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (BabyMonitorAppObj) getActivity().getApplication();

        monitor = (MonitorActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_connected, null);

        initViews();

        firstConnection();

        return mainView;
    }

    private void initViews(){
        btnDisconnect = (Button) mainView.findViewById(R.id.btn_disconnect);
        btnPlayStop = (Button) mainView.findViewById(R.id.btn_stop_play);
        txtBatteryLevel = (TextView) mainView.findViewById(R.id.txt_battery_level);
        txtBatteryStatus = (TextView) mainView.findViewById(R.id.txt_battery_status);
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
                                {
                                        if (app.getStreamConnection().getAudioController().play())
                                        {
                                            monitor.createConnectedNotification(true);
                                            v.setSelected(!v.isSelected());
                                        }
                                        else Toast.makeText(getActivity(), "Cant Play!", Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                        if (app.getStreamConnection().getRecordController().record())
                                        {
                                            monitor.createConnectedNotification(true);
                                            v.setSelected(!v.isSelected());
                                        }
                                        else Toast.makeText(getActivity(), "Cant Record!", Toast.LENGTH_LONG).show();
                                }
                            }
                            else
                            {
                                if(app.getStreamConnection().getConnectionType() == TCPConnection.SERVER)
                                    app.getStreamConnection().getAudioController().stop();
                                else
                                    app.getStreamConnection().getRecordController().stop();

                                monitor.createConnectedNotification(false);

                                v.setSelected(!v.isSelected());
                            }
                        }
                    }
                });


                break;

            case R.id.btn_disconnect:
                if (app.getStreamConnection() != null)
                {
                    if(DEBUG) Log.d(TAG, "Disconnect");

                    app.getStreamConnection().closeAndTriggerConnectionLost();
                    app.closeConnections();
                }
                break;

            case R.id.btn_info:

                if (app.getStreamConnection().isConnected())
                {
                    createWhenConnectedInfoPopup();
                }
                break;
        }
    }

    @Override
    public void onInfoPressed() {
        super.onInfoPressed();
        createWhenConnectedInfoPopup();
    }

    @Override
    public void onResume() {
        super.onResume();
        btnDisconnect.setOnClickListener(this);
        btnPlayStop.setOnClickListener(this);

        if (connectedPhoneData.getBatteryLevel() != -1 && connectedPhoneData.getBatteryStatus() != null)
            setBatteryData(connectedPhoneData.getBatteryLevel(), connectedPhoneData.getBatteryStatus());

    }

    public void setBatteryData(int level, String status) {
        if (DEBUG) Log.d(TAG, "Battery Data, Percentage: " + level + ", Status: " + status);

        connectedPhoneData.setBatteryLevel(level);
        connectedPhoneData.setBatteryStatus(status);

        if (txtBatteryLevel.getVisibility() != View.VISIBLE)
            txtBatteryLevel.setVisibility(View.VISIBLE);

        if (txtBatteryStatus.getVisibility() != View.VISIBLE)
            txtBatteryStatus.setVisibility(View.VISIBLE);

        txtBatteryLevel.setText(String.valueOf(level));
        txtBatteryStatus.setText(status);
    }

    public void onIncomingData(String contactName, String contactNumber, String text){
        if (DEBUG) Log.d(TAG, "onIncomingCall, Contact: " + contactName +", Numner: " + contactNumber + ((text == null) ? ". Call!" : ". SMS! Text: " + text));

        createIncomingDataPopup(contactName, contactNumber, text);

    }

    public void dismissIncomingDataPopup(){
        if (incomingDataPopup.isShowing())
            incomingDataPopup.dismiss();
    }

    private void createWhenConnectedInfoPopup(){
        View v = getActivity().getLayoutInflater().inflate(R.layout.info_popup_layout, null);

        ((TextView)v.findViewById(R.id.txt)).setText("Disconnect button. There's about 5 seconds delay till the other phone get notified that you disconnected.");
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

        ((TextView)v2.findViewById(R.id.txt)).setText("Play/Stop Button.");
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

    private void createIncomingDataPopup(String contactName, String contactNumber, String text){
        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_incoming_call, null);

        ((TextView)v.findViewById(R.id.txt_caller_name)).setText(contactName + " - " + contactNumber);

        if (text != null)
        {
            v.findViewById(R.id.txt_sms_text).setVisibility(View.VISIBLE);
            ((TextView)v.findViewById(R.id.txt_sms_text)).setText(text);
        }

        incomingDataPopup = new PopupWindow(getActivity());
        incomingDataPopup.setContentView(v);
        incomingDataPopup.setOutsideTouchable(true);
        incomingDataPopup.setBackgroundDrawable(new BitmapDrawable());
        incomingDataPopup.setWidth(screenSize.x);
        incomingDataPopup.setHeight(v.getLayoutParams().WRAP_CONTENT);
        incomingDataPopup.setAnimationStyle(R.style.PopupAnimation);
        incomingDataPopup.showAtLocation(mainView, Gravity.CENTER, 0, 0);
    }

    private void firstConnection(){
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREFS_FIRST_CONNECTION, true))
        {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(PREFS_FIRST_CONNECTION, false).commit();

            createWhenConnectedInfoPopup();
        }
    }
}
