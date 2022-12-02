package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.MultiTouchTestActivity
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

class MultiTouchTest (context: Context) : Test(context) {
    override fun requireUserInteraction() = false

    override val title: String = context.getString(R.string.multitouch_title)
    override val detail: String = context.getString(R.string.multitouch_desc)
    override val iconResource = R.drawable.multitouch
    override val hasSubTest = false
    override val activityRequestCode = MultiTouchTestActivity.REQ
    override fun requireActivity() = true
    override val jsonKey = Test.multitouchTestKey

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, MultiTouchTestActivity::class.java)
        return super.perform(context, autoPerformMode)
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