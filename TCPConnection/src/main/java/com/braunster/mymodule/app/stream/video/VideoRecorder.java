package com.braunster.mymodule.app.stream.video;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by itzik on 4/9/2014.
 */
public class VideoRecorder extends Thread{

    private MediaRecorder mediaRecorder;
    private ParcelFileDescriptor pfd;
    private SurfaceHolder surfaceHolder;

    public VideoRecorder(ParcelFileDescriptor pfd, SurfaceHolder holder){
        mediaRecorder = new MediaRecorder();

        this.pfd = pfd;
        this.surfaceHolder = holder;

//        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
    }

    @Override
    public void run() {
        super.run();

        try {

            Camera camera = Camera.open();
            camera.setDisplayOrientation(90);
            camera.unlock();

            mediaRecorder.setCamera(camera);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            // Following code does the same as getting a CamcorderProfile (but customizable)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

            // Output file - into the stream
            mediaRecorder.setOutputFile(pfd.getFileDescriptor());
//            mediaRecorder.setOutputFile(Environment.getDataDirectory() + "/data/myPackage//MVR_video.3gp");

            mediaRecorder.setVideoSize(320, 240);
            mediaRecorder.setMaxDuration(1000 * 60 * 90);

//            mediaRecorder.setVideoFrameRate(25);

//            mediaRecorder.setVideoEncodingBitRate(10000);


//            mediaRecorder.setAudioSamplingRate(44100);
//            mediaRecorder.setAudioEncodingBitRate(10000);

            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
