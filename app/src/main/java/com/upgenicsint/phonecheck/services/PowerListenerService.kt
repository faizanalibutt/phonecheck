package com.upgenicsint.phonecheck.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity.MyTimerTask



class PowerListenerService : Service() {
    internal var powerOffReceiver: PowerOffReceiver? = null

    override fun onCreate() {
        super.onCreate()
        powerOffReceiver = PowerOffReceiver()

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (powerOffReceiver != null) {
            val screenStateFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
            registerReceiver(powerOffReceiver, screenStateFilter)
        }

        return Service.START_NOT_STICKY

    }

    override fun onDestroy() {
        Log.d("PowerListenerService","Service Destroyed")
        if (powerOffReceiver != null) {
            unregisterReceiver(powerOffReceiver)
        }
        super.onDestroy()

    }

    internal inner class PowerOffReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            Log.d("PowerListenerService", "onReceive")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent("PHONE_CHECK_POWER_OFF"))

        }
    }
}
