package com.upgenicsint.phonecheck.utils

import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.TextFieldListener

/**
 * Created by farhanahmed on 12/09/2017.
 */

object DialogUtils {
    @JvmStatic
    fun createConfirmationAlert(context: Context, @StringRes title: Int, @StringRes message: Int, listener: AlertButtonListener): AlertDialog {
        return createConfirmationAlert(context, title, message, "Yes", "No", listener)
    }

    @JvmStatic
    fun createAlert(context: Context, message: String, listener: AlertButtonListener): AlertDialog {
        return createAlert(context, message, "Ok", listener)
    }

    @JvmStatic
    fun createConfirmationAlert(context: Context, title: String, message: String, listener: AlertButtonListener): AlertDialog {
        return createConfirmationAlert(context, title, message, "Yes", "No", listener)
    }

    @JvmStatic
    fun createConfirmationAlert(context: Context, @StringRes title: Int, @StringRes message: Int, left: String, right: String, listener: AlertButtonListener): AlertDialog {
        return createConfirmationAlert(context, context.getString(title), context.getString(message), left, right, listener)
    }

    @JvmStatic
    fun createConfirmationAlert(context: Context, title: String, message: String, left: String, right: String, listener: AlertButtonListener?): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(left) { dialogInterface, i ->
                    listener?.onClick(dialogInterface, AlertButtonListener.ButtonType.LEFT)
                }
                .setPositiveButton(right) { dialogInterface, i ->
                    listener?.onClick(dialogInterface, AlertButtonListener.ButtonType.RIGHT)
                }.create()
    }

    @JvmStatic
    fun createAlert(context: Context, message: String, right: String, listener: AlertButtonListener?): AlertDialog {
        return AlertDialog.Builder(context)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(right) { dialogInterface, i ->
                    listener?.onClick(dialogInterface, AlertButtonListener.ButtonType.RIGHT)
                }.create()
    }


    fun createTextFieldDialog(context: Context, title: String, nagText: String, posText: String, inputType: Int, textFieldListener: TextFieldListener?): AlertDialog {


        val view = LayoutInflater.from(context).inflate(R.layout.edittext_alert, null)
        val alertEditText = view.findViewById<EditText>(R.id.alertEditText)
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val mShowImeRunnable = Runnable {
            imm.showSoftInput(alertEditText, 0)
        }
        alertEditText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                alertEditText.post(mShowImeRunnable)
            } else {
                alertEditText.removeCallbacks(mShowImeRunnable)
                imm.hideSoftInputFromWindow(alertEditText.getWindowToken(), 0)
            }
        }

        alertEditText.inputType = inputType

        return AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .setCancelable(false)
                .setOnDismissListener { dialog ->
                    imm.hideSoftInputFromWindow(alertEditText.windowToken, 0)
                }
                .setNegativeButton(nagText) { dialogInterface, i ->
                    imm.hideSoftInputFromWindow(alertEditText.windowToken, 0)
                    textFieldListener?.onClick(dialogInterface, alertEditText.text.toString(), false)
                }
                .setPositiveButton(posText) { dialogInterface, i ->
                    imm.hideSoftInputFromWindow(alertEditText.windowToken, 0)
                    textFieldListener?.onClick(dialogInterface, alertEditText.text.toString(), true)
                }.create()
    }
}
