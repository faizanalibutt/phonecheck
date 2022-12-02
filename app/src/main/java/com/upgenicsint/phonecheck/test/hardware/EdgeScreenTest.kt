package com.upgenicsint.phonecheck.test.hardware

import android.content.Context

import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.SEdgeActivity
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

/**
 * Created by Farhan on 10/24/2016.
 */

class EdgeScreenTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.edgeScreenTestKey

    override val title: String
        get() = context.getString(R.string.edge_title)

    override val detail: String
        get() = context.getString(R.string.edge_desc)

    override val iconResource: Int
        get() = R.drawable.touch

    override val activityRequestCode: Int
        get() = SEdgeActivity.REQ

    override val hasSubTest: Boolean
        get() = false

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        startIntent(context, SEdgeActivity::class.java, SEdgeActivity.REQ)
        isRunning = false
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.edgeScreenTestKey, toJsonStatus())
        storeOperation.add(Test.edgeScreenTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.edgeScreenTestKey, status)
        Loader.RESULT.put(Test.edgeScreenTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireActivity(): Boolean {
        return true
    }

    override fun requireUserInteraction(): Boolean {
        return false
    }
}
