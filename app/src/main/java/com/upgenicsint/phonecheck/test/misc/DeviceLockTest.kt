package com.upgenicsint.phonecheck.test.misc

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Context.ACCOUNT_SERVICE
import android.os.Build
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.SubTestAdapter
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.models.TestStatusInfo
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

/**
 * Created by Farhan on 10/18/2016.
 */

class DeviceLockTest(context: Context) : Test(context) {
    private var isSupportGoogle: Boolean = false
    private var isSupportSamsung: Boolean = false

    override val title: String
        get() = context.getString(R.string.deviceLock_title)

    override val jsonKey: String
        get() = Test.deviceLockTestKey


    override val detail: String
        get() = context.getString(R.string.frb_desc)

    override val iconResource: Int
        get() = R.drawable.device_lock


    override val hasSubTest: Boolean
        get() = true

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        if (Nammu.checkPermission(Manifest.permission.GET_ACCOUNTS)) {
            testListener?.onPerformDone()
        } else {
            Nammu.askForPermission(context as Activity, Manifest.permission.GET_ACCOUNTS, object : PermissionCallback {
                override fun permissionGranted() {
                    testListener?.onPerformDone()
                }

                override fun permissionRefused() {

                }
            })
        }
        return status
    }

    override fun performUserInteraction() {
        super.performUserInteraction()
        checkAccounts()
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.deviceLockTestKey, toJsonStatus())
        storeOperation
                .add(Test.deviceLockTestKey, status)
                .add(Test.frpLockTestKey, isSupportGoogle)
                .add(Test.samsungLockTestKey, isSupportSamsung)
                .save()
        //storeOperation.add(javaClass.simpleName, isClear).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.deviceLockTestKey, status)
        isSupportGoogle = getOperation.getBoolean(Test.frpLockTestKey, false)
        isSupportSamsung = getOperation.getBoolean(Test.samsungLockTestKey, false)
        Loader.RESULT.put(Test.deviceLockTestKey, toJsonStatus())

        //isClear = getOperation.getBoolean(javaClass.simpleName, true)
        if (status != Test.INIT)
            setSubTest()
        return true
    }


    fun checkAccounts() {
        testStatusInfos.clear()
        try {
            val manager = context.getSystemService(ACCOUNT_SERVICE) as AccountManager

            for (account in manager.accounts) {
                if (account.type.equals("com.google", ignoreCase = true)) {
                    isSupportGoogle = true
                }
                if (account.type.equals("com.osp.app.signin", ignoreCase = true) || account.type.toLowerCase().contains("samsung")) {
                    isSupportSamsung = true
                }
            }

            setSubTest()

            testListener?.onUserInteractionDone(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setSubTest() {
        testStatusInfos.clear()
        if (Build.MANUFACTURER.containsIgnoreCase("samsung")) testStatusInfos.add(TestStatusInfo(context.getString(R.string.samsung_lock), if (isSupportSamsung) context.getString(R.string.frp_on) else context.getString(R.string.frp_off)))
        testStatusInfos.add(TestStatusInfo(context.getString(R.string.frp_lck), if (isSupportGoogle) context.getString(R.string.frp_on) else context.getString(R.string.frp_off)))
        status = if(!isSupportGoogle && !isSupportSamsung) Test.PASS else Test.FAILED
        adapter = SubTestAdapter(context, testStatusInfos)
    }

    override fun requireUserInteraction() = true

}
