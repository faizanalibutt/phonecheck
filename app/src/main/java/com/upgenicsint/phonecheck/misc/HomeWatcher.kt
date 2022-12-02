package com.upgenicsint.phonecheck.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity
import java.lang.ref.WeakReference

/**
 * Created by Farhan on 10/19/2016.
 */

class HomeWatcher(context: Context) {
    private val mContext = WeakReference<Context>(context)
    private val mFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
    public var onHomePressedListener: OnHomePressedListener? = null
    private var mRecevier = InnerRecevier()

    fun startWatch() {
        val ref = mContext.get()
        if (ref != null) {
            ref.registerReceiver(mRecevier, mFilter)
        }
    }

    fun stopWatch() {
        val ref = mContext.get()
        if (ref != null) {
            ref.unregisterReceiver(mRecevier)
        }
    }

    internal inner class InnerRecevier : BroadcastReceiver() {
        val SYSTEM_DIALOG_REASON_KEY = "reason"
        val SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions"
        val SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps"
        val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
                if (reason != null) {
                    Logy.e(TAG, "action:$action,reason:$reason")

                    onHomePressedListener?.onEventOccurred(reason)
                    if (reason == SYSTEM_DIALOG_REASON_HOME_KEY) {
                        ButtonsTestActivity.isUserNavigating = true
                        onHomePressedListener?.onHomePressed()
                    } else if (reason == SYSTEM_DIALOG_REASON_RECENT_APPS) {
                        ButtonsTestActivity.isUserNavigating = true
                        onHomePressedListener?.onRecentPressed()
                    }
                }
            }
        }
    }

    interface OnHomePressedListener {
        fun onEventOccurred(eventString: String)
        fun onHomePressed()
        fun onRecentPressed()
    }

    companion object {

        internal val TAG = "hg"
    }
}