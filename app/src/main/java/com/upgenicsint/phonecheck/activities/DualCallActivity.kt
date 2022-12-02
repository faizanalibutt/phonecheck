package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import com.upgenicsint.phonecheck.R
import android.content.Intent
import android.net.Uri
import kotlinx.android.synthetic.main.call_layout.*
import android.text.Editable
import android.text.TextWatcher
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.misc.DualCallStateListener
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.DualCalTest
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.util.Log
import android.widget.Toast
import com.upgenicsint.phonecheck.misc.TelInfo
import android.os.BatteryManager



@SuppressLint("MissingPermission")
class DualCallActivity : DeviceTestableActivity<DualCalTest>(), View.OnClickListener {

    private val simSlotName = arrayOf("extra_asus_dial_use_dualsim", "com.android.phone.extra.slot",
            "slot", "simslot", "sim_slot", "subscription", "Subscription", "phone", "com.android.phone.DialingMode",
            "simSlot", "slot_id", "simId", "simnum", "phone_type", "slotId", "slotIdx")
    //Loader.instance.clientCustomization?.numberToDial ?:
    internal val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private var numberToDial = DEFAULT_NUMBER
    private var isCallConnected: Boolean = false
    internal var phoneListener: DualCallStateListener? = null
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

    override fun onResume() {
        super.onResume()
        val test = test ?: return
        telephonyManager.listen(phoneListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        if (!isSimAvailable()) {
            return
        }
        if (isCallConnected && BaseActivity.autoPerform && isTestCall1) {
            checkTest()
            //isTestCall1 = false
        }
        if (isCallConnected && BaseActivity.autoPerform && isTestCall2) {
            checkTest()
            //isTestCall2 = false
        }
        if (checkTest() == Test.PASS) {
            finalizeTest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isTestCall1 = false
        isTestCall2 = false
        if (phoneNumberText != null) {
            phoneNumberText.removeTextChangedListener(mTextWatcher)
        }
        try{
            telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)
        }
        catch(e: NullPointerException){
            Log.d("Call Activity", "Attempt to read from field java.lang.Integer android.telephony.PhoneStateListener.mSubId on a null object reference")
        }
        phoneListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_layout)
        registerListeners()
        onCreateNav()
        setNavTitle(getString(R.string.dualcallTest_nav_title))
        test = Loader.instance.getByClassType(DualCalTest::class.java)
        phoneListener = null//DualCallStateListener(this@DualCallActivity)
        phoneNumberText.text = numberToDial
        phoneNumberText.addTextChangedListener(mTextWatcher)

        val mBatteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        var avgCurrent: Long? = null
        var currentNow: Long? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            avgCurrent = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            currentNow = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }
        Log.d(TAG, "BATTERY_PROPERTY_CURRENT_AVERAGE = " + avgCurrent + "mAh")
        Log.d(TAG, "BATTERY_PROPERTY_CURRENT_NOW =  " + currentNow + "mAh")
    }

    private fun registerListeners() {
        tvAsterik.setOnClickListener(this)
        tvZero.setOnClickListener(this)
        tvHash.setOnClickListener(this)
        tvOne.setOnClickListener(this)
        tvTwo.setOnClickListener(this)
        tvThree.setOnClickListener(this)
        tvFour.setOnClickListener(this)
        tvFive.setOnClickListener(this)
        tvSix.setOnClickListener(this)
        tvSeven.setOnClickListener(this)
        tvEight.setOnClickListener(this)
        tvNine.setOnClickListener(this)
        ivBackspace.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tvAsterik -> updateDialedNumber("*")
            R.id.tvZero -> updateDialedNumber("0")
            R.id.tvHash -> updateDialedNumber("#")
            R.id.tvOne -> updateDialedNumber("1")
            R.id.tvTwo -> updateDialedNumber("2")
            R.id.tvThree -> updateDialedNumber("3")
            R.id.tvFour -> updateDialedNumber("4")
            R.id.tvFive -> updateDialedNumber("5")
            R.id.tvSix -> updateDialedNumber("6")
            R.id.tvSeven -> updateDialedNumber("7")
            R.id.tvEight -> updateDialedNumber("8")
            R.id.tvNine -> updateDialedNumber("9")
            R.id.ivBackspace -> removeLastDigit()
        }
    }

    private fun updateDialedNumber(number: String) {
        ivBackspace.visibility = View.VISIBLE
        phoneNumberText.text = phoneNumberText.text.toString() + number
    }

    private fun removeLastDigit() {
        var number = phoneNumberText.text.toString()
        number = number.substring(0, number.length - 1)
        phoneNumberText.text = number
        if (number.isEmpty()) {
            ivBackspace.visibility = View.INVISIBLE
        }
    }

    fun callSim1(view: View) {
        if (isSim1Present) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + numberToDial))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("com.android.phone.force.slot", true)
            intent.putExtra("Cdma_Supp", true)
            //Add all slots here, according to device.. (different device require different key so put all together)
            for (s in simSlotName) {
                isTestCall1 = true
                intent.putExtra(s, 0) //0 or 1 according to sim.......
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.calltst_err), Toast.LENGTH_SHORT).show()
        }
    }

    fun callSim2(view: View) {
        if (isSim2Present) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + numberToDial))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("com.android.phone.force.slot", true)
            intent.putExtra("Cdma_Supp", true)
            //Add all slots here, according to device.. (different device require different key so put all together)
            for (s in simSlotName) {
                intent.putExtra(s, 1) //0 or 1 according to sim.......
                isTestCall2 = true
            }

            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.calltst_err), Toast.LENGTH_SHORT).show()
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

    fun onCallStateIdle() {
        val test = test
        if (!isCallConnected && test != null && isTestCall1) {
            test.sub(Test.callTest1Key)?.value = Test.FAILED
            //isTestCall1 = false
        }
        if (!isCallConnected && test != null && isTestCall2) {
            test.sub(Test.callTest2Key)?.value = Test.FAILED
            //isTestCall2 = false
        }
        Logy.i(TAG, "CALL_STATE_IDLE")
    }

    fun onCallStateOffHook() {
        isCallConnected = true
        val test = test
        if (test != null && isTestCall1) {
            test.sub(Test.callTest1Key)?.value = if (isCallConnected) Test.PASS else Test.FAILED
            //isTestCall1 = false
        }
        if (test != null && isTestCall2) {
            test.sub(Test.callTest2Key)?.value = if (isCallConnected) Test.PASS else Test.FAILED
            //isTestCall2 = false
        }
        Logy.i(TAG, "CALL_STATE_OFFHOOK")
    }

    private var isSim1Present: Boolean = false
    private var isSim2Present: Boolean = false
    private var isTestCall1 = false
    private var isTestCall2 = false

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
                    /*if (infoSim1 != null) {
                        isSim1Present = true
                    }*/
                    return true
                }
            } catch (e: Exception) {}
        } else {
            /*val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (telephonyManager.simSerialNumber != null) {
                return true
            }*/
            try {
                val telephonyInfo = TelInfo.getInstance(this@DualCallActivity)
                isSim1Present = telephonyInfo.isSIM1Ready
                isSim2Present = telephonyInfo.isSIM2Ready
                if (isSim1Present) {
                    if (isSim2Present) {}
                    return true
                }
                if (isSim2Present) {
                    if (isSim1Present) {}
                    return true
                }
            } catch (e: Exception) {}
        }
        return false
    }

    companion object {
        private val ALLOW_MANUAL_CALL = false
        private val DEFAULT_NUMBER = "133"
        private val TAG = DualCallActivity::class.java.simpleName
    }
}
