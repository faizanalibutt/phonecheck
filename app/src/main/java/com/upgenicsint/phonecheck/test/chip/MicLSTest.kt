package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import android.content.pm.PackageManager
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.MicLSTestActivity
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

class MicLSTest (context: Context) : Test(context) {
    override val title: String
        get() = "Mic Loud Speaker"
    override val detail: String
        get() = "Test the mic of this device with loud speaker"
    override val iconResource: Int
        get() = R.drawable.audio
    override val hasSubTest: Boolean
        get() = true
    override val jsonKey = Test.micLSTestResultKey

    override val activityRequestCode = MicLSTestActivity.REQ

    override fun requireUserInteraction() = true

    init {
        resultsFilterMap.put(Test.videoMicTestKey, false)

        subTests.put(Test.micTestKey, SubTest(context.getString(R.string.microphone)))

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            resultsFilterMap.put(Test.videoMicTestKey, true)
            subTests.put(Test.videoMicTestKey, SubTest(context.getString(R.string.video_microphone)))
        } else {
            resultsFilterMap.put(Test.videoMicTestKey, false)
        }

        subTests.put(Test.loudSpeakerTestKey, SubTest(context.getString(R.string.loud_speaker)))

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
        startIntent(context, MicLSTestActivity::class.java)
        return super.perform(context, autoPerformMode)
    }

}