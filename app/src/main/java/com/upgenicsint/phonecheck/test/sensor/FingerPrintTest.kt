package com.upgenicsint.phonecheck.test.sensor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.widget.Toast

import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.FingerPrintActivity
import com.upgenicsint.phonecheck.test.Test

import org.json.JSONException

import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

/**
 * Created by Farhan on 10/19/2016.
 */

class FingerPrintTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.fingerprintTestKey

    override val title: String
        get() = context.getString(R.string.finger_print_title)

    override val detail: String
        get() = context.getString(R.string.fingerprint_desc)

    override val iconResource: Int
        get() = R.drawable.fingerprint

    override val hasSubTest: Boolean
        get() = false

    override val activityRequestCode: Int
        get() = FingerPrintActivity.REQ

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        this.context = context
        //fingerPrintTest();
        if (Nammu.checkPermission(Manifest.permission.USE_FINGERPRINT)) {
            testListener?.onPerformDone()

        } else {
            Nammu.askForPermission(context as Activity, Manifest.permission.USE_FINGERPRINT, object : PermissionCallback {
                override fun permissionGranted() {
                    testListener?.onPerformDone()
                }

                override fun permissionRefused() {
                    Toast.makeText(context, "permissionRefused", Toast.LENGTH_SHORT).show()
                }
            })
        }
        return status
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.fingerprintTestKey, toJsonStatus())
        storeOperation.add(Test.fingerprintTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.fingerprintTestKey, status)
        Loader.RESULT.put(Test.fingerprintTestKey, toJsonStatus())
        return super.onRestoreState(getOperation)
    }

    override fun performUserInteraction() {
        super.performUserInteraction()
        startIntent(context, FingerPrintActivity::class.java, activityRequestCode)
    }

    override fun requireUserInteraction(): Boolean {
        return true
    }

    override fun requireActivity(): Boolean {
        return true
    }
}
