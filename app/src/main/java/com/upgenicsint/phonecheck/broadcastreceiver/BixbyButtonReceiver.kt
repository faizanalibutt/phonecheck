package com.upgenicsint.phonecheck.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity
import com.upgenicsint.phonecheck.test.hardware.ButtonTest
import java.lang.ref.WeakReference

class BixbyButtonReceiver(val activity: ButtonsTestActivity) : BroadcastReceiver() {
    companion object {
        @JvmField val ACTION = "BIXBY_BUTTON_PRESS"
    }
    val activityRef = WeakReference(activity)
    override fun onReceive(context: Context?, intent: Intent?) {
        val ref = activityRef.get()
        if (ref != null) {
            ref.performTest(ButtonTest.BIXBY_KEYCODE_1082)
        }
    }
}
