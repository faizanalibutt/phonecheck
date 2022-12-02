package com.upgenicsint.phonecheck.test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.activities.MainActivity
import com.upgenicsint.phonecheck.adapter.SubTestAdapter
import com.upgenicsint.phonecheck.misc.CameraFace
import com.upgenicsint.phonecheck.models.TestStatusInfo
import com.upgenicsint.phonecheck.test.misc.DeviceLockTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import org.json.JSONException
import java.util.*

/**
 * Created by Farhan on 10/15/2016.
 */

abstract class Test(var context: Context) {

    var status: Int = Test.INIT
    //var isClear = true
    @JvmField
    var completeState = NOT_TOUCHED
    var isRunning = false
    private var isAtWarning = false
    var testListener: Listener? = null
    @JvmField protected var autoPerformMode: Boolean = false

    @JvmField
    var subTests = LinkedHashMap<String, SubTest>()

    /*Filter out test result in TestResutls.json file and SharedPref. Dont keys which are FALSE in test sub ListSelector*/

    @JvmField
    var resultsFilterMap = mutableMapOf<String, Boolean>()

    protected var testStatusInfos = mutableListOf<TestStatusInfo>()


    public var adapter: SubTestAdapter? = null

    abstract val title: String

    abstract val detail: String

    abstract val iconResource: Int

    abstract val hasSubTest: Boolean

    val isPassed get() = status == Test.PASS

    open val activityRequestCode: Int get() = -1


    protected abstract val jsonKey: String

    fun hasSubTest(key: String): Boolean {
        val value = resultsFilterMap[key]
        if (value != null && value == true) {
            return true
        }
        return false
    }

    fun sub(key: String): SubTest? = subTests[key]

    fun reviewTest() {
        testStatusInfos.clear()
        if (subTests.size == 0) {
            return
        }
        for ((key, value) in subTests) {

            if (resultsFilterMap.containsKey(key)) {
                val v = resultsFilterMap[key]
                if (v != null && v == true) {
                    testStatusInfos.add(TestStatusInfo(value.title, value.value))
                }
            } else {
                testStatusInfos.add(TestStatusInfo(value.title, value.value))
            }
        }

        adapter = SubTestAdapter(context, testStatusInfos)
    }

    open fun perform(context: Context, autoPerformMode: Boolean): Int {
        //isClear = false
        return status
    }

    abstract fun requireUserInteraction(): Boolean

    open fun performUserInteraction() {}

    open fun requireActivity() = false


    interface Listener {
        fun onPerformDone()

        fun onUserInteractionDone(shouldGoNext: Boolean)

        fun onUserInteractionCancel(shouldGoNext: Boolean)
    }

    open fun startIntent(context: Context, target: Class<out Activity>, req: Int): Intent {
        val intent = Intent(context, target)
        intent.putExtra("camera_face", CameraFace.FACE_BACK.value)
        intent.putExtra("AutoStart", MainActivity.auto_start_mode)
        try {
            val activity = context as Activity
            activity.startActivityForResult(intent, req)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return intent
    }

    open fun startIntent(context: Context, target: Class<out Activity>): Intent {
        return startIntent(context,target,activityRequestCode)
    }

    fun setAtWarning() {
        isAtWarning = true
    }

    fun unSetAtWarning() {
        isAtWarning = false
    }

    /*
    * Call this for releasing any resources used by test*/
    open fun onFinish() {}

    fun onSaveFilter(storeOperation: StoreOperation) {
        for ((key, value) in resultsFilterMap) {
            storeOperation.add(key, value).save()
        }
    }

    fun onRestoreFilter(getOperation: GetOperation) {
        //for (entry in resultsFilterMap.entries) {
        //  entry.setValue(getOperation.getBoolean(entry.key, false))
        //}
    }

    @Throws(JSONException::class)
    open fun onSaveState(storeOperation: StoreOperation) {
        for ((key, value) in subTests) {

            if (resultsFilterMap.containsKey(key)) {
                val v = resultsFilterMap[key]
                if (v != null && v == true) {
                    Loader.RESULT.put(key, value.value)
//                    Loader.RESULTCOSMETICS.put(key, value.value)
                }
            } else {
                Loader.RESULT.put(key, value.value)
//                Loader.RESULTCOSMETICS.put(key, value.value)
            }

            storeOperation.add(key, value.value).save()
        }


        //storeOperation.add(title + "_clear", isClear).save()

        Log.d("RESULT", Loader.RESULT.toString(2))

        FirebaseUtil.addNew("Result").setValue(FirebaseUtil.jsonToMap(Loader.RESULT))


    }


    @Throws(JSONException::class)
    open fun onSaveStateCosmetics(storeOperation: StoreOperation) {
        for ((key, value) in subTests) {
            Loader.RESULTCOSMETICS.put(key, value.value)
            storeOperation.add(key, value.value).save()
        }
        Log.d("RESULTCOSMETICS", Loader.RESULTCOSMETICS.toString(2))

        FirebaseUtil.addNew("ResultCosmetics").setValue(FirebaseUtil.jsonToMap(Loader.RESULTCOSMETICS))
    }

    fun createResult() {
        try {
            if (!hasSubTest || this is DeviceLockTest) {
                Loader.RESULT.put(jsonKey, if (status == Test.PASS) Test.PASS else Test.FAILED)
                return
            }
            for ((key, value) in subTests) {
                if (resultsFilterMap.containsKey(key)) {
                    val v = resultsFilterMap[key]
                    if (v != null && v == true) {
                        Loader.RESULT.put(key, if (value.value == Test.PASS) Test.PASS else Test.FAILED)
                    }
                } else {
                    Loader.RESULT.put(key, if (value.value == Test.PASS) Test.PASS else Test.FAILED)
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun createCosmeticResult() {
        try {
            for ((key, value) in subTests) {
                if (resultsFilterMap.containsKey(key)) {
                    val v = resultsFilterMap[key]
                    if (v != null && v == true) {
                        Loader.RESULTCOSMETICS.put(key, if (value.value == Test.PASS) Test.PASS else Test.FAILED)
                    }
                } else {
                    Loader.RESULTCOSMETICS.put(key, if (value.value == Test.PASS) Test.PASS else Test.FAILED)
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(JSONException::class)
    open fun onRestoreState(getOperation: GetOperation): Boolean {
        //isClear = getOperation.getBoolean(title + "_clear", false)
        for ((key, value) in subTests) {
            try {
                value.value = getOperation.getInt(key, Test.INIT)

            } catch (e: Throwable) {
                value.value = Test.INIT
            }

        }
        if (status != Test.INIT) {
            reviewTest()
        }
        return false
    }

    open fun showHintMessage(): Boolean = false

    protected fun toJsonStatus(): Int = status

    companion object {

        @JvmField
        val wifiTestKey = "WiFi"
        @JvmField
        val blueToothTestKey = "Bluetooth"
        @JvmField
        val blueToothPlusTestKey = "Enhanced Bluetooth"
        @JvmField
        val gpsTestKey = "GPS"
        @JvmField
        val batteryTestKey = "Column"
        @JvmField
        val wirelessTestKey = "Wireless Charging"
        @JvmField
        val batteryDiagtestKey = "Column Diagnostic"
        @JvmField
        val vibrationTestKey = "Vibration"
        @JvmField
        val autoVibrationTestKey = "Auto Vibration"
        @JvmField
        val proximityTestKey = "Proximity Sensor"
        @JvmField
        val accelerometerTestKey = "Accelerometer"
        @JvmField
        val autoAccelerometer = "Auto Accelerometer"
        @JvmField
        val screenRotationTestKey = "Screen Rotation"
        @JvmField
        val gyroTestKey = "Gyroscope"
        @JvmField
        val fingerprintTestKey = "Fingerprint Sensor"
        @JvmField
        val samsungFingerPrint = "SamsungFingerPrint"
        @JvmField
        val nonsamsungFingerPrint = "NonSamsungFingerPrint"
        /*LCD*/
        @JvmField
        val digitizerTestKey = "Digitizer"
        @JvmField
        val multitouchTestKey = "Multi Touch Test"
        @JvmField
        val samDigitizerTestKey = "Sam Digi"
        @JvmField
        val fingerTrailDigitizer = "FingerTrail Digitizer"
        @JvmField
        val LCDTestKey = "LCD"
        @JvmField
        val LCDGlassTestKey = "LCD Glass"
        @JvmField
        val BacklightTestKey = "Brightness"
        @JvmField
        val glassConditionTestKey = "Glass Condition"
        /*Camera*/
        @JvmField
        val cameraTestKey = "Camera Test"
        @JvmField
        val cameraFrontTestKey = "Front Camera Test"
        @JvmField
        val autoFrontTestKey = "AutoSnapFront"
        @JvmField
        val cameraBackTestKey = "Back Camera Test"
        @JvmField
        val stockCameraTestKey = "Stock Camera Test"
        @JvmField
        val autoRearTestKey = "AutoSnapRear"
        @JvmField
        val barcode = "Camera AutoFocus"
        @JvmField
        val frontCameraTestKey = "Front Camera"
        @JvmField
        val rearCameraTestKey = "Rear Camera"
        @JvmField
        val rearCameraFlashTestKey = "Flash"
        @JvmField
        val frontCameraFlashTestKey = "Front Flash"
        @JvmField
        val frontCameraQualityTestKey = "Front Camera Quality"
        @JvmField
        val rearCameraQualityTestKey = "Rear Camera Quality"
        @JvmField
        val cameraAutoFocusKey = "Auto Focus"
        @JvmField
        val QRtest = "QR Test"
        @JvmField
        val barcodeFlash = "QR Flash"
        @JvmField
        val imageCaptureTime = "Image Capture Time"

        /*Call*/

        //public static final String  networkTestKey = "Network Connectivity";
        @JvmField
        val callTestKey = "Call Test"
        @JvmField
        val dualSimCallTestKey = "Dual Call Test"
        @JvmField
        val networkTestKey = "Network Connectivity"
        @JvmField
        val simReaderTestKey = "Sim Reader"
        @JvmField
        val simReaderTestKey1 = "Sim Reader 1"
        @JvmField
        val simReaderTestKey2 = "Sim Reader 2"
        @JvmField
        val networkTestKey1 = "Network Connectivity 1"
        @JvmField
        val networkTestKey2 = "Network Connectivity 2"
        @JvmField
        val callTest1Key = "Test Call 1"
        @JvmField
        val callTest2Key = "Test Call 2"

        /*Audio*/

        @JvmField
        val autoAudioFilterKey = "Auto Audio"
        @JvmField
        val autoLSFilterKey = "Auto LS"
        @JvmField
        val autoESFilterKey = "Auto ES"
        @JvmField
        val autoAudioEarpieceKey = "Code EarSpeaker"
        @JvmField
        val videoMicTestKey = "Video Microphone"
        @JvmField
        val videoMicQualityTestKey = "Video Microphone Quality"
        @JvmField
        val videoMicQualityReceiverTestKey = "Receiver Video Microphone Quality"
        @JvmField
        val micTestKey = "Microphone"
        @JvmField
        val micESTestKey = "Mic ES"
        @JvmField
        val videoESMicTestKey = "Vid Mic ES"
        @JvmField
        val micQualityTest = "Microphone Quality"
        @JvmField
        val micQualityReceiverTest = "Receiver Microphone Quality"
        @JvmField
        val loudSpeakerTestKey = "Loud Speaker"
        @JvmField
        val audioResultTestKey = "Audio Test"
        @JvmField
        val audioInputTestKey = "Audio Input"
        @JvmField
        val audioPlaybackTestKey = "Mic Playback"
        @JvmField
        val micQualityTestkey = "Mic Quality"
        @JvmField
        val micQualityReceiverkey = "Mic Quality Receiver"
        @JvmField
        val audioOutputTestKey = "Audio Output"
        @JvmField
        val headsetPortKey = "Headset Port"
        @JvmField
        val headsetRightKey = "Headset-Right"
        @JvmField
        val headsetLeftKey = "Headset-Left"
        @JvmField
        val earphoneTestKey = "Earpiece"
        @JvmField
        val disableAudioOutput = "Disable Audio Output"
        @JvmField
        val micQuality = "Speech Recognition"
        @JvmField
        val micResultTestKey = "Speech Recognition Test"
        @JvmField
        val micCheck = "Mic Check"
        @JvmField
        val micLSTestResultKey = "Mic LS Test"
        @JvmField
        val micESTestResultKey = "Mic ES Test"
        @JvmField
        val videoMicCheckTestKey = "Video Microphone Check"
        @JvmField
        val micCheckTestKey = "Microphone Check"

        @JvmField
        val micPlaybackTestKey = "Microphone PB"
        @JvmField
        val vidMicPlaybackTestKey = "Video Mic PB"

        @JvmField
        val earSpeakerPlaybackTestKey = "ES Playback"

        /*Buttons*/
        @JvmField
        val multitaskTestKey = "Multitask Button"
        @JvmField
        val menuButtonTestKey = "Menu Button"
        @JvmField
        val buttonTestKey = "Buttons Test"
        @JvmField
        val homeButtonTestKey = "Home Button"
        @JvmField
        val powerButtonTestKey = "Power Button"
        @JvmField
        val backButtonTestKey = "Back Button"
        @JvmField
        val cameraButtonTestKey = "Camera Button"
        @JvmField
        val samsungActiveButtonTestKey = "Active Button"
        @JvmField
        val samsungBixbyButtonTestKey = "Bixby Button"
        @JvmField
        val volumeUpButtonTestKey = "Volume Up Button"
        @JvmField
        val volumeDownButtonTestKey = "Volume Down Button"
        @JvmField
        val flipSwitchTestKey = "Mute Button"
        /*Samsung SPen */
        @JvmField
        val spenTestKey = "SPen"
        @JvmField
        val spenRemoveTestKey = "SPen Remove"
        @JvmField
        val spenHoverTestKey = "SPen Hover"
        @JvmField
        val spenButtonTestKey = "Spen Plus buttons"
        @JvmField
        val spenMenu = "SPen Menu Button"
        @JvmField
        val spenRecent = "SPen Back Button"
        /*Samsung Edge screen*/
        @JvmField
        val edgeScreenTestKey = "Edge Screen"
        /*Device Lock*/
        @JvmField
        val deviceLockTestKey = "Device Lock"

        /*Non Test Results Key*/
        @JvmField
        val deviceInfoKey = "device_lock"
        @JvmField
        val samsungDeviceInfoKey = "samsung_lock"
        @JvmField
        val frpLockTestKey = "frp_device_lock"
        @JvmField
        val samsungLockTestKey = "samsung_device_lock"
        @JvmField
        val ScreenTestKey = "ScreenTestKey"

        @JvmField
        val FRP_LOCK = "frp_lock"

        @JvmField
        val cosmeticsTestKey = "Cosmetics"

        @JvmField
        val gradesTestKey = "Grading"

        @JvmField
        val PASS = 0
        @JvmField
        val FAILED = 1
        @JvmField
        val INIT = 2

        @JvmField
        val NOT_TOUCHED = 0
        @JvmField
        val TOUCHED = 1
        @JvmField
        val COMPLETED = 2
        @JvmField
        var manualVibrationKey = "Manual Vibration"

        @JvmField
        var autoVibrationKey = "Vibrate123"
        val nfctest: String = "NFC"
    }

}
