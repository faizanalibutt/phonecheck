package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import android.location.LocationManager

import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

/**
 * Created by Farhan on 10/15/2016.
 */

class GPSTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.gpsTestKey

    override val title: String
        get() = "GPS"

    override val detail: String
        get() = context.getString(R.string.gps_desc)

    override val iconResource: Int
        get() = R.drawable.gps

    override val hasSubTest: Boolean
        get() = false

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        isRunning = false
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        status = if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) Test.PASS else Test.FAILED
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.gpsTestKey, toJsonStatus())
        storeOperation.add(Test.gpsTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.gpsTestKey, status)
        Loader.RESULT.put(Test.gpsTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction() = false
}
