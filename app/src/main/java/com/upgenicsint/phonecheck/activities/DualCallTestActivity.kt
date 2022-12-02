package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.CallActivity.Companion.CALL_SCREEN_TIME
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.misc.DualCallStateListener
import com.upgenicsint.phonecheck.misc.ProximitySensorManager
import com.upgenicsint.phonecheck.misc.TelInfo
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.DualCallTest
import com.upgenicsint.phonecheck.test.sensor.ProximityTest
import kotlinx.android.synthetic.main.activity_dual_call_test.*
import java.util.*

@SuppressLint("MissingPermission")
class DualCallTestActivity : DeviceTestableActivity<DualCallTest>(), View.OnClickListener, ProximitySensorManager.Listener {

    override fun onNear() {
        if (Devices.isS10Available()) {
            val test = test
            if (test != null) {
                test.sub(Test.proximityTestKey)?.value = if (isSim1Present || isSim2Present) Test.PASS else Test.FAILED
                setImage(test.sub(Test.proximityTestKey), proxStatusImageView)
            }
            val test1 = Loader.instance.getByClassType(ProximityTest::class.java)
            if (test1 != null) {
                test1.status = if (isSim1Present || isSim2Present) Test.PASS else Test.FAILED
            }
        }
    }

    override fun onFar() {

    }

    // sim slots type
    private val simSlotName = arrayOf(
            "extra_asus_dial_use_dualsim",
            "com.android.phone.extra.slot",
            "slot",
            "simslot",
            "sim_slot",
            "subscription",
            "Subscription",
            "phone",
            "com.android.phone.DialingMode",
            "simSlot",
            "slot_id",
            "simId",
            "simnum",
            "phone_type",
            "slotId",
            "slotIdx"
    )

    private var numberToDial = DEFAULT_NUMBER
    private var phoneListener: DualCallStateListener? = null
    private var isCallConnected: Boolean = false
    private var proximitySensorManager: ProximitySensorManager? = null
    internal val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    private val mTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            try {
                numberToDial = s.toString()
            } catch (e: Exception) {
                numberToDial = DEFAULT_NUMBER
                e.printStackTrace()
            }

        }

        override fun afterTextChanged(s: Editable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dual_call_test)

        Loader.TIME_VALUE = 0
        CallActivity.CALL_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        onCreateNav()
        setNavTitle(getString(R.string.dualcallTest_nav_title))
        test = Loader.instance.getByClassType(DualCallTest::class.java)
        phoneListener = DualCallStateListener(this@DualCallTestActivity)
        phoneNumberEditText.setText(numberToDial)
        phoneNumberEditText.addTextChangedListener(mTextWatcher)
        callSim1.setOnClickListener(this)
        callSim2.setOnClickListener(this)
        getSimReaderTestStatus()
        if (Devices.isS10Available()) {
            proxStatusImageView?.visibility = View.VISIBLE
            proxStatusTextView?.visibility = View.VISIBLE
            proximitySensorManager = ProximitySensorManager(this, this)
            proximitySensorManager?.enable()
        }

    }

    override fun onResume() {
        super.onResume()
        telephonyManager.listen(phoneListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        if (!isSimAvailable()) {
            return
        }
        if (isCallConnected && BaseActivity.autoPerform && isNetworkConnectivity1) {
            checkTest()
        }
        if (isCallConnected && BaseActivity.autoPerform && isNetworkConnectivity2) {
            checkTest()
        }
        if (isCallConnected && BaseActivity.autoPerform && checkTest() == Test.PASS) {
            finalizeTest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (phoneNumberEditText != null) {
            phoneNumberEditText.removeTextChangedListener(mTextWatcher)
        }
        try {
            telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)
        } catch (e: NullPointerException) {
            Log.d("Call Activity", getString(R.string.call_listener_log))
        }
        phoneListener = null
        if (Devices.isS10Available()) {
            proximitySensorManager?.disable(false)
        }
    }

    private fun showDialouge(callSim: Int) {
        val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
        } else {
            AlertDialog.Builder(context)
        }
        builder.setCancelable(false)
        builder.setTitle(getString(R.string.callTest_nav_title))
                .setMessage(getString(R.string.s10proximitytext))
                .setPositiveButton(android.R.string.ok) { _, _ -> dialPhoneNumber(callSim) }
                .setIcon(android.R.drawable.ic_menu_call)

        if (!isFinishing) {
            val dialog = builder.show()
        }
    }

    private fun dialPhoneNumber(callSim: Int) {
        when (callSim) {

            R.id.callSim1 -> if (isSim1Present) {
                if (numberToDial.isEmpty()) {
                    Toast.makeText(this, getString(R.string.empty_call_text), Toast.LENGTH_SHORT).show()
                    return
                }
                val number = Uri.parse("tel:" + numberToDial.replace("#", "%23"))
                val intent = Intent(Intent.ACTION_CALL, number)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("com.android.phone.force.slot", true)
                intent.putExtra("Cdma_Supp", true)
                //Add all slots here, according to device.. (different device require different key so put all together)
                for (s in simSlotName) {
                    isNetworkConnectivity1 = true
                    intent.putExtra(s, 0) //0 or 1 according to sim.......
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.calltst_err), Toast.LENGTH_SHORT).show()
            }

            R.id.callSim2 -> if (isSim2Present) {
                if (numberToDial.isEmpty()) {
                    Toast.makeText(this, getString(R.string.empty_call_text), Toast.LENGTH_SHORT).show()
                    return
                }
                val number = Uri.parse("tel:" + numberToDial.replace("#", "%23"))
                val intent = Intent(Intent.ACTION_CALL, number)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("com.android.phone.force.slot", true)
                intent.putExtra("Cdma_Supp", true)
                //Add all slots here, according to device.. (different device require different key so put all together)
                for (s in simSlotName) {
                    intent.putExtra(s, 1) //0 or 1 according to sim.......
                    isNetworkConnectivity2 = true
                }

                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.calltst_err), Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(this, getString(R.string.calltst_err), Toast.LENGTH_SHORT).show()
            }
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
                CallActivity.CALL_SCREEN_TIME = Loader.TIME_VALUE
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

    private fun getSimReaderTestStatus() {
        val test = test
        if (test != null) {
            isSimAvailable()
            test.sub(Test.simReaderTestKey1)?.value = if (isSim1Present) Test.PASS else Test.FAILED
            test.sub(Test.simReaderTestKey2)?.value = if (isSim2Present) Test.PASS else Test.FAILED

            setImage(test.sub(Test.simReaderTestKey1), simStatusImageView1)
            setImage(test.sub(Test.simReaderTestKey2), simStatusImageView2)
            setImage(test.sub(Test.networkTestKey1), connectionStatusImageView1)
            setImage(test.sub(Test.networkTestKey2), connectionStatusImageView2)
            setImage(test.sub(Test.proximityTestKey), proxStatusImageView)
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

    private var isSim1Present: Boolean = false
    private var isSim2Present: Boolean = false
    private var isNetworkConnectivity1 = false
    private var isNetworkConnectivity2 = false

    private fun isSimAvailable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val sManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val infoSim1 = sManager.getActiveSubscriptionInfoForSimSlotIndex(0)
                val infoSim2 = sManager.getActiveSubscriptionInfoForSimSlotIndex(1)
                if (infoSim1 != null) {
                    isSim1Present = true
                    if (infoSim2 != null) {
                        isSim2Present = true
                    }
                    return true
                }
                if (infoSim2 != null) {
                    isSim2Present = true
                    return true
                }
            } catch (e: Exception) {
            }
        } else {
            try {
                val telephonyInfo = TelInfo.getInstance(this@DualCallTestActivity)
                isSim1Present = telephonyInfo.isSIM1Ready
                isSim2Present = telephonyInfo.isSIM2Ready
                if (isSim1Present) {
                    if (isSim2Present) {
                    }
                    return true
                }
                if (isSim2Present) {
                    if (isSim1Present) {
                    }
                    return true
                }
            } catch (e: Exception) {
            }
        }
        return false
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.callSim1 -> {
                if (test?.sub(Test.proximityTestKey)?.value == Test.PASS) {
                    dialPhoneNumber(R.id.callSim1)
                } else {
                    showDialouge(R.id.callSim1)
                }
                /*try {

                } catch (e: Exception) {
                    e.printStackTrace()
                    val test = test
                    if (test != null) {
                        test.sub(Test.networkTestKey)?.value = Test.FAILED
                        setImage(test.sub(Test.networkTestKey), connectionStatusImageView1)
                        s11.setImageResource(if (test.sub(Test.networkTestKey1)?.isPass ?: false) R.drawable.callworking else R.drawable.calldefault)
                    }
                }*/
            }
            R.id.callSim2 -> {
                if (test?.sub(Test.proximityTestKey)?.value == Test.PASS) {
                    dialPhoneNumber(R.id.callSim2)
                } else {
                    showDialouge(R.id.callSim2)
                }
            }
        }
    }

    fun onCallStateIdle() {
        val test = test
        if (!isCallConnected && test != null && isNetworkConnectivity1) {
            test.sub(Test.networkTestKey1)?.value = Test.FAILED
            setImage(test.sub(Test.networkTestKey1), connectionStatusImageView1)
        }
        if (!isCallConnected && test != null && isNetworkConnectivity2) {
            test.sub(Test.networkTestKey2)?.value = Test.FAILED
            setImage(test.sub(Test.networkTestKey2), connectionStatusImageView2)
        }
        Logy.i(TAG, "CALL_STATE_IDLE")
    }

    fun onCallStateOffHook() {
        isCallConnected = true
        val test = test
        if (test != null && isNetworkConnectivity1) {
            test.sub(Test.networkTestKey1)?.value = if (isCallConnected) Test.PASS else Test.FAILED
            setImage(test.sub(Test.networkTestKey1), connectionStatusImageView1)
        }
        if (test != null && isNetworkConnectivity2) {
            test.sub(Test.networkTestKey2)?.value = if (isCallConnected) Test.PASS else Test.FAILED
            setImage(test.sub(Test.networkTestKey2), connectionStatusImageView2)
        }
        Logy.i(TAG, "CALL_STATE_OFFHOOK")
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

    companion object {
        private val DEFAULT_NUMBER = "611"
        private val TAG = DualCallTestActivity::class.java.simpleName
    }
}
