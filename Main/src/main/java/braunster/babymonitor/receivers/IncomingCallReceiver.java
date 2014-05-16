package braunster.babymonitor.receivers;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class IncomingCallReceiver extends BaseReceiver {
//TODO change to a service
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
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int events = PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR;
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

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            Log.i(TAG,"onCallForwardingIndicatorChanged  CFI ="+cfi);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("CALL_FORWARD_ACTIVE", cfi).commit();
            super.onCallForwardingIndicatorChanged(cfi);
        }
    };


}