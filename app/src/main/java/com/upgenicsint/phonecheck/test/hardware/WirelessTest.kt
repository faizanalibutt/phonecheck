package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.WirelessChargingActivity
import com.upgenicsint.phonecheck.test.Test


class WirelessTest(context: Context) : Test(context) {

    override val title: String
        get() = context.getString(R.string.wireless_title)

    override val detail: String
        get() = context.getString(R.string.wireless_detail)

    override val iconResource: Int
        get() = R.drawable.wireless_charging_icon

    override val jsonKey: String
        get() = Test.wirelessTestKey

    override val hasSubTest: Boolean
        get() = false

    override val activityRequestCode: Int
        get() = WirelessChargingActivity.REQ

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, WirelessChargingActivity::class.java)
        return super.perform(context, autoPerformMode)
    }

    override fun requireUserInteraction(): Boolean = false

    override fun requireActivity() = true
}