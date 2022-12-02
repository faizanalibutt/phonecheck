package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.media.BeepManagerCustom
import com.upgenicsint.phonecheck.misc.BatteryInfo
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile
import com.upgenicsint.phonecheck.misc.WriteObjectFile
import com.upgenicsint.phonecheck.models.Column
import com.upgenicsint.phonecheck.models.ClientCustomization
import com.upgenicsint.phonecheck.remote.ColumnAPIService
import com.upgenicsint.phonecheck.remote.ColumnAPIUtils
import kotlinx.android.synthetic.main.activity_battery_diagnostic.*
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n")
class BatteryDiagnosticActivity : AppCompatActivity(), View.OnClickListener {

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.imageViewReset -> reset()
            R.id.imageViewStartStop -> startStop()
            R.id.relativeLayout -> isFlashStopped = false
        }
    }

    fun cancelTest(view: View) {
        startActivity(Intent(this@BatteryDiagnosticActivity, TestCompletionActivity::class.java))
        stopCountDownTimer()
        this.finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action. When the window gains focus,
        // hide the system UI.
        if (hasFocus) {
            delayedHide(300)
        } else {
            mHideHandler.removeMessages(0)
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_IMMERSIVE
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private val mHideHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            hideSystemUI()
        }
    }

    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeMessages(0)
        mHideHandler.sendEmptyMessageDelayed(0, 1000)
    }

    private fun applyFullScreenWindow() {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // This work only for android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            window.decorView.systemUiVisibility = flags

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView = window.decorView
            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    decorView.systemUiVisibility = flags
                }

                decorView.systemUiVisibility = flags
            }
        }
    }

    private enum class TimerStatus {
        STARTED,
        STOPPED
    }

    val context: Context get() = this
    var earlyBtryLvl: Int = 0
    var updateBtryLvl: Int = 0
    var finalyBtryLvl: Int = 0
    var lvlDifference: Int = 0
    var duration = 3
    var percentage = 25
    private var timeCountInMilliSeconds = (1 * 60000).toLong()
    private var timerStatus = TimerStatus.STOPPED
    private var progressBarCircle: ProgressBar? = null
    private var editTextMinute: TextView? = null
    private var textViewTime: TextView? = null
    private var btrydischDiff: TextView? = null
    private var imageViewReset: ImageView? = null
    private var imageViewStartStop: ImageView? = null
    private var countDownTimer: CountDownTimer? = null
    var relativelayout: RelativeLayout? = null
    private var beepManager: BeepManagerCustom? = null
    private var isTimerStarted: Boolean = false
    private var isResumeDone: Boolean = false
    private var progressUpdate: String? = ""
    private var progressUpdatevalue: Int? = 0
    private var bundle: Boolean? = false
    private var licenseObj: String = ""
    private var serialObj: String = ""
    private var transactionObj: Int = 0
    private var isAutoStarted: Boolean = false
    private var columnAPIService: ColumnAPIService? = null
    private var isBatteryTestCompleted: Boolean = false
    /*private var settingsCanWrite = false*/
    var selectedDrain: Int = R.id.duration
    private val customizations: ClientCustomization? = Loader.instance.clientCustomization
    private var isAutoBatteryTestStart = false
    var flags = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFullScreenWindow()
        setContentView(R.layout.activity_battery_diagnostic)
        Loader.instance.createbatterystatFile()
        bundle = intent.getBooleanExtra(Constants.BATTERY, false)
        relativelayout = findViewById(R.id.relativeLayout)
        btrydischDiff = findViewById(R.id.dischgDiff)

        currentPluggedStatus = isBatteryPluggedn()

        initViews()
        initListeners()
        /**
         * stat working on file read here
         */
        readColumnApiFile()
        columnAPIService = ColumnAPIUtils.getColumnAPIService()

        editTextMinute!!.isEnabled = true
        durationInc!!.isEnabled = true
        durationDec!!.isEnabled = true
        imageViewReset!!.visibility = View.GONE
        imageViewReset!!.isEnabled = true
        LocalBroadcastManager.getInstance(this).registerReceiver(chargerReceiver, IntentFilter(Constants.CHARGER))

        if (bundle as Boolean) {
            if (customizations != null && customizations.duration >= 1) {
                editTextMinute!!.text = "Duration: ${customizations.duration}"
                duration = customizations.duration
                textViewTime!!.text = hmsTimeFormatter((customizations.duration * 60 * 1000).toLong())
            } else {
                editTextMinute!!.text = getString(R.string.battery_duration_value)
                textViewTime!!.text = getString(R.string.battery_duration_value_desc)
            }
        } else {
            if (customizations != null && customizations.duration >= 1) {
                editTextMinute!!.text = "Duration: ${customizations.duration}"
                duration = customizations.duration
                textViewTime!!.text = hmsTimeFormatter((customizations.duration * 60 * 1000).toLong())
            } else {
                editTextMinute!!.text = getString(R.string.battery_duration_value)
                textViewTime!!.text = getString(R.string.battery_duration_value_desc)
            }
        }
        isAutoBatteryTestStart = customizations != null && customizations.isAutoStartBatteryDrain
        selectDrainTest()
        addBatteryIntentFilters()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onTimerValue()
        }
    }

    private fun addBatteryIntentFilters() {
        val batteryFilter = IntentFilter()
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        }
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(autoBatteryReceiver, batteryFilter)
    }

    private fun isBatteryPluggedn(): Boolean {

        val bi = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val s = bi!!.getIntExtra("status", 0)
        return s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL
    }

    var currentPluggedStatus = false
    var timer: Timer? = null
    private fun onTimerValue() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val s = isBatteryPluggedn()

                if (s == currentPluggedStatus) {
                    runOnUiThread { onPluggedStatusChanged(s) }
                    currentPluggedStatus = s
                }
            }
        }, 0, 1000)
    }

    /**
     * #receiver
     */
    private fun onPluggedStatusChanged(status: Boolean) {

        if (status && !isResumeDone) {
            isResumeDone = true
            if (context != null) {
                val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                } else {
                    AlertDialog.Builder(context)
                }
                builder.setCancelable(false)
                if (isAutoBatteryTestStart) {
                    builder.setTitle(getString(R.string.plugged_in))
                            .setMessage(getString(R.string.battery_msg_auto))
                            .setPositiveButton(android.R.string.yes) { dialog, which -> }
                            .setIcon(android.R.drawable.ic_lock_idle_low_battery)
                } else {
                    builder.setTitle(getString(R.string.plugged_in))
                            .setMessage(getString(R.string.battery_msg))
                            .setPositiveButton(android.R.string.yes) { dialog, which -> }
                            .setIcon(android.R.drawable.ic_lock_idle_low_battery)
                }
                if (builder != null) {
                    val dialog = builder.show()
                }

            }
            if (timerStatus == TimerStatus.STARTED) {
                imageViewReset!!.visibility = View.GONE
                imageViewReset!!.isEnabled = false
                imageViewStartStop!!.visibility = View.INVISIBLE
                imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
                editTextMinute!!.isEnabled = true
                durationInc!!.isEnabled = true
                durationDec!!.isEnabled = true
                timerStatus = TimerStatus.STOPPED
                stopCountDownTimer()
                unregisterReceivers()
            }
        } else if (!status && isAutoBatteryTestStart && !isActivityCreated) {
            isActivityCreated = true
            preStartTest()
        }
        //Log.d(TAG, "S8 Atuo action: $isResumeDone $chargerConnected $isActivityCreated ${customizations!!.isAutoStartBatteryDrain}")
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onResumeDialog()
        }
    }

    /**
     * #receiver
     */
    private fun onResumeDialog() {
        if (isConnected(context) && !isResumeDone) {
            val builder: AlertDialog.Builder
            builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
            } else {
                AlertDialog.Builder(context)
            }
            builder.setCancelable(false)
            if (isAutoBatteryTestStart) {
                builder.setTitle(getString(R.string.plugged_in))
                        .setMessage(getString(R.string.battery_msg_auto))
                        .setPositiveButton(android.R.string.yes, { dialog, which -> })
                        .setIcon(android.R.drawable.ic_lock_idle_low_battery)
            } else {
                builder.setTitle(getString(R.string.plugged_in))
                        .setMessage(getString(R.string.battery_msg))
                        .setPositiveButton(android.R.string.yes, { dialog, which -> })
                        .setIcon(android.R.drawable.ic_lock_idle_low_battery)
            }
            /*builder.setTitle(getString(R.string.plugged_in))
                    .setMessage(getString(R.string.battery_msg))
                    .setPositiveButton(android.R.string.yes, { dialog, which -> })
                    .setIcon(android.R.drawable.ic_lock_idle_low_battery)*/
            val dialog = builder.show()
            if (timerStatus == TimerStatus.STARTED) {
                imageViewReset!!.visibility = View.GONE
                imageViewStartStop!!.visibility = View.INVISIBLE
                imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
                editTextMinute!!.isEnabled = true
                durationInc!!.isEnabled = true
                durationDec!!.isEnabled = true
                timerStatus = TimerStatus.STOPPED
                stopCountDownTimer()
            }
            isResumeDone = true
        } else if (!isConnected(context) && !isResumeDone && isAutoBatteryTestStart && !isActivityCreated) {
            isResumeDone = true
            isActivityCreated = true
            preStartTest()
        }
        //Log.d(TAG, "Resume Atuo action: $isResumeDone $chargerConnected $isActivityCreated ${customizations!!.isAutoStartBatteryDrain}")
    }

    private fun initViews() {
        progressBarCircle = findViewById(R.id.progressBarCircle)
        editTextMinute = findViewById(R.id.editTextMinute)
        textViewTime = findViewById(R.id.textViewTime)
        imageViewReset = findViewById(R.id.imageViewReset)
        imageViewStartStop = findViewById(R.id.imageViewStartStop)
    }

    @SuppressLint("SimpleDateFormat")
    private fun initListeners() {
        imageViewReset!!.setOnClickListener(this)
        imageViewStartStop!!.setOnClickListener(this)
        relativelayout!!.setOnClickListener(this)
        simpleDateFormat = SimpleDateFormat("hh:mm:ss aa")
    }

    private var batteryLevelFilter: IntentFilter? = null
    private fun selectDrainTest() {
        selectedDrain = drainGroup.checkedRadioButtonId
        drainGroup.setOnCheckedChangeListener { group, checkedId ->
            selectedDrain = if (checkedId == R.id.duration) {
                Log.d(TAG, "duration selected")
                updateBatteryDuration()
                drainGroup.checkedRadioButtonId
            } else {
                Log.d(TAG, "percentage selected")
                updateBatteryPercentage()
                drainGroup.checkedRadioButtonId
            }
        }
        /**
         * attach this explicit receiver to check battery level
         */
        batteryLevelFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    }

    /**

    Request

    Add Battery Analytics under App Analytics section

    New Column: Battery Drain Type - did the client use "Duration or Percentage" test? (relates to PA-232 )

    New Columns: "Percentage per min" 1-15 mins = at what min % was drained (See sheet example)

    Expected Result

    Should include unique identifiers contained within the sheet

    Build & App Versions required

    All Battery Data available in Cloud DB should be included

     */

    /**
     * #level
     */
    private fun batteryLevelDifference(): Int {
        return startBattery - currentBateryPercentage
    }

    /**
     * start 80
     * after one minute
     * current 79 78
     * updated 80 79
     * if (current < updated) {
     *    batteryDrainDifference = 1 1
     *    updated = current
     * }
     *
     * return batteryDrainDifference
     */
    private fun currentbatteryLevelDifference(): Int {
        batteryDrainDifference = Math.abs(currentBateryPercentage - updatedBatteryPercentage)
        Log.d(TAG, batteryDrainDifference.toString() + "\n")
        updatedBatteryPercentage = currentBateryPercentage
        updateDuration = percentageDuration
        return batteryDrainDifference
    }

    private var currentBateryPercentage: Int = 0
    private var isPTestStarted = false
    private var startBattery = 0
    private val batteryDrainInfo = JSONObject()
    private var isTestClosed: Boolean = false
    private var updatedBatteryPercentage = 0
    private var batteryDrainDifference = 0

    private val batteryLevelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            currentBateryPercentage = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)

            if (!isPTestStarted) {
                startBattery = currentBateryPercentage
                updatedBatteryPercentage = currentBateryPercentage
                isPTestStarted = true
            }

            textViewTime!!.text = "$currentBateryPercentage%"
            progressBarCircle!!.progress = currentBateryPercentage

            if (BuildConfig.DEBUG) {
                showDebugText(View.VISIBLE, "Difference:  $batteryDrainDifference JsonObj: $batteryDrainInfo")
            }
            updatedDate = Calendar.getInstance().time
            calPercentageDifference()

            // to stop battery discharge
            if (currentBateryPercentage <= percentage) {
                context.unregisterReceiver(this)
                isTestClosed = true
                updatedDate = Calendar.getInstance().time
                batteryTemperature.put("endHeat", getTemperatureValue().toString())
                val tempMessage = "Current ${BatteryManager.EXTRA_TEMPERATURE} = ${getTemperatureValue()} ${Character.toString(176.toChar())} C"
                Log.d(TAG, tempMessage)
                calPercentageDifference()
                if (selectedDrain == R.id.duration) {
                    textViewTime!!.text = hmsTimeFormatter(timeCountInMilliSeconds)
                } else {
                    textViewTime!!.text = getString(R.string.battery_percentage_value_desc)
                }
                showDrainGroup(View.VISIBLE)
                imageViewReset!!.visibility = View.GONE
                imageViewReset!!.isEnabled = false
                imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
                imageViewStartStop!!.visibility = View.INVISIBLE
                editTextMinute!!.isEnabled = true
                durationInc!!.isEnabled = true
                durationDec!!.isEnabled = true
                timerStatus = TimerStatus.STOPPED
                autoPerformButtonStateReset()
                stopMotion()
                isTimerStarted = false
                isBatteryTestCompleted = true
                setBrightness(-1F, 0, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                writeAudioReportJson()
                pushBatteryResults()
                setAfterProgressBarValues()
                relativelayout!!.setBackgroundColor(Color.BLUE)
                setBrightness(1F, 255, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                isFlashStopped = true
                showFlashLightContinuous()
            }
        }
    }

    private var simpleDateFormat: SimpleDateFormat? = null
    private var currentDate: Date? = null
    private var updatedDate: Date? = null
    private var percentageDuration: Int = 0
    private fun calPercentageDifference() {
        val myDate1 = simpleDateFormat!!.format(currentDate)
        val Date1 = simpleDateFormat!!.parse(myDate1)

        val myDate2 = simpleDateFormat!!.format(updatedDate)
        val Date2 = simpleDateFormat!!.parse(myDate2)

        val totalMinutes = Date1.time - Date2.time
        val totalMinute = Math.abs(totalMinutes)

        val Mins = (totalMinute / (1000 * 60)).toInt()
        val sec = totalMinute / 1000

        percentageDuration = Mins // updated value every 1 minute
        if (percentageDuration == 0) {
            percentageDuration = 1
        }
        if (selectedDrain != R.id.duration) {
            if (percentageDuration > updateDuration) {
                btrydischDiff!!.text = "$percentageDuration Mins"
                batteryDrainInfo.put(percentageDuration.toString(), "${currentbatteryLevelDifference()}%")
                updateDuration = percentageDuration
            }
            if (isTestClosed) {
                val Seconds = totalMinute / 1000
                Log.d(TAG_1, Seconds.toString())
                if (Seconds > 0 && currentBateryPercentage < updatedBatteryPercentage) {
                    percentageDuration += 1
                }
                /*percentageDuration = Mins // updated value every 1 minute*/
                btrydischDiff!!.text = "$percentageDuration Mins"
                batteryDrainInfo.put(percentageDuration.toString(), "${currentbatteryLevelDifference()}%")
            }
        } else {
            if (percentageDuration > updateDuration) {
                Log.d(TAG, "percentageDuration: $percentageDuration updateDuration: $updateDuration")
                //Toast.makeText(this@BatteryDiagnosticActivity, "percentageDuration: $percentageDuration updateDuration: $updateDuration", Toast.LENGTH_SHORT).show()
                batteryDrainDifference = Math.abs(finalyBtryLvl - updateBtryLvl)
                updateBtryLvl = finalyBtryLvl
                batteryDrainInfo.put(percentageDuration.toString(), "$batteryDrainDifference%")
                if (BuildConfig.DEBUG) {
                    showDebugText(View.VISIBLE, "Difference:  $batteryDrainDifference JsonObj: $batteryDrainInfo")
                }
                updateDuration = percentageDuration
            }
            Log.d(TAG, "percentageDuration: $percentageDuration updateDuration: $updateDuration")
            if (isTestClosed) {
                val Seconds = totalMinute / 1000
                Log.d(TAG_1, Seconds.toString())
                if (Seconds > 0 && finalyBtryLvl < updateBtryLvl) {
                    percentageDuration += 1
                    batteryDrainDifference = Math.abs(finalyBtryLvl - updateBtryLvl)
                    updateBtryLvl = finalyBtryLvl
                    batteryDrainInfo.put(percentageDuration.toString(), "$batteryDrainDifference%")
                    if (BuildConfig.DEBUG) {
                        showDebugText(View.VISIBLE, "Difference:  $batteryDrainDifference JsonObj: $batteryDrainInfo")
                    }
                }
            }
        }
    }

    private fun updateBatteryDuration() {
        editTextMinute!!.text = getString(R.string.battery_duration_text) + " $duration"
        textViewTime!!.text = getString(R.string.battery_duration_value_desc)
        dischgDesc.text = getString(R.string.btry_difference_layout)
        btrydischDiff!!.text = " $lvlDifference%"
    }

    private fun updateBatteryPercentage() {
        editTextMinute!!.text = getString(R.string.battery_percentage_text) + " $percentage%"
        textViewTime!!.text = getString(R.string.battery_percentage_value_desc)
        dischgDesc.text = getString(R.string.btry_duration_layout)
        btrydischDiff!!.text = getString(R.string.discharge_duration_value)
    }

    private fun showDrainGroup(show: Int) {
        drainGroup.visibility = show
        select_drain.visibility = show
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

    private fun playBeep() {
        if (beepManager != null) {
            beepManager!!.close()
            beepManager = null
        }
        beepManager = BeepManagerCustom(this, false)
        beepManager!!.setLoop(true)
        beepManager!!.updatePrefs()
        beepManager!!.playBeepSoundAndVibrate()
    }

    private fun configureVideoView() {
        if (videoView != null) {
            videoView.visibility = View.GONE
            videoView!!.stopPlayback()
        }
        videoView.visibility = View.VISIBLE
        val path: String = "android.resource://" + packageName + "/" + R.raw.video_file
        videoView.setVideoURI(Uri.parse(path))
        videoView.start()
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            Log.i("Column", "Duration = " + videoView.duration)
        }
        videoView.start()
    }

    /**
     * #connected
     */
    @SuppressLint("InlinedApi")
    fun autoPerformClick(view: View) {
        preStartTest()
    }

    private val BATTERY_SETTINGS_WRITE: Int = 0

    fun preStartTest() {
        /*settingsCanWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(context) else true*/
        if (isConnected(context)) {
            autoPerformButton.setImageResource(R.drawable.start)
            Toast.makeText(this, getString(R.string.disconct_device_msg), Toast.LENGTH_LONG).show()
            return
        }
        /*if (!settingsCanWrite) {
            Toast.makeText(this, getString(R.string.allow_modify_settings), Toast.LENGTH_LONG).show()
            autoPerformButton.setImageResource(R.drawable.start)
            startActivityForResult(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS), BATTERY_SETTINGS_WRITE)
            return
        } else {
            startTest()
        }*/
        startTest()
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BATTERY_SETTINGS_WRITE) {
            settingsCanWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(context) else true
            if (!settingsCanWrite) {
                Toast.makeText(this, getString(R.string.click_start_button), Toast.LENGTH_LONG).show()
                autoPerformButton.setImageResource(R.drawable.start)
                return
            } else {
                startTest()
            }
        }
    }*/

    fun durationDecs(view: View) {
        if (selectedDrain == R.id.duration) {
            if (duration <= 1) {
                return
            }
            duration -= 1
            editTextMinute!!.text = getString(R.string.battery_duration_text) + " $duration"
        } else {
            if (percentage <= 1) {
                return
            }
            percentage -= 1
            editTextMinute!!.text = getString(R.string.battery_percentage_text) + " $percentage%"
        }
        isFlashStopped = false
    }

    fun durationIncs(view: View) {
        if (selectedDrain == R.id.duration) {
            duration += 1
            editTextMinute!!.text = getString(R.string.battery_duration_text) + " $duration"
        } else {
            val batteryInfo = BatteryInfo(this)
            if (percentage >= 100) {
                return
            }
            batteryInfo.checkBatteryLevel()
            if (BatteryInfo.batteryLevel <= percentage) {
                Toast.makeText(this, "Give Percentage below Current Battery Level", Toast.LENGTH_SHORT).show()
                return
            }
            percentage += 1
            editTextMinute!!.text = getString(R.string.battery_percentage_text) + " $percentage%"
        }
        isFlashStopped = false
    }

    private fun autoPerformButtonStateReset() {
        autoPerformButton.setImageResource(R.drawable.start)
        autoPerformButton.isEnabled = true
        if (autoPerformButton.animation == null) {
            autoPerformButton.resetAnimation()
        }
        if (autoPerformButton.animation != null)
            autoPerformButton.animation.start()
    }

    private fun startTest() {
        deleteResults()
        isFlashStopped = false
        isTestClosed = false
        val batteryInfo = BatteryInfo(this)
        batteryInfo.checkBatteryLevel()
        earlyBtryLvl = BatteryInfo.batteryLevel
        updateBtryLvl = BatteryInfo.batteryLevel
        if (selectedDrain == R.id.duration) {
            durationFlag = true

            btrydischDiff!!.text = " " + getString(R.string.discharge_difference_value)
            btrydischDiff!!.setBackgroundResource(R.drawable.white_bg)
            setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.drawable_circle_yellow_2)!!)
        } else {
            if (earlyBtryLvl <= percentage) {
                autoPerformButtonStateReset()
                Toast.makeText(this, "Percentage must be below Current Battery Level", Toast.LENGTH_SHORT).show()
                return
            }
            durationFlag = false
            isPTestStarted = false
            btrydischDiff!!.text = " " + getString(R.string.discharge_duration_value)
            btrydischDiff!!.setBackgroundResource(R.drawable.white_bg)
            setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.drawable_circle_yellow_1)!!)
        }
        autoPerformButton.isEnabled = false
        startButtonAnimationStarts()
        setBrightness(1F, 255, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        showDrainGroup(View.GONE)
        startStop()
    }

    private fun startButtonAnimationStarts() {
        autoPerformButton.setImageResource(R.drawable.start_pressed)
        if (autoPerformButton.animation != null) {
            autoPerformButton.animation.cancel()
        }
        autoPerformButton.scaleX = 1f
        autoPerformButton.scaleY = 1f
    }

    private fun deleteResults() {
        if (batteryStats.exists() && batteryStats.length() > 0) {
            batteryStats.delete()
            if (BuildConfig.DEBUG) {
                showDebugText(View.GONE, "")
            }
        }
    }

    private fun unregisterReceivers() {
        if (selectedDrain != R.id.duration) {
            unregisterReceiver(batteryLevelReceiver)
        }
    }

    /**
     * #reset
     */
    private fun reset() {
        deleteResults()
        chargerConnected = false
        isBatteryTestCompleted = false
        unregisterReceivers()
        stopCountDownTimer()
        timerStatus = TimerStatus.STARTED
        startButtonAnimationStarts()
        setBrightness(1F, 255, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        showDrainGroup(View.GONE)
        // get time
        currentDate = Calendar.getInstance().time
        isTestClosed = false
        if (selectedDrain == R.id.duration) {
            val batteryInfo = BatteryInfo(context)
            batteryInfo.checkBatteryLevel()
            earlyBtryLvl = BatteryInfo.batteryLevel
            updateBtryLvl = BatteryInfo.batteryLevel
            setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.drawable_circle_yellow_2)!!)
            startCountDownTimer()
        } else {
            // register receiver
            registerReceiver(batteryLevelReceiver, batteryLevelFilter)
            isPTestStarted = false
            val batteryInfo = BatteryInfo(context)
            batteryInfo.checkBatteryLevel()
            earlyBtryLvl = BatteryInfo.batteryLevel
            updateBtryLvl = BatteryInfo.batteryLevel
            setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.drawable_circle_yellow_1)!!)
        }
        if (videoView != null) {
            videoView.visibility = View.GONE
            videoView.stopPlayback()
        }
        playBeep()
        configureVideoView()
        isTimerStarted = true
    }

    private fun setProgressDrawable(drawable: Drawable) {
        progressBarCircle!!.progressDrawable = drawable
    }

    /**
     * #Percentage
     */
    private fun startStop() {
        chargerConnected = false
        isBatteryTestCompleted = false
        batteryDrainDifference = 0
        percentageDuration = 0
        updatedBatteryPercentage = 0
        currentBateryPercentage = 0
        updateDuration = 0
        if (timerStatus == TimerStatus.STOPPED) {
            imageViewStartStop!!.visibility = View.VISIBLE
            imageViewReset!!.visibility = View.VISIBLE
            imageViewStartStop!!.setImageResource(R.drawable.ic_stop_24dp)
            editTextMinute!!.isEnabled = false
            durationInc!!.isEnabled = false
            durationDec!!.isEnabled = false
            imageViewReset!!.isEnabled = true
            timerStatus = TimerStatus.STARTED
            showDrainGroup(View.GONE)
            configureVideoView()
            playBeep()
            isTimerStarted = true
            batteryTemperature.put("startHeat", getTemperatureValue().toString())
            try {
                Handler().postDelayed({
                    batteryTemperature.put("startHeat", getTemperatureValue().toString())
                }, 1500)
            }catch (e: Exception) {}
            val tempMessage = "Current ${BatteryManager.EXTRA_TEMPERATURE} = ${getTemperatureValue()} ${Character.toString(176.toChar())} C"
            Log.d(TAG, tempMessage)
            if (selectedDrain == R.id.duration) {
                setTimerValues()
                setProgressBarValues()
                // get time
                currentDate = Calendar.getInstance().time
                startCountDownTimer()
            } else {
                setProgressBarValues()
                // get time
                currentDate = Calendar.getInstance().time
                // register receiver
                registerReceiver(batteryLevelReceiver, batteryLevelFilter)
            }
        } else {
            imageViewReset!!.visibility = View.GONE
            imageViewStartStop!!.visibility = View.INVISIBLE
            imageViewReset!!.isEnabled = false
            showDrainGroup(View.VISIBLE)
            imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
            editTextMinute!!.isEnabled = true
            durationInc!!.isEnabled = true
            durationDec!!.isEnabled = true
            timerStatus = TimerStatus.STOPPED
            isTimerStarted = false
            stopCountDownTimer()
            unregisterReceivers()
        }
    }

    private fun setTimerValues() {
        var time = 0
        if (!editTextMinute!!.text.toString().isEmpty() && duration != 0) {
            //time = Integer.parseInt(editTextMinute!!.getText().toString().trim { it <= ' ' })
            val customizations: ClientCustomization? = Loader.instance.clientCustomization
            if (bundle as Boolean) {
                time = duration
            } else {
                if (customizations != null /*&& customizations.isAutoBatteryDrain*/ && !isAutoStarted && customizations.duration >= 1) {
                    time = duration
                    isAutoStarted = true
                } else {
                    time = duration
                }
            }
            //time = duration
        } else if (editTextMinute!!.text.toString().equals("0") || duration == 0) {
            Toast.makeText(applicationContext, getString(R.string.message_minutes_zero), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(applicationContext, getString(R.string.message_minutes), Toast.LENGTH_LONG).show()
        }
        timeCountInMilliSeconds = (time * 60 * 1000).toLong()
    }

    fun showDebugText(isShown: Int, dText: String) {
//        debugText.visibility = isShown
//        debugText.text = dText
    }

    private var updateDuration: Int = 0

    private fun startCountDownTimer() {
        countDownTimer = object : CountDownTimer(timeCountInMilliSeconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                textViewTime!!.text = hmsTimeFormatter(millisUntilFinished)
                progressBarCircle!!.progress = (millisUntilFinished / 1000).toInt()
                val batteryInfo = BatteryInfo(context)
                batteryInfo.checkBatteryLevel()
                finalyBtryLvl = BatteryInfo.batteryLevel
                lvlDifference = earlyBtryLvl - finalyBtryLvl
                btrydischDiff!!.text = " $lvlDifference%"
                updatedDate = Calendar.getInstance().time
                calPercentageDifference()

                //Log.d(TAG, "T data : $percentageDuration $batteryDrainInfo")
            }

            override fun onFinish() {
                isTestClosed = true
                updatedDate = Calendar.getInstance().time
                batteryTemperature.put("endHeat", getTemperatureValue().toString())
                val tempMessage = "Current ${BatteryManager.EXTRA_TEMPERATURE} = ${getTemperatureValue()} ${Character.toString(176.toChar())} C"
                Log.d(TAG, tempMessage)
                calPercentageDifference()
                textViewTime!!.text = hmsTimeFormatter(timeCountInMilliSeconds)
                setAfterProgressBarValues()
                showDrainGroup(View.VISIBLE)
                imageViewReset!!.visibility = View.GONE
                imageViewReset!!.isEnabled = false
                imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
                imageViewStartStop!!.visibility = View.INVISIBLE
                editTextMinute!!.isEnabled = true
                durationInc!!.isEnabled = true
                durationDec!!.isEnabled = true
                timerStatus = TimerStatus.STOPPED
                val batteryInfo = BatteryInfo(context)
                batteryInfo.checkBatteryLevel()
                finalyBtryLvl = BatteryInfo.batteryLevel
                autoPerformButtonStateReset()
                calculateDifference()
                isFlashStopped = true
                stopMotion()
                isTimerStarted = false
                isBatteryTestCompleted = true
                writeAudioReportJson()
                pushBatteryResults()
                showFlashLightContinuous()
            }
        }.start()
    }

    /**
     * #flash /* if (BuildConfig.DEBUG) {}*/
     */
    private var isFlashStopped = false

    private fun showFlashLightContinuous() {
        relativelayout!!.isClickable = true
        relativelayout!!.setBackgroundColor(Color.BLUE)
        setBrightness(1F, 255, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Thread(Runnable {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                if (context != null) {
                    relativelayout!!.setBackgroundColor(Color.WHITE)
                    setBrightness(-1F, 0, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                }
            }
            while (isFlashStopped) {
                Log.d(TAG_1, "Loop Started Thread goes to sleep")
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                runOnUiThread {
                    if (context != null) {
                        Log.d(TAG_1, "First settled blue background after 1 sec")
                        relativelayout!!.setBackgroundColor(Color.BLUE)
                        setBrightness(1F, 255, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    }
                }
                Log.d(TAG_1, "Thread goes to sleep again")
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                runOnUiThread {
                    Log.d(TAG_1, "Now settled white background after 1 sec")
                    if (context != null) {
                        relativelayout!!.setBackgroundColor(Color.WHITE)
                        setBrightness(-1F, 0, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    }
                }
                Log.d(TAG_1, "Thread goes to sleep for last time and loop again started")
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            if (!isFlashStopped) {
                Log.d(TAG_1, "Get out of Loop, User is now collecting data.")
                try {
                    Thread.interrupted()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                runOnUiThread {
                    if (context != null) {
                        relativelayout!!.isClickable = false
                        relativelayout!!.setBackgroundColor(Color.WHITE)
                        setBrightness(-1F, 0, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    }
                }
            }
        }).start()
    }

    private var durationFlag: Boolean = false
    private fun writeAudioReportJson() {
        try {
            if (durationFlag) {
                val jsonObject = JSONObject()
                val batteryStat = JSONObject()
                batteryStat.put("totalDuration", duration.toString())
                batteryStat.put("startBattery", "$earlyBtryLvl%")
                batteryStat.put("endBattery", "$finalyBtryLvl%")
                batteryStat.put("totalDischarge", lvlDifference.toString())
                batteryStat.put("batteryDrainType", "Duration")
                jsonObject.put("batteryStat", batteryStat)
                jsonObject.put("batteryDrainInfo", batteryDrainInfo)
                jsonObject.put("batteryTemperature", batteryTemperature)
                Log.d(TAG, "Battery Drain Info Per Minute: $batteryDrainInfo")
                WriteObjectFile.getInstance().writeObject(jsonObject.toString(), "/BatteryResults.json")

            } else {
                val jsonObject = JSONObject()
                val batteryStat = JSONObject()
                batteryStat.put("totalDuration", percentageDuration.toString())
                batteryStat.put("startBattery", "$earlyBtryLvl%")
                batteryStat.put("endBattery", "$currentBateryPercentage%")
                batteryStat.put("totalDischarge", batteryLevelDifference().toString())
                batteryStat.put("batteryDrainType", "Percentage")
                jsonObject.put("batteryStat", batteryStat)
                jsonObject.put("batteryDrainInfo", batteryDrainInfo)
                jsonObject.put("batteryTemperature", batteryTemperature)
                Log.d(TAG, "Battery Drain Info Per Minute: $batteryDrainInfo")
                WriteObjectFile.getInstance().writeObject(jsonObject.toString(), "/BatteryResults.json")

            }
            /**
             * currentBateryPercentage.toString()
             * percentageDuration.toString()
             */
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }
    }

    private fun pushBatteryResults() {
        if (durationFlag) {
            columnAPIService?.pushColumn(Column(licenseObj, serialObj, transactionObj, duration.toString(),
                    "$earlyBtryLvl%", "$finalyBtryLvl%", lvlDifference.toString(),
                    "Duration", batteryDrainInfo.toString()))!!.enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    showDialog()
                }

                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    if (response!!.body().equals("success"))
                        Toast.makeText(this@BatteryDiagnosticActivity, "Sync Results Successful", Toast.LENGTH_LONG).show()
                    else
                        showDialog()
                }
            })
        } else {
            columnAPIService?.pushColumn(Column(licenseObj, serialObj, transactionObj, percentageDuration.toString(),
                    "$earlyBtryLvl%", "$currentBateryPercentage%", batteryLevelDifference().toString(),
                    "Percentage", batteryDrainInfo.toString()))!!.enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    showDialog()
                }

                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    if (response!!.body().equals("success"))
                        Toast.makeText(this@BatteryDiagnosticActivity, "Sync Results Successful", Toast.LENGTH_LONG).show()
                    else
                        showDialog()
                }
            })
        }
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
                    .setMessage(getString(R.string.battery_msg_revert))
                    .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which -> isFlashStopped = false })
                    .setIcon(android.R.drawable.ic_lock_idle_low_battery)

            if (builder != null) {
                val dialog = builder.show()
            }

        }
    }

    private fun calculateDifference() {
        lvlDifference = earlyBtryLvl - finalyBtryLvl
        btrydischDiff!!.text = " $lvlDifference%"
        btrydischDiff!!.setBackgroundResource(R.drawable.white_bg)
    }

    private fun stopCountDownTimer() {
        if (countDownTimer != null) {
            countDownTimer!!.cancel()
            countDownTimer = null
        }
        stopMotion()
        if (selectedDrain == R.id.duration) {
            textViewTime!!.text = hmsTimeFormatter(timeCountInMilliSeconds)
        } else {
            textViewTime!!.text = getString(R.string.battery_percentage_value_desc)
        }
        showDrainGroup(View.VISIBLE)
        setAfterProgressBarValues()
        setBrightness(-1F, 0, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
        isTimerStarted = false
        isFlashStopped = false
        autoPerformButtonStateReset()
    }

    private fun setProgressBarValues() {
        if (selectedDrain != R.id.duration) {
            if (currentBateryPercentage != 0) {
                val batteryInfo = BatteryInfo(this)
                batteryInfo.checkBatteryLevel()
                earlyBtryLvl = BatteryInfo.batteryLevel
                updateBtryLvl = BatteryInfo.batteryLevel
                progressBarCircle!!.max = 100
                progressBarCircle!!.progress = earlyBtryLvl
            } else {
                progressBarCircle!!.max = 100
                progressBarCircle!!.progress = earlyBtryLvl
            }
        } else {
            progressBarCircle!!.max = timeCountInMilliSeconds.toInt() / 1000
            progressBarCircle!!.progress = timeCountInMilliSeconds.toInt() / 1000
        }
    }

    private fun setAfterProgressBarValues() {
        if (selectedDrain != R.id.duration) {
            if (currentBateryPercentage != 0) {
                val batteryInfo = BatteryInfo(this)
                batteryInfo.checkBatteryLevel()
                earlyBtryLvl = BatteryInfo.batteryLevel
                updateBtryLvl = BatteryInfo.batteryLevel
                progressBarCircle!!.max = 100
                progressBarCircle!!.progress = 100
            } else {
                progressBarCircle!!.max = 100
                progressBarCircle!!.progress = 100
            }
        } else {
            progressBarCircle!!.max = timeCountInMilliSeconds.toInt() / 1000
            progressBarCircle!!.progress = timeCountInMilliSeconds.toInt() / 1000
        }
    }

    private fun hmsTimeFormatter(milliSeconds: Long): String {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliSeconds),
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)))
    }

    fun setBrightness(level: Float, value: Int, mode: Int) {
        try {
            Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
            Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val window = window
        val lp = window.attributes
        lp.screenBrightness = level
        window.attributes = lp
    }

    /**
     * #receiver
     */
    private var chargerConnected: Boolean = false
    private val chargerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            // This work only for android 4.4+

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                val action = intent!!.action
                if (isBatteryTestCompleted) {
                    Log.i(packageName, Loader.BATTERY_START_PREFIX + "Column Test Completed" + Loader.BATTERY_END_PREFIX)
                }
                if ((action != null && action == Constants.CHARGER && isResumeDone && !chargerConnected) ||
                        (action != null && action == Constants.CHARGER && !isResumeDone && !chargerConnected)) {
                    chargerConnected = true
                    if (context != null) {
                        val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                        } else {
                            AlertDialog.Builder(context)
                        }
                        builder.setCancelable(false)
                        builder.setTitle(getString(R.string.plugged_in))
                                .setMessage(getString(R.string.battery_msg))
                                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which -> })
                                .setIcon(android.R.drawable.ic_lock_idle_low_battery)
                        if (builder != null) {
                            val dialog = builder.show()
                        }

                    }
                    if (timerStatus == TimerStatus.STARTED) {
                        imageViewReset!!.visibility = View.GONE
                        imageViewReset!!.isEnabled = false
                        imageViewStartStop!!.visibility = View.INVISIBLE
                        imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
                        editTextMinute!!.isEnabled = true
                        durationInc!!.isEnabled = true
                        durationDec!!.isEnabled = true
                        timerStatus = TimerStatus.STOPPED
                        stopCountDownTimer()
                        unregisterReceivers()
                    }
                }

            }
        }
    }

    /**
     * #receiver
     */
    private var isActivityCreated: Boolean = false
    private var temperatureValue = 0
    private val batteryTemperature = JSONObject()
    private val autoBatteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if ((action != null && action.equals(Intent.ACTION_POWER_DISCONNECTED) && isAutoBatteryTestStart && !isActivityCreated) ||
                    (action != null && action.equals(Intent.ACTION_POWER_DISCONNECTED) && isAutoBatteryTestStart && !isActivityCreated)) {
                isActivityCreated = true
                preStartTest()
            } else if ((action != null && action.equals(Intent.ACTION_POWER_CONNECTED) && !isResumeDone && !chargerConnected) ||
                    (action != null && action.equals(Intent.ACTION_POWER_CONNECTED) && isResumeDone && !chargerConnected)) {
                chargerConnected = true
                if (context != null) {
                    val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                    } else {
                        AlertDialog.Builder(context)
                    }
                    builder.setCancelable(false)
                    if (isAutoBatteryTestStart) {
                        builder.setTitle(getString(R.string.plugged_in))
                                .setMessage(getString(R.string.battery_msg_auto))
                                .setPositiveButton(android.R.string.yes) { dialog, which -> }
                                .setIcon(android.R.drawable.ic_lock_idle_low_battery)
                    } else {
                        builder.setTitle(getString(R.string.plugged_in))
                                .setMessage(getString(R.string.battery_msg))
                                .setPositiveButton(android.R.string.yes) { dialog, which -> }
                                .setIcon(android.R.drawable.ic_lock_idle_low_battery)
                    }
                    if (builder != null) {
                        val dialog = builder.show()
                    }

                }
                if (timerStatus == TimerStatus.STARTED) {
                    imageViewReset!!.visibility = View.GONE
                    imageViewReset!!.isEnabled = false
                    imageViewStartStop!!.visibility = View.INVISIBLE
                    imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
                    editTextMinute!!.isEnabled = true
                    durationInc!!.isEnabled = true
                    durationDec!!.isEnabled = true
                    timerStatus = TimerStatus.STOPPED
                    stopCountDownTimer()
                    unregisterReceivers()
                }
            }

            temperatureValue = intent!!.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

            Log.d(TAG, "Receiver Atuo action: $action resume: $isResumeDone charConne: $chargerConnected " +
                    "ActCreated: $isActivityCreated Auto: $isAutoBatteryTestStart")
        }
    }

    private fun getTemperatureValue(): Float {
        return (temperatureValue / 10).toFloat()
    }

    override fun onPause() {
        Log.v("Column", "pause")
        stopTest()
        isFlashStopped = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        Loader.instance.deletebatterystatFile()
    }

    private fun stopTest() {
        if (timerStatus == TimerStatus.STARTED) {
            imageViewReset!!.visibility = View.GONE
            imageViewStartStop!!.visibility = View.INVISIBLE
            imageViewStartStop!!.setImageResource(R.drawable.ic_play_24dp)
            editTextMinute!!.isEnabled = true
            durationInc!!.isEnabled = true
            durationDec!!.isEnabled = true
            timerStatus = TimerStatus.STOPPED
            stopCountDownTimer()
        }
    }

    override fun onRestart() {
        if (isTimerStarted) {
            playBeep()
            configureVideoView()
        }
        Log.v("Column", "restart")
        Loader.instance.createbatterystatFile()
        super.onRestart()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(chargerReceiver)
        stopMotion()
        isTimerStarted = false
        Loader.instance.deletebatterystatFile()
        if (timer != null) {
            timer?.cancel()
        }
        isActivityCreated = false
        unregisterReceiver(autoBatteryReceiver)
        isFlashStopped = false
        super.onDestroy()
    }

    private fun stopMotion() {
        if (beepManager != null) {
            beepManager!!.close()
            beepManager = null
        }
        if (videoView != null) {
            videoView.visibility = View.GONE
            videoView!!.stopPlayback()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this@BatteryDiagnosticActivity, TestCompletionActivity::class.java))
        stopCountDownTimer()
        this.finish()
    }

    companion object {
        @JvmStatic
        fun isConnected(context: Context): Boolean {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || isCharging
        }

        val REQ = 100
        @JvmStatic
        val batteryStats = File("${Loader.baseFile}/BatteryResults.json")
        val TAG = "BatteryActivity"
        val TAG_1 = "Flash"
    }

}