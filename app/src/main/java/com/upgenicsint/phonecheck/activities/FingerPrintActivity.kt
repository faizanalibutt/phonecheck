package com.upgenicsint.phonecheck.activities

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.*
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.pro100svitlo.fingerprintAuthHelper.FahErrorType
import com.pro100svitlo.fingerprintAuthHelper.FahListener
import com.pro100svitlo.fingerprintAuthHelper.FingerprintAuthHelper
import com.samsung.android.sdk.pass.Spass
import com.samsung.android.sdk.pass.SpassFingerprint
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.misc.D
import com.upgenicsint.phonecheck.misc.Devices.isSimpleSwipeAvailable
import com.upgenicsint.phonecheck.misc.Devices.isSwipeAutomationAvailable
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.services.FingerPrintHideService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.sensor.FingerPrintTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_fringer_print.*
import java.util.*


class FingerPrintActivity : DeviceTestableActivity<FingerPrintTest>() {

    private var mFAH: FingerprintAuthHelper? = null
    private var spassFingerprint: SpassFingerprint? = null
    private var listener: SpassFingerprint.IdentifyListener? = null
    private var isFingerPrintDetected = false
    private var fingerprintService: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fringer_print)
        test = Loader.instance.getByClassType(FingerPrintTest::class.java)
        Logy.setEnable(BuildConfig.DEBUG)
        onCreateNav()
        setNavTitle("FingerPrint Test")
        setAppDeviceAdmin()

//        val intent = Intent(this, FingerPrintHideService::class.java)
//        intent.action = "com.phonecheck.showautomation"
//        startService(intent)
//        intent.component = ComponentName("com.upgenicsint.phonecheck", "com.upgenicsint.phonecheck.service.FingerPrintHideService")
//        appContext.startService(intent)

        if (Loader.RECORD_TIMER_TASK == null) {
            Loader.TIME_VALUE = 0
            FINGERPRINT_SCREEN_TIME = 0
            Loader.RECORD_TIMER_TASK = object : TimerTask() {

                override fun run() {
                    Loader.RECORD_HANDLER.post {
                        Loader.TIME_VALUE++
                    }
                }
            }
            Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)
        }

        if (Build.MANUFACTURER.containsIgnoreCase("samsung") && isSamsungFingerPrint) {
            addAutoFingerPrint()
        } else if (Build.MANUFACTURER.containsIgnoreCase("samsung") && !isSamsungFingerPrint) {
            addAutoFingerPrint()
        } else if ((Build.MANUFACTURER.containsIgnoreCase("Google") && isNonSamsungFingerPrint)
                /*|| (Build.MANUFACTURER.containsIgnoreCase("Xiaomi") && isNonSamsungFingerPrint)*/) {
            addAutoFingerPrint()
        } else {
            fingerPrintTest()
        }

        addBatteryIntentFilters()

    }

    private fun addBatteryIntentFilters() {
        if (Build.MODEL.containsIgnoreCase("SM-N910") || Build.MODEL.containsIgnoreCase("SM-N915")
                || Build.MODEL.containsIgnoreCase("SM-G900")) {
            Log.d(TAG, "No autumation for these devices")
        } else if (isNonSamsungFingerPrint && Build.MANUFACTURER.containsIgnoreCase("Google")) {
            val batteryFilter = IntentFilter()
            batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED)
            batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            registerReceiver(batteryConected, batteryFilter)
        } else if (isNonSamsungFingerPrint && !Build.MANUFACTURER.equals("samsung")) {
            Log.d(TAG, "No autumation for these devices")
        } else {
            val batteryFilter = IntentFilter()
            batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED)
            batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            registerReceiver(batteryConected, batteryFilter)
        }
    }

    private fun addAutoFingerPrint() {

        if (isSwipeAutomationAvailable()) {
            fingerTextView.text = fromHtml("Put your finger on finger print sensor then swipe <b>Up</b>")
        }

        if (isSwipeAutomationAvailable()) {
            Log.i(packageName, Loader.RESULT_ADMIN_START_PREFIX + "Make Device Admin" + Loader.RESULT_ADMIN_END_PREFIX)
            //Toast.makeText(this,"Making Admin s8",Toast.LENGTH_SHORT).show();
            fingerPrintTest()

        } else if (Build.MODEL.containsIgnoreCase("SM-N910") || Build.MODEL.containsIgnoreCase("SM-N915")
                || Build.MODEL.containsIgnoreCase("SM-G900")) {
            Log.d(TAG, "No autumation for these devices")
            fingerPrintTest()

        } else if (Build.MODEL.containsIgnoreCase("Pixel") /*|| Build.MODEL.containsIgnoreCase("Mi A1")*/) {
            fingerTextView.text = getString(R.string.fingerprint_desc_wait)
            Log.i(packageName, Loader.RESULT_ADMIN_START_PREFIX + "Make Device Admin" + Loader.RESULT_ADMIN_END_PREFIX)

        } else {
            fingerTextView.text = getString(R.string.fingerprint_desc_wait)
            Log.i(packageName, Loader.RESULT_ADMIN_START_PREFIX + "Make Device Admin" + Loader.RESULT_ADMIN_END_PREFIX)
            //Toast.makeText(this,"Making Admin s",Toast.LENGTH_SHORT).show();

        }
    }

    lateinit var mDeviceAdminSample: ComponentName
    lateinit var mDPM: DevicePolicyManager
    private fun setAppDeviceAdmin() {
        mDeviceAdminSample = ComponentName(this@FingerPrintActivity, FingerPrintActivity::class.java)
        mDPM = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            try {
                if (intent.action == "com.phonecheck.deviceadminsuccess") {
                    fingerTextView.text = getString(R.string.fingerprint_descs)
                    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (!keyguardManager.isKeyguardSecure) {
                        mDPM.resetPassword("0000", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)
                    }
                    Log.i(packageName, Loader.RESULT_FINGERPRINT_PIN_START_PREFIX + "0000" + Loader.RESULT_FINGERPRINT_PIN_END_PREFIX)
                    fingerPrintTest()
                } else if (intent.action.contains("com.phonecheck.fingerprintsuccess")) {
                    fingerPrintPass()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * replace spassfingerprint.IdentifyListener with below code
     */

    /*
spassFingerprint!!.registerFinger(context, object : SpassFingerprint.RegisterListener {
                override fun onFinished() {

                }
            })
 */

    private fun fingerPrintTest() {
        var isSamsung = false
        try {
            val spass = Spass()
            spass.initialize(context)
            if (spass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT)) {
                isSamsung = true
                spassFingerprint = SpassFingerprint(context)
                if (isSimpleSwipeAvailable()) {
                    listener = object : SpassFingerprint.IdentifyListener {
                        override fun onFinished(i: Int) {
                            FirebaseUtil.addNew(FirebaseUtil.FINGERPRINT).child("onFinished").setValue(i)
                            when (i) {
                                SpassFingerprint.STATUS_USER_CANCELLED -> {
                                }
                                SpassFingerprint.STATUS_TIMEOUT_FAILED -> try {
                                    spassFingerprint?.startIdentify(this)
                                    spassFingerprint?.setCanceledOnTouchOutside(false)
                                } catch (ignored: Exception) {
                                }

                                else -> fingerPrintPass()
                            }

                        }

                        override fun onReady() {

                        }

                        override fun onStarted() {
                            FirebaseUtil.addNew(FirebaseUtil.FINGERPRINT).child("onStarted").setValue("called")
                            fingerPrintPass()
                        }

                        override fun onCompleted() {

                        }
                    }

                    try {
                        spassFingerprint?.startIdentify(listener)
                        spassFingerprint?.setCanceledOnTouchOutside(false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    //Toast.makeText(this@FingerPrintActivity, "reached", Toast.LENGTH_SHORT).show()
                    /*fingerprintService = Intent(this,FingerPrintHideService::class.java)
                fingerprintService?.action = "com.phonecheck.showautomation"
                startService(fingerprintService)*/
                    spassFingerprint!!.registerFinger(context) { }
                    val intent = Intent(this, FingerPrintHideService::class.java)
                    intent.action = "com.phonecheck.showautomation"
                    startService(intent)
                }
            }

        } catch (e: Throwable) {
            e.printStackTrace()
            FirebaseUtil.addNew(FirebaseUtil.FINGERPRINT)
                    .child("SPass Fingerprint").child(FirebaseUtil.EXCEPTION)
                    .child(e.javaClass.simpleName).setValue(e.message)
            isSamsung = false
        }

        if (!isSamsung) {
            try {
                mFAH = FingerprintAuthHelper.Builder(context, object : FahListener {
                    override fun onFingerprintStatus(authSuccessful: Boolean, errorType: Int, errorMess: CharSequence?) {
                        if (authSuccessful) {
                            fingerPrintPass()
                            return
                        }

                        when (errorType) {
                            FahErrorType.General.LOCK_SCREEN_DISABLED, FahErrorType.General.NO_FINGERPRINTS -> {
                                Toast.makeText(context, errorMess, Toast.LENGTH_LONG).show()
                                mFAH?.showSecuritySettingsDialog()
                                fingerPrintFail()
                            }
                            FahErrorType.Auth.AUTH_NOT_RECOGNIZED ->
                                //do some stuff here
                                fingerPrintPass()
                            FahErrorType.Auth.AUTH_TO_MANY_TRIES ->
                                //do some stuff here
                                fingerPrintPass()
                        }
                    }

                    override fun onFingerprintListening(listening: Boolean, milliseconds: Long) {}
                }).build()
                try {
                    mFAH?.cleanTimeOut()

                } catch (ignored: Exception) {

                }
                /*val intent = Intent(this, FingerPrintHideService::class.java)
            intent.action = "com.phonecheck.showautomation"
            startService(intent)*/
            } catch (t: Throwable) {
                t.printStackTrace()

                FirebaseUtil.addNew(FirebaseUtil.FINGERPRINT).child("Native Fingerprint").child(FirebaseUtil.EXCEPTION)
                        .child(t.javaClass.simpleName).setValue(t.message)

                fingerPrintFail()
            }

        }
    }

    override fun checkTest(): Int {

        if (test?.status != Test.PASS) {
            test?.status = Test.FAILED
        }

        return test?.status ?: Test.FAILED
    }

    private fun fingerPrintPass() {
        test?.status = Test.PASS
        fingerImageView.setImageResource(R.drawable.fingerprint_pass)
        if (checkTest() == Test.PASS) {
            stopFingerPrintService()
            finalizeTest()
        }
    }

    private fun fingerPrintFail() {
        test?.status = Test.FAILED
        fingerImageView.setImageResource(R.drawable.fingerprint_default)
        if (checkTest() == Test.PASS) {
            finalizeTest()
        }
    }

    private val batteryConected: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action

            /*if (isNonSamsungFingerPrint && !Build.MANUFACTURER.containsIgnoreCase("samsung")) {
            Log.d(TAG, "no message for non samsung devices")
        } else if (Build.MANUFACTURER.containsIgnoreCase("samsung") && isSamsungFingerPrint) {
            if (action == Intent.ACTION_POWER_CONNECTED) {
                chargeConnectedText.visibility = View.GONE
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                chargeConnectedText.visibility = View.VISIBLE
                chargeConnectedText.text = context!!.getString(R.string.charge_fingerprint_text)
            }
        } else if (Build.MANUFACTURER.containsIgnoreCase("samsung")) {
            if (action == Intent.ACTION_POWER_CONNECTED) {
                chargeConnectedText.visibility = View.GONE
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                chargeConnectedText.visibility = View.VISIBLE
                chargeConnectedText.text = context!!.getString(R.string.charge_fingerprint_text)
            }
        }*/

            if (isNonSamsungFingerPrint && !Build.MANUFACTURER.containsIgnoreCase("samsung")) {
                Log.d(TAG, "no message for non samsung devices")
            } else if (Build.MANUFACTURER.containsIgnoreCase("samsung") && isSamsungFingerPrint) {
                if (Build.MODEL.containsIgnoreCase("SM-N910") || Build.MODEL.containsIgnoreCase("SM-N915")
                        || Build.MODEL.containsIgnoreCase("SM-G900")) {
                    Log.d(TAG, "No autumation for these devices")
                } else if (isNonSamsungFingerPrint && !Build.MANUFACTURER.equals("samsung")) {
                    Log.d(TAG, "No autumation for these devices")
                } else {
                    if (action == Intent.ACTION_POWER_CONNECTED) {
                        chargeConnectedText.visibility = View.GONE
                        chargeConnectedText1.visibility = View.GONE
                    } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                        chargeConnectedText.visibility = View.VISIBLE
                        chargeConnectedText1.visibility = View.VISIBLE
                        chargeConnectedText.text = context!!.getString(R.string.charge_fingerprint_text)
                        chargeConnectedText1.text = context.getString(R.string.charge_fingerprint_text1)
                    }
                }
            }
        }
    }

    private val isNonSamsungFingerPrint: Boolean = Loader.instance.isNonSamsungFingerPrint

    private val isSamsungFingerPrint: Boolean = Loader.instance.isSamsungFingerPrint

    override fun onResume() {
        super.onResume()
        try {
            addConnectionMessage()
            mFAH?.startListening()
        } catch (e: Exception) {
            fingerPrintTest()
        }

    }

    private fun addConnectionMessage() {
        /*if (isNonSamsungFingerPrint && !Build.MANUFACTURER.containsIgnoreCase("samsung")) {
        Log.d(TAG, "no message for non samsung devices")
    } else if (Build.MANUFACTURER.containsIgnoreCase("samsung") && isSamsungFingerPrint) {
        if (!BatteryDiagnosticActivity.isConnected(context)) {
            chargeConnectedText.visibility = View.VISIBLE
            chargeConnectedText.text = getString(R.string.charge_fingerprint_text)
        } else {
            chargeConnectedText.visibility = View.GONE
        }
    } else if (Build.MANUFACTURER.containsIgnoreCase("samsung")) {
        if (!BatteryDiagnosticActivity.isConnected(context)) {
            chargeConnectedText.visibility = View.VISIBLE
            chargeConnectedText.text = getString(R.string.charge_fingerprint_text)
        } else {
            chargeConnectedText.visibility = View.GONE
        }
    }*/

        if (isNonSamsungFingerPrint && !Build.MANUFACTURER.containsIgnoreCase("samsung")) {
            Log.d(TAG, "no message for non samsung devices")
        } else if (Build.MANUFACTURER.containsIgnoreCase("samsung") && isSamsungFingerPrint) {
            if (Build.MODEL.containsIgnoreCase("SM-N910") || Build.MODEL.containsIgnoreCase("SM-N915")
                    || Build.MODEL.containsIgnoreCase("SM-G900")) {
                Log.d(TAG, "No autumation for these devices")
            } else if (isNonSamsungFingerPrint && !Build.MANUFACTURER.equals("samsung")) {
                Log.d(TAG, "No autumation for these devices")
            } else {
                if (!BatteryDiagnosticActivity.isConnected(context)) {
                    chargeConnectedText.visibility = View.VISIBLE
                    chargeConnectedText1.visibility = View.VISIBLE
                    chargeConnectedText.text = context.getString(R.string.charge_fingerprint_text)
                    chargeConnectedText1.text = context.getString(R.string.charge_fingerprint_text1)
                } else {
                    chargeConnectedText.visibility = View.GONE
                    chargeConnectedText1.visibility = View.GONE
                }
            }

        }

    }

    override fun onPause() {
        super.onPause()

        try {
            spassFingerprint?.cancelIdentify()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

    }

    override fun onStop() {
        super.onStop()
        mFAH?.stopListening()
        try {
            val devAdminReceiver = ComponentName(this, D::class.java)
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.removeActiveAdmin(devAdminReceiver)
        } catch (e: Exception) {

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopFingerPrintService()
    }

    private fun stopFingerPrintService() {
        if (fingerprintService != null) {
            stopService(fingerprintService)
            fingerprintService = null
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
                FINGERPRINT_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_fingerprint), -1)] =
                            RecordTest(context.getString(R.string.report_fingerprint_test), FINGERPRINT_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("Fingerprint", "${FINGERPRINT_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopFingerPrintService()
        if (Build.MODEL.containsIgnoreCase("SM-N910") || Build.MODEL.containsIgnoreCase("SM-N915")
                || Build.MODEL.containsIgnoreCase("SM-G900")) {
            Log.d(TAG, "No autumation for these devices")
        } else if (isNonSamsungFingerPrint && !Build.MANUFACTURER.equals("samsung")) {
            Log.d(TAG, "No autumation for these devices")
        } else {
            unregisterReceiver(batteryConected)
        }
        mFAH?.onDestroy()
        listener = null
        spassFingerprint = null
    }

    companion object {
        fun fromHtml(html: String): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
        }

        val REQ = 3
        var FINGERPRINT_SCREEN_TIME = 0
        private val TAG = "FingerPrintActivity"
    }

}