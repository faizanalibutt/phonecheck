package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.SpeechRecognization
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by zohai on 1/3/2018.
 */
class SpeechToTextTest(context: Context) : Test(context) {

    override val title: String
        get() = context.getString(R.string.speechRecog_title)
    override val detail: String
        get() = context.getString(R.string.spechRecog_desc)
    override val iconResource: Int
        get() = R.drawable.audio
    override val hasSubTest: Boolean
        get() = false
    override val jsonKey = Test.micQuality

    override val activityRequestCode = SpeechRecognization.REQ

    override fun requireUserInteraction() = true

    override fun requireActivity() = true

//    init {
//        subTests.put(Test.micQuality, SubTest(context.getString(R.string.microphone)))
//    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.micQuality, toJsonStatus())
        storeOperation.add(jsonKey, status)
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        Loader.RESULT.put(Test.micQuality, toJsonStatus())
        return super.onRestoreState(getOperation)
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, SpeechRecognization::class.java)
        return super.perform(context, autoPerformMode)
    }
}