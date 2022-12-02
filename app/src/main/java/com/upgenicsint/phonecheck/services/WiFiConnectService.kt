package com.upgenicsint.phonecheck.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log


class WiFiConnectService : IntentService("WiFiConnectService") {

    override fun onHandleIntent(intent: Intent?) {

        if (intent != null) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled = true

            val networkSSID = intent.getStringExtra("ssid")
            val networkPass = intent.getStringExtra("password")
            val encryption = intent.getStringExtra("encryption")

            val conf = WifiConfiguration()
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.SSID = String.format("\"%s\"", networkSSID)

            try{
                if (encryption.equals("Open")) {
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }else{
                    conf.preSharedKey = String.format("\"%s\"", networkPass)
                }
            }
            catch(e: NullPointerException){
                Log.d("WifiService", "Wifi connect service encryption exception")
            }

            val netId = wifiManager.addNetwork(conf)

            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }

    }

}
