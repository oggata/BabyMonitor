package braunster.babymonitor.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import TCP.objects.TList;
import TCP.xml.objects.XmlAttr;
import TCP.xml.objects.XmlMessage;
import TCP.xml.objects.XmlTag;
import braunster.babymonitor.BabyMonitorAppObj;

/**
 * Created by itzik on 5/14/2014.
 */
public class BaseReceiver extends BroadcastReceiver {

    private static final String TAG = BaseReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;
    private BabyMonitorAppObj app = BabyMonitorAppObj.getInstance();
    Context context;
    IntentFilter filter;
    Intent intent;
    CallsAndSMSListener callsAndSMSListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;

        if (DEBUG && intent.getAction() != null) Log.d(TAG, "Action: " + intent.getAction());

    }

    public IntentFilter getFilter(){
        filter = new IntentFilter("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");

        return filter;
    }

    public void setFilter(String... filters) {
        filter = new IntentFilter();
        for (String f : filters)
            filter.addAction(f);
    }

    String getContactNameForNumber(String incomingNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));
        Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        return "Unknown";
    }

    public interface CallsAndSMSListener{
        public void onStartRinging(String callerName, String phoneNumber);
        public void onStartDialing(String phoneNumber);
        public void onHangUp(String phoneNumber);
        public void onSmsReceived(String contactName, String contactNumber, String text);
    }

    public void setCallsAndSmsReceiver(CallsAndSMSListener callsAndSMSListener) {
        this.callsAndSMSListener = callsAndSMSListener;
    }

    void sendDataXml(String name, String text, TList<XmlAttr> attrs){
        XmlTag xmlTag;

        if (text == null)
            xmlTag  = XmlTag.getTag(name, attrs);
        else xmlTag = XmlTag.getTag(name, text, attrs);

        Log.d(TAG, "Attr Amount: " + xmlTag.getAttributes().size() + ", Name: "  + xmlTag.getAttributes().get(0).getName() + xmlTag.getAttributes().get(0).getIndex());

        app.getDataConnection().write(XmlMessage.writeMessage(xmlTag));
    }
}
