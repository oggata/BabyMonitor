package braunster.babymonitor.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class IncomingCallReceiver extends BaseReceiver {

    private static final String TAG = IncomingCallReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static int IDLE = 0;
    private static int OUT_CALL = 1;
    private static int IN_CALL = 2;
    private int callState = IDLE;





    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (DEBUG) Log.d(TAG, "onReceived");

        switch (getResultCode())
        {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Toast.makeText(context, "Generic failure",
                        Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                Toast.makeText(context, "No service",
                        Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                Toast.makeText(context, "Null PDU",
                        Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                Toast.makeText(context, "Radio off",
                        Toast.LENGTH_SHORT).show();
                break;
            case Activity.RESULT_OK:
                Toast.makeText(context, "SMS delivered",
                        Toast.LENGTH_SHORT).show();
                break;
            case Activity.RESULT_CANCELED:
                Toast.makeText(context, "SMS not delivered",
                        Toast.LENGTH_SHORT).show();
                break;
        }

        context = context;
        intent = intent;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int events = PhoneStateListener.LISTEN_CALL_STATE;
        tm.listen(phoneStateListener, events);
    }

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DEBUG) Log.d(TAG, "onCallStateChanged - " + incomingNumber + " - State - " + state);

            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (callState == IN_CALL || callState == OUT_CALL)
                        if (callsAndSMSListener != null) callsAndSMSListener.onHangUp(incomingNumber);

                    callState = IDLE;
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Toast.makeText(context, "Local Call - " + incomingNumber, Toast.LENGTH_LONG).show();

                    if (callState == IDLE)
                        if (callsAndSMSListener != null) callsAndSMSListener.onStartRinging(getContactNameForNumber(incomingNumber), incomingNumber);

                    callState = IN_CALL;

                    if (DEBUG) Log.d(TAG, "Ringing...");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    String dialingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

                    if (callState == IDLE)
                        if (callsAndSMSListener != null) callsAndSMSListener.onStartDialing(dialingNumber);

                    callState = OUT_CALL;

                    if (DEBUG) Log.d(TAG, "Dialing - " + dialingNumber);
                    break;
            }

            super.onCallStateChanged(state, incomingNumber);
        }
    };


}