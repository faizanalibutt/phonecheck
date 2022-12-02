package com.upgenicsint.phonecheck.activities

import android.content.Context
import android.os.Bundle
import android.view.View
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.ProximitySensorManager
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.sensor.ProximityTest
import java.util.*

class ProximityActivity : DeviceTestableActivity<ProximityTest>(), ProximitySensorManager.Listener {

    private var proximitySensorManager: ProximitySensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proximity)
        onCreateNav()
        setNavTitle("Proximity Sensor Test")
        Logy.setEnable(BuildConfig.DEBUG)
        test = Loader.instance.getByClassType(ProximityTest::class.java)
        proximitySensorManager = ProximitySensorManager(this, this)
        proximitySensorManager?.enable()

        Loader.TIME_VALUE = 0
        PROXIMITY_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                    //timerStatus!!.text = "value is: ${Loader.TIME_VALUE}"
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)
    }

    override fun onPause() {
        super.onPause()
        proximitySensorManager?.disable(false)
    }

    override fun onNear() {
        if (ButtonsTestActivity.isButtonTestOpen) {
            return
        }
        test!!.status = Test.PASS
        finalizeTest()
    }

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }

    override fun onFar() {

    }

    override fun onDestroy() {
        super.onDestroy()
        proximitySensorManager?.disable(false)
        //closeTimerTest()
    }

    override fun closeTimerTest() {
        super.closeTimerTest()
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                PROXIMITY_SCREEN_TIME = Loader.TIME_VALUE
                if (test != null) {
                    try {
                        val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                        Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_proximity), -1)] =
                                RecordTest(context.getString(R.string.report_proximity_test), PROXIMITY_SCREEN_TIME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Loader.RECORD_TESTS_TIME.put(context.getString(R.string.report_proximity_test), "${PROXIMITY_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {
            ignored.printStackTrace()
        }
    }

    override fun onNavDoneClick(v: View) {
        super.onNavDoneClick(v)
        if (test != null && test!!.status != Test.PASS) {
            test!!.status = Test.FAILED
        }
    }

    companion object {
        val REQ = 2786
        var PROXIMITY_SCREEN_TIME = 0
    }
}
