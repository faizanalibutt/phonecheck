package com.upgenicsint.phonecheck.misc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast

import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.activities.MainActivity



/**
 * Created by farhanahmed on 05/03/2017.
 */

class BatteryInfo(val context: Context) {
    val intent: Intent

    init {
        intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val health: String = getHealthString(intent.getIntExtra("health", 0))

    val batteryCapacity: Double
        @SuppressLint("PrivateApi")
        get() {
            var mPowerProfile_: Any? = null

            val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"

            try {
                mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context::class.java).newInstance(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                return Class
                        .forName(POWER_PROFILE_CLASS)
                        .getMethod("getAveragePower", java.lang.String::class.java)
                        .invoke(mPowerProfile_, "battery.capacity") as Double
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return -1.0
        }


    fun logBatteryInformation() {

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val voltage = intent.getIntExtra("voltage", 0)
        val temperature = intent.getIntExtra("temperature", 0)
        val temps = temperature.toDouble() / 10
        //---
        val isPresent = intent.getBooleanExtra("present", false)
        val technology = intent.getStringExtra("technology")
        val plugged = intent.getIntExtra("plugged", -1)
        val scale = intent.getIntExtra("scale", -1)
        val health = intent.getIntExtra("health", 0)
        val status = intent.getIntExtra("status", 0)

        batteryLevel = level

        Logy.d(TAG, "Column Status: " + level.toString() + "%")
        Logy.d(TAG, "Column Voltage: " + voltage.toString())
        Logy.d(TAG, "Column Temperature: " + temps.toString())
        Logy.d(TAG, "Column Present: " + isPresent.toString())
        Logy.d(TAG, "Column plugged: " + getPlugTypeString(plugged))
        Logy.d(TAG, "Column technology: " + technology)
        Logy.d(TAG, "Column scale: " + scale.toString())
        Logy.d(TAG, "Column health: " + getHealthString(health))
        Logy.d(TAG, "Column status: " + getStatusString(status))
//        Logy.d(TAG, "Column Capacity: " + getBatteryCapacity())
    }

    fun checkBatteryLevel() {
        batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    }

    fun checkPluggedState() {
        plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    }

    private fun getPlugTypeString(plugged: Int): String {
        var plugType = "Unknown"
        when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> plugType = "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> plugType = "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> plugType = "Wireless Charging"
        }
        return plugType
    }

    private fun getStatusString(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }


    private fun getHealthString(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Over Heat"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    public fun closeRegister() {
        context.unregisterReceiver(null)
    }

    companion object {

        private val TAG = "BatteryInfo"
        var batteryLevel = 0
        var plugged = 0
    }
}
