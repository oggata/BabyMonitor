/*
package TCP.xml;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import TCP.archive.XMLParser;
import TCP.interfaces.TaskFinishedListener;

*/
/**
 * Created by itzik on 2/26/14.
 *//*

public class ParseXmlAsyncTask extends AsyncTask<XMLParser, Integer, List<String>> {

    private final static String  TAG = ParseXmlAsyncTask.class.getSimpleName();

    private TaskFinishedListener taskFinishedListener;
    private   List<String> tags = new ArrayList<String>();


    @Override
    protected List doInBackground(XMLParser... params) {
        XMLParser xmlParser = params[0];

        List<String> data = null;


        if (xmlParser != null)
        {
            data = xmlParser.getDataForTags(tags);

            if (data != null)
                for (int i = 0 ; i < data.size(); i++)
                    Log.i(TAG, data.get(i));
        }
        else
            Log.d(TAG, "Parser is empty");

        return data;
    }

    @Override
    protected void onPostExecute(List<String> o) {
        super.onPostExecute(o);

        if (taskFinishedListener != null)
            taskFinishedListener.onFinished();
        else
            Log.v(TAG, "no task finished listener");
    }

    public void setTaskFinishedListener(TaskFinishedListener taskFinishedListener) {
        this.taskFinishedListener = taskFinishedListener;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
*/
