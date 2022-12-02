package com.upgenicsint.phonecheck.services

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.upgenicsint.phonecheck.Loader

class DeviceInfoService : IntentService("DeviceInfoService") {

    override fun onHandleIntent(intent: Intent?) {
        try {
            val jsonObject = Loader.writeDeviceInfoFile(applicationContext)
            val json = Loader.DeviceInfoStartKey + jsonObject.toString() + Loader.DeviceInfoEndKey
            Log.i(packageName, json)
            intent?.putExtra("deviceinfo", json)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i(packageName, Loader.DeviceInfoStartKey + "ERROR" + Loader.DeviceInfoEndKey)
        }

        stopSelf()
    }
}
