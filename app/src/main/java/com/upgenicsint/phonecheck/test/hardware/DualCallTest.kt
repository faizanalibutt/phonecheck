package com.upgenicsint.phonecheck.test.hardware

import android.Manifest
import android.app.Activity
import android.content.Context
import android.widget.Toast

import com.farhanahmed.cabinet.operations.GetOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.DualCallActivity
import com.upgenicsint.phonecheck.activities.DualCallTestActivity
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

class DualCallTest(context: Context) : Test(context) {
    override val jsonKey: String
        get() = Test.dualSimCallTestKey

    override val title: String
        get() = context.getString(R.string.dualcallTest_title)

    override val detail: String
        get() = context.getString(R.string.callTest_desc)

    override val iconResource: Int
        get() = R.drawable.phone

    override val activityRequestCode: Int
        get() = DualCalTest.REQ

    override val hasSubTest: Boolean
        get() = true

    init {
        subTests.put(Test.simReaderTestKey1, SubTest(Test.simReaderTestKey1))
        subTests.put(Test.networkTestKey1, SubTest(Test.networkTestKey1))
        subTests.put(Test.simReaderTestKey2, SubTest(Test.simReaderTestKey2))
        subTests.put(Test.networkTestKey2, SubTest(Test.networkTestKey2))
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
        startIntent(context, DualCallTestActivity::class.java, activityRequestCode)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        super.onRestoreState(getOperation)

        /*status = sub(Test.networkTestKey)?.value ?: Test.INIT
        if (status == Test.PASS) {
            reviewTest()
        }*/
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
        @JvmField val REQ = 110
    }


}
