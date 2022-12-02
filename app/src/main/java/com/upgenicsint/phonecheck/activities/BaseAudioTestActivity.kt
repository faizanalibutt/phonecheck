package com.upgenicsint.phonecheck.activities

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AlertDialog
import android.text.InputType
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.RetryTextFieldListener
import com.upgenicsint.phonecheck.misc.TextFieldListener
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.DialogUtils
import com.upgenicsint.phonecheck.utils.RetryDialog
import java.util.*

/**
 * Created by farhanahmed on 24/11/2017.
 */
abstract class BaseAudioTestActivity<T : Test>: DeviceTestableActivity<T>() {

    protected var isActivityVisible = false
    protected val isAutoAudioEnabled = Loader.instance.isAutoAudioEnabled
    protected val isAutoLSEnabled = Loader.instance.isAutoLSEnabled
    protected val isMicLSEnabled = Loader.instance.isMicLSEnabled
    protected val isAutoESEnabled = Loader.instance.isAutoESEnabled
    protected val isMicESEnabled = Loader.instance.isMicESEnabled
    protected val isAutoEarPieceEnabled = Loader.instance.isAutoEarPieceEnabled
    protected var TEST_LOCK = false
    protected var dialog: AlertDialog? = null
    protected var manual: SharedPreferences.Editor? = null

    protected fun showQuestionAlert(title: String, textFieldListener: TextFieldListener?) {

        val isShowing = dialog?.isShowing ?: false

        if (isShowing) { return }

        dialog = DialogUtils.createTextFieldDialog(context, title, getString(R.string.canc), getString(R.string.ent), InputType.TYPE_CLASS_NUMBER, textFieldListener)

        if (!isFinishing )
            dialog?.show()

    }

    protected fun showQuestionAlert(title: String, message: String, textFieldListener: RetryTextFieldListener?) {

        val isShowing = dialog?.isShowing ?: false

        if (isShowing) return

        dialog = RetryDialog.createConfirmationAlert(context, title, message, getString(R.string.no), getString(R.string.yes),
                getString(R.string.neutral), object : RetryTextFieldListener {
            override fun onClick(dialog: DialogInterface, type: RetryTextFieldListener.ButtonType) {
                textFieldListener?.onClick(dialog, type)
            }
        })

        if (!isFinishing )
            dialog?.show()

    }

    protected fun showQuestionAlert(title: String,message:String, textFieldListener: AlertButtonListener?) {

        val isShowing = dialog?.isShowing ?: false

        if (isShowing) return

        //sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        dialog = DialogUtils.createConfirmationAlert(context, title, message, getString(R.string.no), getString(R.string.yes), object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                //overide this becasue of Bad implementation from ios app
                textFieldListener?.onClick(dialog, type)
            }
        })
        if (!isFinishing)
            dialog?.show()
    }

    protected fun showAlert(message:String, textFieldListener: AlertButtonListener?) {

        val isShowing = dialog?.isShowing ?: false

        if (isShowing) return

        //sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        dialog = DialogUtils.createAlert(context, message, object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                //overide this becasue of Bad implementation from ios app
                textFieldListener?.onClick(dialog, type)
            }
        })
        if (!isFinishing)
            dialog?.show()
    }

    protected fun showQuestionAlert(title: String, textFieldListener: AlertButtonListener?) {

        showQuestionAlert(title,getString(R.string.did_hear_sound),textFieldListener)
    }

    fun showPreDoneAlert(listener: AlertButtonListener) {
        val alertDialog = DialogUtils.createConfirmationAlert(context, R.string.headphone_jack,
                R.string.headphone_jack_present, getString(R.string.no), getString(R.string.yes), listener)
        if (!isFinishing)
            alertDialog.show()
    }

    protected fun testWatcher() {
        if (BaseActivity.autoPerform && checkTest() == Test.PASS) {
            closeTimerTest()
            finish()
        }
    }

    protected fun stopPlayback()
    {
        val intent = Intent(this, TTSService::class.java)
        intent.action = TTSService.STOP_PLAYBACK
        startService(intent)

    }

    protected var randomNumber = 0

    protected fun generateNewNumber(singleDigit: Boolean) = 1 + Random().nextInt(if (singleDigit) 9 else 99)

    protected fun checkAnswer(s: String) = !(s.isEmpty() || !s.equals(randomNumber.toString(), ignoreCase = true))


    override fun getImageForStatus(value: Int) = when (value) {
        Test.PASS -> R.drawable.blue_check
        Test.FAILED -> R.drawable.not_working
        else -> R.drawable.warning
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }
}