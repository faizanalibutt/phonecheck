package com.upgenicsint.phonecheck.activities

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.ButtonsAdapter
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.misc.HomeWatcher
import com.upgenicsint.phonecheck.misc.ProximitySensorManager
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.ButtonTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import com.upgenicsint.phonecheck.utils.Utils
import kotlinx.android.synthetic.main.activity_button_test.*
import java.util.*

class ButtonsTestActivity : DeviceTestableActivity<ButtonTest>(), ProximitySensorManager.Listener {

    internal var mHomeWatcher: HomeWatcher? = null
    internal var powerOffReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            isUserNavigating = false
            powerPressed = true
            performTest(KeyEvent.KEYCODE_POWER)
        }
    }
    var ret = false

    private var ignoreHomeFirstEvent = false
    private var ignoreActivityFocus: Boolean = false

    private var powerServiceIntent: Intent? = null

    private var bixlightServiceId: String? = null

    private val listOfKeyPressed = HashMap<String, String>()

    private var proximitySensorManager: ProximitySensorManager? = null

    var timer: Timer? = null
    internal var myTimerTask: MyTimerTask? = null
    private var userLeaveCalled = false
    private var powerPressed = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button_test)
//        powerServiceIntent = Intent(context, PowerListenerService::class.java)
//        startService(powerServiceIntent)
        Logy.setEnable(BuildConfig.DEBUG)
        onCreateNav()
        isButtonTestOpen = true
        proximitySensorManager = ProximitySensorManager(context, this)
        proximitySensorManager?.disable(false)

        test = Loader.instance.getByClassType(ButtonTest::class.java)
        if (test == null) {
            finalizeTest()
            return
        }

        Loader.TIME_VALUE = 0
        BUTTON_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        //ListSelector = test.ListSelector;
        //ListSelector.clear();
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val test = test
        if (test != null) {
            val adapter = ButtonsAdapter(context, test.hardwareTestArrayList)
            recyclerView.adapter = adapter
        }

//        val keyguardManager = getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
//        val lock = keyguardManager.newKeyguardLock(Context.KEYGUARD_SERVICE)
//        lock.disableKeyguard()

        mHomeWatcher = HomeWatcher(context)
        mHomeWatcher?.onHomePressedListener = object : HomeWatcher.OnHomePressedListener {
            override fun onEventOccurred(eventString: String) {

            }

            override fun onHomePressed() {
                /*
                On android 7+ if menu/recent key is pressed and then going back to app from recants it causes Home button to automatically pass.
                so i ignore first home event here.
                 */
                FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Home Event").child("is Fired").setValue("Fired")

//                if (ignoreHomeFirstEvent && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
//                    ignoreActivityFocus = true // don't check for activity focus
//                    ignoreHomeFirstEvent = false // we just ignored first event now start listen again.
//                    return
//                }
                performTest(KeyEvent.KEYCODE_HOME)

            }

            override fun onRecentPressed() {

                FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu Event").child("is Fired").setValue("Fired")
                /*
                * have hasPermanentMenuKey but if it dont have KeyEvent.KEYCODE_MENU or KeyEvent.KEYCODE_APP_SWITCH so ignore it here.
                */
                if (ViewConfiguration.get(context).hasPermanentMenuKey() && !Build.MODEL.containsIgnoreCase("LG-H700") && (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_APP_SWITCH) || !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_MENU))) {
                    FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu Event").child("Check 1").setValue("Called")

                    return
                }

                performTest(KeyEvent.KEYCODE_MENU)

            }
        }
    }

    private fun showSnackBar() {
        val snackbar = Snackbar
                .make(findViewById(R.id.recyclerView), "Please enable bixby phonecheck service", Snackbar.LENGTH_INDEFINITE)
                .setAction("Enable", object : View.OnClickListener {
                    override fun onClick(view: View) {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                })
        snackbar.show()
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

    override fun onUserLeaveHint() {
//        if (isUserNavigating) {
////            val intent2 = Intent(this@ButtonsTestActivity, ButtonsTestActivity::class.java)
//////            intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
////            intent2.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
////
////            forceHome(this, intent2)
//
//            if (timer == null) {
//                myTimerTask = MyTimerTask()
//                timer = Timer()
//                timer!!.schedule(myTimerTask, 10, 10)
//            }
//        }

        if (menuPressed) {
            userLeaveCalled = true
        }

        if (timer == null) {
            myTimerTask = MyTimerTask()
            timer = Timer()
            timer!!.schedule(myTimerTask, 10, 1000)
        }
        super.onUserLeaveHint()
    }

    @SuppressLint("WrongConstant")
    fun forceHome(paramContext: Context, paramIntent: Intent?) {

        if (paramIntent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    (paramContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExact(AlarmManager.RTC,
                            500,
                            PendingIntent.getActivity(paramContext, 0, paramIntent, 0))
                }
                else {
                    (paramContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC,
                            500,
                            PendingIntent.getActivity(paramContext, 0, paramIntent, 0))
                }
            }
            catch (e : Exception) {

            }
        }
    }


    override fun onStart() {
        super.onStart()
        val key = Utils.samsungBixbyModelCode
        mHomeWatcher?.startWatch()

    }

    override fun onResume() {
        super.onResume()
//        isBixEnabled()
        if (powerOffReceiver != null) {
            val screenStateFilter = IntentFilter()
            screenStateFilter.addAction("PHONE_CHECK_POWER_OFF")
            LocalBroadcastManager.getInstance(applicationContext)
                    .registerReceiver(powerOffReceiver!!, screenStateFilter)
        }

        if (timer != null) {
            timer!!.cancel()
            timer = null
        }

        if (isUserNavigating) {
            if (checkTest() == Test.PASS && menuPressed) {
                if (BaseActivity.autoPerform || Devices.isHomePressed()) {
                    finalizeTest()
                }
            }
            else if (checkTest() == Test.PASS && homePressed) {
                if (BaseActivity.autoPerform) {
                    finalizeTest()
                }
            }
        }
        if (!powerPressed) {
            if (homePressed && menuPressed) {
                isUserNavigating = false
            }
        }
    }

    private fun isBixEnabled() {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (accessibilityManager != null) {
            val runningServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            for (service in runningServices) {
                if (service.id.equals(bixlightServiceId)) {
                    ret = true
                    break
                }
            }
            if(ret == false){
                if(Build.MANUFACTURER.containsIgnoreCase("samsung")){
                    if(KeyCharacterMap.deviceHasKey(1082)) {
                        showSnackBar()
                    }
                }
            }
        }
        Log.v(TAG, "isBixlightEnabled = " + ret)
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
                BUTTON_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_button), -1)] =
                            RecordTest(context.getString(R.string.report_button_test), BUTTON_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put(getString(R.string.report_button_test), "${BUTTON_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onDestroy() {
        super.onDestroy()
        isButtonTestOpen = false
        FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("KeyCodes").setValue(listOfKeyPressed)
        mHomeWatcher?.stopWatch()

        if (powerOffReceiver != null) {
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(powerOffReceiver!!)
        }

//        if (powerServiceIntent != null) {
//            stopService(powerServiceIntent)
//        }
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        isUserNavigating = false
        userLeaveCalled = false
        homePressed = false
        menuPressed = false
        powerPressed = false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        Log.d(TAG, "dispatchKeyEvent " + event.keyCode)

//        var bix = isBixbyPressed
//        if (ret == true){
//            if (event.keyCode == KeyEvent.KEYCODE_BACK && isBixbyPressed){
//                isBixbyPressed = false
//                return false
//            }
//        }
        listOfKeyPressed.put("Key " + event.keyCode, if (event.action == KeyEvent.ACTION_DOWN) "ACTION_DOWN" else "" + event.action)


        return if (event.action == KeyEvent.ACTION_DOWN) {
            if (isBixbyPressed){
                    isBixbyPressed = false
                    return false
            }
            else{
                var testPerformed = performTest(event.keyCode)
                if (checkTest() == Test.PASS && isUserNavigating) {
                    onUserLeaveHint()
                }
                testPerformed
            }
        } else super.dispatchKeyEvent(event)

    }


    fun performTest(keyCode: Int): Boolean {
        if (keyCode == 82) {
            menuPressed = true
            isUserNavigating = true
        } else if (keyCode == 3) {
            homePressed = true
            isUserNavigating = true
        } else if (Devices.isHomePressed() && keyCode == 26) {
            if (checkTest() != Test.PASS) {
                bringApplicationToFront()
            }
        }

        val test = test ?: return false
        val hardwareTest = test.getKeyForKeyCode(keyCode)

        if (hardwareTest != null && hardwareTest.subTest.value != Test.PASS) {
            hardwareTest.subTest.value = Test.PASS
            val position = test.hardwareTestArrayList.indexOf(hardwareTest)
            recyclerView.adapter!!.notifyItemChanged(position)

            if (!isUserNavigating) {
                if (checkTest() == Test.PASS) {
                    if (BaseActivity.autoPerform) {
                        finalizeTest()
                    }
                }
            }
            if (Devices.isHomePressed()) {
                if (checkTest() == Test.PASS) {
                    if (BaseActivity.autoPerform) {
                        finalizeTest()
                    }
                }
            }
//            else if (!userLeaveCalled) {
//                if (checkTest() == Test.PASS) {
//                    if (BaseActivity.autoPerform) {
//                        finalizeTest()
//                    }
//                }
//            }
            return true
        }
        return false
    }


    internal inner class MyTimerTask : TimerTask() {
        override fun run() {
            bringApplicationToFront()
        }
    }

    private fun bringApplicationToFront() {
        Log.d("TAG", "====Bringging Application to Front====")

        val notificationIntent = Intent(this, ButtonsTestActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
        }
    }

    /**
     * to disable proximity while we are in button test
     */
    override fun onNear() {

    }

    override fun onFar() {

    }

    companion object {
        @JvmField var isBixbyPressed = false
        var isButtonTestOpen: Boolean = false
        private val TAG = ButtonsTestActivity::class.java.simpleName
        var homePressed = false
        var menuPressed = false
        var isUserNavigating = false
        var BUTTON_SCREEN_TIME = 0
        val REQ = 5
    }
}
