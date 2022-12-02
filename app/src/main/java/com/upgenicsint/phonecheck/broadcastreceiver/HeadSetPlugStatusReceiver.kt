package com.upgenicsint.phonecheck.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.HeadSetPlugCallBack
import com.upgenicsint.phonecheck.test.Test
import java.lang.ref.WeakReference

/**
 * Created by farhanahmed on 28/09/2017.
 */

class HeadSetPlugStatusReceiver(callbacks: HeadSetPlugCallBack) : BroadcastReceiver() {

    private val ref = WeakReference(callbacks)

    override fun onReceive(context: Context?, intent: Intent?) {
        val callBackRef = ref.get()
        if (callBackRef != null && intent != null) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                when (state) {
                    0 -> {
                        callBackRef.onHeadSetAttachment(false)
                    }
                    1 -> {
                        callBackRef.onHeadSetAttachment(true)
                    }
                }
            }
        }

    }
}
