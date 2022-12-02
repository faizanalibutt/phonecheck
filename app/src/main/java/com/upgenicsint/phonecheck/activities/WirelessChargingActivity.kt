package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.*
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.Cabinet
import com.upgenicsint.phonecheck.BuildConfig

import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.BatteryInfo
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.WirelessTest
import kotlinx.android.synthetic.main.activity_wireless_charging.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class WirelessChargingActivity : DeviceTestableActivity<WirelessTest>() {

    //var thread: CountDownTimer? = null // to start test while power cable plugged in
    private var prefEditor: SharedPreferences.Editor? = null
    private var pref: SharedPreferences? = null

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wireless_charging)
        // setting Nav Bar adding test
        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("Wireless Charging")

        Loader.TIME_VALUE = 0
        WIRELESS_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        val listener = CameraTestClickListener()
        failBtn.setOnClickListener(listener)
        passBtn.setOnClickListener(listener)
        tapToStart.setOnClickListener(listener)

        prefEditor = getSharedPreferences(getString(R.string.wireless_battery_charging), MODE_PRIVATE).edit()
        pref = getSharedPreferences(getString(R.string.wireless_battery_charging), Context.MODE_PRIVATE)
        val battery = BatteryInfo(this)
        battery.logBatteryInformation()

        val wirelessFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(wirelessReceiver, wirelessFilter)

        test = Loader.instance.getByClassType(WirelessTest::class.java)
    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(wirelessChargeReceiver, IntentFilter(Constants.WIRELESS))
        val alreadyStarted = pref!!.getBoolean(WIRELESS_STATUS, false)
        if (!alreadyStarted) {
            testWirelessCharging()
        }
        val status = pref!!.getBoolean(WIRELESS_STATUS_CHARGING, false)
        if (!status) {
            wirelessStatus.setImageResource(getImageForStatus(getStatus()))
        } else {
            wirelessStatus.setImageResource(getImageForStatus(getStatus()))
        }
        super.onResume()
    }

    private fun testWirelessCharging() {
        if (BatteryDiagnosticActivity.isConnected(this@WirelessChargingActivity)) {
            if (isConnected(this)) {
                passTest()
                return
            }
        } else if (isConnected(this)) {
            passTest()
        }
    }

    private fun initTest() {
        prefEditor!!.clear().apply()
        prefEditor!!.putBoolean(WIRELESS_STATUS, false)
        prefEditor!!.putBoolean(WIRELESS_STATUS_CHARGING, false)
        prefEditor!!.commit()
        setStatus(Test.INIT)
        wirelessStatus?.setImageResource(getImageForStatus(getStatus()))
    }

    private fun passTest() {
        setStatus(Test.PASS)
        wirelessStatus?.setImageResource(getImageForStatus(getStatus()))
        prefEditor!!.putBoolean(WIRELESS_STATUS, true)
        prefEditor!!.putBoolean(WIRELESS_STATUS_CHARGING, true)
        prefEditor!!.commit()
        finalizeTest()
    }

    private fun failTest() {
        setStatus(Test.FAILED)
        wirelessStatus?.setImageResource(getImageForStatus(getStatus()))
        prefEditor!!.putBoolean(WIRELESS_STATUS_CHARGING, false)
        prefEditor!!.commit()
    }

    private fun setStatus(status: Int) {
        test!!.status = status
    }

    private fun getStatus(): Int {
        return test!!.status
    }

    override fun onNavDoneClick(v: View) {
        super.onNavDoneClick(v)
        if (test != null && getStatus() != Test.PASS) {
            failTest()
        }
    }

    private var wirelessChargeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action != null && action == Constants.WIRELESS && BatteryDiagnosticActivity.isConnected(this@WirelessChargingActivity)) {
                if (isConnected(this@WirelessChargingActivity)) {
                    passTest()
                    return
                }
            } else if (action != null && action == Constants.WIRELESS && isConnected(this@WirelessChargingActivity)) {
                passTest()
            }
        }
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wirelessChargeReceiver)
        super.onStop()
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
                WIRELESS_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_wireless), -1)] =
                            RecordTest(context.getString(R.string.report_wireless_test), WIRELESS_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("Wireless Charging", "${WIRELESS_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wirelessChargeReceiver)
        unregisterReceiver(wirelessReceiver)
        //closeTimerTest()
        super.onDestroy()
    }

    private val wirelessReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val resource = intent?.getIntExtra("plugged", -1)
            if (action != null && resource == BatteryManager.BATTERY_PLUGGED_WIRELESS && BatteryDiagnosticActivity.isConnected(this@WirelessChargingActivity)) {
                if (isConnected(this@WirelessChargingActivity)) {
                    passTest()
                    return
                }
            } else if (action != null && resource == BatteryManager.BATTERY_PLUGGED_WIRELESS && isConnected(this@WirelessChargingActivity)) {
                passTest()
            }
        }
    }

    private inner class CameraTestClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val test = test ?: return

            if (v.id == R.id.passBtn) {
                passTest()
            } else if (v.id == R.id.failBtn) {
                failTest()
                finalizeTest()
            } else if (v.id == R.id.tapToStart) {
                initTest()
            }
        }
    }

    companion object {
        val TAG = "WirelessActivity"
        @JvmStatic
        fun isConnected(context: Context): Boolean {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS || (isCharging && plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS)
        }
        val WIRELESS_STATUS = "WIRELESS_STATUS_PREF"
        val WIRELESS_STATUS_CHARGING = "wireless_status"
        var WIRELESS_SCREEN_TIME = 0
        val REQ = 7786
    }
}
