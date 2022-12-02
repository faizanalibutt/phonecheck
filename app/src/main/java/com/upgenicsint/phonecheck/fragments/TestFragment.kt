package com.upgenicsint.phonecheck.fragments

import android.content.Context
import android.support.v4.app.Fragment
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Created by farhanahmed on 31/01/2017.
 */

open class TestFragment : Fragment() {
    var testListener: TestListener? = null
        private set

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is TestListener)
            testListener = context
    }

    override fun onDetach() {
        super.onDetach()
        testListener = null
    }

    open fun onTouchEvent(event: MotionEvent?):Boolean {
        return false
    }


}
