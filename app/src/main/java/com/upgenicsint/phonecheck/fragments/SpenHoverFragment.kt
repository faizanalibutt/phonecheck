package com.upgenicsint.phonecheck.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.samsung.android.sdk.pen.Spen
import com.samsung.android.sdk.pen.engine.SpenSimpleSurfaceView
import com.upgenicsint.phonecheck.R
import kotlinx.android.synthetic.main.spen_hover_fragment.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by farhanahmed on 31/01/2017.
 */

class SpenHoverFragment : TestFragment() {

    internal var spenSimpleSurfaceView: SpenSimpleSurfaceView? = null
    private var isHovered: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.spen_hover_fragment, container, false)
        val frameLayout = view.findViewById<View>(R.id.frameLayout) as FrameLayout
        try {

            val spen = Spen()
            spen.initialize(context)
            spenSimpleSurfaceView = SpenSimpleSurfaceView(context)

            spenSimpleSurfaceView?.setOnHoverListener(CustomSpenHoverListener(this))

            frameLayout.addView(spenSimpleSurfaceView)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return view
    }

    internal class CustomSpenHoverListener(spenHoverFragment: SpenHoverFragment) : View.OnHoverListener {
        private val spenHoverFragmentWeakReference = WeakReference(spenHoverFragment)


        override fun onHover(v: View, event: MotionEvent): Boolean {
            val get = spenHoverFragmentWeakReference.get()
            if (get != null) {
                return get.onSpenHover(v, event)
            }
            return false;
        }
    }

    private fun onSpenHover(v: View, event: MotionEvent): Boolean {

        try {
            spenTextView.text = String.format(Locale.US, "X = %1d Y = %1d", event.x.toInt(), event.y.toInt())
        } catch (ignored: Throwable) {
            spenTextView.text = ignored.message
        }

        if (isHovered) {
            return false
        }
        isHovered = true
        testListener?.onDone(this, isHovered)
        return false
    }

    override fun onDetach() {
        spenSimpleSurfaceView?.setOnHoverListener(null)
        super.onDetach()
    }
}
