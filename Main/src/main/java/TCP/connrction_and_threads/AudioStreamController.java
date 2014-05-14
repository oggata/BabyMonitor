package TCP.connrction_and_threads;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import TCP.objects.InOutStreams;
import TCP.stream.audio.LoadAndPlayAudioStream;
import TCP.stream.audio.RecodedAndSendAudioStream;

/**
 * Created by itzik on 3/6/14.
 */
public class AudioStreamController extends BaseThread{

    private final static String TAG = AudioStreamController.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final int FREQUENCY_44100 = 44100;
    public static final int FREQUENCY_22050 = 22050;
    public static final int FREQUENCY_16000 = 16000;
    public static final int FREQUENCY_11025 = 11025;
    public static final int FREQUENCY_8000 = 8000; // Default

    public static final int[] sampleRates = {FREQUENCY_8000, FREQUENCY_11025, FREQUENCY_16000, FREQUENCY_22050, FREQUENCY_44100};

    public static final int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_STEREO;
    public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public static int sampleRate = FREQUENCY_8000;

    public static List<Integer> getSupportedSampleRates(){

        List<Integer> list = new ArrayList<Integer>();

        for (int rate : sampleRates) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, CHANNEL_CONFIGURATION, AUDIO_ENCODING);
            if (bufferSize > 0) {
                list.add(rate);
            }
        }

        return list;
    }

    // Play and Record Threads.
    private LoadAndPlayAudioStream loadAndPlayStream;
    private RecodedAndSendAudioStream recodedAndSendAudioStream;

    public AudioStreamController(InOutStreams inOutStreams, Handler handler){
        super(inOutStreams, handler);
    }

    @Override
    public void close() {
        super.close();
        interrupt();
        stopRecord();
        closeRecord();
        stopLiveAudio();
    }

    /** Start Playing live audio*/
    private boolean playLiveAudio(){

        if (DEBUG) Log.v(TAG, "Playing Live Audio");


        if (loadAndPlayStream == null)
            loadAndPlayStream = new LoadAndPlayAudioStream(inOutStreams.getInputStream(), AudioStreamController.sampleRate);

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
        if (DEBUG) Log.v(TAG, "Already Playing");


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
        if (DEBUG) Log.v(TAG, "Record live audio");

        if (recodedAndSendAudioStream == null)
            recodedAndSendAudioStream = new RecodedAndSendAudioStream(inOutStreams.getOutputStream(), AudioStreamController.sampleRate);

        if (!recodedAndSendAudioStream.isRecording())
        {
            if (DEBUG) Log.i(TAG, "Recording Live Audio");

            recodedAndSendAudioStream.setOnRecordFailed(new RecodedAndSendAudioStream.OnRecordFailed() {
                @Override
                public void onFailed() {
                    if (DEBUG) Log.d(TAG, "onFailed");
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

        if (DEBUG) Log.v(TAG, "Already Recording");

        return false;
    }

    private void stopRecord(){
        if (DEBUG) Log.d(TAG, "Stop Record");
        if (recodedAndSendAudioStream != null)
            recodedAndSendAudioStream.stopRecord();
    }

    private void closeRecord(){
        if (recodedAndSendAudioStream != null)
            recodedAndSendAudioStream.close();

        recodedAndSendAudioStream = null;
    }

    @Override
    public void run() {
        super.run(); // TODO implement some checks maybe.
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

    public AudioController getAudioController() {
        return audioController;
    }

    public RecordController getRecordController() {
        return recordController;
    }
}
