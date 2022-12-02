@file:JvmName("ExtensionsUtils")
package com.upgenicsint.phonecheck

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.upgenicsint.phonecheck.activities.AccelerometerActivity
import com.upgenicsint.phonecheck.activities.AccelerometerActivity.Companion.ACCEL_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.AudioPlaybackTestActivity.Companion.MIC_PLAYBACK_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.BrightnessActivity.Companion.BRIGHTNESS_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity.Companion.BUTTON_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CallActivity
import com.upgenicsint.phonecheck.activities.CallActivity.Companion.CALL_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CameraTestActivity.Companion.FRONT_CAMERA_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CameraTestActivity.Companion.REAR_CAMERA_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CosmeticsTestActivity.COSMETICS_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.DigitizerActivity.Companion.DIGITIZER_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.FingerPrintActivity.Companion.FINGERPRINT_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.GradingsActivity.Companion.GRADES_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.MainActivity
import com.upgenicsint.phonecheck.activities.MicESTestActivity.Companion.MICES_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.MicLSTestActivity.Companion.MICLS_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.MultiTouchTestActivity.MULTI_TOUCH_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.NFCActivity.Companion.NFC_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.ProximityActivity.Companion.PROXIMITY_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SEdgeActivity.Companion.SEDGE_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SPenActivity.Companion.SPENTEST_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SpeechRecognization.Companion.SPEECH_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SpenButtonsTestActivity.Companion.SPENBUTTON_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.TouchTestActivity.Companion.LCD_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.WirelessChargingActivity.Companion.WIRELESS_SCREEN_TIME
import com.upgenicsint.phonecheck.barcode.CaptureActivity.BARCODE_SCREEN_TIME

/**
 * Created by farhanahmed on 15/10/2017.
 */

fun String.containsIgnoreCase(s2: String) = toLowerCase().contains(s2.toLowerCase())
fun Activity.toastLong(msg:String)
{
    Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
}
fun Activity.toast(msg:String)
{
    Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
}

fun getDefaultScreenTimeValues() {
    ACCEL_SCREEN_TIME = 0
    MIC_PLAYBACK_SCREEN_TIME = 0
    BARCODE_SCREEN_TIME = 0
    BRIGHTNESS_SCREEN_TIME = 0
    BUTTON_SCREEN_TIME = 0
    CALL_SCREEN_TIME = 0
    FRONT_CAMERA_SCREEN_TIME = 0
    REAR_CAMERA_SCREEN_TIME = 0
    COSMETICS_SCREEN_TIME = 0
    DIGITIZER_SCREEN_TIME = 0
    FINGERPRINT_SCREEN_TIME = 0
    GRADES_SCREEN_TIME = 0
    MICES_SCREEN_TIME = 0
    MICLS_SCREEN_TIME = 0
    MULTI_TOUCH_SCREEN_TIME = 0
    NFC_SCREEN_TIME = 0
    PROXIMITY_SCREEN_TIME = 0
    SEDGE_SCREEN_TIME = 0
    SPEECH_SCREEN_TIME = 0
    SPENTEST_SCREEN_TIME = 0
    SPENBUTTON_SCREEN_TIME = 0
    LCD_SCREEN_TIME = 0
    WIRELESS_SCREEN_TIME = 0
}

fun removeAllReprotPreferences(activity: MainActivity) {
    Loader.instance.RECORD_COUNTER = 0
    AccelerometerActivity.recordUserTestCameToScreen = false
    activity.getSharedPreferences(activity.getString(R.string.record_tests), Context.MODE_PRIVATE).edit().clear().apply()
}