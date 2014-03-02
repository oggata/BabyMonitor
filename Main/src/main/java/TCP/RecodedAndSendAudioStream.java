package TCP;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by itzik on 2/18/14.
 */
public class RecodedAndSendAudioStream extends Thread {

    private final String TAG = RecodedAndSendAudioStream.class.getSimpleName();

    private static final int FREQUENCY = 44100;
    private static final int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private OutputStream output;

    private OnRecordFailed onRecordFailed;

    // Record Data
    private  AudioRecord audioRecord;
    private int bufferSize;
    private long lastBytesReceivedTime = -1;
    private byte[] buffer;


    private boolean isRecording = false, close = false;

    public RecodedAndSendAudioStream(OutputStream outputStream){

        Log.d(TAG, "RecodedAndSendAudioStream Created.");
        output = outputStream;

        prepareRecording();

    }

    @Override
    public void run() {
        super.run();

        while(!Thread.currentThread().isInterrupted())
        {
            if (isRecording) {

//                Log.d(TAG, "Recording");
                audioRecord.read(buffer, 0, bufferSize);

                try {
//                    Log.d(TAG, "Write");
                    IOUtils.write(buffer, output);
                    output.flush();
                    lastBytesReceivedTime = System.currentTimeMillis();
                } catch (IOException e) {
                    if (onRecordFailed != null)
                        onRecordFailed.onFailed();
                    else
                        Log.e(TAG, "No record fail listener");

                    e.printStackTrace();
                }

/*Check for timeout in recording if other side stop            Log.d(TAG, "IsRecording!");
                if ( lastBytesReceivedTime > -1 && System.currentTimeMillis() - lastBytesReceivedTime > 2000)
                {
                    Log.d(TAG, "Record Timeout!");

                    isRecording = false;

                    // Listener for play/pause button
                    Intent playStopIntent =new Intent(TCPConnection.ACTION_TOGGLE_CONTROLLER);
                    // Extra which controller to use. Server use sound player client us recorder
                    playStopIntent.putExtra(TCPConnection.CONTROLLER, TCPConnection.CONTROLLER_SOUND_RECORDER );
                }*/
            }

            if (close){
                if (audioRecord != null && isRecording)
                {
                    isRecording= false;
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }
            }
        }
    }

    public boolean isRecording(){
        return isRecording;
    }

    private void prepareRecording(){

        try {
            // Create a new AudioRecord object to record the audio.
            bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL_CONFIGURATION, AUDIO_ENCODING);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY, CHANNEL_CONFIGURATION, AUDIO_ENCODING, bufferSize);

            buffer = new byte[bufferSize];

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void startRecord(){

        if (audioRecord == null)
            prepareRecording();

        if (!isRecording)
        {
            audioRecord.startRecording();
        }

        isRecording = true;
    }

    public  void close(){
        close = true;
    }

    public interface OnRecordFailed{
        public void onFailed();
    }

    // Getters And Setters
    public void setOnRecordFailed(OnRecordFailed onRecordFailed) {
        this.onRecordFailed = onRecordFailed;
    }
}
