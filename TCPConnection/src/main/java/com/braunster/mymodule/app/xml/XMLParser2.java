package com.braunster.mymodule.app.xml;

import android.util.Log;
import android.util.Xml;

import com.braunster.mymodule.app.connrction_and_threads.BaseThread;
import com.braunster.mymodule.app.xml.objects.XmlTag;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by itzik on 2/25/14.
 */
public class XMLParser2 extends BaseThread {

    private static final String TAG = XMLParser2.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static String nameSpace = "";
    private XmlPullParser parser;
    private InputStream inputStream;
    private List<String> tags;

    public XMLParser2(List<String> tags){
        this.tags = tags;
    }

    @Override
    public void run() {
        super.run();
    }

    public XmlTag getDataForTags(InputStream inputStream){
        if (DEBUG) Log.d(TAG, "getDataForTags");

        try {
            parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
            parser.setInput(inputStream, null);
            XmlTag doc = readFeed(parser, tags);

            return doc;
        }catch (IOException e){
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static XmlTag readFeed(XmlPullParser parser, List<String> tags) throws XmlPullParserException, IOException {
        if (DEBUG) Log.d(TAG, "readFeed");

        List entries = new ArrayList();
        int event = 0;
        String name = null;
        String text;
        XmlTag doc = null;
        XmlTag tag = null;
        XmlTag tmp = null;
        int counter = 0;

        // Skipping th the doc start;
        parser.next();

        do  {
            if (DEBUG) Log.d(TAG, "event: " + parser.getEventType() + ", Counter: " + counter);

            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    counter++;
                    name = parser.getName();
                    if (DEBUG) Log.d(TAG, "Start Tag, Name: " + name);

                    if (doc == null) {
                        if (DEBUG) Log.d(TAG, "Doc Name: " + name);
                        doc = XmlTag.newInstance();
                        doc.setName(name);
                        doc.setAttributes(parser);
                        break;
                    }

                    if (tag == null) {
                        if (DEBUG) Log.d(TAG, "First Tag Name: " + name);
                        tag = XmlTag.newInstance();
                        tag.setName(name);
                        tag.setAttributes(parser);
                        doc.addChild(tag);
                        break;
                    }

                    if (tag.isEnded()) {
                        tag = tag.getParent();
                    }
                    else
                    {
                        tmp = XmlTag.newInstance();
                        tmp.setName(name);
                        tmp.setAttributes(parser);
                        tag.addChild(tmp);
                        tag = tmp;
                    }
                    break;

                case XmlPullParser.TEXT:
                    text = getTextIfWanted(parser, tags, name);

                    if (text != null)
                    {
                        if (tag != null)
                            tag.setText(text);
                        else if (doc != null)
                            doc.setText(text);
                        else if (DEBUG) throw new RuntimeException("doc and tag are null so text could not been written");
                    }

                    if (DEBUG) Log.d(TAG, text == null ? "Text not wanted" : "Text: " + text);
                    break;

                case XmlPullParser.END_TAG:
                    counter--;
                    if (DEBUG) Log.d(TAG, "End Tag - " + name);
                    if (tag != null)
                        tag.setEnded(true);
                    else if (DEBUG) Log.e(TAG, "Tag is null - EndTag");
                    break;

                default: break;
            }

            if (counter == 0) break;

            parser.next();
// TODO close document so parser will end.
        } while (counter != 0);

        if (DEBUG) Log.d(TAG, "Doc Ended!");

        if (doc != null)
            doc.setEnded(true);
        else if (DEBUG) Log.e(TAG, "Doc is null");

        return doc;
    }

    private static String getTextIfWanted(XmlPullParser parser, List<String> tags, String currentTag){
        if (DEBUG) Log.d(TAG, "getTextIfWanted, Name: " + currentTag);
        boolean wanted = false;

        for (int i = 0 ; i < tags.size() && !wanted ; i++){
            if (tags.get(i).equals(currentTag))
                wanted = true;
        }

        if (wanted) {
            if (DEBUG) Log.d(TAG, "Wanted");

            try {
                return readText(parser);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // For the tags title and summary, extracts their text values.
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.getEventType() == XmlPullParser.TEXT) {
            result = parser.getText();
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
