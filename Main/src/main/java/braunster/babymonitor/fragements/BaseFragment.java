package braunster.babymonitor.fragements;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import TCP.connrction_and_threads.AudioStreamController;
import TCP.objects.TList;
import TCP.xml.objects.XmlAttr;
import braunster.babymonitor.R;
import braunster.babymonitor.activities.MonitorActivity;
import braunster.babymonitor.objects.BabyMonitorAppObj;
import braunster.babymonitor.objects.Call;
import braunster.babymonitor.objects.Prefs;
import braunster.babymonitor.objects.SimpleListAdapter;

/**
 * Created by itzik on 5/9/2014.
 */
public class BaseFragment extends Fragment implements BaseFragmentInterface , CompoundButton.OnCheckedChangeListener{

    private static final String TAG = BaseFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int FADE_DURATION = 400;

    public static final String SCREEN_WIDTH = "screen_width";
    public static final String SCREEN_HEIGHT = "screen_height";

    Point screenSize = new Point();
    View mainView;
    BabyMonitorAppObj app;
    MonitorActivity monitor;

    private Bundle incomingData;
    private PopupWindow incomingDataPopup;
    private boolean showIncomingDataPopup = false;

    private PopupWindow callLogPopup, settingsPopup, audioSettingPopup;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null)
        {
            if (args.getInt(SCREEN_WIDTH, -1) != -1)
                screenSize.x =  args.getInt(SCREEN_WIDTH);

            if (args.getInt(SCREEN_HEIGHT, -1) != -1)
                screenSize.y =  args.getInt(SCREEN_HEIGHT);

            if (args.getString(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER) != null)
            {
                if (DEBUG) Log.v(TAG, "Has Arguments.");
                setIncomingData(args);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate");

        if (getActivity() != null)
        {
            app = (BabyMonitorAppObj) getActivity().getApplication();
            monitor = (MonitorActivity) getActivity();
        }
        else app = BabyMonitorAppObj.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume");
        if (showIncomingDataPopup || incomingData != null)
            mainView.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.v(TAG, "on Resume, Show incomingDataPopup");
                    createIncomingCallDataPopup(app.getDataConnection().isConnected(),
                            incomingData.getString(MonitorActivity.XML_ATTRIBUTE_CALLER_CONTACT_NAME),
                            incomingData.getString(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER),
                            incomingData.getString(MonitorActivity.XML_TAG_SMS),
                            false);
                }
            });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.v(TAG, "onDestroy");
        if (incomingDataPopup != null && incomingDataPopup.isShowing())
        {
            //Making sure that if the fragment recreated it self multiple times it will still show the incoming data popup.
            showIncomingDataPopup = false;
            incomingDataPopup.dismiss();
        }

        dismissAllPopups();
    }

    @Override
    public void createInfoPopup(boolean connected) {

    }

    @Override
    public void createSettingsPopup(boolean connected, View dropDownView) {

        dismissAllPopups();

        settingsPopup = new PopupWindow(getActivity());

        View v = getActivity().getLayoutInflater().inflate(R.layout.pop_settings, null);

        v.findViewById(R.id.btn_cancel_call_forwarding).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) Log.d(TAG, "Cancel Call Forwarding! ");
                monitor.cancelCallForwarding();

            }
        });

        ((CheckBox)v.findViewById(R.id.chk_box_forward_calls)).setOnCheckedChangeListener(this);
        ((CheckBox)v.findViewById(R.id.chk_box_get_notifications_about_sms_received)).setOnCheckedChangeListener(this);
        ((CheckBox)v.findViewById(R.id.chk_box_enable_auto_silent_mode)).setOnCheckedChangeListener(this);
        ((CheckBox)v.findViewById(R.id.chk_box_enable_auto_restore_phone_audio_mode)).setOnCheckedChangeListener(this);

        ((CheckBox)v.findViewById(R.id.chk_box_forward_calls)).setChecked(app.prefs.getBoolean(Prefs.USE_CALL_FORWARDING, true));
        ((CheckBox)v.findViewById(R.id.chk_box_get_notifications_about_sms_received)).setChecked(app.prefs.getBoolean(Prefs.USE_SMS_TUNNELING, true));
        ((CheckBox)v.findViewById(R.id.chk_box_enable_auto_silent_mode)).setChecked(app.prefs.getBoolean(Prefs.AUTO_ENTER_SILENT_MODE, true));
        ((CheckBox)v.findViewById(R.id.chk_box_enable_auto_restore_phone_audio_mode)).setChecked(app.prefs.getBoolean(Prefs.AUTO_RESTORE_PREV_AUDIO_MODE, true));

        // Disable auto restore if auto enable silent mode is false
        if (!app.prefs.getBoolean(Prefs.AUTO_ENTER_SILENT_MODE, true))
            v.findViewById(R.id.chk_box_enable_auto_restore_phone_audio_mode).setEnabled(false);

        if (!app.hasTelephonyService())
            v.findViewById(R.id.chk_box_forward_calls).setEnabled(false);

        if (connected)
        {
            ((CheckBox)v.findViewById(R.id.chk_box_forward_calls)).setTextColor(Color.WHITE);
            ((CheckBox)v.findViewById(R.id.chk_box_get_notifications_about_sms_received)).setTextColor(Color.WHITE);
            ((CheckBox)v.findViewById(R.id.chk_box_enable_auto_silent_mode)).setTextColor(Color.WHITE);
            ((CheckBox)v.findViewById(R.id.chk_box_enable_auto_restore_phone_audio_mode)).setTextColor(Color.WHITE);

            ((TextView)v.findViewById(R.id.txt_cancel_call_forwarding_service)).setTextColor(Color.WHITE);

            v.setBackgroundColor(Color.BLACK);
        }

        hideContent();

        settingsPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                showContent();
            }
        });

        settingsPopup.setContentView(v);
        settingsPopup.setFocusable(true);
        settingsPopup.setOutsideTouchable(true);
        settingsPopup.setBackgroundDrawable(new BitmapDrawable());
        settingsPopup.setWidth((int) (screenSize.x/1.2f));
        settingsPopup.setHeight(v.getLayoutParams().WRAP_CONTENT);
        settingsPopup.setAnimationStyle(R.style.PopupAnimation);
        settingsPopup.showAsDropDown(dropDownView);
    }

    @Override
    public void createCallLogPopup(final boolean connected, String sessionId) {
        dismissIncomingDataPopup();
        dismissAllPopups();

        callLogPopup = new PopupWindow(getActivity());

        Log.i(TAG, "createCallLogPopup");

        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_call_log, null);

        final ArrayList<Call> calls = app.getCallsDataSource().getAllCalls(app.getDataSessionId());

        if (calls.size() > 0) {
            ((ListView) v.findViewById(R.id.list_log)).setAdapter(new SimpleListAdapter(getActivity(), app.getCallsDataSource().getAllCalls(sessionId)));

            ((ListView) v.findViewById(R.id.list_log)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    createIncomingCallDataPopup(connected, calls.get(position).getName(), calls.get(position).getNumber(), calls.get(position).getText(), true);
                }
            });
        }
        else ((TextView)v.findViewById(R.id.txt_header)).setText("No Calls");

        // Dismiss the popup.
        v.findViewById(R.id.btn_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callLogPopup.isShowing())
                    callLogPopup.dismiss();

            }
        });

        hideContent();

        callLogPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                showContent();
            }
        });

        callLogPopup.setContentView(v);
        callLogPopup.setFocusable(true);
        callLogPopup.setOutsideTouchable(true);
        callLogPopup.setBackgroundDrawable(new BitmapDrawable());
        callLogPopup.setWidth((int) (screenSize.x/1.2f));
        callLogPopup.setHeight(v.getLayoutParams().WRAP_CONTENT);
        callLogPopup.setAnimationStyle(R.style.PopupAnimation);

        callLogPopup.showAtLocation(mainView, Gravity.CENTER, 0, 0);
    }

    @Override
    public void createIncomingCallDataPopup(boolean connected, String contactName, final String contactNumber, String text, boolean outSideTouchable) {
        Log.i(TAG, "createIncomingDataPopup, Contact Name: " + contactName + ", Number: " + contactName + (text != null? ", Text: " + text : "") +  ", OutSideTouchable: " + String.valueOf(outSideTouchable) + "." );

        dismissIncomingDataPopup();
        dismissAllPopups();

        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_incoming_call, null);

        ((TextView)v.findViewById(R.id.txt_caller_name)).setText(contactName + " - " + contactNumber);

        final EditText smsInput = ((EditText)v.findViewById(R.id.et_reply_text)   );
        if (smsInput == null) { if (DEBUG) Log.e(TAG, "sms input is null"); return; }

        if (connected)
        {
            // Reply to the caller/sender with SMS sent from the connected device.
            v.findViewById(R.id.btn_reply).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DEBUG) Log.d(TAG, "Reply! ");
                    String data = smsInput.getText().toString();
                    if (!data.matches(""))
                    {
                        if (DEBUG) Log.d(TAG, "Sms: " + data);
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
        }
        else
        {
            smsInput.setVisibility(View.GONE);
            v.findViewById(R.id.btn_reply).setVisibility(View.GONE);
        }


        // Set onClick to start a call.
        if (app.hasTelephonyService())
            v.findViewById(R.id.btn_call).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    monitor.startACall(contactNumber);
                    if (incomingDataPopup.isShowing())
                        incomingDataPopup.dismiss();
                }
            });
            // Hide the call option if this device do not have telephony capabilities.
        else v.findViewById(R.id.btn_call).setVisibility(View.GONE);

        // Dismiss the popup.
        v.findViewById(R.id.btn_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (incomingDataPopup.isShowing())
                    incomingDataPopup.dismiss();

            }
        });

        // If sms
        if (text != null)
        {
            v.findViewById(R.id.txt_sms_text).setVisibility(View.VISIBLE);
            ((TextView)v.findViewById(R.id.txt_sms_text)).setText(text);
        }

        incomingDataPopup = new PopupWindow(getActivity());
        incomingDataPopup.setContentView(v);
        incomingDataPopup.setFocusable(true);
        incomingDataPopup.setOutsideTouchable(outSideTouchable);
        incomingDataPopup.setBackgroundDrawable(new BitmapDrawable());
        incomingDataPopup.setWidth((int) (screenSize.x/1.2f));
        incomingDataPopup.setHeight(v.getLayoutParams().WRAP_CONTENT);
        incomingDataPopup.setAnimationStyle(R.style.PopupAnimation);

        incomingDataPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                Log.d(TAG, "Dismissed");
                if (showIncomingDataPopup)
                {
                    monitor.removeExtra(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER);
                    incomingData = null;
                }
                showIncomingDataPopup = false;
            }
        });

        incomingDataPopup.showAtLocation(mainView, Gravity.CENTER, 0, 0);
    }

    @Override
    public void createAudioSettingPopup(boolean connected, View dropDownView) {
        dismissAllPopups();

        audioSettingPopup = new PopupWindow(getActivity());

        View v = getActivity().getLayoutInflater().inflate(R.layout.popup_audio_setting_layout, null);

        final RadioGroup radioGrp = (RadioGroup) v.findViewById(R.id.radio_grp_samples);
        RadioButton radio;

        if (connected)
        {
            v.setBackgroundColor(Color.BLACK);
            ((TextView)v.findViewById(R.id.txt_header)).setTextColor(Color.WHITE);
        }
        else v.setBackgroundResource(R.color.app_grey);

        // Showing the user all his available SampleRates
        for ( int rate : AudioStreamController.getSupportedSampleRates())
        {
            radio = new RadioButton(getActivity());

            if (rate == AudioStreamController.sampleRate)
                radio.setChecked(true);

            radio.setText(String.valueOf(rate));
            if (connected)
                radio.setTextColor(Color.WHITE);
            radio.setId(rate);
            radioGrp.addView(radio);
        }

        radioGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, final int checkedId) {

                AudioStreamController.sampleRate = checkedId;

                app.prefs.edit().putInt(Prefs.SAMPLE_RATE, checkedId).commit();

                // Small delay so the user will see what he picked.
                mainView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        audioSettingPopup.dismiss();
                    }
                }, 300);
            }
        });

        audioSettingPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                showContent();
            }
        });

        audioSettingPopup.setContentView(v);
        audioSettingPopup.setOutsideTouchable(true);
        audioSettingPopup.setFocusable(true);
        audioSettingPopup.setBackgroundDrawable(new BitmapDrawable());
        audioSettingPopup.setWidth(v.getLayoutParams().WRAP_CONTENT);
        audioSettingPopup.setHeight(v.getLayoutParams().WRAP_CONTENT);
        audioSettingPopup.setAnimationStyle(R.style.PopupAnimation);

        hideContent();

        audioSettingPopup.showAsDropDown(dropDownView);

    }

    @Override
    public void showContent() {}

    @Override
    public void hideContent() {}

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId())
        {
            case R.id.chk_box_forward_calls:
                app.prefs.edit().putBoolean(Prefs.USE_CALL_FORWARDING, isChecked).commit();
                break;

            case R.id.chk_box_enable_auto_silent_mode:
                app.prefs.edit().putBoolean(Prefs.AUTO_ENTER_SILENT_MODE, isChecked).commit();
                if (!isChecked)
                    ((ViewGroup)buttonView.getParent()).findViewById(R.id.chk_box_enable_auto_restore_phone_audio_mode).setEnabled(false);
                else ((ViewGroup)buttonView.getParent()).findViewById(R.id.chk_box_enable_auto_restore_phone_audio_mode).setEnabled(true);

                break;

            case R.id.chk_box_enable_auto_restore_phone_audio_mode:
                app.prefs.edit().putBoolean(Prefs.AUTO_RESTORE_PREV_AUDIO_MODE, isChecked).commit();
                break;

            case R.id.chk_box_get_notifications_about_sms_received:
                app.prefs.edit().putBoolean(Prefs.USE_SMS_TUNNELING, isChecked).commit();
                break;
        }
    }

    public void setIncomingData(Bundle extras) {
        Log.v(TAG, "setIncomingData, " +
                "Name: " + extras.getString(MonitorActivity.XML_ATTRIBUTE_CALLER_CONTACT_NAME) +
                ", Number: " + extras.getString(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER) +
                (extras.getString(MonitorActivity.XML_TAG_SMS) != null ? ", Text: " + extras.getString(MonitorActivity.XML_TAG_SMS) + "." :"." )  );
        this.incomingData = extras;
        showIncomingDataPopup = true;
    }

    public void dismissIncomingDataPopup(){
        if (incomingDataPopup != null && incomingDataPopup.isShowing())
        {
            incomingDataPopup.dismiss();
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

    void dismissAllPopups(){
        if (callLogPopup!= null && callLogPopup.isShowing())
            callLogPopup.dismiss();

        if (settingsPopup!= null && settingsPopup.isShowing())
            settingsPopup.dismiss();

        if (audioSettingPopup!= null && audioSettingPopup.isShowing())
            audioSettingPopup.dismiss();
    }

    /* ----- Animation ----*/
        /* Fade in and Fade out given view*/
    void  fadeViewIn(final View v){

        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);

        v.animate().alpha(1f).setDuration(FADE_DURATION).setListener(null);
    }
    void fadeViewOut(final View v){

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
}
