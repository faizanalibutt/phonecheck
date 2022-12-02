package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.Loader.Companion.jsonObjectFile
import com.upgenicsint.phonecheck.MictestSupportedModels
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.MicESTestActivity
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

class MicESTest (context: Context) : Test(context) {

    override val title: String
        get() = "Mic Ear Speaker"
    override val detail: String
        get() = "Test the mic of this device with ear speaker"
    override val iconResource: Int
        get() = R.drawable.audio
    override val hasSubTest: Boolean
        get() = true
    override val jsonKey = Test.micESTestResultKey

    override val activityRequestCode = MicESTestActivity.REQ

    override fun requireUserInteraction() = true

    init {

        val enableHeadSetTest = if (Loader.instance.isTestListLoaded)
            Loader.instance.filterContains(Test.headsetPortKey)
                    && Loader.instance.filterContains(Test.headsetLeftKey)
                    && Loader.instance.filterContains(Test.headsetRightKey) else true

        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY) && pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && Devices.hasProximity()) {
            Logy.d("MicESTest", "Earpiece FEATURE_SENSOR_PROXIMITY")
            resultsFilterMap.put(Test.earphoneTestKey, true)
            subTests.put(Test.earphoneTestKey, SubTest(context.getString(R.string.earpiece)))
        } else {
            resultsFilterMap.put(Test.earphoneTestKey, false)
        }

        if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())){
            resultsFilterMap.put(Test.videoESMicTestKey, false)
            subTests.put(Test.micESTestKey, SubTest(context.getString(R.string.microphone)))

            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                resultsFilterMap.put(Test.videoESMicTestKey, true)
                subTests.put(Test.videoESMicTestKey, SubTest(context.getString(R.string.video_microphone)))
            } else {
                resultsFilterMap.put(Test.videoESMicTestKey, false)
            }
        }

        val jsonObjectFile = ReadTestJsonFile.getInstance().androidConfiguration
        /*val headset : Boolean = if (jsonObjectFile.has("HeadPhoneJack")) jsonObjectFile.getBoolean("HeadPhoneJack") else false*/

        var headset = true

        if (Devices.hasHeadPhoneJack()) {
            headset = true
        } else {
            headset = false
        }

        if (headset) {

            if (jsonObjectFile.has("HeadPhoneJack")) {
                if (!jsonObjectFile.getBoolean("HeadPhoneJack")) {
                    headset = false
                }
                else {
                    headset = true
                }
            }
            else {
                headset = true
            }

        }

        val reportHeadset = context.getSharedPreferences(context.getString(R.string.report_headset), Context.MODE_PRIVATE)
                .getBoolean(context.getString(R.string.check_headset), false)

        var addHeadsetTest = headset && enableHeadSetTest && !reportHeadset

        if (Build.MODEL.equals("Pixel")) {
            addHeadsetTest = true
        }

        resultsFilterMap.put(Test.headsetPortKey, addHeadsetTest)
        resultsFilterMap.put(Test.headsetLeftKey, addHeadsetTest)
        resultsFilterMap.put(Test.headsetRightKey, addHeadsetTest)

        if (addHeadsetTest) {
            subTests.put(Test.headsetPortKey, SubTest(context.getString(R.string.headset_port)))
            subTests.put(Test.headsetLeftKey, SubTest(context.getString(R.string.left_headset)))
            subTests.put(Test.headsetRightKey, SubTest(context.getString(R.string.right_headset)))
        }
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        storeOperation.add(jsonKey, status)
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        return super.onRestoreState(getOperation)
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, MicESTestActivity::class.java)
        return super.perform(context, autoPerformMode)
    }
}