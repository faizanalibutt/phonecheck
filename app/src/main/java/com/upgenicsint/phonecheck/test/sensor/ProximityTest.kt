package com.upgenicsint.phonecheck.test.sensor

import android.content.Context

import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity
import com.upgenicsint.phonecheck.activities.ProximityActivity
import com.upgenicsint.phonecheck.misc.ProximitySensorManager
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

/**
 * Created by farhanahmed on 21/10/2016.
 */

class ProximityTest(context: Context) : Test(context) {

    override val title: String
        get() = context.getString(R.string.proximityTitle)

    override val detail: String
        get() = context.getString(R.string.proximity_desc)

    override val iconResource: Int
        get() = R.drawable.proximity

    override val hasSubTest: Boolean
        get() = false

    override val jsonKey: String
        get() = Test.proximityTestKey

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, ProximityActivity::class.java)
        return super.perform(context, autoPerformMode)
    }

    override fun requireUserInteraction() = true
    override fun requireActivity() = true
    override val activityRequestCode = ProximityActivity.REQ


    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.proximityTestKey, toJsonStatus())
        storeOperation.add(Test.proximityTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.proximityTestKey, status)
        Loader.RESULT.put(Test.proximityTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }
}
