package TCP;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RadioGroup;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by itzik on 11/8/13.
 */
public class TCPCommThread extends Thread {

    private final String TAG = TCPCommThread.class.getSimpleName();

    private Socket tcpSocket; // The TCP Socket
    private Context context;
    private Handler handler;

    private BufferedReader input;
    private BufferedWriter output;
    private OutputStream tmpOut ;
    private InputStream tmpIn;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    // Play and Record Threads.
    private LoadAndPlayAudioStream loadAndPlayStream;
    private RecodedAndSendAudioStream recodedAndSendAudioStream;

    private AudioStreamForcedStoppedListener audioStreamForcedStoppedListener;

    private boolean isStreamOn = false,
                    playLiveAudio = false, readCommand = false,
                    recordLiveAudio = false,
                    close = false;

    private List<String> messages = new ArrayList<String>();

    private String line, command;

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
                        recordController.stop();

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

            if (playLiveAudio)
            {
                if (isStreamsInitialized())
                {
                    if (loadAndPlayStream != null && !loadAndPlayStream.isPlaying())
                    {
                        Log.i(TAG, "Playing Live Audio");

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
                    }

                    playLiveAudio = false;
                }
                else
                    isStreamOn(true);
            }

            if (recordLiveAudio){
                if (isStreamsInitialized())
                {
                    if (recodedAndSendAudioStream != null && !recodedAndSendAudioStream.isRecording())
                    {
                        Log.i(TAG, "Recording Live Audio");

                        recodedAndSendAudioStream.setOnRecordFailed(new RecodedAndSendAudioStream.OnRecordFailed() {
                            @Override
                            public void onFailed() {
                                reportToHandler(TCPConnection.ERROR_RECORD_STREAM_STOPPED);
                            }
                        });
                        recodedAndSendAudioStream.start();
                        recodedAndSendAudioStream.startRecord();
                    }

                    recordLiveAudio = false;
                }
                else
                    isStreamOn(true);
            }

            //There messages to send
            if (messages.size() > 0){
                if (isStreamsInitialized())
                {
                    Log.i(TAG, "Sending Messages");
                    List<String> messagesToSend = messages;

                    messages = new ArrayList<String>();

                    for (int i = 0; i < messagesToSend.size() ; i++)
                    {
                        try {
                            mmOutStream.write(messagesToSend.get(i).getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                            reportToHandler(TCPConnection.ERROR_MSG_NOT_SENT);

                            interrupt();
                        }
                    }
                }
                else
                    isStreamOn(true);
            }
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
    private void playLiveAudio(){

        Log.d(TAG, "playLiveAudio");

        if (loadAndPlayStream == null)
            loadAndPlayStream = new LoadAndPlayAudioStream(mmInStream);

        if(!loadAndPlayStream.isPlaying())
        {
            playLiveAudio = true;
        }
        else
            Log.d(TAG, "Already Playing");

        // TODO Method is thread safe can be moved outside of thread

    }

    /** Stop Playing live audio*/
    private void stopLiveAudio(){
        if (loadAndPlayStream != null)
            loadAndPlayStream.stopAudio();

        loadAndPlayStream = null;
    }

    /** Start Recording live audio*/
    private void recordLiveAudio(){
        Log.d(TAG, "Record live audio");

        if (recodedAndSendAudioStream == null)
            recodedAndSendAudioStream = new RecodedAndSendAudioStream(mmOutStream);

        if (!recodedAndSendAudioStream.isRecording())
        {
            recordLiveAudio = true;
        }
        else
            Log.d(TAG, "Already Recording");
    }

    private void stopRecord(){
        Log.d(TAG, "Stop Record");
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

    /* Getters and Setters*/
    public AudioController getAudioController() {
        return audioController;
    }

    public RecordController getRecordController() {
        return recordController;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setAudioStreamForcedStoppedListener(AudioStreamForcedStoppedListener audioStreamForcedStoppedListener) {
        this.audioStreamForcedStoppedListener = audioStreamForcedStoppedListener;
    }

    /* Interfaces*/
    public interface AudioStreamForcedStoppedListener{
        public void stopped();
    }

    public interface AudioController{
        public void play();
        public void stop();
        /** Toggle the audio stream ON to OFF or OFF to ON
         * @return  <b>false-</b> if stream had been stopped <b>true-</b> if stream had been started*/
        public boolean toggle();
        public boolean isPlaying();
    }

    public interface RecordController{
        public void record();
        public void stop();
        /** Toggle the record stream ON to OFF or OFF to ON
         * @return  <b>false-</b> if stream had been stopped <b>true-</b> if stream had been started*/
        public boolean toggle();
        public boolean isRecording();
    }

   // An implantation of the RecordController Interface for a better controlling in the stream.
    private RecordController recordController = new RecordController() {
        @Override
        public void record() {
            recordLiveAudio();
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
        public void play() {
            Log.d(TAG, "Play in Controller");
            playLiveAudio();
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
        handler.sendMessage(msg);
    }
}
