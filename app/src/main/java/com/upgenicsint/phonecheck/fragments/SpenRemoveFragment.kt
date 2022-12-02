package com.upgenicsint.phonecheck.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget
import com.samsung.android.sdk.pen.Spen
import com.samsung.android.sdk.pen.engine.SpenPenDetachmentListener
import com.samsung.android.sdk.pen.engine.SpenSimpleSurfaceView
import com.upgenicsint.phonecheck.R
import java.lang.ref.WeakReference

/**
 * Created by farhanahmed on 31/01/2017.
 */

class SpenRemoveFragment : TestFragment() {

    internal var spenSimpleSurfaceView: SpenSimpleSurfaceView? = null
    private var isRemoved: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.spen_remove_layout, container, false)
        val spenImageView = view.findViewById<View>(R.id.spenImageView) as ImageView
        try {
            val spen = Spen()
            spen.initialize(context)
            spenSimpleSurfaceView = SpenSimpleSurfaceView(context)
            val imageViewTarget = GlideDrawableImageViewTarget(spenImageView)
            val spenAnimUri = Uri.parse("android.resource://" + context!!.packageName + "/" + R.raw.spen_anim)
            Glide.with(context)
                    .load(spenAnimUri)
                    .error(R.drawable.spen)
                    .dontAnimate()
//                    .crossFade()
                    .into(imageViewTarget)

            spenSimpleSurfaceView?.setPenDetachmentListener(CustomSpenPenDetachmentListener(this))


        } catch (e: Exception) {
            e.printStackTrace()
        }

        return view
    }

    internal class CustomSpenPenDetachmentListener(spenRemoveFragment: SpenRemoveFragment) : SpenPenDetachmentListener {
        private val spenRemoveFragmentWeakReference = WeakReference(spenRemoveFragment)

        override fun onDetached(b: Boolean) {
            val get = spenRemoveFragmentWeakReference.get()
            if (get != null) {
                get.onSpenDetached(b)
            }
        }
    }

    private fun onSpenDetached(b: Boolean) {
        if (testListener != null) {
            if (!b && !isRemoved) {
                isRemoved = true
                try{
                    testListener?.onDone(this, isRemoved)
                }
                catch (e: IllegalStateException){
                    Log.d("SpenRemoveFragment","Cannot perform this action")
                }
            }
        }
    }

    override fun onDetach() {
        spenSimpleSurfaceView?.setPenDetachmentListener(null)
        spenSimpleSurfaceView?.close()
        super.onDetach()
    }
}
