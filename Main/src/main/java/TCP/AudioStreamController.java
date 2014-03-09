package TCP;

import android.media.AudioFormat;
import android.media.AudioRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itzik on 3/6/14.
 */
public class AudioStreamController {

    private final static String TAG = AudioStreamController.class.getSimpleName();

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
}
