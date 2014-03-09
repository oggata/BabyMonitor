package TCP;

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

    private OutputStream output;

    private OnRecordFailed onRecordFailed;

    // Record Data
    private  AudioRecord audioRecord;
    private int bufferSize;
    private long lastBytesReceivedTime = -1;
    private byte[] buffer;


    private boolean isRecording = false, close = false, prepare = false, startRecording = false, stop = false;
    private int sampleRate = AudioStreamController.FREQUENCY_8000; // Default

    public RecodedAndSendAudioStream(OutputStream outputStream){

        Log.d(TAG, "RecodedAndSendAudioStream Created.");
        output = outputStream;

        prepareRecording();

    }

    public RecodedAndSendAudioStream(OutputStream outputStream, int sampleRate){

        Log.d(TAG, "RecodedAndSendAudioStream Created. Sample Rate: " + sampleRate);
        output = outputStream;

        this.sampleRate = sampleRate;

        prepareRecording();

    }

    @Override
    public void run() {
        super.run();

        while(!Thread.currentThread().isInterrupted())
        {
            if (prepare){
                try {

                    // TODO fix auto pick sample rate, pass data to other connection of pereferd rate.

                    /*for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
                        int bufferSize = AudioRecord.getMinBufferSize(rate, CHANNEL_CONFIGURATION, AUDIO_ENCODING);
                        if (bufferSize > 0) {
                            Log.d(TAG, "Rate of recording is: " + rate);
                            // buffer size is valid, Sample rate supported
                            // Create a new AudioRecord object to record the audio.
                            bufferSize = AudioRecord.getMinBufferSize(44100, CHANNEL_CONFIGURATION, AUDIO_ENCODING);
                            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, CHANNEL_CONFIGURATION, AUDIO_ENCODING, bufferSize);

                            buffer = new byte[bufferSize];

                            prepare = false;

                            break;
                        }
                    }*/

                    /*if (prepare)
                    {
                        if (onRecordFailed != null)
                            onRecordFailed.onFailed();
                        else
                            Log.e(TAG, "No recored failed listener");

                        Log.e(TAG, "Record failed because of no buffer size found");//TODO notify user
                    }*/

                    bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioStreamController.CHANNEL_CONFIGURATION, AudioStreamController.AUDIO_ENCODING);
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioStreamController.CHANNEL_CONFIGURATION, AudioStreamController.AUDIO_ENCODING, bufferSize);

                    buffer = new byte[bufferSize];

                    prepare = false;



                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            if (startRecording)
            {
                if (audioRecord == null)
                {
                    prepareRecording();
                    return;
                }

                if (!isRecording)
                {
                    audioRecord.startRecording();
                }

                isRecording = true;

                startRecording = false;
            }

            if (isRecording) {

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
                    {
                        Log.e(TAG, "No record fail listener");
                        close();
                    }
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

            if (stop)
            {
                Log.i(TAG, "Stoping recored!");
                if (audioRecord != null)
                    audioRecord.stop();

                isRecording = false;

                stop = false;
            }

            if (close){
                if (audioRecord != null && isRecording)
                {
                    audioRecord.stop();
                    audioRecord.release();
                }

                isRecording= false;
                audioRecord = null;
                interrupt();
            }
        }
    }

    public boolean isRecording(){
        return isRecording;
    }

    private void prepareRecording(){
        if (!isRecording)
            prepare = true;
    }

    public void startRecord(){
        startRecording = true;
    }

    public  void close(){
        close = true;
    }

    public void stopRecord() {
        stop = true;
    }

    public interface OnRecordFailed{
        public void onFailed();
    }

    // Getters And Setters
    public void setOnRecordFailed(OnRecordFailed onRecordFailed) {
        this.onRecordFailed = onRecordFailed;
    }
}
