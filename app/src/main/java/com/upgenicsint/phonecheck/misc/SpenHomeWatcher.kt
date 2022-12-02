package com.upgenicsint.phonecheck.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import co.balrampandey.logy.Logy
import java.lang.ref.WeakReference

/**
 * Created by zohai on 3/9/2018.
 */
class SpenHomeWatcher (context: Context){
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
        val SYSTEM_DIALOG_REASON_KEY = "reason1"
        val SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions1"
        val SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps1"
        val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey1"

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
                if (reason != null) {
                    Logy.e(TAG, "action:$action,reason:$reason")

                    onHomePressedListener?.onEventOccurred(reason)
                    if (reason == SYSTEM_DIALOG_REASON_HOME_KEY) {
                        onHomePressedListener?.onHomePressed()
                    } else if (reason == SYSTEM_DIALOG_REASON_RECENT_APPS) {
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