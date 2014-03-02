package TCP;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itzik on 2/26/14.
 */
public class ParseXmlAsyncTask extends AsyncTask<XMLParser, Integer, List<String>> {

    private final static String  TAG = ParseXmlAsyncTask.class.getSimpleName();

    private Context context;
    private ProgressDialog progressDialog;

    public ParseXmlAsyncTask(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Parsing...");
        progressDialog.show();

    }

    @Override
    protected List doInBackground(XMLParser... params) {
        XMLParser xmlParser = params[0];

        List<String> data = null;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();



        if (xmlParser != null)
        {
            List<String> tags = new ArrayList<String>();
            tags.add("Ip");
            tags.add("CountryCode");
            tags.add("CountryName");
            tags.add("City");
            tags.add("Latitude");

            data = xmlParser.getDataForTags(tags);

            for (int i = 0 ; i < data.size(); i++)
                Log.i(TAG, data.get(i));
        }
        else
            Log.d(TAG, "Parser is empty");

        return data;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<String> o) {
        super.onPostExecute(o);

        progressDialog.dismiss();


    }
}
