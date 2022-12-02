package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import android.content.pm.PackageManager
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AudioPlaybackTestActivity
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

class AudioPlaybackTest(context: Context) : Test(context) {

    override val title: String
        get() = context.getString(R.string.mic_playback)
    override val detail: String
        get() = context.getString(R.string.playback_description)
    override val iconResource: Int
        get() = R.drawable.audio
    override val hasSubTest: Boolean
        get() = true
    override val jsonKey = Test.audioPlaybackTestKey

    override val activityRequestCode = AudioPlaybackTestActivity.REQ

    override fun requireUserInteraction() = true

    init {
        subTests.put(Test.micPlaybackTestKey, SubTest(context.getString(R.string.microphone)))

        resultsFilterMap.put(Test.vidMicPlaybackTestKey, false)

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            resultsFilterMap.put(Test.vidMicPlaybackTestKey, true)
            subTests.put(Test.vidMicPlaybackTestKey, SubTest(context.getString(R.string.video_microphone)))
        } else {
            resultsFilterMap.put(Test.vidMicPlaybackTestKey, false)
        }
        val pm = context.packageManager
        /*if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY) &&
                pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                && Devices.hasProximity() && Loader.instance.isESPlayBack) {
            resultsFilterMap.put(Test.earSpeakerPlaybackTestKey, true)
            subTests.put(Test.earSpeakerPlaybackTestKey, SubTest(context.getString(R.string.earSpeaker)))
        } else {
            resultsFilterMap.put(Test.earSpeakerPlaybackTestKey, false)
        }*/
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
        startIntent(context, AudioPlaybackTestActivity::class.java)
        return super.perform(context, autoPerformMode)
    }
}