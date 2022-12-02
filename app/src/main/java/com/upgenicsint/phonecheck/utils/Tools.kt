package com.upgenicsint.phonecheck.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Typeface
import android.hardware.Camera
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.upgenicsint.phonecheck.containsIgnoreCase

/**
 * Created by Farhan on 10/14/2016.
 */

object Tools {
    @JvmStatic
    fun getJunctionFontRegular(context: Context) = Typeface.createFromAsset(context.assets, "font/junction.regular.otf")

    @JvmStatic
    fun getMaxSupportedSize(sizes: List<Camera.Size>?): Camera.Size? {

        if (sizes == null || sizes.size < 0) {
            return null
        }
        var max = 0
        var index = 0
        for (i in sizes.indices) {
            val s = sizes[i]
            val size = s.height * s.width
            Log.i("setupParameters", "Preview w ${s.width} h ${s.height}")
            if (size > max) {
                index = i
                max = size
            }
        }

        return sizes[index]
    }

    @JvmStatic
    fun getSizeCloseTo(sizes: List<Camera.Size>?, width: Int, height: Int): Camera.Size? {

        if (sizes == null || sizes.size < 0) {
            return null
        }

        val number = (width * height);

        var prev = Math.abs((sizes[0].width * sizes[0].height) - number)
        var closest = 0
        for (i in 0 until sizes.size) {
            val diff = Math.abs((sizes[i].width * sizes[i].height) - number)

            if (diff < prev) {
                prev = diff
                closest = i
            }
        }

        return sizes[closest]
    }

    @JvmStatic
    fun openFrontFacingCameraGingerbread(): Camera? {
        var cameraCount = 0
        var cam: Camera? = null
        val cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()
        for (camIdx in 0 until cameraCount) {
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx)
                } catch (e: RuntimeException) {
                }

            }
        }
        return cam
    }

    @JvmStatic
    fun checkHasFlash(): Boolean {

        var camera = openFrontFacingCameraGingerbread()

        if (camera != null) {
            val params = camera.parameters
            val flashModes = params.supportedFlashModes ?: return false
            for (flashMode in flashModes) {
                if (Camera.Parameters.FLASH_MODE_ON == flashMode /*|| Camera.Parameters.FLASH_MODE_TORCH == flashMode*/) {
                    return true
                }
            }
            camera.unlock()
            camera = null
        }

        return false
    }

    @JvmStatic
    fun checkHasFlash(camera: Camera?): Boolean {
        if (camera != null) {
            val params = camera.parameters
            val flashModes = params.supportedFlashModes ?: return false
            for (flashMode in flashModes) {
                if (Camera.Parameters.FLASH_MODE_ON == flashMode) {
                    return true
                }
            }
        }

        return false
    }

    @JvmStatic
    fun flipView(view: View?) {
        if (view == null) {
            return
        }
        val animator = ObjectAnimator.ofFloat(view, View.ROTATION_X, 90f, 360f)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 500
        animator.start()
    }

    @JvmStatic
    fun scaleUp(view: ImageView?) {
        if (view == null) {
            return
        }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f), ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f))
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.duration = 500
        animatorSet.start()
    }

    @JvmStatic
    fun setBrightness(window: Window, level: Float) {

        //constrain the value of brightness

        val lp = window.attributes
        lp.screenBrightness = level
        window.attributes = lp

    }

    @JvmStatic
    fun setupParameters(camera: Camera, context: Context) {

        val nativeCamera = camera

        val params = nativeCamera.parameters
        params.jpegQuality = 80

        val supportedPictureFormats = params.supportedPictureFormats
        if (supportedPictureFormats != null && supportedPictureFormats.contains(ImageFormat.JPEG)) {
            params.pictureFormat = ImageFormat.JPEG
        }

//        val pictureSizes = params.supportedPictureSizes
//
//        val displayMetrics = context.resources.displayMetrics

//        val pictureSize = Tools.getSizeCloseTo(pictureSizes, displayMetrics.widthPixels, displayMetrics.heightPixels)
//        if (pictureSize != null) {
//            Log.i("setupParameters", "Picture w " + pictureSize.width + " h " + pictureSize.height)
//            params.setPictureSize(pictureSize.width, pictureSize.height)
//        }

        val supportedColorEffects = params.supportedColorEffects

        if (supportedColorEffects != null && supportedColorEffects.contains(Camera.Parameters.EFFECT_NONE)) {
            params.colorEffect = Camera.Parameters.EFFECT_NONE
        }

        val supportedSceneModes = params.supportedSceneModes

        if (supportedSceneModes != null && supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_AUTO)) {
            params.sceneMode = Camera.Parameters.SCENE_MODE_AUTO
        }

        val supportedWhiteBalance = params.supportedWhiteBalance

        if (supportedWhiteBalance != null && supportedWhiteBalance.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            params.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
        }

        val supportedFlashModes = params.supportedFlashModes
        if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
            params.flashMode = Camera.Parameters.FLASH_MODE_ON
        }

        val previewSizes = params.supportedPreviewSizes
        val previewSize = Tools.getMaxSupportedSize(previewSizes)
        if (previewSize != null) {
            Log.i("setupParameters", "Preview w " + previewSize.width + " h " + previewSize.height)
            params.setPreviewSize(previewSize.width, previewSize.height)
        }

        val supportedFocusModes = params.supportedFocusModes

        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }

        if (Build.MANUFACTURER.containsIgnoreCase("huawei") &&
                supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }

        nativeCamera.parameters = params
    }
}
