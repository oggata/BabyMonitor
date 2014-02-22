package TCP;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
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
    int bufferSize;
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
                audioRecord.read(buffer, 0, bufferSize);

                try {
                    IOUtils.write(buffer, output);
                } catch (IOException e) {
                    if (onRecordFailed != null)
                        onRecordFailed.onFailed();
                    else
                        Log.e(TAG, "No record fail listener");

                    e.printStackTrace();
                }
            }

            if (close){
                if (audioRecord != null && isRecording)
                {
                    isRecording= false;
                    audioRecord.stop();
                    audioRecord.release();
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
        if (!isRecording)
        {
            audioRecord.startRecording();
            isRecording = !isRecording;
        }
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
