package com.upgenicsint.phonecheck

import android.Manifest
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Camera
import android.os.Build
import android.os.Environment
import android.os.Vibrator
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import android.util.Log
import com.farhanahmed.cabinet.Cabinet
import com.google.gson.Gson
import com.samsung.android.sdk.look.Slook
import com.samsung.android.sdk.pass.Spass
import com.samsung.android.sdk.pen.Spen
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.models.ClientCustomization
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.*
import com.upgenicsint.phonecheck.test.hardware.*
import com.upgenicsint.phonecheck.test.misc.DeviceLockTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*

import android.net.ConnectivityManager
import android.os.Handler
import android.support.annotation.RequiresApi
import com.google.gson.GsonBuilder
import com.upgenicsint.phonecheck.activities.*
import com.upgenicsint.phonecheck.barcode.CaptureActivity
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile
import com.upgenicsint.phonecheck.models.*
import com.upgenicsint.phonecheck.test.misc.CosmeticsTest
import com.upgenicsint.phonecheck.test.misc.GradesTest
import com.upgenicsint.phonecheck.test.sensor.*
import kotlin.collections.ArrayList


/**
 * Created by Farhan on 10/28/2016.
 */
class Loader
private constructor() {
    var context: Context? = null
    @JvmField
    var clientCustomization: ClientCustomization? = null
    @JvmField
    var testPlanList: List<String>? = null
    val testList = ArrayList<Test>()
    var RECORD_COUNTER = 0
    val recordList = ArrayList<RecordTest>()
    var cosmeticsList: List<CosmeticsKeys>? = null

    var recordPreferences: SharedPreferences.Editor? = null

    @JvmField
    var fingerPrintSupport: Boolean = false
    @JvmField
    var isAutoAudioEnabled = false
    @JvmField
    var isMicLSEnabled = false
    @JvmField
    var isAutoLSEnabled = false
    @JvmField
    var isAutoESEnabled = false
    @JvmField
    var isMicESEnabled = false
    @JvmField
    var checkMicLS: Boolean = false
    @JvmField
    var checkAudioInput: Boolean = false
    @JvmField
    var isAutoEarPieceEnabled = false
    @JvmField
    var isTestListLoaded = false
    @JvmField
    var isCosmeticsConfigLoaded = false
    @JvmField
    var connectedToNetwork = false
    @JvmField
    var isAutoFrontEnabled: Boolean = false
    @JvmField
    var isAutoRearEnabled: Boolean = false
    @JvmField
    var isAutoAccelEnabled: Boolean = false
    @JvmField
    var isAccelEnabled: Boolean = false
    @JvmField
    var isSamsungFingerPrint = false
    @JvmField
    var isNonSamsungFingerPrint = false
    @JvmField
    var isTimeCapture = false
    @JvmField
    var isFingerTrail = false
    @JvmField
    var isESPlayBack = false


    fun loadTest(context: Context) {

        var haveConnectedWifi = false
        var haveConnectedMobile = false

        recordPreferences = context.getSharedPreferences(context.getString(R.string.record_tests), Context.MODE_PRIVATE).edit()

        val cm = context.getSystemService(context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.getAllNetworkInfo()
        for (ni in netInfo) {
            if (ni.getTypeName().containsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true
            if (ni.getTypeName().containsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true

        }

        if (haveConnectedWifi == false && haveConnectedMobile == false) {
            //do something to handle if wifi & mobiledata is disabled
            connectedToNetwork = false
        } else {
            //do something else..
            connectedToNetwork = true
        }

        loadCustomizations()

        val pm = context.packageManager
        loadFilter()
        loadCosmetics()
        testList.clear()

        if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            addTest(WifiTest(context), Test.wifiTestKey)
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("WifiTest").setValue("Not a Feature")
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            addTest(BluetoothTest(context), Test.blueToothTestKey)
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("BluetoothTest").setValue("Not a Feature")
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && isTestListLoaded) {
            addTest(BluetoothTest2(context), Test.blueToothPlusTestKey)
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child(Test.blueToothPlusTestKey).setValue("Not a Feature")
        }

        val jsonObjectFile = ReadTestJsonFile.getInstance().androidConfiguration
        val wireless: Boolean = if (jsonObjectFile.has("WirelessCharging")) jsonObjectFile.getBoolean("WirelessCharging") else false
        if (isTestListLoaded && wireless && filterContains(Test.wirelessTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_wireless_test), WirelessChargingActivity.WIRELESS_SCREEN_TIME), WirelessTest(context), Test.wirelessTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_wireless), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            addTest(GPSTest(context), Test.gpsTestKey)
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("GPSTest").setValue("Not a Feature")
        }

        if (isTestListLoaded && filterContains(Test.batteryTestKey)) {
            addTest(BatteryTest(context), Test.batteryTestKey)
        }

        // 0
        if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY))

            if (Devices.hasProximity()) {
                addTest(RecordTest(context.getString(R.string.report_proximity_test), ProximityActivity.PROXIMITY_SCREEN_TIME), ProximityTest(context), Test.proximityTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_proximity), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            } else {
                FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                        .child("ProximityTest").setValue("Not a Feature")
            }

        // 1
        addTest(RecordTest(context.getString(R.string.report_button_test), ButtonsTestActivity.BUTTON_SCREEN_TIME), ButtonTest(context), Test.buttonTestKey)
        recordPreferences!!.putInt(context.getString(R.string.record_button), RECORD_COUNTER)
        recordPreferences!!.apply()
        RECORD_COUNTER++

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (isTestListLoaded && filterContains(Test.autoVibrationTestKey)) {
                addTest(AutoVibrationTest(context), Test.autoVibrationTestKey)
            } else {
                addTest(VibrationTest(context), Test.vibrationTestKey)
            }
        }

        // 2
        if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            addTest(RecordTest(context.getString(R.string.report_accelerometer_test), AccelerometerActivity.ACCEL_SCREEN_TIME), AccelerometerTest(context.applicationContext), Test.accelerometerTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_accel), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("AccelerometerTest").setValue("Not a Feature")
        }

        // 3
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            if (isTestListLoaded && filterContains(Test.cameraTestKey) && filterContains(Test.autoFrontTestKey)) {
                addTest(RecordTest(context.getString(R.string.report_frontcam_test), CameraTestActivity.FRONT_CAMERA_SCREEN_TIME), AutoFrontCameraTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_frontcam), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            } else if (isTestListLoaded && filterContains(Test.cameraTestKey)) {
                addTest(RecordTest(context.getString(R.string.report_frontcam_test), CameraTestActivity.FRONT_CAMERA_SCREEN_TIME), FrontCameraTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_frontcam), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            } else {
                addTest(RecordTest(context.getString(R.string.report_frontcam_test), CameraTestActivity.FRONT_CAMERA_SCREEN_TIME), AutoFrontCameraTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_frontcam), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            }
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("Camera Front Test").setValue("Not a Feature")
        }

        // 4
        if (isTestListLoaded && filterContains(Test.QRtest)) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                addTest(RecordTest(context.getString(R.string.report_barcode_test), CaptureActivity.BARCODE_SCREEN_TIME), AutofocusTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_barcode), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            } else {
                FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                        .child("Camera Back Test").setValue("Not a Feature")
            }
        }

        // 5
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            if (isTestListLoaded && filterContains(Test.cameraTestKey) && filterContains(Test.autoRearTestKey)) {
                addTest(RecordTest(context.getString(R.string.report_rear_test), CameraTestActivity.REAR_CAMERA_SCREEN_TIME), AutoBackCameraTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_rearcam), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            } else if (isTestListLoaded && filterContains(Test.cameraTestKey)) {
                addTest(RecordTest(context.getString(R.string.report_rear_test), CameraTestActivity.REAR_CAMERA_SCREEN_TIME), BackCameraTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_rearcam), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            } else {
                addTest(RecordTest(context.getString(R.string.report_rear_test), CameraTestActivity.REAR_CAMERA_SCREEN_TIME), AutoBackCameraTest(context), Test.cameraTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_rearcam), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            }
        } else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("Camera Back Test").setValue("Not a Feature")
        }

//        addTest(RecordTest(context.getString(R.string.report_rear_test), StockCameraActivity.REAR_CAMERA_SCREEN_TIME), StockCamera(context), Test.stockCameraTestKey)


        // 6
        if (isTestListLoaded && filterContains(Test.micLSTestResultKey) && filterContains(Test.autoLSFilterKey)) {
            addTest(RecordTest(context.getString(R.string.report_micls_test), MicLSTestActivity.MICLS_SCREEN_TIME), NewMicLSTest(context), Test.autoLSFilterKey)
            recordPreferences!!.putInt(context.getString(R.string.record_micls), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else if (isTestListLoaded && filterContains(Test.micLSTestResultKey)) {
            addTest(RecordTest(context.getString(R.string.report_micls_test), MicLSTestActivity.MICLS_SCREEN_TIME), MicLSTest(context), Test.micLSTestResultKey)
            recordPreferences!!.putInt(context.getString(R.string.record_micls), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else {
            addTest(RecordTest(context.getString(R.string.report_micls_test), MicLSTestActivity.MICLS_SCREEN_TIME), NewMicLSTest(context), Test.autoLSFilterKey)
            recordPreferences!!.putInt(context.getString(R.string.record_micls), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        // 7
        if (isTestListLoaded) {
            val showEarpiece = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY)
            val showHeadset = filterContains(Test.headsetLeftKey) && filterContains(Test.headsetLeftKey) && filterContains(Test.headsetPortKey)
            if (filterContains(Test.micESTestResultKey) && filterContains(Test.autoESFilterKey)) {
                if (showHeadset) {
                    addTest(RecordTest(context.getString(R.string.report_mices_test), MicESTestActivity.MICES_SCREEN_TIME),
                            NewMicESTest(context), Test.autoESFilterKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                } else if (showEarpiece) {
                    addTest(RecordTest(context.getString(R.string.report_mices_test),
                            MicESTestActivity.MICES_SCREEN_TIME), NewMicESTest(context), Test.micESTestResultKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                }
            } else if (filterContains(Test.micESTestResultKey)) {
                if (showHeadset) {
                    addTest(RecordTest(context.getString(R.string.report_mices_test),
                            MicESTestActivity.MICES_SCREEN_TIME), MicESTest(context), Test.autoESFilterKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                } else if (showEarpiece) {
                    addTest(RecordTest(context.getString(R.string.report_mices_test),
                            MicESTestActivity.MICES_SCREEN_TIME), MicESTest(context), Test.autoESFilterKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                }
            } else {
                if (showHeadset) {
                    addTest(RecordTest(context.getString(R.string.report_mices_test), MicESTestActivity.MICES_SCREEN_TIME),
                            NewMicESTest(context), Test.autoESFilterKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                } else if (showEarpiece) {
                    addTest(RecordTest(context.getString(R.string.report_mices_test), MicESTestActivity.MICES_SCREEN_TIME),
                            NewMicESTest(context), Test.autoESFilterKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                }
            }
        } else {
            addTest(RecordTest(context.getString(R.string.report_mices_test),
                    MicESTestActivity.MICES_SCREEN_TIME), NewMicESTest(context), Test.autoESFilterKey)
            recordPreferences!!.putInt(context.getString(R.string.record_mices), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        // 8
        if (isTestListLoaded && filterContains(Test.audioPlaybackTestKey)) {
            if (filterContains(Test.earSpeakerPlaybackTestKey)) {
                isESPlayBack = true
            }
            addTest(RecordTest(context.getString(R.string.report_micplayback_test), AudioPlaybackTestActivity.MIC_PLAYBACK_SCREEN_TIME), AudioPlaybackTest(context), Test.audioPlaybackTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_micplayback), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        // 9
        // disable speech recog no wifi requires
        if (isTestListLoaded && Build.MODEL.containsIgnoreCase(SpeechSupportedModels.choose())
                && filterContains(Test.micResultTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_speech_test), SpeechRecognization.SPEECH_SCREEN_TIME), SpeechToTextTest(context), Test.micResultTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_speech), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        if (isTestListLoaded && filterContains(Test.micQualityTestkey)) {
            addTest(MicQualityTest(context), Test.micQualityTestkey)
        }

        // 10
        if (isTestListLoaded && filterContains(Test.LCDTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_lcd_test), TouchTestActivity.LCD_SCREEN_TIME), LCDTest(context), Test.LCDTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_lcd), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else if (isTestListLoaded && filterContains("Glass Cracked")) {
            addTest(RecordTest(context.getString(R.string.report_lcd_test), TouchTestActivity.LCD_SCREEN_TIME), LCDTest(context), "Glass Cracked")
            recordPreferences!!.putInt(context.getString(R.string.record_lcd), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else if (isTestListLoaded && filterContains(Test.BacklightTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_lcd_test), TouchTestActivity.LCD_SCREEN_TIME), LCDTest(context), Test.BacklightTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_lcd), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else if (!isTestListLoaded) {
            addTest(RecordTest(context.getString(R.string.report_lcd_test), TouchTestActivity.LCD_SCREEN_TIME), LCDTest(context), Test.LCDTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_lcd), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }
        //addTest(DigitizerTestAdvance(context), Test.samDigitizerTestKey)

        // 11
        if (isTestListLoaded && filterContains(Test.samDigitizerTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_digitizer_test), DigitizerActivity.DIGITIZER_SCREEN_TIME), DigitizerTestAdvance(context), Test.samDigitizerTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_digi), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        } else {
            addTest(RecordTest(context.getString(R.string.report_digitizer_test), DigitizerActivity.DIGITIZER_SCREEN_TIME), DigitizerTest(context), Test.digitizerTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_digi), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        // 12
        if (isTestListLoaded && filterContains(Test.multitouchTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_multitouch_test), MultiTouchTestActivity.MULTI_TOUCH_SCREEN_TIME), MultiTouchTest(context), Test.multitouchTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_mutlitouch), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        val hasFingerPrintKey = isTestListLoaded && filterContains(Test.fingerprintTestKey)
        val hasSamsungFingerPrintKey = isTestListLoaded && filterContains(Test.samsungFingerPrint)
        val hasNonSamsungFingerPrintKey = isTestListLoaded && filterContains(Test.nonsamsungFingerPrint)


        /*if (hasFingerPrintKey && isNonSamsungFingerPrint && !Build.MANUFACTURER.containsIgnoreCase("samsung")) {
            addFingerprint(context)
        } else if (hasFingerPrintKey && Build.MANUFACTURER.containsIgnoreCase(context.getString(R.string.samsung)) && isSamsungFingerPrint) {
            addFingerprint(context)
        } else if (hasFingerPrintKey && Build.MANUFACTURER.containsIgnoreCase("samsung") && !isSamsungFingerPrint) {
            Log.d(TAG, "For Custom Test Plan Samsung Fingerprint key is not available")
        } else if (Build.MANUFACTURER.containsIgnoreCase("samsung")) {
            addFingerprint(context)
        }

        if (filterContains(Test.nonsamsungFingerPrint) && !Build.MANUFACTURER.containsIgnoreCase(context.getString(R.string.samsung))) {
            addFingerprint(context)
        } else if (isTestListLoaded && Build.MANUFACTURER.containsIgnoreCase(context.getString(R.string.samsung)) && filterContains(Test.samsungFingerPrint)) {
            addFingerprint(context)
        }*/

        // 13
        if (hasFingerPrintKey && hasNonSamsungFingerPrintKey && !Build.MANUFACTURER.containsIgnoreCase(context.getString(R.string.samsung))) {
            addFingerprint(context)
        } else if (hasFingerPrintKey && hasSamsungFingerPrintKey && Build.MANUFACTURER.containsIgnoreCase(context.getString(R.string.samsung))) {
            addFingerprint(context)
        }

        // 14
        try {
            val spen = Spen()
            spen.initialize(context.applicationContext)
            if (spen.isFeatureEnabled(Spen.DEVICE_PEN)) {
                if (isTestListLoaded) {
                    if (filterContains(Test.spenTestKey)) {
                        val record = RecordTest(context.getString(R.string.report_spen_test), SPenActivity.SPENTEST_SCREEN_TIME)
                        recordList.add(record)
                        recordPreferences!!.putInt(context.getString(R.string.record_spen), RECORD_COUNTER)
                        recordPreferences!!.apply()
                        RECORD_COUNTER++
                    }
                } else {
                    val record = RecordTest(context.getString(R.string.report_spen_test), SPenActivity.SPENTEST_SCREEN_TIME)
                    recordList.add(record)
                    recordPreferences!!.putInt(context.getString(R.string.record_spen), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                }
                addTest(SPenTest(context.applicationContext), Test.spenTestKey)

            }
        } catch (ignored: Throwable) {

        }

        // 15
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val slook = Slook()
                slook.initialize(context.applicationContext)
                if (slook.isFeatureEnabled(Slook.COCKTAIL_BAR)) {
                    addTest(RecordTest(context.getString(R.string.report_spenedge_test), SEdgeActivity.SEDGE_SCREEN_TIME), EdgeScreenTest(context.applicationContext), Test.edgeScreenTestKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_spenedge), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                }
            } catch (ignored: Throwable) {

            }
        }

        // 16
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            if (isTestListLoaded) {
                if (isDualSimDevice(context) && filterContains(Test.dualSimCallTestKey)) {
                    addTest(RecordTest(context.getString(R.string.report_call_test), CallActivity.CALL_SCREEN_TIME), DualCallTest(context.applicationContext), Test.dualSimCallTestKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_call), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                } else if (!isDualSimDevice(context) && filterContains(Test.callTestKey)) {
                    addTest(RecordTest(context.getString(R.string.report_call_test), CallActivity.CALL_SCREEN_TIME), CallTest(context.applicationContext), Test.callTestKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_call), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                } else {
                    FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                            .child("CallTest").setValue("Not a Feature")
                }
            } else {
                /*if (isDualSimDevice(context))
                    addTest(DualCallTest(context.applicationContext), Test.dualSimCallTestKey)
                else if (!isDualSimDevice(context))*/
                addTest(RecordTest(context.getString(R.string.report_call_test), CallActivity.CALL_SCREEN_TIME), CallTest(context.applicationContext), Test.callTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_call), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
                /*else {
                    FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                            .child("CallTest").setValue("Not a Feature")
                }*/
            }
        }

        /*if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            addTest(CallTest(context.applicationContext), Test.callTestKey)
        else {
            FirebaseUtil.addNew(FirebaseUtil.LOADED_TEST)
                    .child("CallTest").setValue("Not a Feature")
        }*/

        // 17
        if (pm.hasSystemFeature(PackageManager.FEATURE_NFC) && isTestListLoaded && filterContains(Test.nfctest)) {
            addTest(RecordTest(context.getString(R.string.report_nfc_test), NFCActivity.NFC_SCREEN_TIME), NfcTest(context), Test.nfctest)
            recordPreferences!!.putInt(context.getString(R.string.record_nfc), RECORD_COUNTER)
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        // 18
        if (isTestListLoaded && filterContains(Test.cosmeticsTestKey)) {
            if (cosmeticsConfigFile.exists() && !instance.loadCosmetics()!!.getCosmetics().isEmpty()) {
                addTest(RecordTest(context.getString(R.string.report_cosmetics_test), CosmeticsTestActivity.COSMETICS_SCREEN_TIME), CosmeticsTest(context.applicationContext), Test.cosmeticsTestKey)
                recordPreferences!!.putInt(context.getString(R.string.record_cosmetics), RECORD_COUNTER)
                recordPreferences!!.apply()
                RECORD_COUNTER++
            }
        }

        // 19
        if (isTestListLoaded && filterContains(Test.gradesTestKey)) {
            addTest(RecordTest(context.getString(R.string.report_grade_test), GradingsActivity.GRADES_SCREEN_TIME), GradesTest(context.applicationContext), Test.gradesTestKey)
            recordPreferences!!.putInt(context.getString(R.string.record_grades), RECORD_COUNTER).apply()
            recordPreferences!!.apply()
            RECORD_COUNTER++
        }

        addTest(DeviceLockTest(context.applicationContext), Test.deviceLockTestKey)

//        addTest(BatteryDiagnostic(context), Test.batteryDiagtestKey)

        val state = Cabinet.open(context.applicationContext, R.string.device_results)
        val filter = Cabinet.open(context.applicationContext, R.string.device_filter)

        for (test in testList) {
            try {

                test.onRestoreFilter(filter)
                test.onRestoreState(state)

            } catch (ignored: JSONException) {
            }
        }

        try {
            isAutoAudioEnabled = isTestListLoaded && filterContains(Test.autoAudioFilterKey)
            isMicLSEnabled = isTestListLoaded && filterContains(Test.micLSTestResultKey)
            isAutoLSEnabled = isTestListLoaded && filterContains(Test.autoLSFilterKey)
            isMicESEnabled = isTestListLoaded && filterContains(Test.micESTestResultKey)
            isAutoESEnabled = isTestListLoaded && filterContains(Test.autoESFilterKey)
            isAutoEarPieceEnabled = isTestListLoaded && filterContains(Test.autoAudioEarpieceKey)
            isAutoFrontEnabled = isTestListLoaded && filterContains(Test.autoFrontTestKey)
            isAutoRearEnabled = isTestListLoaded && filterContains(Test.autoRearTestKey)
            isAutoAccelEnabled = isTestListLoaded && filterContains(Test.autoAccelerometer)
            isSamsungFingerPrint = isTestListLoaded && filterContains(Test.samsungFingerPrint)
            isNonSamsungFingerPrint = isTestListLoaded && filterContains(Test.nonsamsungFingerPrint)
            isAccelEnabled = isTestListLoaded && filterContains(Test.accelerometerTestKey)
            isTimeCapture = isTestListLoaded && filterContains(Test.imageCaptureTime)
            isFingerTrail = isTestListLoaded && filterContains(Test.fingerTrailDigitizer)
            isESPlayBack = isTestListLoaded && filterContains(Test.earSpeakerPlaybackTestKey)
            
        } catch (ignored: Exception) {
            isAutoAudioEnabled = false
            isMicLSEnabled = false
            isAutoLSEnabled = false
            isAutoEarPieceEnabled = false
            isAutoESEnabled = false
            isMicESEnabled = false
            isAutoFrontEnabled = false
            isAutoRearEnabled = false
            isAutoAccelEnabled = false
            isAccelEnabled = false
            isSamsungFingerPrint = false
            isNonSamsungFingerPrint = false
            isTimeCapture = false
            isFingerTrail = false
            isESPlayBack = false
        }
    }

    private fun isDualSimDevice(context: Context?): Boolean {
        val telephonyManager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {

                if (telephonyManager != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (telephonyManager.phoneCount > 1) {
                                return true
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && telephonyManager.phoneCount > 1) {
                                return true
                            } else /*if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)*/ {
                                try {
                                    val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                                    val telephonyClass = Class.forName(telephonyManager.javaClass.name)
                                    val parameter = arrayOfNulls<Class<*>>(1)
                                    parameter[0] = Int::class.javaPrimitiveType
                                    val getFirstMethod = telephonyClass.getMethod("getDeviceId", *parameter)
                                    Log.d("SimData", getFirstMethod.toString())
                                    val obParameter = arrayOfNulls<Any>(1)
                                    obParameter[0] = 0
                                    val first = getFirstMethod.invoke(telephony, 0) as String
                                    Log.d("SimData", "first :$first")
                                    obParameter[0] = 1
                                    val second = getFirstMethod.invoke(telephony, 1) as String
                                    Log.d("SimData", "Second :$second")
                                    if (first != second) {
                                        return true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
        return false
    }

    private fun addFingerprint(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            var samsungFingerprint = false
            try {
                val spass = Spass()
                spass.initialize(context.applicationContext)
                if (spass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT)) {
                    samsungFingerprint = true
                }
            } catch (e: Throwable) {
                samsungFingerprint = false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (samsungFingerprint || fingerPrintSupport) {
                    addTest(RecordTest(context.getString(R.string.report_fingerprint_test), FingerPrintActivity.FINGERPRINT_SCREEN_TIME), FingerPrintTest(context.applicationContext), Test.fingerprintTestKey)
                    recordPreferences!!.putInt(context.getString(R.string.record_fingerprint), RECORD_COUNTER)
                    recordPreferences!!.apply()
                    RECORD_COUNTER++
                }
            }
        }
    }

    private fun loadCustomizations() {

        val s = readFileToString(customizationFile)
        if (s != null && !s.isEmpty()) {
            clientCustomization = Gson().fromJson(s, ClientCustomization::class.java)
        }
    }

    fun filterContains(filter: String): Boolean {
        if (testPlanList == null) {
            loadFilter()
        }
        val testFilter = testPlanList
        if (testFilter != null && testListFile.exists()) {
            if (testFilter.contains(filter) || filter.equals(ALLOW_TEST, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun addTest(test: Test, filter: String) {

        if (isTestListLoaded) {
            if (filterContains(filter)) {
                testList.add(test)
            } else {
                FirebaseUtil.addNew(FirebaseUtil.NOT_LOADED_TEST)
                        .child(test.title).setValue("No Filtered Out")
            }
        } else {
            testList.add(test)
        }

    }

    private fun addTest(record: RecordTest, test: Test, filter: String) {

        if (isTestListLoaded) {
            if (filterContains(filter)) {
                testList.add(test)
                recordList.add(record)
            } else {
                FirebaseUtil.addNew(FirebaseUtil.NOT_LOADED_TEST)
                        .child(test.title).setValue("No Filtered Out")
            }
        } else {
            recordList.add(record)
            testList.add(test)
        }

    }

    fun <T : Test> getByClassType(testClass: Class<T>): T? {
        for (test in testList) {
            if (test.javaClass == testClass) {
                try {
                    return test as T
                } catch (ignored: Exception) {
                }
            }
        }
        return null
    }

    private fun loadFilter() {
        try {
            val jsonObject = JSONObject(readFileToString(testListFile))
            if (jsonObject.has("Test")) {
                val testString = jsonObject.getString("Test")
                val b = testString != null && !testString.isEmpty()
                if (b) {
                    testPlanList = testString.split(",")
                    isTestListLoaded = true
                    Log.d(TAG, testPlanList.toString())
                } else {
                    isTestListLoaded = false
                }
            } else {
                isTestListLoaded = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isTestListLoaded = false
        }

    }

    fun loadCosmetics(): Cosmetics? {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val fileData = readFileToString(cosmeticsConfigFile)
        val cosmeticsData = gson.fromJson(fileData, Cosmetics::class.java)
        return cosmeticsData
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getCosmeticsShortKey() {
        val filtered = java.util.ArrayList<CosmeticsKeys>()
        cosmeticsList = Objects.requireNonNull<Cosmetics>(Loader.instance.loadCosmetics()).getCosmetics()

        if (cosmeticsList != null) {
            for (e in 0 until cosmeticsList!!.size) {
                if (cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("Android") || cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("All")) {
                    filtered.add(cosmeticsList!!.get(e))
                }
            }
        }
        cosmeticsList = filtered
    }

    fun dumpResultToFile(context: Context) {
        try {
            try {
                saveManualTest()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            saveContentOnFile(RESULT.toString(), resultFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveManualTest() {
        val sharedPreferences = context!!.getSharedPreferences(context!!.getString(R.string.manual), Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean(Constants.MANUAL_ES, false)) {
            if (RESULT.has(Test.earphoneTestKey)) {
                RESULT.remove(Test.earphoneTestKey)
            }
        } else {
            if (RESULT.has(Test.earphoneTestKey + "-M")) {
                RESULT.remove(Test.earphoneTestKey + "-M")
            }
        }
        if (sharedPreferences.getBoolean(Constants.MANUAL_LS, false)) {
            if (RESULT.has(Test.loudSpeakerTestKey)) {
                RESULT.remove(Test.loudSpeakerTestKey)
            }
        } else {
            if (RESULT.has(Test.loudSpeakerTestKey + "-M")) {
                RESULT.remove(Test.loudSpeakerTestKey + "-M")
            }
        }
    }

    fun dumpCosmeticsResultToFile() {
        try {
            saveContentOnFile(RESULTCOSMETICS.toString(), resultFileCosmetics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createEmptyFile() {
        emptyFile.createNewFile()
    }

    fun deleteEmptyFile() {
        try {
            emptyFile.delete()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun createbatterystatFile() {
        batterystat.createNewFile()
    }

    fun deletebatterystatFile() {
        try {
            batterystat.delete()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    companion object {
        private val ALLOW_TEST = "ALLOW_TEST"
        @JvmField
        val TAG = Loader::class.java.simpleName
        @JvmField
        val RESULT_START_PREFIX = "rhkg38yw4w-"
        @JvmField
        val RESULT_END_PREFIX = "-4rhjg7x9gw"
        @JvmField
        val RESULT_COSMETICS_START_PREFIX = "cosmeticTestsStart-"
        @JvmField
        val RESULT_COSMETICS_END_PREFIX = "-cosmeticTestsEnd"
        @JvmField
        val RESULT_BARCODE_START_PREFIX = "ScannedBarcode-"
        @JvmField
        val RESULT_BARCODE_END_PREFIX = "-ScannedBarcode"
        @JvmStatic
        val RESULT_HEADSET_START_PREFIX = "HeadsetReport-"
        @JvmStatic
        val RESULT_HEADSET_END_PREFIX = "-HeadsetReport"
        @JvmStatic
        val RESULT_FINGERPRINT_END_PREFIX: String = "FingerPrintScanner-"
        @JvmStatic
        val RESULT_FINGERPRINT_PIN_START_PREFIX = "FingerPrintPin-"
        @JvmStatic
        val RESULT_FINGERPRINT_PIN_END_PREFIX = "-FingerPrintPin"
        @JvmStatic
        val RESULT_FINGERPRINT_START_PREFIX: String = "-FingerPrintScanner"
        @JvmStatic
        val RESULT_ADMIN_START_PREFIX = "DeviceAdmin-"
        @JvmStatic
        val RESULT_ADMIN_END_PREFIX = "-DeviceAdmin"
        @JvmField
        val BATTERY_START_PREFIX = "batteryResultStart-"
        @JvmField
        val BATTERY_END_PREFIX = "-batteryResultEnd"
        @JvmField
        var DeviceInfoEndKey = "-ZGV2aWNlaW5mb2VuZA"
        @JvmField
        var DeviceInfoStartKey = "ZGV2aWNlaW5mb3N0YXJ0-"
        @JvmField
        var imei = ""
        @JvmField
        var imei2 = ""
        @JvmField
        var RESULT: JSONObject = JSONObject()
        @JvmField
        var RESULTCOSMETICS: JSONObject = JSONObject()
        @JvmField
        val jsonObjectFile = ReadTestJsonFile.getInstance().androidConfiguration!!
        @JvmField
        var RESULT_TIME: JSONObject = JSONObject()
        @JvmField
        var TOTAL_SCREEN_TIME = 0
        @JvmField
        var TIME_VALUE = 0
        @JvmField
        val RECORD_TIMER_TEST: Timer = Timer()
        @JvmField
        val RECORD_HANDLER = Handler()
        @JvmField
        var RECORD_TIMER_TASK: TimerTask? = null
        @JvmField
        var RECORD_TESTS_TIME = JSONObject()

        @JvmStatic
        var instance: Loader = Loader()
            private set

        @JvmStatic
        fun readFileToString(file: File): String? {

            if (file.exists() && file.canRead()) {
                try {
                    return file.readText()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            return null
        }

        @Throws(Exception::class)
        @JvmStatic
        fun saveContentOnFile(content: String, file: File) {
            try {
                file.delete()
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            try {
                file.writeText(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            /*try {
                val dataOutputStream = DataOutputStream(FileOutputStream(file))
                dataOutputStream.write(content.toByteArray())
                dataOutputStream.close()
            } catch (e: Throwable) {
                e.printStackTrace()
            }*/

        }

        @JvmStatic
        fun writeDeviceInfoFile(context: Context?): JSONObject {
            val deviceInfo = JSONObject()
            if (context != null) {

                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {

                        if (telephonyManager != null) {
                            deviceInfo.put("Network", telephonyManager.networkOperatorName)
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (telephonyManager.phoneCount > 1) {
                                        imei = telephonyManager.getImei(0)
                                        imei2 = telephonyManager.getImei(1)
                                    } else {
                                        imei = telephonyManager.imei
                                    }
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && telephonyManager.phoneCount > 1) {
                                        imei = telephonyManager.getDeviceId(0)
                                        imei2 = telephonyManager.getDeviceId(1)

                                    } else {
                                        try {
                                            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                                            val telephonyClass = Class.forName(telephonyManager.javaClass.name)
                                            val parameter = arrayOfNulls<Class<*>>(1)
                                            parameter[0] = Int::class.javaPrimitiveType
                                            val getFirstMethod = telephonyClass.getMethod("getDeviceId", *parameter)
                                            Log.d("SimData", getFirstMethod.toString())
                                            val obParameter = arrayOfNulls<Any>(1)
                                            obParameter[0] = 0
                                            val first = getFirstMethod.invoke(telephony, 0) as String
                                            Log.d("SimData", "first :$first")
                                            obParameter[0] = 1
                                            val second = getFirstMethod.invoke(telephony, 1) as String
                                            Log.d("SimData", "Second :$second")
                                            imei2 = second

                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }

                                        imei = telephonyManager.deviceId
                                    }
                                }
                            } catch (e: Exception) {
                            }
                        }
                    } else {
                        if (telephonyManager != null) {
                            try {
                                imei = telephonyManager.deviceId
                            } catch (e: Exception) {

                            }
                        }
                    }

                    var isSupportGoogle = false
                    var isSupportSamsung = false

                    val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager?

                    if (accountManager != null) {
                        for (account in accountManager.accounts) {
                            if (account.type.equals("com.google", ignoreCase = true)) {
                                isSupportGoogle = true
                            }
                            if (account.type.equals("com.osp.app.signin", ignoreCase = true) || account.type.toLowerCase().contains("samsung")) {
                                isSupportSamsung = true
                            }
                        }

                        val deviceLockStatus = JSONObject()
                        deviceLockStatus.put("status", if (isSupportSamsung || isSupportGoogle) Test.FAILED else Test.PASS)
                        deviceLockStatus.put(Test.FRP_LOCK, if (isSupportGoogle) Test.FAILED else Test.PASS)
                        deviceLockStatus.put(Test.samsungDeviceInfoKey, if (isSupportSamsung) Test.FAILED else Test.PASS)
                        deviceInfo.put(Test.deviceInfoKey, deviceLockStatus)
                        try {
                            deviceInfo.put("AppVersion", context.getString(R.string.build_version))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                }
                deviceInfo.put("IMEI", imei)
                deviceInfo.put("IMEI2", imei2)
                deviceInfo.put("Language", Locale.getDefault().displayLanguage)
                deviceInfo.put("OS", "Android")
                deviceInfo.put("OsVersion", Build.VERSION.RELEASE)
                deviceInfo.put("Device", Build.DEVICE)
                deviceInfo.put("Model", Build.MODEL)
                deviceInfo.put("Product", Build.PRODUCT)
                deviceInfo.put("Brand", Build.BRAND)
                deviceInfo.put("Display", Build.DISPLAY)
                deviceInfo.put("Hardware", Build.HARDWARE)
                deviceInfo.put("Id", Build.ID)
                deviceInfo.put("Manufacturer", Build.MANUFACTURER)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    deviceInfo.put("Serial", Build.getSerial())
                } else {
                    deviceInfo.put("Serial", Build.SERIAL)
                }

                deviceInfo.put("User", Build.USER)
                deviceInfo.put("Host", Build.HOST)
                deviceInfo.put("devicerooted", if (isPhoneRooted == 1) "true" else "false")

                saveContentOnFile(deviceInfo.toString(4), deviceInfoFile)

            }

            return deviceInfo

        }


        @JvmStatic
        val baseFile by lazy {
            val sdcard = File("/sdcard")
            if (sdcard.exists()) {
                sdcard
            } else {
                Environment.getExternalStorageDirectory()
            }
        }


        @JvmStatic
        val resultFileCosmetics = File("$baseFile/CosmeticResults.json")

        @JvmStatic
        val resultFile = File("$baseFile/TestResults.json")

        @JvmStatic
        val testListFile = File("$baseFile/TestList.json")

        @JvmStatic
        val emptyFile = File("$baseFile/canerase.txt")

        @JvmStatic
        val batterystat = File("$baseFile/batterystat.txt")

        @JvmStatic
        val deviceInfoFile = File("$baseFile/deviceinfo.json")

        @JvmStatic
        val customizationFile = File("$baseFile/ClientCustomization.json")

        @JvmStatic
        val cosmeticsConfigFile = File("$baseFile/Config.json")

        @JvmStatic
        val timeTestTakenFile = File("$baseFile/time.json")

        @JvmStatic
        private val isPhoneRooted: Int
            get() {
                val buildTags = Build.TAGS
                if (buildTags != null && buildTags.contains("test-keys")) {
                    return 1
                }
                try {
                    val file = File("/system/app/Superuser.apk")
                    if (file.exists()) {
                        return 1
                    }
                } catch (e1: Throwable) {
                }

                return 0
            }

    }
}

private val Context.CONNECTIVITY_SERVICE: String?
    get() {
        return Context.CONNECTIVITY_SERVICE
    }



