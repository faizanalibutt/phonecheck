package com.upgenicsint.phonecheck.test.chip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import radonsoft.net.rta.RTA

/**
 * Created by zohai on 3/27/2018.
 */
class ReceiverAndMic(context: Context) : Test(context) {
    override val title: String
        get() = "Receiver and Mic"
    override val detail: String
        get() = context.getString(R.string.mic_quality_desc)

    override val iconResource: Int
        get() = R.drawable.microphone

    override val hasSubTest: Boolean
        get() = true

    override val jsonKey = Test.micQualityReceiverkey

    override val activityRequestCode = RTA.REQ

    override fun requireUserInteraction() = true

    init {
        subTests.put(Test.micQualityReceiverTest, SubTest(context.getString(R.string.microphone)))
        resultsFilterMap.put(Test.videoMicQualityReceiverTestKey, false)

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            resultsFilterMap.put(Test.videoMicQualityReceiverTestKey, true)
            subTests.put(Test.videoMicQualityReceiverTestKey, SubTest(context.getString(R.string.video_microphone)))
        } else {
            resultsFilterMap.put(Test.videoMicQualityReceiverTestKey, false)
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
//        startIntent(context, RTA::class.java)
        val intent = Intent(context, RTA::class.java)
        intent.putExtra("Receiver", true)
        val activity = context as Activity
        activity.startActivity(intent)
        return super.perform(context, autoPerformMode)
    }
}