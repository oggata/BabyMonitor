package com.braunster.mymodule.app.stream.video;/*
package TCP;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalSocket;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

*/
/**
 * Created by itzik on 4/6/2014.
 *//*

public class VideoStreamer {

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private LocalSocket mSender;
    private int mVideoEncoder;
    private SurfaceHolder mSurfaceHolder;
    private VideoQuality mQuality;

    */
/**
     * Encoding of the audio/video is done by a MediaRecorder.
     *//*

    protected void encodeWithMediaRecorder() throws IOException {

        // We need a local socket to forward data output by the camera to the packetizer
        createSockets();

        // Opens the camera if needed
        createCamera();

        // Stops the preview if needed
        if (mPreviewStarted) {
            lockCamera();
            try {
                mCamera.stopPreview();
            } catch (Exception e) {}
            mPreviewStarted = false;
        }

        // Unlock the camera if needed
        unlockCamera();

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setVideoEncoder(mVideoEncoder);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setVideoSize(mQuality.resX,mQuality.resY);
        mMediaRecorder.setVideoFrameRate(mQuality.framerate);
        mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);

        // We write the ouput of the camera in a local socket instead of a file !                       
        // This one little trick makes streaming feasible quiet simply: data from the camera
        // can then be manipulated at the other end of the socket
        mMediaRecorder.setOutputFile(mSender.getFileDescriptor());

        mMediaRecorder.prepare();
        mMediaRecorder.start();

        try {
            // mReceiver.getInputStream contains the data from the camera
            // the mPacketizer encapsulates this stream in an RTP stream and send it over the network
            mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
            mPacketizer.setInputStream(mReceiver.getInputStream());
            mPacketizer.start();
            mStreaming = true;
        } catch (IOException e) {
            stop();
            throw new IOException("Something happened with the local sockets :/ Start failed !");
        }

    }

    protected synchronized void createCamera() throws RuntimeException, IOException {
        if (mSurfaceHolder == null || mSurfaceHolder.getSurface() == null || !mSurfaceReady)
            throw new IllegalStateException("Invalid surface holder !");

        if (mCamera == null) {
            mCamera = Camera.open(mCameraId);
            mUnlocked = false;
            mCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                    // On some phones when trying to use the camera facing front the media server will die
                    // Whether or not this callback may be called really depends on the phone
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        // In this case the application must release the camera and instantiate a new one
                        Log.e(TAG,"Media server died !");
                        // We don't know in what thread we are so stop needs to be synchronized
                        mCameraOpenedManually = false;
                        stop();
                    } else {
                        Log.e(TAG, "Error unknown with the camera: " + error);
                    }
                }
            });

            Camera.Parameters parameters = mCamera.getParameters();

            if (mMode == MODE_MEDIACODEC_API) {
                getClosestSupportedQuality(parameters);
                parameters.setPreviewFormat(ImageFormat.YV12);
                parameters.setPreviewSize(mQuality.resX, mQuality.resY);
                parameters.setPreviewFrameRate(mQuality.framerate);
            }

            if (mFlashState) {
                if (parameters.getFlashMode()==null) {
                    // The phone has no flash or the choosen camera can not toggle the flash
                    throw new IllegalStateException("Can't turn the flash on !");
                } else {
                    parameters.setFlashMode(mFlashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
                }
            }

            try {
                mCamera.setParameters(parameters);
                mCamera.setDisplayOrientation(mQuality.orientation);
                mCamera.setPreviewDisplay(mSurfaceHolder);
            } catch (RuntimeException e) {
                destroyCamera();
                throw e;
            } catch (IOException e) {
                destroyCamera();
                throw e;
            }
        }
    }

    protected synchronized void destroyCamera() {
        if (mCamera != null) {
            if (mStreaming) super.stop();
            lockCamera();
            mCamera.stopPreview();
            try {
                mCamera.release();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
            }
            mCamera = null;
            mUnlocked = false;
            mPreviewStarted = false;
        }
    }

}
*/
