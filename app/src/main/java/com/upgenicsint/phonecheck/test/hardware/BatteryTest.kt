package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.BatteryInfo
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by Farhan on 10/17/2016.
 */

class BatteryTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.batteryTestKey

    override val title: String
        get() = context.getString(R.string.battery_title)

    override val detail: String
        get() = context.getString(R.string.battery_desc)

    override val iconResource: Int
        get() = R.drawable.battery

    override val hasSubTest: Boolean
        get() = false

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        val batteryInfo = BatteryInfo(context)
        batteryInfo.logBatteryInformation()
        if(Build.MODEL.toLowerCase().contains("vs988")){
            status = if (isConnected(context)) Test.PASS else Test.FAILED
        } else{
            status =  if(batteryInfo.health == "Good" && isConnected(context)) Test.PASS else Test.FAILED
        }
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.batteryTestKey, toJsonStatus())
        storeOperation.add(Test.batteryTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.batteryTestKey, status)
        Loader.RESULT.put(Test.batteryTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction() = false

    companion object {

        fun isConnected(context: Context): Boolean {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || isCharging

        }
    }
}
