package com.upgenicsint.phonecheck.test.sensor

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AutoVibration
import com.upgenicsint.phonecheck.activities.AutoVibrationActivity
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by faizi on 9/17/2018.
 */


class AutoVibratorTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.autoVibrationTestKey

    override val hasSubTest: Boolean
        get() = false

    override val title: String
        get() = context.getString(R.string.vibration_title)

    override val detail: String
        get() = context.getString(R.string.vibration_desc)

    override val iconResource: Int
        get() = R.drawable.vibration

    override val activityRequestCode = AutoVibrationActivity.REQ

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, AutoVibration::class.java)
        return super.perform(context, autoPerformMode)
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.autoVibrationTestKey, toJsonStatus())
        storeOperation.add(Test.autoVibrationTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.autoVibrationTestKey, status)
        Loader.RESULT.put(Test.autoVibrationTestKey, toJsonStatus())
        return super.onRestoreState(getOperation)
    }
    override fun requireActivity(): Boolean {
        return true
    }

    override fun requireUserInteraction() = true
}