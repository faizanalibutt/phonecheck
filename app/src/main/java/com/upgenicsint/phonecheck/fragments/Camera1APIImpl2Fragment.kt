package com.upgenicsint.phonecheck.fragments

import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.ragnarok.rxcamera.config.CameraUtil
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.misc.CameraFace
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.test.chip.FrontCameraTest
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.fragment_2_camera.*

/**
 * Created by farhanahmed on 27/01/2017.
 */

class Camera1APIImpl2Fragment : CameraFragment<Camera>() {

    var displayOrientation = 90

    // View to display the mCamera output.
    private var mPreview: View? = null
    private var cameraSurfacePreview: CameraSurfacePreview? = null
    private var surfaceView: SurfaceView? = null
    /*private var handler: CaptureActivityHandler? = null
    private var decodeFormats: Collection<BarcodeFormat>? = null
    private var decodeHints: Map<DecodeHintType, *>? = null
    private var characterSet: String? = null
    private var viewfinderView: ViewfinderView? = null*/


    private val mPicture = Camera.PictureCallback { data, mCamera ->
        Thread(Runnable {
            if (BuildConfig.FLAVOR === "camera") {
                try {
                    onPictureTaken.invoke(saveImageOnFile(data))
                } catch (e: Exception) {
                    onCameraError.invoke(e)
                }

            } else {
                if (Build.MODEL.containsIgnoreCase("Nexus 6")) {
                    onPictureTaken.invoke(saveImageOnFile(data))
                } else {
                    onPictureTaken.invoke(data)
                }
            }
        }).start()
    }

    /*fun getViewfinderView(): ViewfinderView? {
        return viewfinderView
    }*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_2_camera, container, false)
        /*val textView : TextView = findViewById(R.id.android_text) as TextView
        viewfinderView = view?.findViewById(R.id.viewfinder_view)
                viewfinderView?.setCameraManager()*/
        return view
    }

    private var supportedFocusModes: MutableList<String>? = null
    override fun openCamera() {

        camera = getCameraInstance(currentFace)

        if (camera != null) {

            try {
                displayOrientation = CameraUtil.getPortraitCameraDisplayOrientation(context, currentFace.value,
                        currentFace === CameraFace.FACE_FRONT)

                val camera = camera

                if (camera != null) {

                    //Toast.makeText(context, "camera is not null", Toast.LENGTH_LONG).show()
                    camera.setDisplayOrientation(displayOrientation)

                    if (BuildConfig.FLAVOR === "camera") {
                        Tools.setupParameters(camera, context!!)
                    } else {
                        if (!Build.MODEL.containsIgnoreCase(Constants.S3)) {
                            val parameters = camera.parameters
                            // Set the mCamera to Auto Flash mode.
                            val supportedFlashModes = parameters.supportedFlashModes

                            if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                parameters.flashMode = Camera.Parameters.FLASH_MODE_ON
                                //Toast.makeText(context, "flash modes", Toast.LENGTH_LONG).show()
                            }

                            val supportedPictureFormats = camera.parameters.supportedPictureFormats
                            if (supportedPictureFormats != null && supportedPictureFormats.contains(ImageFormat.JPEG)) {
                                parameters.pictureFormat = ImageFormat.JPEG
                                //Toast.makeText(context, "picture format", Toast.LENGTH_LONG).show()
                            }

                            //auto da focus start

                            supportedFocusModes = parameters.supportedFocusModes

                            if (supportedFocusModes != null && supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                            }

//                            val previewSizes = parameters.supportedPreviewSizes
//                            val previewSize = Tools.getMaxSupportedSize(previewSizes)
//                            if (previewSize != null) {
//                                Log.i("setupParameters", "Preview w " + previewSize.width + " h " + previewSize.height)
//                                parameters.setPreviewSize(previewSize.width, previewSize.height)
//                                //Toast.makeText(context, "preview size", Toast.LENGTH_LONG).show()
//                                //parameters.setPictureSize(previewSize.width, previewSize.height)
//                            }

                            //auto da focus end


                            camera.parameters = parameters
                        } else {
                            val parameters = camera.parameters
                            // Set the mCamera to Auto Flash mode.
                            val supportedFlashModes = parameters.supportedFlashModes
                            if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                parameters.flashMode = Camera.Parameters.FLASH_MODE_ON
                                //Toast.makeText(context, "flash modes", Toast.LENGTH_LONG).show()
                            }
                            val supportedPictureFormats = camera.parameters.supportedPictureFormats
                            if (supportedPictureFormats != null && supportedPictureFormats.contains(ImageFormat.JPEG)) {
                                parameters.pictureFormat = ImageFormat.JPEG
                                //Toast.makeText(context, "picture format", Toast.LENGTH_LONG).show()
                            }
                            camera.parameters = parameters
                        }
                    }
                    if (cameraSurfacePreview == null) {
                        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        surfaceView = SurfaceView(activity)
                        container.addView(surfaceView, params)
                        cameraSurfacePreview = CameraSurfacePreview(context!!, surfaceView!!)
                    }
                    try {
                        cameraSurfacePreview?.setCamera(camera)
                    } catch (e: Exception) {
                        onCameraError.invoke(e)
                    }
                    onCameraOpen.invoke(camera)
                    /*if (handler == null) {
                        handler = CaptureActivityHandler(activity as CameraTestActivity?, decodeFormats, decodeHints, characterSet)
                    }*/
                }

            } catch (e: Exception) {
                onCameraError(e)
            }

        } else {
            onCameraError.invoke(Exception("Camera Open Fail"))
        }

    }


    override fun onCloseCamera() {
        releaseCameraAndPreview()
    }

    override fun takePicture() {
        try {
            if (camera != null && mPicture != null) {
                if (supportedFocusModes != null && (supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_AUTO))) {
                    camera?.autoFocus(object : Camera.AutoFocusCallback {
                        override fun onAutoFocus(success: Boolean, camera: Camera?) {
                            camera?.takePicture(null, null, mPicture)
                        }
                    })
                } else {
                    camera?.takePicture(null, null, mPicture)
                }

            } else {
                onCameraError.invoke(Exception("Camera take failed"))
            }

        } catch (e: Exception) {
            onCameraError.invoke(e)
        }

    }

    /**
     * Safe method for getting a mCamera instance.
     *
     * @return
     */
    fun getCameraInstance(face: CameraFace): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open(face.value) // attempt to get a Camera instance
            FrontCameraTest.hasFrontFlash = Tools.checkHasFlash(c)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return c // returns null if mCamera is unavailable
    }

    /**
     * Clear any existing preview / mCamera.
     */
    private fun releaseCameraAndPreview() {
        if (camera != null) {
            camera?.stopPreview()
            camera?.setPreviewCallback(null)
            camera?.release()
            camera = null
        }
        if (cameraSurfacePreview != null) {
            container.removeView(surfaceView)
            surfaceView = null
            cameraSurfacePreview = null
        }
        /*if (mPreview != null) {
            mPreview?.destroyDrawingCache()
            //mPreview.mCamera = null;
        }*/
    }

    companion object {

        val TAG = ""
    }

}