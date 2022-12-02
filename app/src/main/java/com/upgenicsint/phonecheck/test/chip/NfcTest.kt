package com.upgenicsint.phonecheck.test.chip

import android.content.Context
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.NFCActivity
import com.upgenicsint.phonecheck.test.Test

class NfcTest (context: Context) : Test(context) {
    override val title: String
        get() = "NFC"
    override val detail: String
        get() = "Test  Near Field Communication of your device"
    override val iconResource: Int
        get() = R.drawable.nfc
    override val hasSubTest: Boolean
        get() = false
    override val jsonKey: String
        get() = Test.nfctest

    override fun requireUserInteraction(): Boolean = true
    override val activityRequestCode: Int
        get() = NFCActivity.REQ

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, NFCActivity::class.java)
        return super.perform(context, autoPerformMode)
    }

}