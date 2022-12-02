package com.upgenicsint.phonecheck.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import android.widget.Toast
import com.farhanahmed.cabinet.Cabinet
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.fragments.DigitizerFragment
import com.upgenicsint.phonecheck.fragments.SpenHoverFragment
import com.upgenicsint.phonecheck.fragments.SpenRemoveFragment
import com.upgenicsint.phonecheck.fragments.TestListener
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.SPenTest
import java.util.*

class SPenActivity : DeviceTestableActivity<SPenTest>(), TestListener {
    private val pref by lazy { Cabinet.open(context, R.string.spen_buttons_pref) }

    override fun onDone1(activity: Activity, isPass: Boolean) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentApiVersion = Build.VERSION.SDK_INT
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
            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    decorView.systemUiVisibility = flags
                }
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spen)

        Loader.TIME_VALUE = 0
        SPENTEST_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)
        
        onCreateNav()
        test = Loader.instance.getByClassType(SPenTest::class.java)

        val test = test ?: return
        test.isHovered = false
        test.isRemoved = test.isHovered
        //autoPerformMode.setText(autoPerform ? "In AutoPerform Mode" : "In Manual Mode");

        if(Build.MODEL.containsIgnoreCase("SM-T827V") && Build.MANUFACTURER.containsIgnoreCase("samsung")){
            test.isRemoved = true
            supportFragmentManager.beginTransaction().replace(R.id.container, SpenHoverFragment()).commit()
        }
        else{
            supportFragmentManager.beginTransaction().replace(R.id.container, SpenRemoveFragment()).commit()
        }
    }
    override fun onDone(fragment: Fragment, isPass: Boolean) {

        val test = test ?: return


        if (fragment is SpenRemoveFragment && isPass) {
            test.isRemoved = true
            Toast.makeText(context, getString(R.string.spen_remove_test), Toast.LENGTH_SHORT).show()

            supportFragmentManager.beginTransaction().replace(R.id.container, SpenHoverFragment()).commit()

        } else if (fragment is SpenHoverFragment) {
            if (isPass) {
                supportFragmentManager.beginTransaction().replace(R.id.container, DigitizerFragment.newInstance(true)).commitAllowingStateLoss()
                Toast.makeText(context, getString(R.string.spen_pop_bubles), Toast.LENGTH_LONG).show()
            } else {
                //the code below never gonna happen.
                test.isHovered = true

                if (checkTest() == Test.PASS) {
                    finalizeTest()
                }
            }

        } else if (fragment is DigitizerFragment) {

            supportFragmentManager.beginTransaction().remove(fragment).commit()

            test.isHovered = isPass

//            supportFragmentManager.beginTransaction().replace(R.id.container, SpenButtonFragment()).commit()

            if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.spenButtonTestKey)) {
                val intent = Intent(this@SPenActivity, SpenButtonsTestActivity::class.java)
                SpenButtonsTestActivity.spenBackPressed = false
                SpenButtonsTestActivity.spenMenuPressed = false
                startActivityForResult(intent, 20)
                closeTimerTest()
            }
            else {
                finalizeTest()
            }
        }
//        else if (fragment is SpenButtonFragment){
//            supportFragmentManager.beginTransaction().remove(fragment).commit()
//
//            finalizeTest()
//        }
        else {
            finalizeTest()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 20){
            if (SpenButtonsTestActivity.spenMenuPressed){
                SPenTest.isMenuPressed = true
            }
            else {
                SPenTest.isMenuPressed = false
            }
            if (SpenButtonsTestActivity.spenBackPressed){
                SPenTest.isBackPressed = true
            }
            else {
                SPenTest.isBackPressed = false
            }
//            if (checkTest() == Test.PASS) {
//                finalizeTest()
//            }
//            else {
//                finalizeTest()
//            }
            finalizeTest()
        }
    }

    override fun checkTest(): Int {
        val test = test ?: return Test.INIT
        if (Loader.instance.isTestListLoaded && Loader.instance.filterContains(Test.spenButtonTestKey)) {
            test.status = if (test.isHovered && test.isRemoved && SPenTest.isMenuPressed && SPenTest.isBackPressed) Test.PASS else Test.FAILED
        }
        else {
            test.status = if (test.isHovered && test.isRemoved) Test.PASS else Test.FAILED
        }
        return test.status
    }


    override fun onDestroy() {
        super.onDestroy()

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
                SPENTEST_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_spen), -1)] = RecordTest(context.getString(R.string.report_spen_test), SPENTEST_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("SPENTEST", "${SPENTEST_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    private fun failTest() {
        setResult(Activity.RESULT_CANCELED, Intent())
        finish()
    }

    companion object {

        val TAG = "SPENActivity"
        var SPENTEST_SCREEN_TIME = 0
        val REQ = 6
    }


}
