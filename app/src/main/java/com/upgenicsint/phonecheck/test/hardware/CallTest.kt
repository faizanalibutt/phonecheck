package com.upgenicsint.phonecheck.test.hardware

import android.Manifest
import android.app.Activity
import android.content.Context
import android.widget.Toast

import com.farhanahmed.cabinet.operations.GetOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.CallActivity
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

/**
 * Created by Farhan on 10/29/2016.
 */

class CallTest(context: Context) : Test(context) {
    override val jsonKey: String
        get() = Test.networkTestKey

    override val title: String
        get() = context.getString(R.string.callTest_title)

    override val detail: String
        get() = context.getString(R.string.callTest_desc)

    override val iconResource: Int
        get() = R.drawable.phone

    override val activityRequestCode: Int
        get() = CallTest.REQ

    override val hasSubTest: Boolean
        get() = true

    init {

        subTests.put(Test.simReaderTestKey, SubTest(context.getString(R.string.sim_reader)))
        subTests.put(Test.networkTestKey, SubTest(context.getString(R.string.network_connec)))
        if (Devices.isS10Available()) {
            subTests[Test.proximityTestKey] = SubTest(context.getString(R.string.proximityTitle))
        }
    }


    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
       
        val activity = context as Activity
        if (Nammu.checkPermission(Manifest.permission.CALL_PHONE)) {
            callPhone(context)
        } else {
            Nammu.askForPermission(activity, Manifest.permission.CALL_PHONE, object : PermissionCallback {
                override fun permissionGranted() {
                    callPhone(context)
                }

                override fun permissionRefused() {
                    Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                }
            })
        }
        return status
    }

    private fun callPhone(context: Context) {
        startIntent(context, CallActivity::class.java, activityRequestCode)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        super.onRestoreState(getOperation)

        status = sub(Test.networkTestKey)?.value ?: Test.INIT
        if (status == Test.PASS) {
            reviewTest()
        }
        return super.onRestoreState(getOperation)
    }

    override fun onFinish() {
        super.onFinish()
    }

    override fun requireActivity(): Boolean {
        return true
    }

    override fun requireUserInteraction(): Boolean {
        return false
    }

    companion object {
        @JvmField val REQ = 11
    }


}
