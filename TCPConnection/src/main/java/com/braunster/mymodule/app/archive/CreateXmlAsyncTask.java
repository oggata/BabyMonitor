package com.braunster.mymodule.app.archive;/*
package TCP.xml;

import android.os.AsyncTask;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.StringWriter;
import java.util.List;

import TCP.interfaces.TaskFinishedListener;
import TCP.xml.objects.XmlMessage;

*/
/**
 * Created by itzik on 2/26/14.
 *//*

public class CreateXmlAsyncTask extends AsyncTask<List<XmlMessage>, Integer, String> {

    private final static String  TAG = CreateXmlAsyncTask.class.getSimpleName();
    private TaskFinishedListener taskFinishedListener;

    public CreateXmlAsyncTask(TaskFinishedListener taskFinishedListener){
        this.taskFinishedListener = taskFinishedListener;
    }

    public CreateXmlAsyncTask(){

    }

    @Override
    protected String doInBackground(List<XmlMessage>... params) {
        return writeXml(params[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        taskFinishedListener.onFinished();
    }

    private String writeXml(List<XmlMessage> messages){
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "messages");
            serializer.attribute("", "number", String.valueOf(messages.size()));

            int counter = 0;
            for (XmlMessage msg: messages){

                serializer.startTag("", "message");
                serializer.attribute("", "number", String.valueOf(counter));
                serializer.startTag("", "battery");
                serializer.text(msg.getBatteryPercentage());
                serializer.endTag("", "battery");
                serializer.endTag("", "message");

                counter++;
            }

            serializer.endTag("", "messages");
            serializer.endDocument();

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setTaskFinishedListener(TaskFinishedListener taskFinishedListener) {
        this.taskFinishedListener = taskFinishedListener;
    }
}
*/
