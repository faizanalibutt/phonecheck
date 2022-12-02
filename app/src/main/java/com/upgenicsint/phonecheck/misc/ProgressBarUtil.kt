package com.upgenicsint.phonecheck.misc

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.TextView
import com.upgenicsint.phonecheck.R

/**
 * Created by farhanahmed on 23/09/2017.
 */

object ProgressBarUtil {
    @JvmStatic fun get(text: String, context: Context): AlertDialog {

        val view = LayoutInflater.from(context).inflate(R.layout.progress_layout, null)
        val textView = view.findViewById<TextView>(R.id.text)
        textView.text = text
        val builder = AlertDialog.Builder(context).setView(view)
        return builder.create()

    }
}
