package com.upgenicsint.phonecheck.fragments

import android.content.Context
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AudioInputTestActivity
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.audio_input_test_layout.view.*
import kotlinx.android.synthetic.main.lcd_display_fragment.*

/**
 * Created by Farhan on 10/20/2016.
 */

class LCDFragment : Fragment() {

    private var listener: TestListener? = null
    var color:Int = -1
    private var countDownTimerCallStarted: CountDownTimer? = null

    companion object {
        fun newInstance(color: Int,buttonColor:Int): LCDFragment {
            val b = Bundle()
            b.putInt("color", color)
            b.putInt("buttonColor", buttonColor)
            val frag = LCDFragment()
            frag.arguments = b
            return frag
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as TestListener?
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.lcd_display_fragment, container, false)
        Tools.setBrightness(activity!!.window, 1f)
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Thread(Runnable {
//            try {
//                Thread.sleep(1500)
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//            activity.runOnUiThread {
//                Tools.setBrightness(activity.window, 1f)
//            }
//        }).start()

        //Toast.makeText(context, "Do you see white or black spots on the screen?", Toast.LENGTH_LONG).show()
        if (arguments != null) {
            color = arguments!!.getInt("color")
            val buttonColor = arguments!!.getInt("buttonColor")
            //failBtn.setTextColor(buttonColor)
            //passBtn.setTextColor(buttonColor)
            view.setBackgroundColor(color)
            if (color == Color.BLACK) {
                lcd?.setTextColor(Color.WHITE)
                passBtn.setTextColor(Color.WHITE)
                failBtn.setTextColor(Color.WHITE)
                passBtn2.setTextColor(Color.WHITE)
                failBtn2.setTextColor(Color.WHITE)
                passBtn3.setTextColor(Color.WHITE)
                failBtn3.setTextColor(Color.WHITE)
            }
        }
        failBtn.setOnClickListener {

            listener?.onDone(this@LCDFragment, false)

        }
        passBtn.setOnClickListener {
            listener?.onDone(this@LCDFragment, true)
        }

        failBtn2.setOnClickListener {

            listener?.onDone(this@LCDFragment, false)

        }
        passBtn2.setOnClickListener {
            listener?.onDone(this@LCDFragment, true)
        }

        failBtn3.setOnClickListener {

            listener?.onDone(this@LCDFragment, false)

        }
        passBtn3.setOnClickListener {
            listener?.onDone(this@LCDFragment, true)
        }


    }
}
