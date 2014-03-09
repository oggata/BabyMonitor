package TCP;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by itzik on 11/8/13.
 */
public class TCPCommThread extends Thread {

    // TODO Add another TCP connection for data transfer like battery power and preforming check.

    private static final String TAG = TCPCommThread.class.getSimpleName();

    private Socket tcpSocket; // The TCP Socket
    private Context context;
    private Handler handler;

    private BufferedReader input;
    private BufferedWriter output;
    private OutputStream tmpOut ;
    private InputStream tmpIn;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    private XMLParser xmlParser;

    // Play and Record Threads.
    private LoadAndPlayAudioStream loadAndPlayStream;
    private RecodedAndSendAudioStream recodedAndSendAudioStream;

    private AudioStreamForcedStoppedListener audioStreamForcedStoppedListener;

    // Flags for the threads.
    private boolean isStreamOn = false,
                    playLiveAudio = false, readCommand = false,
                    recordLiveAudio = false,
                    close = false, checkConnection = false,
                    readXml = false;

    // List of messages that are pending to be sent
    private List<String> messages = new ArrayList<String>();

    // For the message reading
    private String line, command;

    // keep the data of the last check or communication with the other side of the connection.
    private long lastCommTime;

    public TCPCommThread(Socket socket) {

        Log.i(TAG, "TCP Communication server is created.");

        tcpSocket = socket;

        isStreamOn(true);
    }

    @Override
    public void run() {
        super.run();

        while (!isInterrupted())
        {
            if (isStreamOn)
            {
                if (!isStreamsInitialized())
                {
                    Log.d(TAG, "Opening Stream");
                    // Getting the input string.
                    try {

                        tmpIn = tcpSocket.getInputStream();
                        tmpOut = tcpSocket.getOutputStream();

                    } catch (IOException e) {
                        e.printStackTrace();
                        reportToHandler(TCPConnection.ERROR_CANT_OPEN_IN_OUT_STREAM);
                        interrupt();
                    }

                    mmInStream = tmpIn;
                    mmOutStream = tmpOut;
                    input = new BufferedReader(new InputStreamReader(mmInStream));
                }
            }
            else close();

            if (close){
                Log.i(TAG, "Closing The Comm Thread.");

                try {
                    if (isStreamsInitialized())
                    {
                        audioController.stop();

                        closeRecord();

                        mmOutStream.close();
                        mmInStream.close();
                    }

                    tcpSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mmOutStream = null;
                mmInStream = null;

                interrupt();

                isStreamOn(false);

                close = false;
            }

            if (readCommand)
            {
                readCommand();
                readCommand = false;
            }

            if (!close && isStreamsInitialized() && readXml){
                readXml();
            }

            //There messages to send
            if (messages.size() > 0){
                if (isStreamsInitialized())
                {
                    Log.i(TAG, "Sending Messages, Amount: " + messages.size());

                    List<String> messagesToSend = messages;

                    messages = new ArrayList<String>();

                    for (int i = 0; i < messagesToSend.size() ; i++)
                    {
                        try {
                            mmOutStream.write(messagesToSend.get(i).getBytes());

                            // Set the last time the devices communicate successfully.
                            lastCommTime = System.currentTimeMillis();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            reportToHandler(TCPConnection.ERROR_MSG_NOT_SENT);

                            interrupt();
                        }
                    }
                }
                else
                    isStreamOn(true);
            }

            // If stream is open and not playing and not recording preform a check of the connection
            if ( isStreamsInitialized() && checkConnection
                    && (
                    (recodedAndSendAudioStream == null || !recodedAndSendAudioStream.isRecording())
                            &&
                            (loadAndPlayStream == null || !loadAndPlayStream.isPlaying())
                        )
                    && ( System.currentTimeMillis() - lastCommTime > TCPConnection.TIME_BETWEEN_CHECKS ))
            {
                write(" ");

                if (recodedAndSendAudioStream != null && recodedAndSendAudioStream.isRecording())
                    Log.d(TAG, "check while recording ");
                // Set the last check time
                lastCommTime = System.currentTimeMillis();
            }


        /*End of while.*/
        }
    }

    /** Write text to the socket*/
    public void write(String msg){
        messages.add(msg);
    }

    /** Close the streams and the socket*/
    public synchronized void  close(){
        close = true;
    }

    /** Open the Input and Output Streams of the socket.*/
    public synchronized void isStreamOn(boolean state){
        isStreamOn = state;
    }

    /** Check if the streams is initialized*/
    private synchronized boolean isStreamsInitialized(){
        if (mmInStream != null && mmOutStream != null)
            return true;
        else
            return false;
    }

    /** Start Playing live audio*/
    private boolean playLiveAudio(){

        Log.v(TAG, "Playing Live Audio");

        if (isStreamsInitialized() && !readXml)
        {
            if (loadAndPlayStream == null)
                loadAndPlayStream = new LoadAndPlayAudioStream(mmInStream, AudioStreamController.sampleRate);

            if(!loadAndPlayStream.isPlaying())
            {
                // Letting the main view know the stram has been stopped
                loadAndPlayStream.setAudioStreamForcedStoppedListener(new AudioStreamForcedStoppedListener() {
                    @Override
                    public void stopped() {
                        reportToHandler(TCPConnection.ERROR_AUDIO_STREAM_FAIL);
                    }
                });

                if (!loadAndPlayStream.isAlive())
                    loadAndPlayStream.start();

                loadAndPlayStream.startAudio();

                return true;            }
            else
                Log.v(TAG, "Already Playing");
        }
        else Log.v(TAG, "Stream not initialized or on xml mode");

        return false;
    }

    /** Stop Playing live audio*/
    private void stopLiveAudio(){
        if (loadAndPlayStream != null)
            loadAndPlayStream.stopAudio();

        loadAndPlayStream = null;
    }

    /** Start Recording live audio*/
    private boolean recordLiveAudio(){
        Log.v(TAG, "Record live audio");

        if (isStreamsInitialized() && !readXml)
        {
            if (recodedAndSendAudioStream == null)
                recodedAndSendAudioStream = new RecodedAndSendAudioStream(mmOutStream, AudioStreamController.sampleRate);

            if (!recodedAndSendAudioStream.isRecording())
            {
                Log.i(TAG, "Recording Live Audio");

                recodedAndSendAudioStream.setOnRecordFailed(new RecodedAndSendAudioStream.OnRecordFailed() {
                    @Override
                    public void onFailed() {
                        reportToHandler(TCPConnection.ERROR_RECORD_STREAM_STOPPED);
                        closeRecord();
                    }
                });

                try {
                    if (!recodedAndSendAudioStream.isAlive())
                        recodedAndSendAudioStream.start();
                } catch (IllegalThreadStateException e) {
                    e.printStackTrace();
                }

                recodedAndSendAudioStream.startRecord();

                return true;
            }
        }
        else Log.v(TAG, "Stream not initialized or on xml mode");

        Log.v(TAG, "Already Recording");

        return false;
    }

    private void stopRecord(){
        Log.d(TAG, "Stop Record");
        if (recodedAndSendAudioStream != null)
            recodedAndSendAudioStream.stopRecord();
    }

    private void closeRecord(){
        if (recodedAndSendAudioStream != null)
            recodedAndSendAudioStream.close();

        recodedAndSendAudioStream = null;
    }

    private void readCommand(){
        try {
            while (input.ready())
            {
                line = input.readLine();

                // printing received data.
//                    Log.d(TAG, " TCP incoming data: " + line);

                if (line.length() > 1)
                {
                    int counter = 0;
                    String temp;

                    // If line doesn't have start command on it it will find it.
                    if (line.charAt(0) != Command.START)
                    {
                        while (counter < line.length() && line.charAt(counter) != Command.START)
                        {
                            counter++;
                        }

                        temp = line.substring(counter);
                        line = temp
                        ;

                        if (line.length() < 3)
                        {
                            return;
                        }

                    }

                    // Command end
                    if ( line.length() >= 3 && line.charAt( line.length() -1 ) == Command.END )
                    {
                        command = line.substring(1, line.length() -1 );

                        Log.d("itzik", " Save Command: " + command);

                        //TODO Handle incoming messages by the type of the message. like TEMP for temperature message.
                        break;
                    }

                    // TODO handle half lines, handle more text exceptions
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readXml(){
        try {
            if (mmInStream  != null && mmInStream.available() > 0)
            {
                Log.i(TAG, "readXml");

                xmlParser = new XMLParser(mmInStream);
                reportToHandler(TCPConnection.XML_DATA_IS_RECEIVED);

            }

            sleep(100);

        } catch (IOException e) {
            e.printStackTrace();
            reportToHandler(TCPConnection.ERROR_MSG_NOT_SENT);
        } catch (InterruptedException e) {
//            e.printStackTrace();
            Log.e(TAG, "Sleep exception");
        } catch (Exception e) {
//            e.printStackTrace();
            Log.e(TAG, "Create XML exception");
        }
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

    /* Getters and Setters*/
    public AudioController getAudioController() {
        return audioController;
    }

    public RecordController getRecordController() {
        return recordController;
    }

    public void setCheckConnection(boolean checkConnection) {
        this.checkConnection = checkConnection;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setReadXml(boolean readXml) {
        this.readXml = readXml;
    }

    public void setAudioStreamForcedStoppedListener(AudioStreamForcedStoppedListener audioStreamForcedStoppedListener) {
        this.audioStreamForcedStoppedListener = audioStreamForcedStoppedListener;
    }

    /* Interfaces*/
    public interface AudioStreamForcedStoppedListener{
        public void stopped();
    }

    public interface AudioController{
        public boolean  play();
        public void stop();
        /** Toggle the audio stream ON to OFF or OFF to ON
         * @return  <b>false-</b> if stream had been stopped <b>true-</b> if stream had been started*/
        public boolean toggle();
        public boolean isPlaying();
    }

    public interface RecordController{
        public boolean record();
        public void stop();
        /** Toggle the record stream ON to OFF or OFF to ON
         * @return  <b>false-</b> if stream had been stopped <b>true-</b> if stream had been started*/
        public boolean toggle();
        public boolean isRecording();
    }

   // An implantation of the RecordController Interface for a better controlling in the stream.
    private RecordController recordController = new RecordController() {
        @Override
        public boolean record() {
            return recordLiveAudio();
        }

        @Override
        public void stop() {
            stopRecord();
        }

       @Override
       /** Toggle the record stream ON to OFF or OFF to ON
        * return false if stream had been stopped
        *  return true if stream had been started*/
       public boolean toggle() {
           if (isRecording())
           {
               stop();
               return false;
           }

           record();
           return true;
       }

       @Override
        public boolean isRecording() {
            return recodedAndSendAudioStream != null && recodedAndSendAudioStream.isRecording();
        }
    };

    // An implantation of the AudioController Interface for a better controlling in the stream.
    private AudioController audioController = new AudioController() {
        @Override
        public boolean play() {
            return playLiveAudio();
        }

        @Override
        public void stop() {
            stopLiveAudio();
        }

        @Override

        public boolean toggle() {
            if(isPlaying())
            {
                stop();
                return false;
            }

            play();
            return true;
        }

        @Override
        public boolean isPlaying() {
            return loadAndPlayStream != null && loadAndPlayStream.isPlaying();
        }
    };

    // Send message to the handler
    private void reportToHandler(int code){
        Message msg = new Message();
        msg.what = code;

        if (code == TCPConnection.XML_DATA_IS_RECEIVED)
            msg.obj = xmlParser;

        handler.sendMessage(msg);
    }

}
