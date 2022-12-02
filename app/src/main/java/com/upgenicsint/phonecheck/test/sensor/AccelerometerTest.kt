package com.upgenicsint.phonecheck.test.sensor

import android.content.Context

import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AccelerometerActivity
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

/**
 * Created by Farhan on 10/27/2016.
 */

class AccelerometerTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.accelerometerTestKey

    override val title: String
        get() = context.getString(R.string.accelerometer)

    override val detail: String
        get() = context.getString(R.string.accelero_desc)

    override val iconResource: Int
        get() = R.drawable.accelerometer

    override val activityRequestCode: Int
        get() = AccelerometerActivity.REQ

    override val hasSubTest: Boolean
        get() = true

    init {
        subTests.put(Test.accelerometerTestKey, SubTest(context.getString(R.string.accelerometer)))
        subTests.put(Test.gyroTestKey, SubTest(context.getString(R.string.gyro_title)))
        subTests.put(Test.screenRotationTestKey, SubTest(context.getString(R.string.screen_rotation)))
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        startIntent(context, AccelerometerActivity::class.java, activityRequestCode)
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.accelerometerTestKey, toJsonStatus())
        storeOperation.add(Test.accelerometerTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.accelerometerTestKey, status)
        Loader.RESULT.put(Test.accelerometerTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireActivity(): Boolean {
        return true
    }

    override fun requireUserInteraction(): Boolean {
        return true
    }

}
