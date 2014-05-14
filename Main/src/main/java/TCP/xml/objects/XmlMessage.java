package TCP.xml.objects;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

import TCP.objects.TList;


/**
 * Created by itzik on 3/3/14.
 */
public class XmlMessage {

    private static final String TAG = XmlMessage.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static XmlSerializer serializer;
    private static StringWriter writer;

    public static final String XML_TAG_CHECK = "check";

    public static String writeMessage(XmlTag tag){
        return writeFullXml(tag);
    }

    public static String getCheckMessage(){
        return writeMessage( XmlTag.getTag( "check", "hello", new TList<XmlAttr>(new XmlAttr( "time", String.valueOf(System.currentTimeMillis()) ) ) ) );
    }

    public static String writeXml(XmlTag tags){
        if (DEBUG) Log.d(TAG, "WriteMessage");

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "messages");
            // TODO prefix
            serializer.attribute("", "amount", String.valueOf(tags.getChildren().size()));

            for (XmlTag tag: tags.getChildren().asList()){

                serializer.startTag("", tag.getName());

                for (XmlAttr att : tag.getAttributes()) {
                    if (DEBUG) Log.d(TAG, "Attribute, Name: " + att.getName() + ", Value: " + att.getValue());
                    serializer.attribute("", att.getName(), att.getValue());
                }

                if (tag.hasText())
                    serializer.text(tag.getText());

                serializer.endTag("", tag.getName());

            }

            serializer.endTag("", "messages");

            serializer.endDocument();

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /*
      Writing a full xml from one xmlTag
                                        */
    private static void startDoc() throws IOException {
        serializer = Xml.newSerializer();
        writer = new StringWriter();
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);
    }

    private static void startSingleTag(XmlTag tag) throws IOException {
        serializer.startTag("", tag.getName());

        for (XmlAttr att : tag.getAttributes()) {
            if (DEBUG) Log.d(TAG, "Attribute, Name: " + att.getName() + ", Value: " + att.getValue());
            serializer.attribute("", att.getName(), att.getValue());
        }

        if (tag.hasText())
            serializer.text(tag.getText());
    }

    private static void endSingleTag(XmlTag tag) throws IOException {
        serializer.endTag("", tag.getName());
    }

    private static void endDoc() throws IOException {
        serializer.endDocument();
    }

    private static String writeFullXml(XmlTag xml){
        try {
            if (xml == null)
                return "xml is null";

            startDoc();

            XmlTag xmlTag = xml;

            // Write the first tag
            startSingleTag(xmlTag);

            while (true)
            {
                if (DEBUG) Log.d(TAG, "Tag, Name: " + xmlTag.getName() + ", Children: " + xmlTag.getChildren().size());
                // If has no childs go to the parent
                if (xmlTag.getChildren().size() == 0 || !xmlTag.getChildren().hasNext()) {
                    if (DEBUG) Log.d(TAG, ">Name: " + xmlTag.getName() + " No Child's");
                    endSingleTag(xmlTag);

                    if (xmlTag.hasParent())
                        xmlTag = xmlTag.getParent();
                    else
                        break;
                }
                else
                {
                    xmlTag = xmlTag.getChildren().getNext();
                    xmlTag.getChildren().resetCounting();

                    startSingleTag(xmlTag);

                    if (DEBUG) Log.d(TAG, "<Name: " + xmlTag.getName());

                    if (xmlTag.getText() != null)
                        if (DEBUG) Log.d(TAG, "Text: " + xmlTag.getText());

                    for (XmlAttr attr : xmlTag.getAttributes())
                        if (DEBUG) Log.d(TAG, "Attribute name: " + attr.getName() + ", Value: " + attr.getValue());
                }
            }

            endDoc();
            if (DEBUG) Log.d(TAG, "End Doc: ");

            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
