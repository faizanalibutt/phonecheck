package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.view.MotionEvent
import android.view.View
import android.view.Window
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.fragments.*
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.DigitizerTest
import com.upgenicsint.phonecheck.utils.Tools
import java.util.*


class DigitizerActivity : DeviceTestableActivity<DigitizerTest>(), TestListener {
    override fun onDone1(activity: Activity, isPass: Boolean) {

    }

    var fragment: TestFragment? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        fragment?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // This work only for android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            window.decorView.systemUiVisibility = flags

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView = window.decorView
            decorView
                    .setOnSystemUiVisibilityChangeListener { visibility ->
                        if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                            decorView.systemUiVisibility = flags
                        }
                    }
        }
        setContentView(R.layout.activity_touch_test)

        Loader.TIME_VALUE = 0
        DIGITIZER_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        if (BuildConfig.DEBUG)
            Tools.setBrightness(window, 0.3f)
        else
            Tools.setBrightness(window, 1f)

        test = Loader.instance.getByClassType(DigitizerTest::class.java)
        if (test != null) {

            if (customizations != null && customizations.fullScreenBubbleTest) {
                fragment = FullScreenBubbleFragment()
            } else if (customizations != null && customizations.GuidedDigi) {
                fragment = GuidedDigitizerFragment.newInstance(false)
            } else {
                fragment = DigitizerFragment.newInstance(false)
            }

            if (fragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.container, fragment!!).commit()
            }

        }

    }


    @SuppressLint("NewApi")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }


    override fun onDone(fragment: Fragment, isPass: Boolean) {

        val test = test ?: return

        if (fragment is DigitizerFragment || fragment is FullScreenBubbleFragment || fragment is GuidedDigitizerFragment) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
            test.status = if (isPass) Test.PASS else Test.FAILED
            finalizeTest()
            /*LAST TEST IN THIS ACTIVITY SO FINISHING ACTIVITY AND SETTING RESULT*/

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
                DIGITIZER_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_digi), -1)] =
                            RecordTest(context.getString(R.string.report_digitizer_test), DIGITIZER_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("Digitizer", "${DIGITIZER_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    companion object {
        @JvmField
        var DIGITIZER_SCREEN_TIME = 0
        @JvmField
        val REQ = 4
    }

    private val customizations = Loader.instance.clientCustomization


}