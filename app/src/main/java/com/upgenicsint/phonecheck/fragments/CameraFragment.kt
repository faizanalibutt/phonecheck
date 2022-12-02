package com.upgenicsint.phonecheck.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import com.upgenicsint.phonecheck.misc.CameraFace
import java.io.File

/**
 * Created by farhanahmed on 27/01/2017.
 */

abstract class CameraFragment<C> : Fragment() {
    open var camera: C? = null
    @JvmField var hasFrontCamera: Boolean = false
    @JvmField var hasBackCamera: Boolean = false
    @JvmField var hasBackFlash: Boolean = false
    @JvmField var hasFrontFlash: Boolean = false
    @JvmField
    var onAutoFocusCompleted: () -> Unit = {}
    @JvmField
    var onCameraError: (Throwable) -> Unit = {}
    @JvmField
    var onCameraOpen: (C) -> Unit = {}
    @JvmField
    var onPictureTaken: (Any) -> Unit = { }

    @JvmField var currentFace = CameraFace.FACE_FRONT
    @JvmField val isFacingFront = currentFace == CameraFace.FACE_FRONT


    /*public var cameraOpenCallBack: CameraOpenCallBack? = null
    public var pictureRequestCallBack: PictureRequestCallBack? = null
    public var onePictureRequestCallBack: OneShotPictureCallBack? = null*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasBackCamera = (context!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        hasFrontCamera = (context!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
        hasBackFlash = (context!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        hasFrontFlash = (false)

    }

    abstract fun openCamera()
    abstract fun onCloseCamera()
    abstract fun takePicture()

    fun saveImageOnFile(cameraData: ByteArray): File {

            val jpg: File
            val imageFileName = "camera.jpg"
            if (activity!!.cacheDir != null && activity!!.cacheDir.exists()) {
                jpg = File(activity!!.cacheDir, imageFileName)
            } else if (activity!!.filesDir != null && activity!!.filesDir.exists()) {
                jpg = File(activity!!.filesDir, imageFileName)
            } else {
                jpg = File(Environment.getExternalStorageDirectory(), imageFileName)
            }

            jpg.writeBytes(cameraData)
            return jpg
    }

}
