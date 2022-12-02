package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.zxing.common.StringUtils
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AccelerometerActivity.Companion.ACCEL_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.AudioPlaybackTestActivity.Companion.MIC_PLAYBACK_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.BatteryDiagnosticActivity.Companion.batteryStats
import com.upgenicsint.phonecheck.activities.BrightnessActivity.Companion.BRIGHTNESS_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity.Companion.BUTTON_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CallActivity.Companion.CALL_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CameraTestActivity.Companion.FRONT_CAMERA_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CameraTestActivity.Companion.REAR_CAMERA_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.CosmeticsTestActivity.COSMETICS_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.DigitizerActivity.Companion.DIGITIZER_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.FingerPrintActivity.Companion.FINGERPRINT_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.GradingsActivity.Companion.GRADES_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.MicESTestActivity.Companion.MICES_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.MicLSTestActivity.Companion.MICLS_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.MultiTouchTestActivity.MULTI_TOUCH_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.NFCActivity.Companion.NFC_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.ProximityActivity.Companion.PROXIMITY_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SEdgeActivity.Companion.SEDGE_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SPenActivity.Companion.SPENTEST_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SpeechRecognization.Companion.SPEECH_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.SpenButtonsTestActivity.Companion.SPENBUTTON_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.TouchTestActivity.Companion.LCD_SCREEN_TIME
import com.upgenicsint.phonecheck.activities.WirelessChargingActivity.Companion.WIRELESS_SCREEN_TIME
import com.upgenicsint.phonecheck.adapter.TestCompletionAdapter
import com.upgenicsint.phonecheck.barcode.CaptureActivity.BARCODE_SCREEN_TIME
import com.upgenicsint.phonecheck.misc.BottomDividerDecorator
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile
import com.upgenicsint.phonecheck.misc.WriteObjectFile
import com.upgenicsint.phonecheck.models.Column
import com.upgenicsint.phonecheck.models.TestResults
import com.upgenicsint.phonecheck.remote.ColumnAPIService
import com.upgenicsint.phonecheck.remote.ColumnAPIUtils
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_test_completion.*
import org.json.JSONException
import org.json.JSONObject
import pl.tajchert.nammu.Nammu
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TestCompletionActivity : BaseActivity() {

    private var columnAPIService: ColumnAPIService? = null
    private var column: Column? = null
    private var licenseObj: String = ""
    private var serialObj: String = ""
    private var transactionObj: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        setContentView(R.layout.activity_test_completion)
        val batteryDiagnose: LinearLayout = findViewById(R.id.btryDiagLayout)
        Nammu.init(applicationContext)

        Loader.instance.createEmptyFile()

        FirebaseUtil.addNew(FirebaseUtil.RESULT).child("TestResultJSON").setValue(Loader.resultFile.exists())
        FirebaseUtil.addNew(FirebaseUtil.RESULT).child("DeviceInfoJSON").setValue(Loader.deviceInfoFile.exists())
        FirebaseUtil.addNew("testWatcher").child("isOnTestCompleteScreen").setValue(true)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)

        val testList = Loader.instance.testList

        val adapter = TestCompletionAdapter(testList, this)
        recyclerView.addItemDecoration(BottomDividerDecorator(this))
        recyclerView.adapter = adapter

        batteryDiagnose.setOnClickListener {
            startActivity(Intent(this, BatteryDiagnosticActivity::class.java).putExtra(Constants.BATTERY, true))
            this.finish()
        }
        /*LocalBroadcastManager.getInstance(this).registerReceiver(chargerReceiver, IntentFilter("charger"))
        readColumnApiFile()
        columnAPIService = ColumnAPIUtils.getColumnAPIService()
        sync.setImageResource(R.drawable.warning)
        sync.visibility = View.VISIBLE
        sync_results.visibility = View.VISIBLE
        sync_results.text = "Sync..."
        sync_results.setTextColor(ContextCompat.getColor(context, R.color.dark_black))
        if (chargerConnected) {
            sync.setImageResource(R.drawable.blue_check)
            sync.visibility = View.VISIBLE
            sync_results.visibility = View.VISIBLE
            sync_results.text = "Sync Successfull"
            sync_results.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            pushAppResults(testList)
        }*/

        showTimeSpent()
    }

    @SuppressLint("SetTextI18n")
    private fun showTimeSpent() {

        Loader.TOTAL_SCREEN_TIME = 0
        Loader.TOTAL_SCREEN_TIME = Loader.TOTAL_SCREEN_TIME + ( ACCEL_SCREEN_TIME + MIC_PLAYBACK_SCREEN_TIME
        +BARCODE_SCREEN_TIME + BRIGHTNESS_SCREEN_TIME + BUTTON_SCREEN_TIME + CALL_SCREEN_TIME + FRONT_CAMERA_SCREEN_TIME
        +REAR_CAMERA_SCREEN_TIME + COSMETICS_SCREEN_TIME + DIGITIZER_SCREEN_TIME + FINGERPRINT_SCREEN_TIME
        +GRADES_SCREEN_TIME + MICES_SCREEN_TIME + MICLS_SCREEN_TIME + MULTI_TOUCH_SCREEN_TIME + NFC_SCREEN_TIME
        +PROXIMITY_SCREEN_TIME + SEDGE_SCREEN_TIME + SPEECH_SCREEN_TIME + SPENTEST_SCREEN_TIME + SPENBUTTON_SCREEN_TIME
        +LCD_SCREEN_TIME + WIRELESS_SCREEN_TIME )
        totalTimeSpent!!.text = "Total Time: ${Loader.TOTAL_SCREEN_TIME}s"

    }

    fun showTimeActivity(view: View) {
        startActivity(Intent(this@TestCompletionActivity, RecordTimeActivity::class.java))
    }

    fun routePhoneCheck(view: View) {
        startActivity(Intent(this@TestCompletionActivity, PhoneCheckWebsiteActivity::class.java))
    }

    private fun writeAudioReportJson() {
        try {
            if (batteryStats.exists() && batteryStats.length() > 0) {
                val jsonObject: JSONObject
                try {
                    jsonObject = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(batteryStats))
                    val batteryStat = jsonObject.getJSONObject("batteryStat")
                    batteryStat.put("totalDuration", column?.BatteryDrainDuration)
                    batteryStat.put("startBattery", column?.BatteryChargeStart)
                    batteryStat.put("endBattery", column?.BatterChargeEnd)
                    batteryStat.put("totalDischarge", column?.BatteryDrain)
                    jsonObject.put("BatteryStat", batteryStat)
                    WriteObjectFile.getInstance().writeObject(jsonObject.toString(), "/BatteryResults.json")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                val jsonObject = JSONObject()
                val batteryStat = JSONObject()
                batteryStat.put("totalDuration", column?.BatteryDrainDuration)
                batteryStat.put("startBattery", column?.BatteryChargeStart)
                batteryStat.put("endBattery", column?.BatterChargeEnd)
                batteryStat.put("totalDischarge", column?.BatteryDrain)
                jsonObject.put("batteryStat", batteryStat)
                WriteObjectFile.getInstance().writeObject(jsonObject.toString(), "/BatteryResults.json")
            }
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }
    }

    private var chargerConnected: Boolean = false
    private val chargerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            val action = intent!!.action
            if (action != null && action == "charger" && !chargerConnected) {
                chargerConnected = true
                sync.setImageResource(R.drawable.blue_check)
                sync.visibility = View.VISIBLE
                sync_results.visibility = View.VISIBLE
                sync_results.text = "Sync Successfull"
                sync_results.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            }
        }
    }

    private fun readColumnApiFile() {
        val batteryDefValuesJson = ReadTestJsonFile()
        val columnDefault: Column? = batteryDefValuesJson.columnApi
        if (columnDefault != null) {
            licenseObj = columnDefault.LicenseID
            serialObj = columnDefault.Serial
            transactionObj = columnDefault.TransactionID
        }
    }

    private fun pushAppResults(testList: ArrayList<Test>) {

        /***
         * tranverse through list and get passed failed pending values using test object
         */

        val passed: MutableList<String> = ArrayList()
        val failed: MutableList<String> = ArrayList()
        val pending: MutableList<String> = ArrayList()
        val working: String
        var passedS: String
        var failedS: String

        for (test: Test in testList) {
            when {
                test.status == Test.PASS -> passed.add(test.title)
                test.status == Test.FAILED -> failed.add(test.title)
                else -> { pending.add(test.title) }
            }
        }

        working = if (passed.size == testList.size) { "Yes" } else if (pending.size == testList.size) { "Pending" } else  { "No" }

        if (pending.size == testList.size) {
            failed.clear()
            failed.addAll(pending)
        }

        passedS = passed.toString()
        passedS = passedS.replace("[", "")
        passedS = passedS.replace("]", "")
        failedS = failed.toString()
        failedS = failedS.replace("[", "")
        failedS = failedS.replace("]", "")


        columnAPIService?.pushTestResults(TestResults(licenseObj, serialObj, transactionObj, passedS, failedS, working))!!.enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>?, t: Throwable?) {
                if (chargerConnected) {
                    sync.setImageResource(R.drawable.blue_check)
                    sync.visibility = View.VISIBLE
                    sync_results.visibility = View.VISIBLE
                    sync_results.text = "Sync Successfull"
                    sync_results.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                } else {
                    sync.setImageResource(R.drawable.not_working)
                    sync.visibility = View.VISIBLE
                    sync_results.visibility = View.VISIBLE
                    sync_results.text = "Sync to Desktop"
                    sync_results.setTextColor(ContextCompat.getColor(context, R.color.default_waveform))
                    showDialog()
                }
            }

            override fun onResponse(call: Call<String>?, response: Response<String>?) {
                if (response!!.body().equals("success")) {
                    sync.setImageResource(R.drawable.blue_check)
                    sync.visibility = View.VISIBLE
                    sync_results.visibility = View.VISIBLE
                    sync_results.text = "Sync Successfull"
                    sync_results.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                } else {
                    if (chargerConnected) {
                        sync.setImageResource(R.drawable.blue_check)
                        sync.visibility = View.VISIBLE
                        sync_results.visibility = View.VISIBLE
                        sync_results.text = "Sync Successfull"
                        sync_results.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    } else {
                        sync.setImageResource(R.drawable.not_working)
                        sync.visibility = View.VISIBLE
                        sync_results.visibility = View.VISIBLE
                        sync_results.text = "Sync to Desktop"
                        sync_results.setTextColor(ContextCompat.getColor(context, R.color.default_waveform))
                        showDialog()
                    }
                }
            }
        })
    }

   private fun showDialog() {
        if (context != null) {
            val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
            } else {
                AlertDialog.Builder(context)
            }
            builder.setCancelable(false)
            builder.setTitle(getString(R.string.plugged_in_revert))
                    .setMessage(getString(R.string.app_results_revert))
                    .setPositiveButton(android.R.string.yes) { _, _ -> }
                    .setIcon(android.R.drawable.ic_lock_idle_low_battery)
            if (builder != null && !isFinishing) {
                builder.show()
            }
        }
   }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        /*if (BatteryDiagnosticActivity.isConnected(this)) {
            chargerConnected = true
            sync.setImageResource(R.drawable.blue_check)
            sync.visibility = View.VISIBLE
            sync_results.visibility = View.VISIBLE
            sync_results.text = "Sync Successfull"
            sync_results.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        Loader.instance.deleteEmptyFile()
    }
}
