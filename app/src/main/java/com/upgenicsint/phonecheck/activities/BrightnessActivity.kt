package com.upgenicsint.phonecheck.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.LCDTest
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.lcd_display_fragment.*
import java.util.*

class BrightnessActivity : AppCompatActivity() {

    internal var brightLvlLowToHigh = floatArrayOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0f)
    var timerObj: Timer? = null
    private var currentApiVersion = android.os.Build.VERSION.SDK_INT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

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
        setContentView(R.layout.activity_brightness)

        /*Loader.TIME_VALUE = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)*/

        brightnessAdjustment()

        failBtn.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
            Loader.instance.getByClassType(LCDTest::class.java)!!.sub(Test.BacklightTestKey)!!.value = Test.FAILED
        }
        passBtn.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            val returnIntent = Intent()
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
            Loader.instance.getByClassType(LCDTest::class.java)!!.sub(Test.BacklightTestKey)!!.value = Test.PASS
        }

        failBtn2.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
            //closeTimerTest()
            Loader.instance.getByClassType(LCDTest::class.java)!!.sub(Test.BacklightTestKey)!!.value = Test.FAILED
        }

        passBtn2.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            //closeTimerTest()
            val returnIntent = Intent()
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
            Loader.instance.getByClassType(LCDTest::class.java)!!.sub(Test.BacklightTestKey)!!.value = Test.PASS
        }

        failBtn3.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
            //closeTimerTest()
            Loader.instance.getByClassType(LCDTest::class.java)!!.sub(Test.BacklightTestKey)!!.value = Test.FAILED
        }

        passBtn3.setOnClickListener {
            timerObj!!.cancel()
            timerObj!!.purge()
            //closeTimerTest()
            val returnIntent = Intent()
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
            Loader.instance.getByClassType(LCDTest::class.java)!!.sub(Test.BacklightTestKey)!!.value = Test.PASS
        }
    }

    private fun closeTimerTest() {
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                BRIGHTNESS_SCREEN_TIME = Loader.TIME_VALUE
                Loader.RECORD_TESTS_TIME.put("Digitizer", "${BRIGHTNESS_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    private fun brightnessAdjustment() {
        timerObj = Timer()
        val timerTaskObj = object : TimerTask() {
            override fun run() {
                for (i in brightLvlLowToHigh) {
                    Thread.sleep(150)
                    runOnUiThread{
                        Tools.setBrightness(window, i)
                    }
                }
            }
        }
        timerObj!!.schedule(timerTaskObj, 0, 100)
    }

    companion object {
        var BRIGHTNESS_SCREEN_TIME = 0
    }
}
