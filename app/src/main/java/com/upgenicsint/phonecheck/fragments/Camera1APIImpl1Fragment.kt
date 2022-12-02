package com.upgenicsint.phonecheck.fragments


import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.ragnarok.rxcamera.RxCamera
import com.ragnarok.rxcamera.config.RxCameraConfig
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.CameraTestActivity
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.misc.CameraFace
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.fragment_1_camera.*
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import java.security.Policy

/**
 * Created by farhanahmed on 27/01/2017.
 */

class Camera1APIImpl1Fragment : CameraFragment<RxCamera>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_1_camera, container, false)
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.d(TAG, "called onAttach")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "called onDetach")
    }

    private var supportedFocusModes: MutableList<String>? = null
    override fun openCamera() {
        try {
            val config = RxCameraConfig.Builder()

            if (currentFace === CameraFace.FACE_FRONT) {
                config.useFrontCamera()
            } else {
                config.useBackCamera()
            }.setHandleSurfaceEvent(true).setPreferPreviewSize(cameraPoints(), false)

            RxCamera.open(context, config.build()).flatMap { rxCamera ->
                val camera = rxCamera

                if (camera != null) {

                    when {
                        Build.MODEL.contains("XT1710-02") -> {
                            Tools.setupParameters(camera.nativeCamera, context!!)
                        }
                        BuildConfig.FLAVOR === "camera" -> Tools.setupParameters(camera.nativeCamera, context!!)
                        else -> {

                            var params: Camera.Parameters? = null
                            try {
                                params = camera.nativeCamera.parameters
                            } catch (e: Exception) {
                                showErrorMessage("getting parameters for camera")
                            }

                            try {
                                val supportedFlashModes = params!!.supportedFlashModes
                                if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                    params.flashMode = Camera.Parameters.FLASH_MODE_ON
                                }
                            } catch (e: Exception) {
                                showErrorMessage("flash mode is not set")
                            }

                            try {
                                val previewSizes = params!!.supportedPreviewSizes
                                val previewSize = Tools.getMaxSupportedSize(previewSizes)
                                if (previewSize != null) {
                                    Log.i("setupParameters", "Preview w " + previewSize.width + " h " + previewSize.height)
                                    params.setPreviewSize(previewSize.width, previewSize.height)
                                }
                            } catch (e: Exception) {
                                showErrorMessage("camera preview is not set")
                            }

                            try {
                                val isTablet = context!!.resources.getBoolean(R.bool.isTablet)
                                if (isTablet) {
                                    Log.d(TAG, "Tab detected, don't set picture size exception")
                                } else {
                                    val pictureSizes = params!!.supportedPictureSizes
                                    val pictureSize = Tools.getMaxSupportedSize(pictureSizes)
                                    if (pictureSize != null) {
                                        params.setPictureSize(pictureSize.width, pictureSize.height)
                                    }
                                }
                            } catch (e: java.lang.Exception) {
                                showErrorMessage("picture size is not set")
                            }

                            try {
                                //auto da focus start

                                supportedFocusModes = params!!.supportedFocusModes

                                /*if (supportedFocusModes != null && supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                                    params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                                } else*/
                                if (supportedFocusModes != null && supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                    params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                                }
                                //Build.MANUFACTURER.containsIgnoreCase("huawei") &&

                                //auto da focus end
                            } catch (e: Exception) {
                                showErrorMessage("focus mode is not set")
                            }


                            try {
                                camera.nativeCamera.parameters = params
                            } catch (e: Exception) {
                                showErrorMessage("parameters not set")
                            }

                            /*val pictureSize = params.pictureSize
                            params.setPictureSize(previewSize!!.width, previewSize.height)*/
                        }
                    }

                    onCameraOpen.invoke(camera)
                } else {
                    onCameraError.invoke(Exception("error"))
                }
                rxCamera.bindTexture(textureView)

            }.flatMap { rxCamera -> rxCamera.startPreview() }.observeOn(AndroidSchedulers.mainThread()).subscribe(object : Subscriber<RxCamera>() {
                override fun onCompleted() {

                }

                override fun onError(e: Throwable) {
                    onCameraError.invoke(e)
                }

                override fun onNext(rxCamera: RxCamera) {
                    camera = rxCamera
                }
            })
        } catch (e: Exception) {
            if (e.message?.containsIgnoreCase("Fail to get camera") == true) {
                onCameraError.invoke(Exception("Can't connect to camera, Restarting Device can fix this issue."))
            } else {
                onCameraError.invoke(e)
            }

        }
    }

    private fun showErrorMessage(message: String) {
        Log.d(TAG, message)
        //Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCloseCamera() {
        if (camera != null) {
            camera?.closeCameraWithResult()?.subscribe { aBoolean ->
                FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                        .child("closeCameraWithResult")
                        .setValue(aBoolean)
            }
        }
        camera = null

    }

    override fun takePicture() {

        val camera = camera ?: return

        try {
            if (supportedFocusModes == null && supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                camera.nativeCamera?.autoFocus(object : Camera.AutoFocusCallback {
                    override fun onAutoFocus(success: Boolean, cam: Camera?) {
                        camera.request().takePictureRequest(false, { }).subscribe { rxCameraData ->
                            when {
                                Build.MODEL.contains("XT1710-02") -> onPictureTaken(saveImageOnFile(rxCameraData.cameraData))
                                BuildConfig.FLAVOR === "camera" -> onPictureTaken(saveImageOnFile(rxCameraData.cameraData))
                                else -> onPictureTaken(rxCameraData)
                            }
                        }
                    }
                })
            } else {
                camera.request().takePictureRequest(false, { }).subscribe { rxCameraData ->
                    when {
                        Build.MODEL.contains("XT1710-02") -> {
                            onPictureTaken(saveImageOnFile(rxCameraData.cameraData))
                        }
                        BuildConfig.FLAVOR == "camera" -> onPictureTaken(saveImageOnFile(rxCameraData.cameraData))
                        else -> onPictureTaken(rxCameraData)
                    }
                }
            }

        } catch (e: Throwable) {
            onCameraError.invoke(e)
        }

    }

    private fun cameraPoints(): Point {
        val displayMetrics = context!!.resources.displayMetrics
        return Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    companion object {
        val TAG = "Camera1APIImpl1Fragment"
    }

}
