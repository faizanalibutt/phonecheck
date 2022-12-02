package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.*
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.CallTest
import com.upgenicsint.phonecheck.test.sensor.ProximityTest
import com.upgenicsint.phonecheck.utils.DialogUtils
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*

class CallActivity : DeviceTestableActivity<CallTest>(), ProximitySensorManager.Listener {

    override fun onNear() {
        if (Devices.isS10Available()) {
            val test = test
            if (test != null && isSimPresent) {
                test.sub(Test.proximityTestKey)?.value = if (isSimPresent) Test.PASS else Test.FAILED
                setImage(test.sub(Test.proximityTestKey), proxStatusImageView)
            }
            val test1 = Loader.instance.getByClassType(ProximityTest::class.java)
            if (test1 != null) {
                test1.status = if (isSimPresent) Test.PASS else Test.FAILED
            }
        }
    }

    override fun onFar() {

    }

    private var shouldOpenResult: Boolean = false
    internal var phoneListener: CallStateListener? = null
    internal val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private var isCallConnected: Boolean = false
    private var showEnterNumberDialog: Boolean = false
    private var dialogLOCK: Boolean = false
    private var callDelayHandler: Handler? = null
    private var proximitySensorManager: ProximitySensorManager? = null
    private val callDelayRunnable = Runnable {
        showEnterNumberDialog()
    }
    private var numberToDial = Loader.instance.clientCustomization?.numberToDial ?: DEFAULT_NUMBER
    private val mTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            try {
                numberToDial = s.toString()
            } catch (e: Exception) {
                numberToDial = DEFAULT_NUMBER
                e.printStackTrace()
            }

        }

        override fun afterTextChanged(s: Editable) {

        }
    }

    val isSimPresent: Boolean
        get() = when (telephonyManager.simState) {
            TelephonyManager.SIM_STATE_ABSENT -> false
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> false
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> false
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> false
            TelephonyManager.SIM_STATE_READY -> true
            TelephonyManager.SIM_STATE_UNKNOWN -> false
            else -> false
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        Loader.TIME_VALUE = 0
        CALL_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)


        onCreateNav()
        setNavTitle(getString(R.string.callTest_nav_title))
        test = Loader.instance.getByClassType(CallTest::class.java)
        phoneListener = CallStateListener(this)
        phoneNumberEditText.setText(numberToDial)
        phoneNumberEditText.addTextChangedListener(mTextWatcher)

        callBtn.setOnClickListener {
            if (isSimPresent) {
                if (callBtn.tag == null) {
                    callBtn.tag = "LOCK"
                    if (isSimPresent) {
                        if (ALLOW_MANUAL_CALL) {
                            showEnterNumberDialog()
                        } else {
                            if (Devices.isS10Available()) {
                                if (test?.sub(Test.proximityTestKey)?.value == Test.PASS) {
                                    dialPhoneNumber(numberToDial)
                                } else {
                                    showDialog()
                                }
                            } else {
                                dialPhoneNumber(numberToDial)
                            }
                        }
                    }
                } else if (isSimPresent) {
                    if (ALLOW_MANUAL_CALL) {
                        showEnterNumberDialog()
                    } else {
                        if (Devices.isS10Available()) {
                            showDialog()
                        } else {
                            dialPhoneNumber(numberToDial)
                        }
                    }
                }
            } else {
                Toast.makeText(context, getString(R.string.calltst_err), Toast.LENGTH_SHORT).show()
            }
        }
        val test = test
        if (test != null) {
            test.sub(Test.simReaderTestKey)?.value = if (isSimPresent) Test.PASS else Test.FAILED

            setImage(test.sub(Test.simReaderTestKey), simStatusImageView)
            setImage(test.sub(Test.networkTestKey), connectionStatusImageView)
            setImage(test.sub(Test.proximityTestKey), proxStatusImageView)

            callBtn.setImageResource(if (test.sub(Test.networkTestKey)?.isPass
                            ?: false) R.drawable.callworking else R.drawable.calldefault)

            if (!isSimPresent) {

                val confirmationAlert = DialogUtils.createConfirmationAlert(context, R.string.sim_notfound, R.string.continue_test, context.getString(R.string.no), context.getString(R.string.cont), object : AlertButtonListener {
                    override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                        if (type == AlertButtonListener.ButtonType.RIGHT) {
                            finalizeTest()
                        }
                        dialog.dismiss()
                    }
                })
                confirmationAlert.show()

            } else {
                callBtn.performClick()
            }
        }

        if (Devices.isS10Available()) {
            proxStatusImageView?.visibility = View.VISIBLE
            proxStatusTextView?.visibility = View.VISIBLE
            proximitySensorManager = ProximitySensorManager(this, this)
            proximitySensorManager?.enable()
        }

    }

    private fun showDialog() {
        val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
        } else {
            AlertDialog.Builder(context)
        }
        builder.setCancelable(false)
        builder.setTitle(getString(R.string.callTest_nav_title))
                .setMessage(getString(R.string.s10proximitytext))
                .setPositiveButton(android.R.string.ok) { _, _ -> dialPhoneNumber(numberToDial) }
                .setIcon(android.R.drawable.ic_menu_call)

        if (!isFinishing) {
            val dialog = builder.show()
        }
    }

    private fun setImage(i: SubTest?, view: ImageView) {
        if (i == null) {
            return
        }
        if (i.value == Test.INIT) {
            view.setImageResource(R.drawable.warning)
        } else {
            view.setImageResource(if (i.value == Test.PASS) R.drawable.blue_check else R.drawable.not_working)
        }
    }

    override fun checkTest(): Int {
        val test = test ?: return Test.FAILED

        var s = Test.INIT
        for ((_, value) in test.subTests) {
            if (value.value == Test.PASS) {
                s = Test.PASS
            } else {
                s = Test.FAILED
                break
            }
        }
        test.status = s
        return test.status
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return callBtn != null && callBtn.tag != null
    }

    override fun onResume() {
        super.onResume()
        val test = test ?: return
        telephonyManager.listen(phoneListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        if (!isSimPresent) {
            return
        }
        if (shouldOpenResult && isCallConnected) {
            setImage(test.sub(Test.networkTestKey), connectionStatusImageView)
            callBtn.setImageResource(R.drawable.callworking)
            callBtn.tag = null
            shouldOpenResult = false
        } else if (shouldOpenResult && showEnterNumberDialog) {
            callBtn.tag = null
            shouldOpenResult = false
            callDelayHandler = Handler()
            callDelayHandler?.postDelayed(callDelayRunnable, 500)

        }
        if (isCallConnected && BaseActivity.autoPerform) {
            finalizeTest()
        }
    }

    private fun showEnterNumberDialog() {
        if (dialogLOCK) {
            return
        }
        dialogLOCK = true

        val createTextFieldDialog = DialogUtils.createTextFieldDialog(context, getString(R.string.enter_number), getString(R.string.ok_id), context.getString(R.string.canc), InputType.TYPE_CLASS_PHONE, object : TextFieldListener {
            override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                val test = test
                if (isTrue) {
                    dialPhoneNumber(text)
                } else if (test != null) {
                    test.sub(Test.networkTestKey)?.value = if (isCallConnected) Test.PASS else Test.FAILED
                    setImage(test.sub(Test.networkTestKey), connectionStatusImageView)
                }

                if (BaseActivity.autoPerform) {
                    finalizeTest()
                }
                if (callBtn != null)
                    callBtn.tag = null
                dialog.dismiss()
                dialogLOCK = false
                showEnterNumberDialog = false
            }

        })

        /*if (callBtn != null)
            callBtn.tag = null
        dialogLOCK = false
        showEnterNumberDialog = false*/

        if (!isFinishing) {
            createTextFieldDialog.show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (phoneNumberEditText != null) {
            phoneNumberEditText.removeTextChangedListener(mTextWatcher)
        }
        showEnterNumberDialog = false
        if (callDelayHandler != null && callDelayRunnable != null) {
            callDelayHandler?.removeCallbacks(callDelayRunnable)
        }
        try {
            telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)
        } catch (e: NullPointerException) {
            Log.d("Call Activity", "Attempt to read from field java.lang.Integer android.telephony.PhoneStateListener.mSubId on a null object reference")
        }
        phoneListener = null
        if (Devices.isS10Available()) {
            proximitySensorManager?.disable(false)
        }
    }

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }

    override fun closeTimerTest() {
        super.closeTimerTest()
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                CALL_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_call), -1)] =
                            RecordTest(context.getString(R.string.report_call_test), CALL_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("Call Test", "${CALL_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {
            ignored.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun dialPhoneNumber(s: String) {
        val number = Uri.parse("tel:" + s.replace("#", "%23"))
        val callIntent = Intent(Intent.ACTION_CALL, number)
        try {
            startActivityForResult(callIntent, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            showEnterNumberDialog = false
            val test = test
            if (test != null) {
                test.sub(Test.networkTestKey)?.value = Test.FAILED
                setImage(test.sub(Test.networkTestKey), connectionStatusImageView)
                callBtn.setImageResource(if (test.sub(Test.networkTestKey)?.isPass
                                ?: false) R.drawable.callworking else R.drawable.calldefault)
            }
        }

    }

    fun onCallStateIdle() {
        val test = test
        if (!isCallConnected && test != null) {
            test.sub(Test.networkTestKey)?.value = Test.FAILED
        }
        Logy.i(TAG, "CALL_STATE_IDLE")
        shouldOpenResult = true
        showEnterNumberDialog = false
        dialogLOCK = false
    }

    fun onCallStateOffHook() {
        isCallConnected = true
        dialogLOCK = true
        showEnterNumberDialog = false
        val test = test
        if (test != null)
            test.sub(Test.networkTestKey)?.value = if (isCallConnected) Test.PASS else Test.FAILED
        Logy.i(TAG, "CALL_STATE_OFFHOOK")
    }

    companion object {

        private val ALLOW_MANUAL_CALL = false
        private val DEFAULT_NUMBER = "611"
        var CALL_SCREEN_TIME = 0
        private val TAG = CallActivity::class.java.simpleName
    }
}
