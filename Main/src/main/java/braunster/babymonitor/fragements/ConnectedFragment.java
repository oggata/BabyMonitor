package braunster.babymonitor.fragements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
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
import braunster.babymonitor.R;
import braunster.babymonitor.activities.MonitorActivity;
import braunster.babymonitor.objects.BabyMonitorAppObj;
import braunster.babymonitor.objects.ConnectedPhoneData;
import braunster.babymonitor.objects.NotUtil;
import braunster.babymonitor.objects.Prefs;

/**
 * Created by itzik on 5/9/2014.
 */
public class ConnectedFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = ConnectedFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    /* Views*/
    private Button btnPlayStop, btnDisconnect;
    private PopupWindow disconnectPopUp, playStopPopUp;
    private TextView txtBatteryLevel, txtBatteryStatus;

    private ConnectedPhoneData connectedPhoneData = new ConnectedPhoneData();
    private AudioManager am;

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
        if (DEBUG) Log.v(TAG, "onCreate");

        am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onCreateView");
        mainView = inflater.inflate(R.layout.fragment_connected, null);

        NotUtil.createConnectedNotification(getActivity(), false, app.getDataConnection().isServer());

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
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume");
        btnDisconnect.setOnClickListener(this);
        btnPlayStop.setOnClickListener(this);

        if (connectedPhoneData.getBatteryLevel() != -1 && connectedPhoneData.getBatteryStatus() != null)
            setBatteryData(connectedPhoneData.getBatteryLevel(), connectedPhoneData.getBatteryStatus());

        // Making sure the baby phone is in silent mode so incoming data wont wake up the baby.
        if (app.prefs.getBoolean(Prefs.AUTO_ENTER_SILENT_MODE, true) && app.getStreamConnection().getConnectionType() == TCPConnection.CLIENT)
        {
//            if(DEBUG) Log.d(TAG, "Phone Audio Mode: " + am.getRingerMode());
            if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT)
            {
//                if(DEBUG) Log.d(TAG, "Saving the last mode");
                app.prefs.edit().putInt(Prefs.AUDIO_MODE, am.getRingerMode()).commit();
            }

            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.v(TAG, "OnDestroy");
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
                                        NotUtil.createConnectedNotification(getActivity(), true, app.getDataConnection().isServer());
                                        v.setSelected(!v.isSelected());
                                    }
                                    else Toast.makeText(getActivity(), "Cant Play!", Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    if (app.getStreamConnection().getRecordController().record())
                                    {
                                        NotUtil.createConnectedNotification(getActivity(), true, app.getDataConnection().isServer());
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

                                NotUtil.createConnectedNotification(getActivity(), false, app.getDataConnection().isServer());

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

                    app.getDataConnection().closeAndTriggerConnectionLost();
                    app.closeConnections();
                }
                break;
        }
    }

    @Override
    public void createInfoPopup(boolean connected) {
        super.createInfoPopup(connected);
        createWhenConnectedInfoPopup();
    }

    @Override
    public void showContent(){
        super.showContent();
        fadeViewIn(mainView.findViewById(R.id.relative_contents));
    }

    @Override
    public void hideContent() {
        super.hideContent();
        fadeViewOut(mainView.findViewById(R.id.relative_contents));
    }

    public void setBatteryData(int level, String status) {
        if (DEBUG) Log.d(TAG, "Battery Data, Percentage: " + level + ", Status: " + status);
        if (app.getDataConnection().isConnected()) {
            connectedPhoneData.setBatteryLevel(level);
            connectedPhoneData.setBatteryStatus(status);

            if (txtBatteryLevel.getVisibility() != View.VISIBLE)
                txtBatteryLevel.setVisibility(View.VISIBLE);

            if (txtBatteryStatus.getVisibility() != View.VISIBLE)
                txtBatteryStatus.setVisibility(View.VISIBLE);

            txtBatteryLevel.setText(String.valueOf(level));
            txtBatteryStatus.setText(status);
        }
    }

    public void onIncomingData(String contactName, String contactNumber, String text){
        if (DEBUG) Log.d(TAG, "onIncomingCall, Contact: " + contactName +", Numner: " + contactNumber + ((text == null) ? ". Call!" : ". SMS! Text: " + text));
        if (app == null)
            app = BabyMonitorAppObj.getInstance();

        if (app.getDataConnection().isConnected())
            if (app.isVisible()) {
                if (DEBUG) Log.d(TAG, "App is visible");
                createIncomingCallDataPopup(true, contactName, contactNumber, text, true);
            }
            else
            {
                if (DEBUG) Log.d(TAG, "App is not visible");
                Intent resultIntent = new Intent(app.getApplicationContext(), MonitorActivity.class);
                resultIntent.putExtra(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER, contactNumber);
                resultIntent.putExtra(MonitorActivity.XML_ATTRIBUTE_CALLER_CONTACT_NAME, contactName);

                Bundle data = new Bundle();

                if (text == null)
                {
                    data.putString(NotUtil.TICKER, "You have a call from: " + (contactName.equals("UNKNOWN") ? contactNumber : contactName));
                    data.putString(NotUtil.CONTENT, (app.hasTelephonyService() ? "Press for response options." : "press for sending back an SMS"));
                }
                else
                {
                    data.putString(NotUtil.TICKER, "You have a SMS from: " + (contactName.equals("UNKNOWN") ? contactNumber : contactName));
                    resultIntent.putExtra(MonitorActivity.XML_TAG_SMS, text);
                    String tmpText = text;
                    if (text.length() > 10)
                        tmpText = text.substring(0, 10) + "...";

                    text = '"' + tmpText + '"';

                    data.putString(NotUtil.CONTENT, (app.hasTelephonyService() ? text + ", Press for response options." : text + ", Press for sending back an SMS"));
                }

                setIncomingData(resultIntent.getExtras());

                data.putString(NotUtil.TITLE, contactName.equals("UNKNOWN") ? contactNumber : contactName);

                NotUtil.createAlertNotification(getActivity(), 2000, resultIntent, data);
            }
    }

    public  void restoreAudioModeIfWanted(){
        // Returning the device audio back to his prev state.
        if (app.prefs.getBoolean(Prefs.AUTO_ENTER_SILENT_MODE, true) &&
                app.prefs.getBoolean(Prefs.AUTO_RESTORE_PREV_AUDIO_MODE, true) &&
                    app.prefs.getInt(Prefs.AUDIO_MODE, -1000) != -1000)
        {
            if(DEBUG) Log.v(TAG, "Putting the phone back to prev state: " + app.prefs.getInt(Prefs.AUDIO_MODE, 0));

            // Setting the audio back to the mode he was before the app set the mode to silent.
            am.setRingerMode(app.prefs.getInt(Prefs.AUDIO_MODE, 0));
            // Removing the audio mode saved.
            app.prefs.edit().remove(Prefs.AUDIO_MODE);
        }
    }

    /** popups */
    private void createWhenConnectedInfoPopup(){
        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_info_layout, null);

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


        View v2 = getActivity().getLayoutInflater().inflate(R.layout.popup_info_layout, null);

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

    private void firstConnection(){
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Prefs.FIRST_CONNECTION, true))
        {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(Prefs.FIRST_CONNECTION, false).commit();

            mainView.post(new Runnable() {
                @Override
                public void run() {
                    createWhenConnectedInfoPopup();
                }
            });
        }
    }
}
