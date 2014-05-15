package braunster.babymonitor.fragements;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import TCP.connrction_and_threads.TCPConnection;
import TCP.objects.TList;
import TCP.xml.objects.XmlAttr;
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
    private static final String PREFS_AUDIO_MODE = "prefs.audio_mode";

    /* Views*/
    private Button btnPlayStop, btnDisconnect;
    private PopupWindow disconnectPopUp, playStopPopUp, incomingDataPopup;
    private TextView txtBatteryLevel, txtBatteryStatus;

    private MonitorActivity monitor;
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
        monitor = (MonitorActivity) getActivity();

        am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
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

        // Making sure the baby phone is in silent mode so incoming data wont wake up the baby.
        if (app.getStreamConnection().getConnectionType() == TCPConnection.CLIENT)
        {
//            if(DEBUG) Log.d(TAG, "Phone Audio Mode: " + am.getRingerMode());
            if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT)
            {
//                if(DEBUG) Log.d(TAG, "Saving the last mode");
                app.prefs.edit().putInt(PREFS_AUDIO_MODE, am.getRingerMode()).commit();
            }
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OnDestroy");
        // Returning the device audio back to his prev state.
        if (app.prefs.getInt(PREFS_AUDIO_MODE, -1000) != -1000)
        {
//            if(DEBUG) Log.d(TAG, "Putting the phone back to prev state: " + app.prefs.getInt(PREFS_AUDIO_MODE, 0));
            am.setRingerMode(app.prefs.getInt(PREFS_AUDIO_MODE, 0));
        }
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
        if (app.getDataConnection().isConnected())
            createIncomingDataPopup(contactName, contactNumber, text);
    }

    public void dismissIncomingDataPopup(){
        if (incomingDataPopup != null && incomingDataPopup.isShowing())
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

    private void createIncomingDataPopup(String contactName, final String contactNumber, final String text){
        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_incoming_call, null);

        ((TextView)v.findViewById(R.id.txt_caller_name)).setText(contactName + " - " + contactNumber);

        final EditText smsInput = ((EditText)v.findViewById(R.id.et_reply_text)   );
        if (smsInput == null) { if (DEBUG) Log.e(TAG, "sms input is null"); return; }

        v.findViewById(R.id.btn_reply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) Log.d(TAG, "Reply! ");
                String data = smsInput.getText().toString();
                if (!data.matches(""))
                {
                    if (DEBUG) Log.d(TAG, "Data: " + data);
                    // Send the sms text back to the connected phone.
                    monitor.sendDataXml(MonitorActivity.XML_TAG_SMS, data, new TList<XmlAttr>(
                            new XmlAttr(MonitorActivity.XML_ATTRIBUTE_TODO, MonitorActivity.SEND),
                            new XmlAttr(MonitorActivity.XML_ATTRIBUTE_CALLER_CONTACT_NAME, getContactNameForNumber(contactNumber)),
                            new XmlAttr(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER, contactNumber)));

                    if (incomingDataPopup.isShowing())
                        incomingDataPopup.dismiss();
                }
                else Toast.makeText(getActivity(), "Please enter some text.", Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.btn_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monitor.startACall(contactNumber);
                if (incomingDataPopup.isShowing())
                    incomingDataPopup.dismiss();
            }
        });

        v.findViewById(R.id.btn_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (incomingDataPopup.isShowing())
                    incomingDataPopup.dismiss();
            }
        });

        if (text != null)
        {
            v.findViewById(R.id.txt_sms_text).setVisibility(View.VISIBLE);
            ((TextView)v.findViewById(R.id.txt_sms_text)).setText(text);
        }

        incomingDataPopup = new PopupWindow(getActivity());
        incomingDataPopup.setContentView(v);
        incomingDataPopup.setFocusable(true);
        incomingDataPopup.setOutsideTouchable(true);
        incomingDataPopup.setBackgroundDrawable(new BitmapDrawable());
        incomingDataPopup.setWidth((int) (screenSize.x/1.5f));
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

    String getContactNameForNumber(String incomingNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));
        Cursor cursor = getActivity().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        return "Unknown";
    }
}
