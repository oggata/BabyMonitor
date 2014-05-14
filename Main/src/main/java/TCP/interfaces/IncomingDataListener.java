package TCP.interfaces;

import TCP.xml.objects.XmlTag;

/**
 * Created by itzik on 3/3/14.
 */
public interface IncomingDataListener {

    public void onStringDateReceived(String data);
    public void onParedXmlReady(XmlTag xmlTag);

}
