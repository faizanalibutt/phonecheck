package com.upgenicsint.phonecheck.fragments


import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.lcd_display_fragment.*
import java.util.*

class BrightnessFragment : Fragment() {

    private var listener: TestListener? = null
    internal var brightLvlLowToHigh = floatArrayOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0f)
    var timerObj: Timer? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as TestListener?
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_brightness, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        brightnessAdjustment()

        failBtn.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            listener?.onDone(this@BrightnessFragment, false)

        }
        passBtn.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            listener?.onDone(this@BrightnessFragment, true)
        }
    }

    private fun brightnessAdjustment() {
        timerObj = Timer()
        val timerTaskObj = object : TimerTask() {
            override fun run() {
                for (i in brightLvlLowToHigh) {
                    Thread.sleep(150)
                    activity!!.runOnUiThread{
                        Tools.setBrightness(activity!!.window, i)
                    }
                }
            }
        }
        timerObj!!.schedule(timerTaskObj, 0, 100)

    }
}
