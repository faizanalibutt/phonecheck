package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.Window
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.fragments.*
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.LCDTest
import com.upgenicsint.phonecheck.utils.DialogUtils
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.activity_touch_test.*
import java.util.*


class TouchTestActivity : DeviceTestableActivity<LCDTest>(), TestListener {

    private var digitizerFragment: TestFragment? = null
    private var currentApiVersion = android.os.Build.VERSION.SDK_INT
    private val customizations = Loader.instance.clientCustomization
    private val colorMap: MutableMap<Int, Int> = mutableMapOf()
    internal var brightLvl = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
    private var countDownTimerCallStarted: CountDownTimer? = null

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
        setContentView(R.layout.activity_touch_test)

        Loader.TIME_VALUE = 0
        LCD_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        Tools.setBrightness(window, 1f)
        test = Loader.instance.getByClassType(LCDTest::class.java)
        if (test != null) {


            if (customizations != null) {
                if (customizations.lCDColorWhite) {
                    colorMap.put(Color.WHITE, Test.INIT)
                }
                if (customizations.lCDColorRed) {
                    colorMap.put(Color.RED, Test.INIT)
                }
                if (customizations.lCDColorGreen) {
                    colorMap.put(Color.GREEN, Test.INIT)
                }
                if (customizations.lCDColorBlue) {
                    colorMap.put(Color.BLUE, Test.INIT)
                }
                if (customizations.LCDColorGray) {
                    //colorMap.put(Constants.GRAY, Test.INIT)
                }
                if (customizations.lCDColorBlack) {
                    colorMap.put(Color.BLACK, Test.INIT)
                }
            }

            if (colorMap.isEmpty()) {
                colorMap.put(Color.WHITE, Test.INIT)
            }

            if (!Loader.instance.isTestListLoaded) {
                supportFragmentManager.beginTransaction().add(R.id.container, GlassConditionFragment()).commit()
            }
            if (Loader.instance.isTestListLoaded && Loader.instance.filterContains("Glass Cracked")) {
                supportFragmentManager.beginTransaction().add(R.id.container, GlassConditionFragment()).commit()
            } else if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.BacklightTestKey)) {
//                backlitBrightness()
//                supportFragmentManager.beginTransaction().add(R.id.container, BrightnessFragment()).commit()
                val intent = Intent(this@TouchTestActivity, BrightnessActivity::class.java)
                startActivityForResult(intent, 13)
            } else if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.LCDTestKey)) {
                supportFragmentManager.beginTransaction().add(R.id.container, LCDFragment()).commit()
            }
//            else{
//                backlitBrightness()
//                supportFragmentManager.beginTransaction().add(R.id.container, LCDFragment.newInstance(colorMap.keys.first(), Color.BLUE)).commit()
//            }

//            val alertDialog = DialogUtils.createConfirmationAlert(context, R.string.glass_condition, R.string.is_glass_cracked, "No", "Yes", object : AlertButtonListener {
//                override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
//                    val test = test ?: return
//                    test.sub(Test.glassConditionTestKey)?.value = if (type == AlertButtonListener.ButtonType.LEFT) Test.PASS else Test.FAILED
//                    mainLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
//                    dialog.dismiss()
//                    supportFragmentManager
//                            .beginTransaction()
//                            .add(R.id.container, LCDFragment.newInstance(colorMap.keys.first(), Color.BLUE))
//                            .commit()
//
//                }
//            })
//            alertDialog.show();
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // check if the request code is same as what is passed  here it is 2
        if (requestCode == 13) {

            if (!Loader.instance.isTestListLoaded) {
                supportFragmentManager.beginTransaction().replace(R.id.container, LCDFragment()).commit()
            } else if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.LCDTestKey)) {
                supportFragmentManager.beginTransaction().replace(R.id.container, LCDFragment()).commit()
            } else {
                finalizeTest()
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
                LCD_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_lcd), -1)] =
                            RecordTest(context.getString(R.string.report_lcd_test), LCD_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("LCD", "${LCD_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    private fun backlitBrightness() {
        Tools.setBrightness(window, 0f)
        val alertDialog = DialogUtils.createConfirmationAlert(context, R.string.backlight_brightness, R.string.brightness_increase, context.getString(R.string.fail), context.getString(R.string.pass), object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                val test = test ?: return
                test.sub(Test.BacklightTestKey)?.value = if (type == AlertButtonListener.ButtonType.RIGHT) Test.PASS else Test.FAILED
                mainLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                if (Loader.instance.isTestListLoaded && !Loader.instance.filterContains(Test.LCDTestKey)) {
                    finalizeTest()
                } else {
                    supportFragmentManager.beginTransaction().add(R.id.container, LCDFragment()).commit()
                }
                dialog.dismiss()
            }
        })
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false

        val timerObj = Timer()
        val timerTaskObj = object : TimerTask() {
            override fun run() {
                for (i in brightLvl.indices) {
                    var bLvl = brightLvl[i]
                    Thread.sleep(300)
                    runOnUiThread {
                        Tools.setBrightness(window, bLvl)
                    }
                    if (i == 5) {
                        timerObj.cancel()
                        runOnUiThread {
                            Tools.setBrightness(window, 1f)
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                        }
                    }
                }
            }
        }
        timerObj.schedule(timerTaskObj, 0, 1000)
    }

    @SuppressLint("NewApi")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    /*override fun checkTest(): Int {
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
    }*/

    /* @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (digitizerFragment != null) {
            digitizerFragment.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }*/


    override fun onDone(fragment: Fragment, isPass: Boolean) {
        val test = test ?: return

        if (fragment is GlassConditionFragment) {
            test.sub(Test.glassConditionTestKey)?.value = if (isPass) Test.PASS else Test.FAILED

//            if(!Loader.instance.isTestListLoaded){
////                backlitBrightness()
//               // supportFragmentManager.beginTransaction().add(R.id.container, LCDFragment.newInstance(colorMap.keys.first(), Color.BLUE)).commit()
////                supportFragmentManager.beginTransaction().add(R.id.container, BrightnessFragment()).commit()
//                val intent = Intent(this@TouchTestActivity, BrightnessActivity::class.java)
//                startActivityForResult(intent, 13)
//            }

            if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.BacklightTestKey)) {
//                backlitBrightness()
//                supportFragmentManager.beginTransaction().add(R.id.container, BrightnessFragment()).commit()
                val intent = Intent(this@TouchTestActivity, BrightnessActivity::class.java)
                startActivityForResult(intent, 13)
            } else if ((Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.LCDTestKey)) || !Loader.instance.isTestListLoaded) {
//                if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.BacklightTestKey)){
//                    backlitBrightness()
//                }
                supportFragmentManager.beginTransaction().replace(R.id.container, LCDFragment()).commit()
            } else {
                finalizeTest()
            }
        }

        if (fragment is BrightnessFragment) {
            test.sub(Test.BacklightTestKey)?.value = if (isPass) Test.PASS else Test.FAILED

            if (!Loader.instance.isTestListLoaded) {
                supportFragmentManager.beginTransaction().replace(R.id.container, LCDFragment()).commit()
            } else if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.LCDTestKey)) {
                supportFragmentManager.beginTransaction().replace(R.id.container, LCDFragment()).commit()
            } else {
                finalizeTest()
            }
        }

        if (fragment is LCDFragment) {

            colorMap[fragment.color] = if (isPass) Test.PASS else Test.FAILED

            val nextColor = colorMap.entries.filter { it.value == Test.INIT }.firstOrNull()

            if (nextColor != null && nextColor.value == Test.INIT) {
                supportFragmentManager
                        .beginTransaction()
                        .remove(fragment)
                        .add(R.id.container, LCDFragment.newInstance(nextColor.key, Color.BLUE))
                        .commit()
            } else {

                test.sub(Test.LCDTestKey)?.value = if (colorMap.filter { it.value == Test.PASS }.count() == colorMap.count()) Test.PASS else Test.FAILED

                finalizeTest()
            }
        }
    }

    override fun onDone1(activity: Activity, isPass: Boolean) {

    }

    companion object {

        @JvmField
        val REQ = 4
        var LCD_SCREEN_TIME = 0
    }


}
