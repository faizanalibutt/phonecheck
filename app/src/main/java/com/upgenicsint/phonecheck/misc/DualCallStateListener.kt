package com.upgenicsint.phonecheck.misc

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

import com.upgenicsint.phonecheck.activities.CallActivity
import com.upgenicsint.phonecheck.activities.DualCallActivity
import com.upgenicsint.phonecheck.activities.DualCallTestActivity

import java.lang.ref.WeakReference

/**
 * Created by farhanahmed on 18/11/2016.
 */

class DualCallStateListener(activity: DualCallTestActivity) : PhoneStateListener() {
    private var callTestCompleted: Boolean = false
    private val activityWeakReference = WeakReference(activity)

    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
        val ref = activityWeakReference.get()
        if (ref != null) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> if (callTestCompleted) {
                    ref.onCallStateIdle()
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> ref.onCallStateOffHook()
            }
            callTestCompleted = true
        }

    }


}

