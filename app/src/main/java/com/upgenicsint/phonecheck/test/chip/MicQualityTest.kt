package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import android.content.pm.PackageManager
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import radonsoft.net.rta.RTA


class MicQualityTest(context: Context) : Test(context) {
    override val title: String
        get() = context.getString(R.string.mic_quality)
    override val detail: String
        get() = context.getString(R.string.mic_quality_desc)
    
    override val iconResource: Int
        get() = R.drawable.microphone
    
    override val hasSubTest: Boolean
        get() = true
    
    override val jsonKey = Test.micQualityTestkey

    override val activityRequestCode = RTA.REQ

    override fun requireUserInteraction() = true

    init {
        subTests.put(Test.micQualityTest, SubTest(context.getString(R.string.microphone)))
        resultsFilterMap.put(Test.videoMicQualityTestKey, false)

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            resultsFilterMap.put(Test.videoMicQualityTestKey, true)
            subTests.put(Test.videoMicQualityTestKey, SubTest(context.getString(R.string.video_microphone)))
        } else {
            resultsFilterMap.put(Test.videoMicQualityTestKey, false)
        }
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.micQualityTestkey, toJsonStatus())
        storeOperation.add(jsonKey, status)
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        Loader.RESULT.put(Test.micQualityTestkey, toJsonStatus())
        return super.onRestoreState(getOperation)
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, RTA::class.java)
        return super.perform(context, autoPerformMode)
    }
}