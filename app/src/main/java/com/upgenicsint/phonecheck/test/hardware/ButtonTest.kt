package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Locale.LanguageSupport
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.models.HardwareTest
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.Utils
import org.json.JSONException

/**
 * Created by Farhan on 10/17/2016.
 */

class ButtonTest(context: Context) : Test(context) {
    var passCount = 0
    @JvmField
    var hardwareTestArrayList = mutableListOf<HardwareTest>()
    var hardwareTestArrayList2 = mutableListOf<HardwareTest>()

    override val jsonKey: String
        get() = Test.buttonTestKey

    override val title: String
        get() = context.getString(R.string.buttonTitle)

    override val detail: String
        get() = context.getString(R.string.ButtonTestText)

    override val iconResource: Int
        get() = R.drawable.keyboard

    override val activityRequestCode: Int
        get() = ButtonsTestActivity.REQ

    override val hasSubTest: Boolean
        get() = true

    init {
        if (ButtonTest.hasHomeButton(context)) {
            val subTest = SubTest(context.getString(R.string.home_button))
            subTests.put(Test.homeButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.home_button, R.drawable.home_button_pass, KeyEvent.KEYCODE_HOME, subTest))

        }
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)) {
            val subTest = SubTest(context.getString(R.string.power_button))
            subTests.put(Test.powerButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.power, R.drawable.power_pass, KeyEvent.KEYCODE_POWER, subTest))
        }
        if (Utils.hasMenuButton(context)) {
            val subTest = SubTest(context.getString(R.string.menu_button))
            subTests.put(Test.menuButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.recent_button, R.drawable.recent_button_pass, KeyEvent.KEYCODE_MENU, subTest))
            hardwareTestArrayList2.add(HardwareTest(R.drawable.recent_button, R.drawable.recent_button_pass, KeyEvent.KEYCODE_MENU, subTest))
        }
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP)) {
            val subTest = SubTest(context.getString(R.string.volume_up))
            subTests.put(Test.volumeUpButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.volume_up, R.drawable.volume_up_pass, KeyEvent.KEYCODE_VOLUME_UP, subTest))
        }
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN)) {
            val subTest = SubTest(context.getString(R.string.volume_down))
            subTests.put(Test.volumeDownButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.volume_down, R.drawable.volume_down_pass, KeyEvent.KEYCODE_VOLUME_DOWN, subTest))


        }
        if (ButtonTest.hasBackButton(context)) {
            val subTest = SubTest(context.getString(R.string.back_button))
            subTests.put(Test.backButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.back_button, R.drawable.back_button_pass, KeyEvent.KEYCODE_BACK, subTest))
            hardwareTestArrayList2.add(HardwareTest(R.drawable.back_button, R.drawable.back_button_pass, KeyEvent.KEYCODE_BACK, subTest))

        }
        if (Utils.isSonyXperia && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_CAMERA)) {
            val subTest = SubTest(context.getString(R.string.camera_button))
            subTests.put(Test.cameraButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.camera_hardware_button, R.drawable.camera_hardware_button_pass, KeyEvent.KEYCODE_CAMERA, subTest))
        }

        val key = Utils.samsungActiveModelCode
        if (key != -1) {
            val subTest = SubTest(context.getString(R.string.active_button))
            subTests.put(Test.samsungActiveButtonTestKey, subTest)
            hardwareTestArrayList.add(HardwareTest(R.drawable.camera_hardware_button, R.drawable.camera_hardware_button_pass, key, subTest))
        }
//        val bix_key = Utils.samsungBixbyModelCode
//        if (bix_key != -1) {
//        val subTest = SubTest("Bixby Button")
//            subTests.put(Test.samsungBixbyButtonTestKey,subTest )
//            hardwareTestArrayList.add(HardwareTest(R.drawable.bixby_normal, R.drawable.bixby_pass, bix_key, subTest))
//        }
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        if (isRunning) {
            return status
        }
        isRunning = true
        passCount = 0
        startIntent(context, ButtonsTestActivity::class.java, activityRequestCode)
        return status
    }


    fun getKeyForKeyCode(keyCode: Int): HardwareTest? {
        for (hardwareTest in hardwareTestArrayList) {
            if (hardwareTest.keyCode == keyCode) {
                return hardwareTest
            }
        }
        return null
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {

        storeOperation.add(Test.buttonTestKey, status)
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.buttonTestKey, status)
        return super.onRestoreState(getOperation)
    }

    override fun requireActivity() = true

    override fun requireUserInteraction() = false

    companion object {
        @JvmField
        val ACTIVE_KEYCODE_1015 = 1015
        @JvmField
        val ACTIVE_KEYCODE_275 = 275
        @JvmField
        val ACTIVE_KEYCODE_238 = 238
        @JvmField
        val BIXBY_KEYCODE_1082 = 1082

        @JvmStatic
        fun hasBackButton(context: Context): Boolean {
            if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK) || Utils.hasNavBar(context.resources)) {
                return true
            } else if (Build.MODEL.containsIgnoreCase("SM-J700")) {
                return true
            }
            return false
        }

        @JvmStatic
        fun hasHomeButton(context: Context): Boolean {
            if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME) || Utils.hasNavBar(context.resources)) {
                return true
            } else if (Build.MODEL.containsIgnoreCase("HTC 10")) {
                return true
            }
            return false
        }
    }
}

