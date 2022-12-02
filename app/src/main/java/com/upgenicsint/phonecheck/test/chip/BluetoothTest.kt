package com.upgenicsint.phonecheck.test.chip

import android.bluetooth.BluetoothAdapter
import android.content.Context

import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

/**
 * Created by Farhan on 10/15/2016.
 */

class BluetoothTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.blueToothTestKey

    override val title: String
        get() = "Bluetooth"

    override val detail: String
        get() = context.getString(R.string.bluetooth_desc)

    override val iconResource: Int
        get() = R.drawable.bluetooth

    override val hasSubTest: Boolean
        get() = false

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        isRunning = false
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        status = if(bluetoothAdapter != null && (bluetoothAdapter.isEnabled || bluetoothAdapter.isDiscovering)) Test.PASS else Test.FAILED
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.blueToothTestKey, toJsonStatus())
        storeOperation.add(Test.blueToothTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.blueToothTestKey, status)
        Loader.RESULT.put(Test.blueToothTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction(): Boolean {
        return false
    }

}
