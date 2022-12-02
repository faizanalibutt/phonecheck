package com.upgenicsint.phonecheck.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.upgenicsint.phonecheck.Locale.LanguageSupport

import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.lcd_display_fragment.*


/**
 * A simple [Fragment] subclass.
 */
class GlassConditionFragment : Fragment() {

    private var listener: TestListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as TestListener?
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_glass_condition, container, false)
//        Tools.setBrightness(activity.window, 1f)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        failBtn.setOnClickListener {

            listener?.onDone(this@GlassConditionFragment, false)

        }
        passBtn.setOnClickListener {
            listener?.onDone(this@GlassConditionFragment, true)
        }

        failBtn2.setOnClickListener {

            listener?.onDone(this@GlassConditionFragment, false)

        }
        passBtn2.setOnClickListener {
            listener?.onDone(this@GlassConditionFragment, true)
        }

        failBtn3.setOnClickListener {

            listener?.onDone(this@GlassConditionFragment, false)

        }
        passBtn3.setOnClickListener {
            listener?.onDone(this@GlassConditionFragment, true)
        }
    }
}
