package com.upgenicsint.phonecheck.test.chip

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Log
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.activities.MainActivity
import com.upgenicsint.phonecheck.misc.ProgressBarUtil
import org.json.JSONException
import java.lang.ref.WeakReference


/**
 * Created by farhanahmed on 21/11/2017.
 */

class BluetoothTest2(context: Context) : Test(context) {

    private val bluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var progressDialog: AlertDialog? = null

    override val title: String = context.getString(R.string.enhanced_bt_title)

    override val detail: String
        get() = context.getString(R.string.enhanceBT_desc)

    override val iconResource: Int
        get() = R.drawable.bluetooth_plus

    override val hasSubTest: Boolean
        get() = false

    override val jsonKey: String
        get() = blueToothPlusTestKey

    private var handler: Handler? = null
    private var timeoutRunnable: TimeoutRunnable? = null
    private var mReceiver: BroadcastReceiver? = null

    override fun requireUserInteraction() = true

    override fun perform(context: Context, autoPerformMode: Boolean): Int {

        testListener?.onPerformDone()

        return super.perform(context, autoPerformMode)
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        Loader.RESULT.put(jsonKey, toJsonStatus())
        storeOperation.add(jsonKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        Loader.RESULT.put(jsonKey, toJsonStatus())

        return super.onRestoreState(getOperation)
    }


    override fun performUserInteraction() {
        super.performUserInteraction()
        clearHandlers()
        handler = Handler()
        timeoutRunnable = TimeoutRunnable(this)
        handler?.postDelayed(timeoutRunnable, 5000)
        bluetoothAdapter.enable()
        bluetoothAdapter.startDiscovery()

        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.getAction()

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // Add the name and address to an array adapter to show in a ListView

                    if (device != null && device.address != null) {
                        onFoundDevices();
                    }

                }
            }
        }

        progressDialog = ProgressBarUtil.get(context.getString(R.string.BT_progrs), context)

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


        val filter = IntentFilter(ACTION_FOUND)
        context.registerReceiver(mReceiver, filter)
    }

    private var foundDevices = false

    class TimeoutRunnable(test: BluetoothTest2) : Runnable {

        private val weakRef = WeakReference<BluetoothTest2>(test)

        override fun run() {
            val ref = weakRef.get()

            if (ref != null) {
                if (!ref.foundDevices) {
                    ref.foundNoDevices()
                }
            }
        }

    }


    private fun onFoundDevices() {
        foundDevices = true
        status = Test.PASS


        clearHandlers()


        testListener?.onUserInteractionDone(true)
    }

    private fun clearHandlers() {

        mReceiver?.let { mReceiver ->
            context.unregisterReceiver(mReceiver)
        }
        mReceiver = null

        handler?.removeCallbacks(timeoutRunnable)
        handler = null
        timeoutRunnable = null

        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    private fun foundNoDevices() {

        status = Test.FAILED
        clearHandlers()
        testListener?.onUserInteractionDone(true)
    }

    override fun onFinish() {
        super.onFinish()
        clearHandlers()
    }
}
