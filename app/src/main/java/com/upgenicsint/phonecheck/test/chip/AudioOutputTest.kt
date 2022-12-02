package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import android.content.pm.PackageManager
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AudioInputTestActivity
import com.upgenicsint.phonecheck.activities.AudioOutputTestActivity
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by farhanahmed on 24/11/2017.
 */

class AudioOutputTest(context: Context) : Test(context) {
    override val title = context.getString(R.string.audioOuput_title)
    override val detail = context.getString(R.string.audioOutput_desc)
    override val iconResource = R.drawable.audio
    override val hasSubTest = true
    override val jsonKey = Test.audioOutputTestKey

    override val activityRequestCode = AudioOutputTestActivity.REQ

    override fun requireUserInteraction() = true

    init {
        val enableHeadSetTest = if (Loader.instance.isTestListLoaded) Loader.instance.filterContains(Test.headsetLeftKey) && Loader.instance.filterContains(Test.headsetRightKey) else true

        resultsFilterMap.put(Test.videoMicTestKey, false)

        val pm = context.packageManager

        if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY) && pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && Devices.hasProximity()) {
            Logy.d("AudioTest", "Earpiece FEATURE_SENSOR_PROXIMITY")
            resultsFilterMap.put(Test.earphoneTestKey, true)
            subTests.put(Test.earphoneTestKey, SubTest(context.getString(R.string.earpiece)))
        } else {
            resultsFilterMap.put(Test.earphoneTestKey, false)
        }

        val addHeadsetTest = Devices.hasHeadPhoneJack() && enableHeadSetTest

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
        startIntent(context, AudioOutputTestActivity::class.java)
        return super.perform(context, autoPerformMode)
    }
}