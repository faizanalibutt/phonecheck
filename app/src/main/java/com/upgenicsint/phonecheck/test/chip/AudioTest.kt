package com.upgenicsint.phonecheck.test.chip

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AudioTestActivity
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

/**
 * Created by Farhan on 10/28/2016.
 */

class AudioTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.audioResultTestKey

    override val title: String
        get() = "Audio"

    override val detail: String
        get() = "You will need handsfree or headset for this test as well."

    override val iconResource: Int
        get() = R.drawable.audio

    override val activityRequestCode: Int
        get() = AudioTestActivity.REQ

    override val hasSubTest: Boolean
        get() = true


    init {
        val enableHeadSetTest = if (Loader.instance.isTestListLoaded) Loader.instance.filterContains(Test.headsetLeftKey) && Loader.instance.filterContains(Test.headsetRightKey) else true


        resultsFilterMap.put(Test.earphoneTestKey, false)
        resultsFilterMap.put(Test.videoMicTestKey, false)

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            resultsFilterMap.put(Test.videoMicTestKey, true)
            subTests.put(Test.videoMicTestKey, SubTest("Video Microphone"))
        } else {
            resultsFilterMap.put(Test.videoMicTestKey, false)
        }
        subTests.put(Test.micTestKey, SubTest("Microphone"))

        if (hasHeadphoneJack() && enableHeadSetTest) {

            resultsFilterMap.put(Test.headsetPortKey, true)
            resultsFilterMap.put(Test.headsetLeftKey, true)
            resultsFilterMap.put(Test.headsetRightKey, true)

            subTests.put(Test.headsetPortKey, SubTest("Headset Port"))
            subTests.put(Test.headsetLeftKey, SubTest("Left HeadSet"))
            subTests.put(Test.headsetRightKey, SubTest("Right HeadSet"))
        }

        val pm = context.packageManager


        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            resultsFilterMap.put(Test.videoMicTestKey, true)
        } else {
            subTests.remove(Test.videoMicTestKey)
            resultsFilterMap.put(Test.videoMicTestKey, false)
        }

        if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY) && context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Logy.d("AudioTest", "Earpiece FEATURE_SENSOR_PROXIMITY")
            resultsFilterMap.put(Test.earphoneTestKey, true)
            subTests.put(Test.earphoneTestKey, SubTest("Earpiece"))
        } else {
            resultsFilterMap.put(Test.earphoneTestKey, false)
        }

        subTests.put(Test.loudSpeakerTestKey, SubTest("Loud Speaker"))
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)

        if (Nammu.checkPermission(Manifest.permission.RECORD_AUDIO)
                && Nammu.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                && Nammu.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            startIntent(context, AudioTestActivity::class.java, activityRequestCode)
        } else {
            Nammu.askForPermission(context as Activity, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), object : PermissionCallback {
                override fun permissionGranted() {
                    startIntent(context, AudioTestActivity::class.java, activityRequestCode)
                }

                override fun permissionRefused() {

                }
            })
        }
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        //Loader.RESULT.put(audioResultTestKey, toJsonStatus());
        storeOperation.add(Test.audioResultTestKey, status)
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.audioResultTestKey, status)
        return super.onRestoreState(getOperation)
    }

    override fun requireActivity() = true

    override fun requireUserInteraction() = false

    companion object {

        @JvmStatic fun hasHeadphoneJack():Boolean {

            return !Build.MODEL.contains("XT1650") ||  !Build.MODEL.contains("G011") || !Build.MODEL.contains("Pixel 2")

        }
    }
}
