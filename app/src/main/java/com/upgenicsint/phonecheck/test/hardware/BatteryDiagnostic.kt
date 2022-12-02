package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.BatteryDiagnosticActivity
import com.upgenicsint.phonecheck.activities.TouchTestActivity
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException


class BatteryDiagnostic (context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.batteryDiagtestKey

    override val title: String
        get() = context.getString(R.string.battery_diag)

    override val detail: String
        get() = context.getString(R.string.battery_desc)

    override val iconResource: Int
        get() = R.drawable.battery

    override val hasSubTest: Boolean
        get() = false

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        startIntent(context, BatteryDiagnosticActivity::class.java, BatteryDiagnosticActivity.REQ)
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

    override fun requireUserInteraction() = false

    override fun requireActivity() = true}