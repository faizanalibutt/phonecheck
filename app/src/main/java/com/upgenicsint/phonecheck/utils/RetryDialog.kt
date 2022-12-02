package com.upgenicsint.phonecheck.utils

import android.content.Context
import android.support.v7.app.AlertDialog
import com.upgenicsint.phonecheck.misc.RetryTextFieldListener

object RetryDialog {

    @JvmStatic
    fun createConfirmationAlert(context: Context, title: String, message: String, left: String, right: String, neutral: String, listener: RetryTextFieldListener?): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(left) { dialogInterface, i ->
                    listener?.onClick(dialogInterface, RetryTextFieldListener.ButtonType.LEFT)
                }
                .setPositiveButton(right) { dialogInterface, i ->
                    listener?.onClick(dialogInterface, RetryTextFieldListener.ButtonType.RIGHT)
                }.setNeutralButton(neutral) { dialogInterface, i ->
                    listener?.onClick(dialogInterface, RetryTextFieldListener.ButtonType.NEUTRAL)
                }
                .create()
    }
}