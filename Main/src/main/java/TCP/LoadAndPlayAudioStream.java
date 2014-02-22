package TCP;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import TCP.AudioStreamPlayer;
import TCP.TCPCommThread;

/** Stream audio from the server and play it using The AudioStreamPlayer Obj*/
class LoadAndPlayAudioStream extends Thread {

    private final String TAG = LoadAndPlayAudioStream.class.getSimpleName();

    private final int MAX_BYTE_IN_BUFFER_BEFORE_SKIP = 1000 * 100;
    private final int MIN_BYTE_NUMBER_TO_KEEP_IN_BUFFER = 1000 * 50;
    private final int BYTE_TO_BUFFER = 15000;

    private AudioStreamPlayer player;
    private int bufferCount = 0, sampleRate;
    private long fileSize;

    private InputStream inStream;

    byte[] bytesFromStream = new byte[BYTE_TO_BUFFER];

    private TCPCommThread.AudioStreamForcedStoppedListener audioStreamForcedStoppedListener;

    public LoadAndPlayAudioStream(InputStream inputStream){
        this.inStream = inputStream;

        sampleRate = AudioStreamPlayer.SAMPLE_44100; //  Default
    }

    public LoadAndPlayAudioStream(InputStream inputStream, int sampleRate){
        this.inStream = inputStream;
        this.sampleRate = sampleRate;
    }

    @Override
    public void run() {
        while (!isInterrupted())
        {
            try {
                if(inStream.available() > 0)
                {
                    Log.d(TAG, "Available Bytes: " + inStream.available());

                    if (player == null && inStream.available() > MAX_BYTE_IN_BUFFER_BEFORE_SKIP)
                    {
                        Log.d(TAG, "Buffer was to full bytes skipped, Number skipped: "
                                + (inStream.available() - MIN_BYTE_NUMBER_TO_KEEP_IN_BUFFER ));

                        inStream.skip(inStream.available() - MIN_BYTE_NUMBER_TO_KEEP_IN_BUFFER);
                    }

//                            Log.d(TAG, "Available Bytes: " + mmInStream.available());

                    IOUtils.readFully(inStream, bytesFromStream);

                    bufferCount += bytesFromStream.length;

                    // Creating the player
                    if (player == null)
                    {
                        // Start the play sound thread.
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                player =new AudioStreamPlayer(bytesFromStream, sampleRate);

                                new Thread(player).start();
                            }
                        }).start();

                        try {
                            Log.d(TAG, "SHORT SLEEP FOR INIT");
                            Thread.currentThread().sleep(100);

                            player.setMarker(fileSize);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        player.write(bytesFromStream);

                }
            } catch (IOException e) {

                if (audioStreamForcedStoppedListener != null)
                    audioStreamForcedStoppedListener.stopped();
                else Log.e(TAG, " No audio stream forced stop listener");

                stopAudio();
                e.printStackTrace();
            }
        }

        stopAudio();
    }

    /** Closing the connection and stopping the stream player if playing */
    public void stopAudio(){

        Log.i(TAG, "Closing the audio stream");

        if (player != null)
            player.stop();

        interrupt();
    }

    public void setAudioStreamForcedStoppedListener(TCPCommThread.AudioStreamForcedStoppedListener audioStreamForcedStoppedListener) {
        this.audioStreamForcedStoppedListener = audioStreamForcedStoppedListener;
    }

    public synchronized boolean isPlaying(){
        return player != null && player.isPlaying();
    }

}