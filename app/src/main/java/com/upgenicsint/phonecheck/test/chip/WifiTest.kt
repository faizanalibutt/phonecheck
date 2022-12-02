package com.upgenicsint.phonecheck.test.chip

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.MainActivity
import com.upgenicsint.phonecheck.misc.ProgressBarUtil
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by Farhan on 10/15/2016.
 */

class WifiTest(context: Context) : Test(context) {

    val manager by lazy { context.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var progressDialog: AlertDialog? = null

    private var wifiReceiver: BroadcastReceiver? = null
    private val isLocationEnabled: Boolean
        get() {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    val isWifiEnabled: Boolean
        get() = manager.isWifiEnabled
    val isAlreadyConnected: Boolean
        get() {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = manager.activeNetworkInfo
            return if (activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                activeNetworkInfo.isConnectedOrConnecting
            } else false
        }
    val isConnected: Boolean
        get() {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = manager.activeNetworkInfo
            return if (activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                activeNetworkInfo.isConnected
            } else false
        }

    override val jsonKey: String
        get() = Test.wifiTestKey
    override val title: String
        get() = "WiFi"
    override val detail: String
        get() = context.getString(R.string.wifi_desc)
    override val iconResource: Int
        get() = R.drawable.signal_black
    override val activityRequestCode: Int
        get() = 14
    override val hasSubTest: Boolean
        get() = false

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        isRunning = false
        if (isAlreadyConnected) {
            status = Test.PASS
        } else if (!isLocationEnabled) {
            val activity = context as Activity?
            if (activity != null) {
                Toast.makeText(context, context.getString(R.string.wifi_enableLoc), Toast.LENGTH_SHORT).show()
                activity.startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        activityRequestCode)
            }
        } else {
            testListener?.onPerformDone()
        }
        return status
    }

    override fun performUserInteraction() {
        super.performUserInteraction()
        if (isWifiEnabled) {
            progressDialog = ProgressBarUtil.get(context.getString(R.string.wifi_progrs), context)

            progressDialog?.let {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
                if (context is MainActivity) {
                    val activity = context as MainActivity
                    if (!activity.isFinishing && !it.isShowing) {
                        it.show()
                    }
                }
            }
            //Toast.makeText(getContext(), "Scanning Please wait", Toast.LENGTH_SHORT).show();
            manager.startScan()
            wifiReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (manager.scanResults != null && testListener != null) {
                        //Toast.makeText(getContext(),"Total Networks Found "+manager.getScanResults().size(),Toast.LENGTH_SHORT).show();
                        status = if(manager.scanResults.size > 0) Test.PASS else Test.FAILED
                        testListener?.onUserInteractionDone(true)
                    }
                }
            }
            context.registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        } else {
            testListener?.onUserInteractionDone(true)
            status = Test.FAILED
        }
    }

    override fun onFinish() {
        super.onFinish()
        if (wifiReceiver != null) {
            Log.d(javaClass.simpleName, "onFinish wifiReceiver")
            context.unregisterReceiver(wifiReceiver)
            wifiReceiver = null
        }

        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }

    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(Test.wifiTestKey, toJsonStatus())
        storeOperation
                .add(Test.wifiTestKey, status)
                .save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.wifiTestKey, status)
        Loader.RESULT.put(Test.wifiTestKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }

    override fun requireActivity(): Boolean {
        return !isAlreadyConnected && !isLocationEnabled
    }

    override fun requireUserInteraction(): Boolean {
        return !isAlreadyConnected
    }
}