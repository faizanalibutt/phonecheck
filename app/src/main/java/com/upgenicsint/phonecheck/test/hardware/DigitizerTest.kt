package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.DigitizerActivity
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by farhanahmed on 22/11/2017.
 */
class DigitizerTest(context: Context) : Test(context) {
    override fun requireUserInteraction() = false

    override val title: String = context.getString(R.string.digitizer_title)
    override val detail: String = context.getString(R.string.digitizer_desc)
    override val iconResource = R.drawable.touch
    override val hasSubTest = false
    override val activityRequestCode = 15
    override fun requireActivity() = true
    override val jsonKey = Test.digitizerTestKey

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        startIntent(context, DigitizerActivity::class.java, DigitizerActivity.REQ)
        isRunning = false
        return status

    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        storeOperation.add(jsonKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        return super.onRestoreState(getOperation)
    }


}