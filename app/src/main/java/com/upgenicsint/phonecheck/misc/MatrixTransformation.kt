package com.upgenicsint.phonecheck.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

/**
 * Created by farhanahmed on 11/10/2017.
 */

class MatrixTransformation(context: Context, val matrix:Matrix) : BitmapTransformation(context) {


    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {

        Log.d("RotateTransformation", "Width " + toTransform.width + " Height " + toTransform.height)


        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
    }

    override fun getId(): String = "rotate" + matrix.hashCode()
}
