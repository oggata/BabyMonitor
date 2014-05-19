package braunster.babymonitor.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import TCP.objects.TList;
import TCP.xml.objects.XmlAttr;
import braunster.babymonitor.activities.MonitorActivity;
import braunster.babymonitor.objects.Prefs;

/**
 * Created by itzik on 5/14/2014.
 */
public class SmsReceiver extends BaseReceiver {
    private static final String TAG = SmsReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (DEBUG) Log.d(TAG, "onReceived");

        if (!app.getDataConnection().isServer() && app.getDataConnection().isConnected() && app.prefs.getBoolean(Prefs.USE_SMS_TUNNELING, true))
        {
            if (intent.getAction() == "android.provider.Telephony.SMS_RECEIVED") {//---get the SMS message passed in---
                Bundle bundle = intent.getExtras();
                SmsMessage[] msgs = null;
                String contactNumber, text;
                if (bundle != null) {
                    //---retrieve the SMS message received---
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        contactNumber = msgs[i].getOriginatingAddress();
                        text = msgs[i].getMessageBody().toString();
                        if (DEBUG) Log.d(TAG, "Number: " + contactNumber + ", Text: " + text);
                        if (callsAndSMSListener != null)
                            callsAndSMSListener.onSmsReceived(getContactNameForNumber(contactNumber), contactNumber, text);
                        else
                        {
                            if (DEBUG) Log.e(TAG, "No call and sms listener");
                            sendDataXml(MonitorActivity.XML_TAG_SMS, text, new TList<XmlAttr>(
                                    new XmlAttr(MonitorActivity.XML_ATTRIBUTE_TODO, MonitorActivity.READ),
                                    new XmlAttr(MonitorActivity.XML_ATTRIBUTE_CALLER_CONTACT_NAME, getContactNameForNumber(contactNumber)),
                                    new XmlAttr(MonitorActivity.XML_ATTRIBUTE_PHONE_NUMBER, contactNumber) ) );
                        }
                    }
                }
            }
        }
    }
}
