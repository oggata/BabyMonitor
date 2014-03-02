package TCP;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/** Stream audio from the server and play it using The AudioStreamPlayer Obj*/
class LoadAndPlayAudioStream extends Thread {

    // TODO check low sample rate, bit encoding size(8BIT, 16BIT) for bad internet
    // TODO analyz sound for static noise

    private final String TAG = LoadAndPlayAudioStream.class.getSimpleName();

    private final int MAX_BYTE_IN_BUFFER_BEFORE_SKIP = 1000 * 50;
    private final int MIN_BYTE_NUMBER_TO_KEEP_IN_BUFFER = 1000 * 30;
    private final int BYTE_TO_BUFFER = 15000;

    // The object that play the sound
    private AudioStreamPlayer player;

    // Flag state that the thread is playing sound
    private boolean isPlaying = false;

    // The input stream that get bytes
    private InputStream inStream;

    private int bufferCount = 0, sampleRate;
    private long fileSize, lastBytesReceivedTime = -1;
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
            if (isPlaying)
            {
                try {
                    if(inStream.available() > 0)
                    {
//                        Log.d(TAG, "Available Bytes: " + inStream.available());

                        if ( (player == null || !player.isPlaying()) && inStream.available() > MAX_BYTE_IN_BUFFER_BEFORE_SKIP)
                        {
                            Log.d(TAG, "Buffer was to full bytes skipped, Number skipped: "
                                    + (inStream.available() - MIN_BYTE_NUMBER_TO_KEEP_IN_BUFFER ));

                            inStream.skip(inStream.available() - MIN_BYTE_NUMBER_TO_KEEP_IN_BUFFER);
                        }

                        // Read the buffer
                        IOUtils.readFully(inStream, bytesFromStream);

                        // Set the time of the last communication.
                        lastBytesReceivedTime = System.currentTimeMillis();

                        // count the received bytes.
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

/*Check for timeout in playing if other side stop                Log.d(TAG, "IsPlaying!");
                if ( lastBytesReceivedTime != -1 && System.currentTimeMillis() - lastBytesReceivedTime > 2000)
                {
                    Log.d(TAG, "Playing Timeout!");
                    isPlaying = false;

                    // Listener for play/pause button
                    Intent playStopIntent =new Intent(TCPConnection.ACTION_TOGGLE_CONTROLLER);
                    // Extra which controller to use. Server use sound player client us recorder
                    playStopIntent.putExtra(TCPConnection.CONTROLLER, TCPConnection.CONTROLLER_SOUND_PLAYER );
                }*/

            }
            /* End Of While*/
        }
        /* End Of Run*/
    }

    public void startAudio(){
        isPlaying = true;
    }

    /** Closing the connection and stopping the stream player if playing */
    public void stopAudio(){

        Log.i(TAG, "Closing the audio stream");


        if (player != null)
            player.stop();

        player = null;

        isPlaying = false;

        interrupt();
    }

    public void setAudioStreamForcedStoppedListener(TCPCommThread.AudioStreamForcedStoppedListener audioStreamForcedStoppedListener) {
        this.audioStreamForcedStoppedListener = audioStreamForcedStoppedListener;
    }

    public synchronized boolean isPlaying(){
        return isPlaying;
    }

}