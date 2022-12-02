package com.upgenicsint.phonecheck.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.upgenicsint.phonecheck.broadcastreceiver.BixbyButtonReceiver

/**
 * Created by farhanahmed on 26/09/2017.
 */
/*
class PhoneCheckAccessibilityService : AccessibilityService() {
    val BIXBY_PACKAGE = "com.samsung.android.app.spage"

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.getPackageName()?.toString()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            if (packageName.equals(BIXBY_PACKAGE)) {
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(BixbyButtonReceiver.ACTION))
            }

        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (packageName.equals(BIXBY_PACKAGE)) {
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(BixbyButtonReceiver.ACTION))
            }
        }
    }

    override fun onGesture(gestureId: Int) = super.onGesture(gestureId)

    override fun onServiceConnected() {
        super.onServiceConnected()

//        val info = serviceInfo
//        info.feedbackType = 16
//        info.flags = 66
//        info.notificationTimeout = 100
//        info.packageNames = arrayOf(BIXBY_PACKAGE)
//        serviceInfo = info
    }

    override fun onInterrupt() {

    }

    override fun onKeyEvent(event: KeyEvent?) = super.onKeyEvent(event)

    companion object {
        val TAG = "PhoneCheckAccessibilityService"
    }
}
*/