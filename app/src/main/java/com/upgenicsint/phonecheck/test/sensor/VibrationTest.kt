package com.upgenicsint.phonecheck.test.sensor

import android.content.Context
import android.content.DialogInterface
import android.os.SystemClock
import android.os.Vibrator
import android.support.v7.app.AlertDialog
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.MainActivity
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.AlertButtonListener.*
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.DialogUtils
import org.json.JSONException
import java.util.*

/**
 * Created by Farhan on 10/17/2016.
 */

class VibrationTest(context: Context) : Test(context) {
    private var times: Int = 0

    override val jsonKey: String
        get() = Test.vibrationTestKey

    override val hasSubTest: Boolean
        get() = false

    override val title: String
        get() = context.getString(R.string.vibration_title)

    override val detail: String
        get() = context.getString(R.string.vibration_desc)

    override val iconResource: Int
        get() = R.drawable.vibration

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        this.autoPerformMode = autoPerformMode
        if (isRunning) {
            return status
        }
        isRunning = true
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (!vibrator.hasVibrator()) {
            return Test.INIT
        }
        val isAuto = Loader.instance.filterContains(Test.autoVibrationKey)
//        val isAuto = true
        times = Random().nextInt(if (isAuto) 3 else 1)
        val vibratorTask = object : TimerTask() {
            internal var vibrated = 0

            override fun run() {
                if (vibrated >= times) {
                    testListener?.onPerformDone()
                    cancel()
                }
                vibrator.vibrate(VIBRATE_TIME)
                vibrated++
                SystemClock.sleep(VIBRATE_SLEEP_TIME)
            }
        }
        val vibrationTimer = Timer()
        vibrationTimer.schedule(vibratorTask, 0, VIBRATE_SLEEP_TIME + VIBRATE_TIME)
        return status
    }

    override fun performUserInteraction() {

        val activity = context as MainActivity?
        if (activity != null) {

            activity.runOnUiThread {
                if (testListener != null) {
                    val alertDialog: AlertDialog?
                    val isAuto = Loader.instance.filterContains(Test.autoVibrationKey)
//                    val isAuto = true

                    if (isAuto) {

                        val items = arrayOf("0", "1", "2", "3")
                        //final MultiChoiceAlert alert = new MultiChoiceAlert(getContext(), items);
                        alertDialog = AlertDialog.Builder(context)
                                .setTitle(R.string.how_many_vibrate)
                                .setCancelable(false)
                                .setSingleChoiceItems(items, -1) { dialogInterface, position ->
                                    status = if(items[position].equals((times + 1).toString(), ignoreCase = true)) Test.PASS else Test.FAILED
                                    testListener?.onUserInteractionDone(true)
                                    isRunning = false
                                    dialogInterface.dismiss()
                                }.setNegativeButton(context.getString(R.string.cancel)) { dialogInterface, i ->
                                    isRunning = false
                                    dialogInterface.dismiss()
                                    testListener?.onUserInteractionCancel(true)
                                }.create()
                    }
                    else {
                        alertDialog = DialogUtils.createConfirmationAlert(context, R.string.vibration, R.string.did_device_vibrate, context.getString(R.string.fail), context.getString(R.string.pass), object : AlertButtonListener {
                            override fun onClick(dialog: DialogInterface, type: ButtonType) {

                                isRunning = false


                                status = if (type == ButtonType.RIGHT) Test.PASS else Test.FAILED
                                dialog.dismiss()
                                testListener?.onUserInteractionDone(true)
                            }
                        })

                    }
                    if (alertDialog != null && !alertDialog.isShowing) {
                        if (context is MainActivity) {
                            val mainActivity = context as MainActivity?
                            if (mainActivity != null && !mainActivity.isFinishing) {
                                alertDialog.show()
                            }
                        }
                    }

                }
            }
        }


    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.vibrationTestKey, toJsonStatus())
        storeOperation.add(Test.vibrationTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.vibrationTestKey, status)
        Loader.RESULT.put(Test.vibrationTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction() = true

    companion object {
        private val VIBRATE_TIME: Long = 800
        private val VIBRATE_SLEEP_TIME: Long = 500
    }
}
