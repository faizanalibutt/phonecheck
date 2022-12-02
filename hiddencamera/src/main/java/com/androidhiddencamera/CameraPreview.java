/*
 * Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.androidhiddencamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.androidhiddencamera.config.CameraResolution;
import com.androidhiddencamera.config.CameraRotation;

import java.io.IOException;
import java.security.Policy;
import java.util.Collections;
import java.util.List;

/**
 * Created by Keval on 10-Nov-16.
 * This surface view works as the fake preview for the camera.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private CameraCallbacks mCameraCallbacks;

    private SurfaceHolder mHolder;
    public Camera mCamera;

    private CameraConfig mCameraConfig;
    Camera.Parameters parameters;

    private volatile boolean safeToTakePicture = false;

    public CameraPreview(@NonNull Context context, CameraCallbacks cameraCallbacks) {
        super(context);

        mCameraCallbacks = cameraCallbacks;

        //Set surface holder
        initSurfaceView();
    }
    /**
     * Initilize the surface view holder.
     */
    private void initSurfaceView() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        //Do nothing
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Do nothing
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
//        counter++;
        if (mCamera == null) {  //Camera is not initialized yet.
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            return;
        } else if (surfaceHolder.getSurface() == null) { //Surface preview is not initialized yet
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        // Make changes in preview size
        parameters = mCamera.getParameters();
        List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();

        //Sort descending
        Collections.sort(pictureSizes, new PictureSizeComparator());

        //set the camera image size based on config provided
        Camera.Size cameraSize;
        switch (mCameraConfig.getResolution()) {
            case CameraResolution.HIGH_RESOLUTION:
                cameraSize = pictureSizes.get(0);   //Highest res
                break;
            case CameraResolution.MEDIUM_RESOLUTION:
                cameraSize = pictureSizes.get(pictureSizes.size() / 2);     //Resolution at the middle
                break;
            case CameraResolution.LOW_RESOLUTION:
                cameraSize = pictureSizes.get(pictureSizes.size() - 1);       //Lowest res
                break;
            default:
                throw new RuntimeException("Invalid camera resolution.");
        }

        parameters.setPictureSize(cameraSize.width, cameraSize.height);
        if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        }
        if(Build.MODEL.contains(ExposureSupportedModels.choose())){
            parameters.setExposureCompensation(-8);
        }
        else if(Build.MANUFACTURER.toLowerCase().contains("motorola")){
            if(Build.MODEL.toLowerCase().contains("moto g (4)")){
                parameters.setExposureCompensation(parameters.getMinExposureCompensation());
            }
            else{
                parameters.setExposureCompensation(-4);
            }
        }
        else {
            parameters.setExposureCompensation(parameters.getMinExposureCompensation());
        }
        if(Build.MODEL.toLowerCase().contains("moto g (4)")){
            if(parameters.isAutoExposureLockSupported()){
                parameters.setAutoExposureLock(false);
            }
        }
        else{
            if(parameters.isAutoExposureLockSupported()){
                parameters.setAutoExposureLock(true);
            }
        }
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

        try{
            mCamera.setParameters(parameters);
        }
        catch (NullPointerException e){
            Log.d("CameraSurface", "Unable to set parameters");
        }

        requestLayout();

        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            safeToTakePicture = true;
        } catch (IOException | NullPointerException e) {
            //Cannot start preview
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
        if (mCamera != null) mCamera.stopPreview();
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param cameraConfig camera config builder.
     */
    void startCameraInternal(@NonNull CameraConfig cameraConfig) {
        mCameraConfig = cameraConfig;

        if (safeCameraOpen(mCameraConfig.getFacing())) {
            if (mCamera != null) {
                requestLayout();

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
                }
            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    public void setFlashMode(boolean code) {
        if(code){
            if(Build.MANUFACTURER.toLowerCase().contains("samsung")){
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                parameters.setExposureCompensation(0);

                try{
                    mCamera.setParameters(parameters);
                }
                catch (RuntimeException e){
                    Log.d("CameraSetFlash", "Unable to set parameters");
                }
            }
            else if (Build.MANUFACTURER.toLowerCase().contains("motorola")){
//                try {
//                    mCamera.stopPreview();
//                } catch (Exception e) {
//                    // Ignore: tried to stop a non-existent preview
//                }

                List supportedFlashmOdes = parameters.getSupportedFlashModes();
                if(supportedFlashmOdes != null && supportedFlashmOdes.contains("torch")){
                    parameters.setFlashMode("torch");
                }
                if(Build.MODEL.toLowerCase().contains("moto g (4)")){
                    parameters.setExposureCompensation(parameters.getMinExposureCompensation());
                }
                else{
                    parameters.setExposureCompensation(+2);
                }

//                try {
//                    mCamera.setDisplayOrientation(90);
//                    mCamera.startPreview();
//                    safeToTakePicture = true;
//                } catch (NullPointerException e) {
//                    //Cannot start preview
//                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
//                }

                try{
                    mCamera.setParameters(parameters);
                }
                catch (RuntimeException e){
                    Log.d("CameraSetFlash", "Unable to set parameters");
                }
            }
            else {
                try {
                    mCamera.stopPreview();
                } catch (Exception e) {
                    // Ignore: tried to stop a non-existent preview
                }
                List supportedFlashmOdes = parameters.getSupportedFlashModes();
                if(supportedFlashmOdes != null && supportedFlashmOdes.contains("torch")){
                    parameters.setFlashMode("torch");
                }
                try {
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();

                    safeToTakePicture = true;
                } catch (NullPointerException e) {
                    //Cannot start preview
                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
                }

                try{
                    mCamera.setParameters(parameters);
                }
                catch (RuntimeException e){
                    Log.d("CameraSetFlash", "Unable to set parameters");
                }
            }
        }

        takePictureInternal();
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            stopPreviewAndFreeCamera();

            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e("CameraPreview", "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    boolean isSafeToTakePictureInternal() {

        return safeToTakePicture;
    }

    void takePictureInternal() {
        safeToTakePicture = false;
        if (mCamera != null) {
            try{
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(final byte[] bytes, final Camera camera) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //Convert byte array to bitmap
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                //Rotate the bitmap
                                Bitmap rotatedBitmap;
                                if (mCameraConfig.getmImageRotation() != CameraRotation.ROTATION_0) {
                                    rotatedBitmap = HiddenCameraUtils.rotateBitmap(bitmap, mCameraConfig.getmImageRotation());

                                    //noinspection UnusedAssignment
                                    bitmap = null;
                                } else {
                                    rotatedBitmap = bitmap;
                                }

                                //Save image to the file.
                                if (HiddenCameraUtils.saveImageFromFile(rotatedBitmap,
                                        mCameraConfig.getImageFile(),
                                        mCameraConfig.getImageFormat())) {
                                    //Post image file to the main thread
                                    new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mCameraCallbacks.onImageCapture(mCameraConfig.getImageFile());
                                        }
                                    });
                                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                    try{
                                        mCamera.setParameters(parameters);
                                    }
                                    catch (RuntimeException e){
                                        Log.d("CameraPicTaken", "Unable to set parameters");
                                    }
                                } else {
                                    //Post error to the main thread
                                    new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mCameraCallbacks.onCameraError(CameraError.ERROR_IMAGE_WRITE_FAILED);
                                        }
                                    });
                                }
                                safeToTakePicture = true;
                                try{
                                    mCamera.startPreview();
                                }
                                catch (RuntimeException e){
                                    Log.d("CameraPreview", "Unable to start preview");
                                }
                            }
                        }).start();
                    }
                });
            }
            catch (RuntimeException e){

            }
        }
        else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            safeToTakePicture = true;
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    void stopPreviewAndFreeCamera() {
        safeToTakePicture = false;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
