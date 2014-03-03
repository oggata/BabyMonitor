package braunster.babymonitor;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by itzik on 2/26/14.
 */
public class GetObjectFromURLAsyncTask extends AsyncTask<Void, String, Object > {

    private String returnType;
    private Context context;
    private ProgressDialog progressDialog;

    public GetObjectFromURLAsyncTask(Context context, String returnType){
       this.returnType = returnType;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

    }

    @Override
    protected Object doInBackground(Void... params) {

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

        if (returnType.equals(HTTPClientConnection.TYPE_JSON))
            return HTTPClientConnection.connect("http://freegeoip.net/json", HTTPClientConnection.TYPE_JSON);
        else if (returnType.equals(HTTPClientConnection.TYPE_XML))
            return HTTPClientConnection.connect("http://freegeoip.net/xml", HTTPClientConnection.TYPE_XML);
        else return null;

    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        progressDialog.dismiss();

        if (returnType.equals(HTTPClientConnection.TYPE_JSON))
        {

        }
        else if (returnType.equals(HTTPClientConnection.TYPE_XML))
        {

        }


    }
}
