package com.upgenicsint.phonecheck.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

/**
 * Created by Farhan on 12/9/2016.
 */

class RotateTransformation(context: Context, val rotateRotationAngle: Float) : BitmapTransformation(context) {


    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {

        Log.d("RotateTransformation", "Width " + toTransform.width + " Height " + toTransform.height)

        val matrix = Matrix()

        matrix.postRotate(rotateRotationAngle, 0.5f, 0.5f)

        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
    }

    override fun getId(): String = "rotate" + rotateRotationAngle
}