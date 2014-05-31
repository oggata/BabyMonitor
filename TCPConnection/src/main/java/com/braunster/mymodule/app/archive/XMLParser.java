package com.braunster.mymodule.app.archive;

import android.util.Log;
import android.util.Xml;

import com.braunster.mymodule.app.connrction_and_threads.BaseThread;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by itzik on 2/25/14.
 */
public class XMLParser extends BaseThread {

    private static final String TAG = XMLParser.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static String nameSpace = "";
    private XmlPullParser parser;
    private InputStream inputStream;
    private List<String> tags;

    public XMLParser(List<String> tags){
        this.tags = tags;
    }

    public XMLParser(InputStream inputStream){
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        super.run();
        while (!isInterrupted())
        {
            List<String> tags = new ArrayList<String>();
            tags.add("message");
            tags.add("battery");

            List<String> data = getDataForTags(tags);

            if (data != null)
                for (String s : data)
                    Log.i(TAG, "Data: " + s);

            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if (DEBUG) Log.d(TAG, "GetName = " + name);
            // Starts by looking for the entry tag
            String result = getTextIfWanted(parser, tags, name);
            if (DEBUG) Log.d(TAG, "Result: " + result);

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

    public static String convertStreamToString(InputStream is) {
    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                if (DEBUG) Log.d(TAG, line);
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public List getDataForTags(InputStream inputStream){
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
}
