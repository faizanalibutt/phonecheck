package com.upgenicsint.phonecheck.fragments


/**
 * Created by farhanahmed on 17/10/2017.
 */

/**
 * @author Jose Davis Nidhin
 */

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.Size
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup

class CameraSurfacePreview(context: Context, val mSurfaceView: SurfaceView) : ViewGroup(context), SurfaceHolder.Callback {
    private val TAG = "CameraSurfacePreview"
    internal var mPreviewSize: Size? = null
    internal var mSupportedPreviewSizes: List<Size>? = null
    internal var mCamera: Camera? = null

    init {
        //        addView(mSurfaceView);

        val holder = mSurfaceView.holder
        holder.addCallback(this)
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    fun setCamera(camera: Camera) {
        mCamera = camera
        mSupportedPreviewSizes = mCamera?.parameters?.supportedPreviewSizes

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Source: http://stackoverflow.com/questions/7942378/android-mCamera-will-not-work-startpreview-fails
        val width = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes!!, width, height)
        }
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed && childCount > 0) {
            val child = getChildAt(0)

            val width = r - l
            val height = b - t

            var previewWidth = width
            var previewHeight = height

            mPreviewSize?.let {
                previewWidth = it.width
                previewHeight = it.height
            }


            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                val scaledChildWidth = previewWidth * height / previewHeight
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height)
            } else {
                val scaledChildHeight = previewHeight * width / previewWidth
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2)
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        try {
            mCamera?.setPreviewDisplay(mSurfaceView.holder)
            mCamera?.startPreview()
        } catch (exception: Exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val value = 0
    }


    private fun getOptimalPreviewSize(sizes: List<Camera.Size>, width: Int, height: Int): Camera.Size? {
        var optimalSize: Camera.Size? = null

        val ASPECT_TOLERANCE = 0.1
        val targetRatio = height.toDouble() / width

        for (size in sizes) {

            if (size.height != width) continue
            val ratio = size.width.toDouble() / size.height
            if (ratio <= targetRatio + ASPECT_TOLERANCE && ratio >= targetRatio - ASPECT_TOLERANCE) {
                optimalSize = size
            }
        }

        if (optimalSize == null) {

        }

        return optimalSize
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        val v = 0
    }

}
