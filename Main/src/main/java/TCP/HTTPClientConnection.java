package TCP;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by itzik on 2/25/14.
 */
public class HTTPClientConnection {

    private static final String TAG = HTTPClientConnection.class.getSimpleName();

    public static final String TYPE_XML = "httpclient.return_type.xml";
    public static final String TYPE_JSON = "httpclient.return_type.json";

    public static Object connect(String url, String returnType)
    {
        String result = null;
        URL geoipURL = null;
        HttpURLConnection httpURLConnection;
        try {
            geoipURL = new URL(url);
            httpURLConnection = (HttpURLConnection) geoipURL.openConnection();
            Log.d(TAG, httpURLConnection.getContentType());
            InputStream instream = httpURLConnection.getInputStream();

            if (returnType.equals(TYPE_JSON))
                return stringToJSON(convertStreamToString(instream));
            else if (returnType.equals(TYPE_XML))
                return new XMLParser(instream);


            // now you have the string representation of the HTML request
            instream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


//        HttpClient httpclient = new DefaultHttpClient();
//
//        // Prepare a request object
//        HttpGet httpget = new HttpGet(url);
//
//        // Execute the request
//        HttpResponse response;
//        try {
//            response = httpclient.execute(httpget);
//            // Examine the response status
//
//            Log.i(TAG,response.getStatusLine().toString());
//
//            // Get hold of the response entity
//            HttpEntity entity = response.getEntity();
//            // If the response does not enclose an entity, there is no need
//            // to worry about connection release
//
//            if (entity != null) {
//
//                // A Simple JSON Response Read
//                InputStream instream = entity.getContent();
//
//
//                if (returnType.equals(TYPE_JSON))
//                    return stringToJSON(convertStreamToString(instream));
//                else if (returnType.equals(TYPE_XML))
//                    return new XMLParser(instream);
//
//
//                // now you have the string representation of the HTML request
//                instream.close();
//            }
//
//
//        } catch (Exception e) {e.printStackTrace();}

        return null;
    }

    private static String convertStreamToString(InputStream is) {
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
                Log.d(TAG, line);
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

    private static JSONObject stringToJSON(String input){
        if (input != null)
            try {
                return new JSONObject(input);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        return null;
    }

}