package TCP.connrction_and_threads;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import TCP.objects.InOutStreams;
import TCP.xml.XMLParser2;
import TCP.xml.objects.XmlTag;

/**
 * Created by itzik on 5/12/2014.
 */
public class TCPXmlReader extends BaseThread {

    private static final String TAG = TCPXmlReader.class.getSimpleName();
    private static final boolean DEBUG = false;

    private XMLParser2 xmlParser;
    private XmlTag xmlDoc;

    public TCPXmlReader(InOutStreams inOutStreams, Handler handler, TCPConnection connection){
        super(inOutStreams, handler, connection);
    }

    public void setTags(List<String> tags){
        xmlParser = new XMLParser2(tags);
    }

    @Override
    public void run() {
        super.run();

        while (!isInterrupted())
        {
            try {
                currentThread().sleep(100);
            } catch (InterruptedException e) {
                interrupt();
                e.printStackTrace();
            }

            try {
                while (inOutStreams.getInputStream().available() > 0)
                {
                    if (DEBUG) Log.d(TAG, "Available: "  + inOutStreams.getInputStream().available());
                    xmlDoc = xmlParser.getDataForTags(inOutStreams.getInputStream());
                }
            } catch (IOException e) {
                if (DEBUG) Log.e(TAG, "Cane write to socket, socket is closed");
                close();
                reportToHandler(TCPConnection.ERROR_IN_STREAM);
            }

            if (DEBUG) Log.d(TAG, "" + ((xmlDoc == null) ? "Doc is null." : "Doc Size: " + xmlDoc.getChildren().size()) );

            if (xmlDoc != null) {
                reportToHandler(TCPConnection.XML_DATA_IS_RECEIVED, xmlDoc);

                xmlDoc = null;
            }
        }

    }

    @Override
    public void close() {
        super.close();
        interrupt();
    }
}
