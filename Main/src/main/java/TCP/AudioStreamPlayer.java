package TCP;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by itzik on 1/31/14.
 */
public class AudioStreamPlayer implements Runnable{

    private final String TAG = AudioStreamPlayer.class.getSimpleName();

    public static final int SAMPLE_44100 = 44100;
    public static final int SAMPLE_10000 = 10000;

    private static final int MIN_BUFFER_FOR_PLAY = 1000 * 50;

    private byte[] buffer;
    private AudioTrack track;
    private long byteCount = 0;
    private boolean isPlaying = false;

    private final int sampleRate;
    private int trackSeconds = -1, trackLastSeconds = -1, trackFrames = -1, trackLastFrames = -1;

    public AudioStreamPlayer(byte[] bytes, int sampleRate){
        buffer = bytes;
        this.sampleRate = sampleRate;

        Log.i(TAG, "Sample Rate: " + sampleRate);
    }

    @Override
    public void run() {
        startBufferAudio();
    }

    public void startBufferAudio() {

        track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                MIN_BUFFER_FOR_PLAY / 2, AudioTrack.MODE_STREAM);

        track.setStereoVolume(1,1);

        // Write the first bytes to the player.
        write(buffer);

        try {
            Thread.currentThread().sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true)
            if (isPlaying)
            {
                trackLastSeconds = trackSeconds;
                trackSeconds = track.getPlaybackHeadPosition() / track.getSampleRate();

                if ( trackSeconds > trackLastSeconds)
                    Log.d(TAG, "Time in Seconds: " + trackSeconds);

//                        trackLastFrames = trackFrames;
//                        trackFrames = track.getPlaybackHeadPosition();
//
//                        if (trackFrames > trackLastFrames)
//                            Log.d(TAG, "Frames: " + track.getPlaybackHeadPosition());

            }
    }

    private void play(){
        if (!isPlaying)
        {
            Log.i(TAG, "Play!");

            track.play();

            isPlaying = !isPlaying;
        }
    }

    public void stop(){
        if (track != null && isPlaying)
            track.stop();
    }

    public void pause(){
        if (track != null && isPlaying)
            track.pause();
    }

    public void write(byte[] bytes){
        byteCount += bytes.length;

        Log.i(TAG, "Write, Bytes Count: " + byteCount);

        try {
            track.write(bytes, 0, bytes.length);
        } catch (NullPointerException e) {
            if (track == null)
                Log.e(TAG, "TRACK");

            if (bytes == null)
                Log.e(TAG, "BYTES");

            e.printStackTrace();
        }

        if ( byteCount > MIN_BUFFER_FOR_PLAY)
            play();
    }

    public void setMarker(long marker){
//        Log.d(TAG, "Marker as int: " + (int) marker);
//
////                int status = track.setNotificationMarkerPosition(  ((int) marker - 3000) /2 );
//
//        track.setNotificationMarkerPosition(  1000000 * 2 );
//
////                Log.d(TAG, "Status: " + status);
//
//        track.setPositionNotificationPeriod(track.getPlaybackHeadPosition());
//        track.setPlaybackPositionUpdateListener( new AudioTrack.OnPlaybackPositionUpdateListener() {
//            @Override
//            public void onMarkerReached(AudioTrack track) {
//                Log.d(TAG, "Marker Reaced");
//            }
//
//            @Override
//            public void onPeriodicNotification(AudioTrack track) {
//                Log.d(TAG, "Marker Reaced2");
//            }
//        });
    }

    public synchronized boolean isPlaying() {
        return isPlaying;
    }
}
