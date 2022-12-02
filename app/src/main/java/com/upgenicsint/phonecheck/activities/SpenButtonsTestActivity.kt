package com.upgenicsint.phonecheck.activities

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import android.widget.Toast
import com.farhanahmed.cabinet.Cabinet
import com.samsung.android.sdk.pen.engine.SpenPenDetachmentListener
import com.samsung.android.sdk.pen.engine.SpenSimpleSurfaceView
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.ButtonsAdapter
import com.upgenicsint.phonecheck.fragments.SpenRemoveFragment
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.HomeWatcher
import com.upgenicsint.phonecheck.misc.SpenHomeWatcher
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.SPenTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_spen_buttons_test.*
import java.lang.ref.WeakReference
import java.util.*

class SpenButtonsTestActivity : DeviceTestableActivity<SPenTest>() {

    internal var mHomeWatcher: HomeWatcher? = null
    private val listOfKeyPressed = HashMap<String, String>()
    private var ignoreHomeFirstEvent = false
    private var ignoreActivityFocus: Boolean = false
    private var countDownTimerCallStarted: CountDownTimer? = null
    internal var spenSimpleSurfaceView: SpenSimpleSurfaceView? = null
    private var x: Boolean = false

    private val pref by lazy { Cabinet.open(context, R.string.spen_buttons_pref) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_spen_buttons_test)
        onCreateNav()
        spenSimpleSurfaceView = SpenSimpleSurfaceView(context)

        test = Loader.instance.getByClassType(SPenTest::class.java)

        Loader.TIME_VALUE = 0
        SPENBUTTON_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

//        if (test == null) {
//            finalizeTest()
//            return
//        }
        recyclerView_spenButton.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val test = test
        if (test != null) {
            val adapter = ButtonsAdapter(context, test.hardwareTestArrayList2)
            recyclerView_spenButton.adapter = adapter
        }

        spenBackPressed = pref.getBoolean(spenBackPressedPref, false)
        spenMenuPressed = pref.getBoolean(spenMenuPressedPref, false)

        mHomeWatcher = HomeWatcher(context)
        mHomeWatcher?.onHomePressedListener = object : HomeWatcher.OnHomePressedListener{
            override fun onEventOccurred(eventString: String) {

            }

            override fun onHomePressed() {

            }

            override fun onRecentPressed() {

                FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu Event").child("is Fired").setValue("Fired")
                /*
                * have hasPermanentMenuKey but if it dont have KeyEvent.KEYCODE_MENU or KeyEvent.KEYCODE_APP_SWITCH so ignore it here.
                */
                if (ViewConfiguration.get(context).hasPermanentMenuKey() && (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_APP_SWITCH) || !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_MENU))) {
                    FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu Event").child("Check 1").setValue("Called")

                    return
                }
                performTest(KeyEvent.KEYCODE_MENU)
            }

        }
        countDownTimerCallStarted = object : CountDownTimer(7000, 500){
            override fun onFinish() {
                if ((!spenMenuPressed && !spenBackPressed) || !spenDetached){
                    onNavDoneSpen()
                }
                else if (!spenMenuPressed && spenDetached) {
                    onNavDoneSpen()
                }
                else if (!spenBackPressed && spenDetached) {
                    onNavDoneSpen()
                }

//                else if ((!spenMenuPressed && !spenBackPressed) || spenDetached){
//                    onNavDoneSpen()
//                }
            }

            override fun onTick(millisUntilFinished: Long) {
//                Toast.makeText(this@SpenButtonsTestActivity, "Ticked", Toast.LENGTH_SHORT).show()

            }
        }
        countDownTimerCallStarted?.start()

        spenSimpleSurfaceView?.setPenDetachmentListener(SpenButtonsTestActivity.CustomSpenPenDetachmentListener(this))
    }

    private fun onNavDoneSpen() {
//        if (checkTest() == Test.PASS) {
//            finalizeTest()
//        else {
            showDoneAlert(object : AlertButtonListener {
                override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                    if (type == AlertButtonListener.ButtonType.RIGHT) {

                        val test = test
                        if (test!=null)
                        {
                            test.status = Test.FAILED

                            if (test.hasSubTest) {
                                val predicate: (Map.Entry<String, SubTest>) -> Boolean = {

                                    if (test.resultsFilterMap.containsKey(it.key)) {
                                        test.resultsFilterMap[it.key] == true
                                    } else {
                                        true
                                    }

                                }
                                test.subTests.filter(predicate).forEach {
                                    if (it.value.value == Test.INIT)
                                    {
                                        it.value.value = Test.FAILED
                                    }
                                }
                            }
                        }
                        closeTimerTest()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    if (type == AlertButtonListener.ButtonType.LEFT) {
                        countDownTimerCallStarted?.start()
                    }
                    dialog.dismiss()
                }
            })
//        }
    }

    internal class CustomSpenPenDetachmentListener(spenButtonsTestActivity: SpenButtonsTestActivity) : SpenPenDetachmentListener {
        override fun onDetached(b: Boolean) {
            if (!b){
                spenDetached = true
            }
            else {
                spenDetached = false
            }
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        /*
         * activity is out of focus so we should ignore first event for home button
         */
        if (!hasFocus && !ignoreActivityFocus) {
            ignoreHomeFirstEvent = true
        }
    }

    override fun onStart() {
        super.onStart()
        mHomeWatcher?.startWatch()

    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        Log.d(SpenButtonsTestActivity.TAG, "dispatchKeyEvent " + event.keyCode)

//        var bix = isBixbyPressed
//        if (ret == true){
//            if (event.keyCode == KeyEvent.KEYCODE_BACK && isBixbyPressed){
//                isBixbyPressed = false
//                return false
//            }
//        }
        listOfKeyPressed.put("Key " + event.keyCode, if (event.action == KeyEvent.ACTION_DOWN) "ACTION_DOWN" else "" + event.action)


        return if (event.action == KeyEvent.ACTION_DOWN) {
            if (SpenButtonsTestActivity.isBixbyPressed){
                SpenButtonsTestActivity.isBixbyPressed = false
                return false
            }
            else{
                performTest(event.keyCode)
            }
        } else super.dispatchKeyEvent(event)

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
                SPENBUTTON_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_spenbuttons), -1)] =
                            RecordTest(context.getString(R.string.report_spenbutton_test), SPENBUTTON_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("SPENBUTTON", "${SPENBUTTON_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onDestroy() {
        mHomeWatcher?.stopWatch()
        spenSimpleSurfaceView?.setPenDetachmentListener(null)
        spenSimpleSurfaceView?.close()
        super.onDestroy()
    }

    fun performTest(keyCode: Int): Boolean {

        val test = test ?: return false
        val hardwareTest = test.getKeyForKeyCode(keyCode)

        if (hardwareTest != null && hardwareTest.subTest.value != Test.PASS) {
            hardwareTest.subTest.value = Test.PASS
            val position = test.hardwareTestArrayList2.indexOf(hardwareTest)
            recyclerView_spenButton.adapter!!.notifyItemChanged(position)

            if(keyCode == 82){
                spenMenuPressed = true
            }
            else if(keyCode == 4){
                spenBackPressed = true
            }
            else {
                spenMenuPressed = false
                spenBackPressed = false
            }
            setSharedPrefs()
            if (spenBackPressed && spenMenuPressed && MainActivity.auto_start_mode){
                closeTimerTest()
                finish()
            }
//             if (spenBackPressed && spenMenuPressed && MainActivity.auto_start_mode){
//                 finish()
//             }

//            if (spenMenuPressed && spenBackPressed){
//                finalizeTest()
//            }

//            if (checkTest() == Test.PASS) {
//                if (BaseActivity.autoPerform) {
//                    finalizeTest()
//                }
//            }

            return true
        }
        return false
    }

    private fun setSharedPrefs() {
        pref.add(spenBackPressedPref, spenBackPressed)
        pref.add(spenMenuPressedPref, spenMenuPressed)
        pref.save()
    }

    companion object {
        @JvmField var isBixbyPressed = false
        private val TAG = SpenButtonsTestActivity::class.java.simpleName
        var spenDetached = true
        var spenBackPressed = false
        var spenMenuPressed = false
        var SPENBUTTON_SCREEN_TIME = 0
        var spenBackPressedPref = "Spen_BackPressed_Pref"
        var spenMenuPressedPref = "Spen_MenuPressed_Pref"
        val REQ = 5
    }
}
