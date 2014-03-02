package TCP;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by itzik on 2/25/14.
 */
public class XMLParser {

    private static final String TAG = XMLParser.class.getSimpleName();

    private static String nameSpace = "";
    private XmlPullParser parser;
    private InputStream inputStream;

    public XMLParser(InputStream inputStream){
        this.inputStream = inputStream;
    }

    public List getDataForTags(List<String> tags){

        try {
            parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag();
            return readFeed(parser, tags);
        }catch (IOException e){
            e.printStackTrace();
        } catch (XmlPullParserException e){
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static List readFeed(XmlPullParser parser, List<String> tags) throws XmlPullParserException, IOException {
        List entries = new ArrayList();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.d(TAG, "GetName = " + name);
            // Starts by looking for the entry tag
            String result = getTextIfWanted(parser, tags, name);
            if (result != null)
            {
                entries.add(result);
            } else {
                skip(parser);
            }
        }

        return entries;
    }

    private static String getTextIfWanted(XmlPullParser parser, List<String> tags, String currentTag){

        boolean wanted = false;

        for (int i = 0 ; i < tags.size() && !wanted ; i++){
            if (tags.get(i).equals(currentTag))
                wanted = true;
        }

        if (wanted)
            try {
                return readText(parser);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

        return null;
    }

    // For the tags title and summary, extracts their text values.
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
