package com.upgenicsint.phonecheck.test.misc

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.GradingsActivity
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

class GradesTest (context: Context) : Test(context) {

    override val title: String get() = "Grading Test"
    override val detail: String get() = "Please give grade to the tested device"
    override val iconResource: Int get() = R.drawable.grading
    override val hasSubTest: Boolean get() = false
    override val jsonKey: String get() = Test.gradesTestKey
    override val activityRequestCode = GradingsActivity.REQ
    override fun requireActivity(): Boolean { return true }
    override fun requireUserInteraction(): Boolean { return true }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, GradingsActivity::class.java)
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