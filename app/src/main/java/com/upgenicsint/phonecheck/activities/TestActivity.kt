package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.tyorikan.voicerecordingvisualizer.RecordingSampler
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.barcode.Camera.CameraManager
import com.upgenicsint.phonecheck.misc.BatteryInfo
import kotlinx.android.synthetic.main.activity_test.*
import java.util.*


/*
* activity for testing pre production code before using it*/
class TestActivity : AppCompatActivity() {

    var recordingSampler: RecordingSampler? = null
    var state = false
    private val TAG = "BatteryInfo"

    //    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("PrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)


         button3.setOnClickListener {

//             flashLightOn()

             var mPowerProfile_: Any? = null

             val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"

             try {
                 mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS)
                         .getConstructor(Context::class.java).newInstance(this)
             } catch (e: Exception) {
                 e.printStackTrace()
             }


             try {
                 val batteryCapacity = Class
                         .forName(POWER_PROFILE_CLASS)
                         .getMethod("getAveragePower", java.lang.String::class.java)
                         .invoke(mPowerProfile_, "battery.capacity") as Double
                 Toast.makeText(this@TestActivity, batteryCapacity.toString() + " mah",
                         Toast.LENGTH_LONG).show()
                 Log.d(TAG,"Column Present: " + batteryCapacity.toString())
             } catch (e: Exception) {
                 e.printStackTrace()
             }

             val batteryInfo = BatteryInfo(this)
             batteryInfo.logBatteryInformation()
//             getBatteryCapacity()
//             if (state) {
//                 state = false
//                 recordingSampler?.stopRecording()
//                 toast((recordingSampler?.amplitude ?: 0).toString())
//                 recordingSampler?.release()
//             } else {
//                 state = true
//                 recordingSampler = RecordingSampler(MediaRecorder.AudioSource.CAMCORDER, RecordingSampler.Listener { sampler, e -> })
//                 recordingSampler?.setSamplingInterval(100)
//                 recordingSampler?.link(visualizer)
//                 recordingSampler?.startRecording()
//             }


         }
    }

    private fun flashLightOn() {
        try {
            if (packageManager.hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                var cam: Camera? = null
                cam = Camera.open()
                val p = cam.getParameters()
                p.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                cam.setParameters(p)
                cam.startPreview()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val flashstate = CameraMetadata.FLASH_STATE_FIRED
                    if(flashstate.equals(CameraMetadata.FLASH_STATE_FIRED))
                    {
                        Toast.makeText(this, "Flash Fired", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("Flash", "Not supported")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(baseContext, "Exception flashLightOn()",
                    Toast.LENGTH_SHORT).show()
        }

    }

//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    private fun getBatteryCapacity() {
//        val capacity = getBatteryCapacity(this)
//        Toast.makeText(this@TestActivity, "capacity is: " +capacity, Toast.LENGTH_SHORT).show()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    fun getBatteryCapacity(ctx: Context): Long {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            val mBatteryManager = ctx.getSystemService(Context.BATTERY_SERVICE) as ColumnManager
//            val chargeCounter = mBatteryManager.getLongProperty(ColumnManager.BATTERY_PROPERTY_CHARGE_COUNTER)
//            val capacity = mBatteryManager.getLongProperty(ColumnManager.BATTERY_PROPERTY_CAPACITY)
//
//            if (chargeCounter != null && capacity != null) {
//                return (chargeCounter.toFloat() / capacity.toFloat() * 100f).toLong()
//            }
//        }
//
//        return 0
//    }
}
