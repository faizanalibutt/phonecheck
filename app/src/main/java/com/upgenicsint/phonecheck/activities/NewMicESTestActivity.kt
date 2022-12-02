package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.Cabinet
import com.google.gson.Gson
import com.newventuresoftware.waveform.WaveformView
import com.upgenicsint.phonecheck.*
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.activities.MicESTestActivity.Companion.MICES_SCREEN_TIME
import com.upgenicsint.phonecheck.barcode.BeepManagerES
import com.upgenicsint.phonecheck.broadcastreceiver.HeadSetPlugStatusReceiver
import com.upgenicsint.phonecheck.media.RecordingThreadES
import com.upgenicsint.phonecheck.misc.*
import com.upgenicsint.phonecheck.models.Amplitude
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AudioPlaybackTest
import com.upgenicsint.phonecheck.test.chip.NewMicESTest
import com.upgenicsint.phonecheck.test.chip.NewMicLSTest
import kotlinx.android.synthetic.main.activity_new_mic_estest.*
import kotlinx.android.synthetic.main.activity_new_mic_estest.view.*
import kotlinx.android.synthetic.main.audio_new_input_test_layout.view.*
import org.json.JSONObject
import java.util.*
import kotlin.jvm.internal.Intrinsics

class NewMicESTestActivity : BaseAudioTestActivity<NewMicESTest>(), HeadSetPlugCallBack {

    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var isAutoStartRunning = false
    private var micText: TextView? = null
    private var isBeepfromHeadset: Boolean = false //headset is connected or not
    private var runNext = false //flag to stop auto start next test when user select a single test
    private var earpiecePlay = false
    private var muteEarpieceSound: Boolean = false
    private var isSamsung = true
    private var isMicrophone = false
    private var audioManager: AudioManager? = null
    private var beepManager: BeepManagerES? = null
    private var mRecordingThreadES: RecordingThreadES? = null
    private var amplitude: Int = 0
    private var amplitudeMicList: MutableList<Amplitude> = ArrayList()
    private var aboveCounter = 0
    private val pref by lazy { Cabinet.open(context, R.string.newmic_es) }
    private var headSetReceiver: HeadSetPlugStatusReceiver? = null
    private var countDownTimerCallStarted: CountDownTimer? = null
    // new testLayout
    private var isComingDirectly = false
    private var isSpeakerTestFailed: Boolean = false
    private var isSpeakerTestPassed: Boolean = false
    private var isSpeakerPlayonFailed: Boolean = false
    private var isSpeakerPlayonPassed: Boolean = false
    private var isMicPasClicked: Boolean = false
    private var isBeepRunning: Boolean = false
    private var headsetLeftCode = -1
    private var headsetRightCode = -1
    private var isPluggedOut: Boolean = false
    private var isHeadsetDialogClicked: Boolean = true
    private var isManualTesting: Boolean = false
    private var isComingFromHeadset: Boolean = false
    private var progressMaxValue: Long = 0
    private var isMicClicked: Boolean = false
    private var isvideoMicFailed: Boolean = false
    private var isRetryingMic: Boolean = false
    private var isLSRetesting: Boolean = false
    private var isManual: Boolean = false
    private var isMicFailed: Boolean = false
    private var isRetryClicked: Boolean = false
    private var runMorseCode = true
    private var isMicEsFailed: Boolean = false
    private var isMicC: Boolean = false
    private var maxAmplitude = 0
    private var micFrequency = 1000
    private var micDuration = 500
    // get Duration from media player and put to text
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "AutoES"
    private var tone1List: MutableList<Amplitude> = ArrayList()
    private var tone2List: MutableList<Amplitude> = ArrayList()
    private var tone3List: MutableList<Amplitude> = ArrayList()
    private var gap1List: MutableList<Amplitude> = ArrayList()
    private var gap2List: MutableList<Amplitude> = ArrayList()
    private fun getVolumeForMode(mode: Int): Int {
        val volume: Int
        volume = if (BuildConfig.DEBUG) {
            (audioManager!!.getStreamMaxVolume(mode).toDouble() * 0.1F).toInt()
        } else {
            val audioManager = this.audioManager
            if (this.audioManager == null) {
                Intrinsics.throwNpe()
            }
            (audioManager!!.getStreamMaxVolume(mode).toDouble() * 1F).toInt()
        }
        return volume
    }

    private var isDoneClick = false
    private var saveHeadsetObject: SharedPreferences.Editor? = null

    /**
     *  Listener keeps on listening when headphone is connected or not.
     */
    private var showHeadset: Boolean = false

    override fun onHeadSetAttachment(isAttached: Boolean) {
        if (!showHeadset) {
            earpieceLayout.isClickable = !isAttached
            earpieceLayout.isEnabled = !isAttached
            headsetLayout.isClickable = isAttached
            headsetLayout.isEnabled = isAttached
            headsetLayout.alpha = if (isAttached) 1f else 0.5f
            earpieceLayout.alpha = if (!isAttached) 1f else 0.5f
            if (isAttached) {
                isPluggedOut = false
                muteEarpieceSound = true
                if (!showHeadset) {
                    headsetLayout.performClick()
                }
            } else {
                muteEarpieceSound = false
                runNext = true
                earpiecePlay = true
                isPluggedOut = true
                if (isSamsung && isAutoStartRunning && !isHeadsetDialogClicked) {
                    playBeep()
                }
                if (isBeepfromHeadset) {
                    resetAutoStart()
                }
            }
        }
    }

    private var timerTask : TimerTask? = null

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_new_mic_estest)
        /**
         * reset and done button declaration and set title of activity
         */
        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("Mic Ear Speaker Test")

        try {
            Loader.TIME_VALUE = 0
            MICES_SCREEN_TIME = 0
            if (BaseActivity.autoPerform) {
                timerTask = object : TimerTask() {
                    override fun run() {
                        Loader.RECORD_HANDLER.post {
                            Loader.TIME_VALUE++
                            Log.d(TAG, Loader.TIME_VALUE.toString())
                        }
                    }
                }
                Loader.RECORD_TIMER_TEST.schedule(timerTask, 1000, 1000)
            } else {
                Loader.RECORD_TIMER_TASK = object : TimerTask() {

                    override fun run() {
                        Loader.RECORD_HANDLER.post {
                            Loader.TIME_VALUE++
                            Log.d(TAG, Loader.TIME_VALUE.toString())
                        }
                    }
                }
                Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        saveHeadsetObject = getSharedPreferences(getString(R.string.report_headset), Context.MODE_PRIVATE).edit()

        test = Loader.instance.getByClassType(NewMicESTest::class.java)
        val reportHeadset = getSharedPreferences(getString(R.string.report_headset), Context.MODE_PRIVATE)
                .getBoolean(getString(R.string.check_headset), false)
        isDoneClick = getSharedPreferences(getString(R.string.report_headset),
                Context.MODE_PRIVATE).getBoolean(getString(R.string.onetime_headsetreport), false)

        showHeadset = reportHeadset && test != null && test!!.hasSubTest(Test.headsetRightKey)
                && test!!.hasSubTest(Test.headsetLeftKey) && test!!.hasSubTest(Test.headsetPortKey)

        if (reportHeadset && test != null && test!!.hasSubTest(Test.headsetRightKey)
                && test!!.hasSubTest(Test.headsetLeftKey) && test!!.hasSubTest(Test.headsetPortKey)) {
            /*test?.resultsFilterMap?.remove(Test.headsetPortKey)*/
            test?.subTests?.remove(Test.headsetPortKey)
            test?.subTests?.remove(Test.headsetLeftKey)
            test?.subTests?.remove(Test.headsetRightKey)
        }

        headSetReceiver = HeadSetPlugStatusReceiver(this)
        /**
         * audio manager initializing
         */
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
        audioManager!!.isSpeakerphoneOn = false
        //headset init
        headsetStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.headsetLeftKey)?.value
                ?: Test.INIT))
        if (!reportHeadset && test?.hasSubTest(Test.headsetLeftKey) == true && test?.hasSubTest
                (Test.headsetRightKey) == true) {
            headsetLayout.visibility = View.VISIBLE
        } else {
            headsetLayout.visibility = View.GONE
        }
        headsetLayout.isClickable = false
        headsetLayout.isEnabled = false
        headsetLayout.setOnClickListener {
            headsetLeftCode = generateNewNumber(true)
            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_HEADSET_LEFT
            intent.putExtra(TTSService.RANDOM_NUMBER, headsetLeftCode.toString())
            startService(intent)
            runNext = false
            isAutoStartRunning = true
        }
        /**
         * check whether test involved Tests within Test
         */
        if (test?.hasSubTest(Test.earphoneTestKey) == false) {
            earpieceLayout.visibility = View.GONE
        } else {
            earpieceStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.earphoneTestKey)?.value
                    ?: Test.INIT))
            earpieceLayout.setOnClickListener {
                if (!isAutoStartRunning) {
                    earpiecePlay = true
                    runNext = false
                    isAutoStartRunning = true
                    isComingDirectly = true
                    isManual = false
                    isMicEsFailed = false
                    isLSRetesting = true
                    isMicClicked = false
                    isRetryClicked = false
                    isRetryingMic = true
                    if (isSamsung) lsTapProgress.visibility = View.VISIBLE else lsTapProgress.visibility = View.GONE
                    audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
                    addViews()
                    playBeepForEarSpeaker()
                }
            }
        }
        // add microphone and video mic runtime
        val micContainer = micTestLayout as ViewGroup
        if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
            isSamsung = true
            // show text at runtime on Speaker fail.
            showMicText(micContainer)
            microphoneView = LayoutInflater.from(context).inflate(R.layout.audio_new_input_test_layout, null)
            micContainer.addView(microphoneView)
            val resultsFilterMap = test?.resultsFilterMap
            if (resultsFilterMap != null) {
                if (resultsFilterMap[Test.videoESMicTestKey] == true) {
                    videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.audio_new_input_test_layout, null)
                    micContainer.addView(videoMicrophoneView)
                }
            }
            microphoneView?.let { microphoneView ->
                microphoneView.nameTextView.text = context.getString(R.string.microphone)
                microphoneView.imageView.setImageResource(R.drawable.microphone)
                microphoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.micESTestKey)?.value
                        ?: Test.INIT))
                microphoneView.amplitudeTextView.text = "${pref.getInt(MIC_ES_AMPLITUDE_PREF, 0)}"
            }
            videoMicrophoneView?.let { videoMicrophoneView ->
                videoMicrophoneView.nameTextView.text = context.getString(R.string.video_microphone)
                videoMicrophoneView.imageView.setImageResource(R.drawable.microphone)
                videoMicrophoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.videoESMicTestKey)?.value
                        ?: Test.INIT))
                videoMicrophoneView.amplitudeTextView.text = "${pref.getInt(VID_ES_MIC_AMPLITUDE_PREF, 0)}"

            }
            microphoneView?.setBackgroundResource(R.drawable.selector_row)
            videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)
            showResultView()
            if (BuildConfig.FLAVOR === Constants.AUDIO) handleAudioSimulation()
        } else {
            isSamsung = false
            micTestLayoutScroll.visibility = View.GONE
            micContainer.visibility = View.GONE
            startButton.visibility = View.GONE
        }
        // Event Listeners
        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlay = true
                isMicPasClicked = true
                isMicClicked = true
                isLSRetesting = false
                isRetryClicked = false
                isMicEsFailed = false
                playBeep()
                audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
            }
        }
        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlay = true
                isMicPasClicked = true
                isMicClicked = true
                isRetryingMic = false
                isLSRetesting = false
                isRetryClicked = false
                isMicEsFailed = false
                videoMicTest()
                audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
            }
        }
        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                resetTest()
                Handler().postDelayed({
                    if (!muteEarpieceSound) {
                        if (!isBeepRunning) {
                            if (BuildConfig.FLAVOR !== Constants.AUDIO) {
                                playBeep()
                            }
                        }
                    }
                }, 1000)
            }
        }
        //samsung supports autoStart else exception and Manual Prefs
        manual = getSharedPreferences(getString(R.string.manual), Context.MODE_PRIVATE).edit()
        if (isSamsung && !muteEarpieceSound) {
            val alreadyStarted = pref.getBoolean(AUDIO_AUTO_ES_START_KEY, false)
            if (!alreadyStarted) {
                startButton.performClick()
            }
        }
    }

    private fun resetTest() {
        isAutoStartRunning = true
        runNext = true
        earpiecePlay = true
        isMicPasClicked = false
        isMicClicked = false
        isManualTesting = false
        isBeepRunning = false
        isSpeakerPlayonFailed = false
        isSpeakerPlayonPassed = false
        isSpeakerTestPassed = false
        isSpeakerTestFailed = false
        isComingDirectly = false
        isHeadsetDialogClicked = true
        isPluggedOut = false
        TEST_LOCK = false
        isRetryingMic = false
        isvideoMicFailed = false
        isMicrophone = false
        isLSRetesting = false
        isManual = false
        isMicFailed = false
        runMorseCode = true
        isMicC = false
        micDuration = 0
        micFrequency = 0
        isMicEsFailed = false
        releaseAudioRecorder()
        test?.sub(Test.earphoneTestKey)?.value = Test.INIT
        test?.sub(Test.micESTestKey)?.value = Test.INIT
        test?.sub(Test.videoESMicTestKey)?.value = Test.INIT
        test?.sub(Test.headsetPortKey)?.value = Test.INIT
        microphoneView?.statusImageView?.setImageResource(getImageForStatus(Test.INIT))
        videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(Test.INIT))
        earpieceStatusImageView.setImageResource(getImageForStatus(Test.INIT))
        aboveCounter = 0
        if (amplitudeMicList.size > 0) {
            amplitudeMicList.clear()
        }
        if (tone1List.size > 0) {
            tone1List.clear()
        }
        if (tone2List.size > 0) {
            tone2List.clear()
        }
        if (tone3List.size > 0) {
            tone3List.clear()
        }
        if (gap1List.size > 0) {
            gap1List.clear()
        }
        if (gap2List.size > 0) {
            gap2List.clear()
        }
        addViews()
    }

    @SuppressLint("NewApi")
    private fun showMicText(micContainer: ViewGroup) {
        micText = TextView(this)
        if (micText != null) {
            val spanText = SpannableString("Say \"Hello\" Loud and Clear")
            spanText.setSpan(StyleSpan(Typeface.BOLD), 5, 10, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            micText!!.text = spanText
            micText!!.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            micText!!.textSize = 22F
            val params = micText!!.layoutParams as LinearLayout.LayoutParams
            params.setMargins(5, 5, 5, 20)
            micText!!.layoutParams = params
            micText!!.visibility = View.GONE
            micText!!.gravity = Gravity.CENTER
            micText!!.setTextColor(ContextCompat.getColor(this@NewMicESTestActivity, R.color.dark_black))
        }
        micContainer.addView(micText)
    }

    private fun showResultView() {
        videoMicrophoneView?.setOnLongClickListener {

            showQuestionAlert("Enter Pin", object : TextFieldListener {
                val pin = 5612
                override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                    try {
                        if (isTrue && pin == text.toInt()) {
                            startActivity(Intent(this@NewMicESTestActivity,
                                    AmplitudeResultsDetail::class.java).putExtra(Constants.ES_VID_MIC, true))
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this@NewMicESTestActivity, "Please Input Correct Pin", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Pin code not matched.")
                        e.printStackTrace()
                    }
                }

            })
            return@setOnLongClickListener true
        }
    }

    /**
     * #headset
     */
    override fun onNavDoneClick(v: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (isDoneClick) {
                super.onNavDoneClick(v)
                return
            }

            if (test?.sub(Test.headsetPortKey)?.value == Test.PASS) {
                super.onNavDoneClick(v)
            } else {
                val showHeadset = Loader.instance.filterContains(Test.headsetLeftKey) &&
                        Loader.instance.filterContains(Test.headsetLeftKey) && Loader.instance.filterContains(Test.headsetPortKey)
                if (showHeadset) {
                    showPreDoneAlert(object : AlertButtonListener {
                        override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                            if (type == AlertButtonListener.ButtonType.RIGHT) {
                                saveHeadsetObject!!.putBoolean(getString(R.string.onetime_headsetreport), true)
                                saveHeadsetObject!!.apply()
                                isDoneClick = true
                                onNavDoneClick(v)
                                reportHeadphoneJack(getString(R.string.yes), false)
                            } else {
                                saveHeadsetObject!!.putBoolean(getString(R.string.onetime_headsetreport), true)
                                saveHeadsetObject!!.apply()
                                isDoneClick = true
                                if (test != null && test!!.hasSubTest(Test.headsetRightKey)
                                        && test!!.hasSubTest(Test.headsetLeftKey) && test!!.hasSubTest(Test.headsetPortKey)) {
                                    test?.subTests?.remove(Test.headsetPortKey)
                                    test?.subTests?.remove(Test.headsetLeftKey)
                                    test?.subTests?.remove(Test.headsetRightKey)
                                    headsetLayout.visibility = View.GONE
                                }
                                onNavDoneClick(v)
                                reportHeadphoneJack(getString(R.string.no), true)
                            }
                            dialog.dismiss()
                        }
                    })
                } else {
                    super.onNavDoneClick(v)
                }
            }
        } else {
            super.onNavDoneClick(v)
        }

    }

    private fun reportHeadphoneJack(value: String, result: Boolean) {
        val reportHeadsetObject = JSONObject()
        reportHeadsetObject.put("Manufacturer_Name", Build.MANUFACTURER)
        reportHeadsetObject.put("Model_No", Build.MODEL)
        reportHeadsetObject.put("User_Selection", value)
        Log.i(packageName, Loader.RESULT_HEADSET_START_PREFIX + reportHeadsetObject.toString() + Loader.RESULT_HEADSET_END_PREFIX)
        //WriteObjectFile.getInstance().writeObject(reportHeadsetObject.toString(), "/HeadsetReport.json")

        saveHeadsetObject?.putBoolean(getString(R.string.check_headset), result)
        saveHeadsetObject?.apply()
    }

    private val simulateTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            try {
                MIC_AMPLITUDE_CHECKING = s.toString().toDouble()
            } catch (e: Exception) {
                AMPLITUDE_CHECKING = 1200.00
                MIC_AMPLITUDE_CHECKING = 1200.00
                e.printStackTrace()
            }
        }
    }

    private val simulateTextWatcher1 = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            try {
                VID_AMPLITUDE_CHECKING = s.toString().toDouble()
            } catch (e: Exception) {
                AMPLITUDE_CHECKING = 1200.00
                VID_AMPLITUDE_CHECKING = 1200.00
                e.printStackTrace()
            }
        }
    }

    private fun handleAudioSimulation() {
        microphoneView?.simulateMic?.addTextChangedListener(simulateTextWatcher)
        videoMicrophoneView?.simulateMic?.addTextChangedListener(simulateTextWatcher1)
        microphoneView?.simulateMic?.visibility = View.VISIBLE
        videoMicrophoneView?.simulateMic?.visibility = View.VISIBLE
        earpieceLayout.simulateCheck?.visibility = View.VISIBLE
        microphoneView?.simulateMic?.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                playBeep()
                val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun removeTextListener() {
        microphoneView?.simulateMic?.removeTextChangedListener(simulateTextWatcher)
        videoMicrophoneView?.simulateMic?.removeTextChangedListener(simulateTextWatcher1)
    }

    private fun playBeepForEarSpeaker() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        releaseAudioRecorder()
        progressMaxValue = 8000L
        aboveCounter = 0
        isLSRetesting = true
        isMicClicked = false
        maxAmplitude = 0
        initCountdownTimer()
        Thread(Runnable {
            try {
                Thread.sleep(700)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (isSamsung) {
                if (amplitudeMicList.size > 0) {
                    amplitudeMicList.clear()
                }
                if (tone1List.size > 0) {
                    tone1List.clear()
                }
                if (tone2List.size > 0) {
                    tone2List.clear()
                }
                if (tone3List.size > 0) {
                    tone3List.clear()
                }
                RecordingThreadES.isMic = false
                isMicrophone = false
                beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone)
                beepManager!!.setDevice(isSamsung)
                beepManager!!.setMicrophone(isMicrophone)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()
                mRecordingThreadES = RecordingThreadES(RecordingThreadES.CallBack {
                    if (!isLSRetesting) updateVisualizer(it, videoMicrophoneView?.visualizer) else updateVisualizer(it, null)
                    Logy.d(amplitude.toString())
                })
                // start recording for 4 sec
                maxAmplitude = 0
                countDownTimerCallStarted?.start()
                mRecordingThreadES!!.startRecording()
            } else {
                RecordingThreadES.isMic = false
                beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone)
                beepManager!!.setDevice(isSamsung)
                beepManager!!.setMicrophone(isMicrophone)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()
                runOnUiThread { Handler().postDelayed({ showSpeakerPopup() }, 1500) }
            }
        }).start()
    }

    private fun updateVisualizer(samples: ShortArray?, visualizer: WaveformView?) {
        amplitude = if (Build.SERIAL == Constants.S6_EDGE_VID_MIC) {
            (mRecordingThreadES?.currentAmplitude ?: 0) * 2
        } else {
            mRecordingThreadES?.currentAmplitude ?: 0
        }
        visualizer?.samples = samples
    }

    private fun playBeep() {
        if (!isActivityVisible) {
            return
        }
        if (test?.sub(Test.micESTestKey)?.value == Test.PASS) {
            if (isMicPasClicked) {
                test?.sub(Test.micESTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                playBeep()
                return
            }
            videoMicTest()
            return
        }
        if (test?.sub(Test.micESTestKey)?.value == Test.FAILED && runNext) {
            if (isComingFromHeadset && !isManualTesting) {
                TEST_LOCK = false
                videoMicTest()
                return
            } else {
                if (!isMicPasClicked && !isRetryClicked) {
                    micText!!.visibility = View.VISIBLE
                }
                isSpeakerTestFailed = true
                isSpeakerTestPassed = false
                if (isRetryClicked) {
                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                    microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                } else {
                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                    microphoneView?.micProgressBar?.visibility = View.VISIBLE
                }
            }
        }
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        isMicrophone = true
        progressMaxValue = 4000L
        microphoneView?.micProgressBar!!.max = 4200
        releaseAudioRecorder()
        amplitude = 0
        maxAmplitude = 0
        initCountdownTimer()

        startRecording(microphoneView?.visualizer)
    }

    private fun startRecording(view: WaveformView?) {
        Thread(Runnable {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            RecordingThreadES.isMic = true
            mRecordingThreadES = RecordingThreadES(RecordingThreadES.CallBack {
                updateVisualizer(it, view)
                Logy.d(amplitude.toString())
            })
            mRecordingThreadES?.startRecording()
            playZentone()

            // start recording for 4 sec
            countDownTimerCallStarted?.start()

        }).start()
    }

    private fun videoMicTest() {
        if (test?.sub(Test.videoESMicTestKey)?.value == Test.PASS) {
            if (isMicEsFailed) {
                return
            }
            if (isMicPasClicked) {
                test?.sub(Test.videoESMicTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                isManualTesting = false
                isMicFailed = false
                videoMicTest()
            }
            TEST_LOCK = false
            resetAutoStart()
            return
        }
        if ((test?.sub(Test.videoESMicTestKey)?.value == Test.FAILED && runNext) ||
                (test?.sub(Test.videoESMicTestKey)?.value == Test.FAILED && isComingDirectly && isManual)) {
            if (isComingFromHeadset && !isManualTesting) {
                TEST_LOCK = false
                isComingFromHeadset = false
                runNext = false
                resetAutoStart()
                return
            } else {
                isMicFailed = true
                if (!isMicPasClicked && !isRetryClicked) {
                    micText!!.visibility = View.VISIBLE
                }
                isSpeakerTestFailed = true
                isSpeakerTestPassed = false
                isvideoMicFailed = true
                if (isRetryClicked) {
                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                    videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                } else {
                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                    videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
                }
            }
        }
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        isMicrophone = false
        if ((isvideoMicFailed && isManual) || isMicClicked) {
            progressMaxValue = 4000
            videoMicrophoneView?.micProgressBar!!.max = 4000
        } else {
            progressMaxValue = 8000
            videoMicrophoneView?.micProgressBar!!.max = 8000
        }
        releaseAudioRecorder()
        // used to enable/disable visualizer with the help of CountDownTimer.
        initCountdownTimer()
        Thread(Runnable {
            runOnUiThread {
                RecordingThreadES.isMic = false
                mRecordingThreadES = RecordingThreadES(RecordingThreadES.CallBack {
                    updateVisualizer(it, videoMicrophoneView?.visualizer)
                    Logy.d(amplitude.toString())
                })
                mRecordingThreadES?.startRecording()
                // start recording for 4 sec
                maxAmplitude = 0
                countDownTimerCallStarted?.start()
            }
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if ((isvideoMicFailed && isManual) || isMicClicked) {
                isMicrophone = true
                beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone)
                beepManager!!.setDevice(isSamsung)
                beepManager!!.setMicrophone(isMicrophone)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()
            } else {
                beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone)
                beepManager!!.setDevice(isSamsung)
                beepManager!!.setMicrophone(isMicrophone)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()
            }
        }).start()
    }

    private fun retrySpeakerTest() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        releaseAudioRecorder()
        if (test?.sub(Test.videoESMicTestKey)?.value == Test.PASS && test?.sub(Test.micESTestKey)?.value == Test.PASS
                && test?.sub(Test.earphoneTestKey)?.value != Test.PASS) {
            if (isSamsung) lsTapProgress.visibility = View.VISIBLE else lsTapProgress.visibility = View.GONE
        }
        if (isMicEsFailed) {
            lsTapProgress.visibility = View.VISIBLE
        }
        progressMaxValue = 8000
        aboveCounter = 0
        isMicClicked = false
        isManual = false
        RecordingThreadES.isMic = false
        initCountdownTimer()
        Thread(Runnable {
            try {
                Thread.sleep(700)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (isSamsung) {
                if (amplitudeMicList.size > 0) {
                    amplitudeMicList.clear()
                }
                if (tone1List.size > 0) {
                    tone1List.clear()
                }
                if (tone2List.size > 0) {
                    tone2List.clear()
                }
                if (tone3List.size > 0) {
                    tone3List.clear()
                }
                isMicrophone = false
                beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone)
                beepManager!!.setDevice(isSamsung)
                beepManager!!.setMicrophone(isMicrophone)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()
                mRecordingThreadES = RecordingThreadES(RecordingThreadES.CallBack {
                    updateVisualizer(it, null)
                    Logy.d(amplitude.toString())
                })
                maxAmplitude = 0
                // start recording for 4 sec
                countDownTimerCallStarted?.start()
                mRecordingThreadES?.startRecording()
            } else {
                RecordingThreadES.isMic = false
                beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone)
                beepManager!!.setDevice(isSamsung)
                beepManager!!.setMicrophone(isMicrophone)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()
                runOnUiThread { Handler().postDelayed({ showSpeakerPopup() }, 1500) }
            }
        }).start()
    }

    private fun initCountdownTimer() {
        countDownTimerCallStarted = object : CountDownTimer(progressMaxValue, 100L) {
            var progress = 0
            var duration = 0L
            override fun onTick(p0: Long) {
                if (!runMorseCode) {
                    this.cancel()
                    return
                }
                if (RecordingThreadES.isMic) {
                    progress += 105
                    duration += 100L
                    microphoneView?.micProgressBar?.progress = progress
                    if (isManual && !isMicClicked) {
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
                    }
                } else {
                    progress += 105
                    videoMicrophoneView?.micProgressBar?.progress = progress
                    /**
                     * what was done in a big list now divided into 3 lists
                     */
                    duration += 100L
                    Logy.d("Results", "Duration: $duration , Amplitude: $amplitude")
                    if (duration in 1000L..3000L) {
                        if (amplitude >= 1200L) {
                            tone1List.add(Amplitude(amplitude, true))
                        } else {
                            tone1List.add(Amplitude(amplitude, false))
                        }
                    }
                    if (duration in 3500L..5000L) {
                        if (amplitude >= 1200L) {
                            tone2List.add(Amplitude(amplitude, true))
                        } else {
                            tone2List.add(Amplitude(amplitude, false))
                        }
                    }
                    if (duration in 6000L..8000L) {
                        if (amplitude >= 1200L) {
                            tone3List.add(Amplitude(amplitude, true))
                        } else {
                            tone3List.add(Amplitude(amplitude, false))
                        }
                    }
                    if (isLSRetesting && !isManual) {
                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                    }
                }
                if (amplitude > maxAmplitude) {
                    maxAmplitude = amplitude
                }
                if (RecordingThreadES.isMic) {
                    if (maxAmplitude >= AMPLITUDE_CHECKING && duration >= 2000L) {
                        releaseMicBeep()
                        countDownTimerCallStarted!!.onFinish()
                    }
                }
            }

            override fun onFinish() {
                RecordingThreadES.mShouldContinue = false
                if (!runMorseCode) {
                    return
                }
                TEST_LOCK = false
                lsTapProgress.visibility = View.GONE
                val value: Int
                if (RecordingThreadES.isMic) {
                    if (BuildConfig.FLAVOR === Constants.AUDIO) {
                        value = if (maxAmplitude >= MIC_AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    } else {
                        value = if (maxAmplitude >= 1000) Test.PASS else Test.FAILED
                    }
                    releaseAudioRecorder()
                    test?.sub(Test.micESTestKey)?.value = value
                    microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    microphoneView?.amplitudeTextView?.text = maxAmplitude.toString()
                    pref.add(MIC_ES_AMPLITUDE_PREF, maxAmplitude).save()
                    maxAmplitude = 0
                    progress = 0
                    duration = 0L
                    microphoneView?.micProgressBar?.progress = progress
                    if (runNext || (isLSRetesting && isRetryClicked)) {
                        if (videoMicrophoneView != null) {
                            if (isSpeakerTestFailed) {
                                if (test?.sub(Test.videoESMicTestKey)!!.value == Test.PASS) {
                                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                    videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                                } else {
                                    if (isRetryClicked) {
                                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                                        videoMicrophoneView?.micProgressBar?.progress = 0
                                    } else {
                                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                        videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
                                        videoMicrophoneView?.micProgressBar?.progress = 0
                                    }
                                }
                                if (test?.sub(Test.micESTestKey)!!.value == Test.PASS) {
                                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                    microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                                } else {
                                    if (isRetryClicked) {
                                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                                        microphoneView?.micProgressBar?.progress = 0
                                    } else {
                                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
                                        microphoneView?.micProgressBar?.progress = 0
                                    }
                                }
                            }
                            Thread(Runnable {
                                try {
                                    Thread.sleep(700)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                                runOnUiThread {
                                    videoMicTest()
                                }
                            }).start()
                        }
                    }
                    if (isMicFailed && isManual && test?.sub(Test.videoESMicTestKey)?.value == Test.PASS) {
                        micText!!.visibility = View.GONE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                    }
                    if (!isMicEsFailed && isRetryClicked && test?.sub(Test.videoESMicTestKey)?.value == Test.PASS) {
                        showSpeakerPopup()
                    }
                    if ((isMicEsFailed && isRetryClicked)) {
                        manual?.putBoolean(Constants.MANUAL_ES, false)?.apply()
                        isRetryingMic = true
                        retrySpeakerTest()
                    }
                } else {
                    setSpeakerResult()
                    amplitudeMicList.addAll(tone1List)
                    amplitudeMicList.addAll(gap1List)
                    amplitudeMicList.addAll(tone2List)
                    amplitudeMicList.addAll(gap2List)
                    amplitudeMicList.addAll(tone3List)
                    if (BuildConfig.FLAVOR === Constants.AUDIO) {
                        value = if (maxAmplitude >= VID_AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    } else {
                        value = if (maxAmplitude >= AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    }
                    releaseAudioRecorder()
                    if (!isRetryingMic) {
                        isRetryingMic = true
                        test?.sub(Test.videoESMicTestKey)?.value = value
                        videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        videoMicrophoneView?.amplitudeTextView?.text = maxAmplitude.toString()
                        pref.add(VID_ES_MIC_AMPLITUDE_PREF, maxAmplitude).save()
                    }
                    if (isLSRetesting && isRetryClicked && !isRetryingMic) {
                        test?.sub(Test.videoESMicTestKey)?.value = value
                        videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        videoMicrophoneView?.amplitudeTextView?.text = maxAmplitude.toString()
                        pref.add(VID_ES_MIC_AMPLITUDE_PREF, maxAmplitude).save()
                    }
                    if (!isMicClicked) {
                        if (aboveCounter >= 3 && !simulateCheck.isChecked) {
                            test?.sub(Test.earphoneTestKey)?.value = Test.PASS
                            Loader.RESULT.put(Test.earphoneTestKey, Test.PASS)
                            manual?.putBoolean(Constants.MANUAL_ES, false)?.apply()
                            earpieceStatusImageView.setImageResource(getImageForStatus(Test.PASS))
                        } else {
                            //removeMicPlayBackTest()
                            test?.sub(Test.earphoneTestKey)?.value = Test.FAILED
                            Loader.RESULT.put(Test.earphoneTestKey, Test.FAILED)
                            manual?.putBoolean(Constants.MANUAL_ES, false)?.apply()
                            earpieceStatusImageView.setImageResource(getImageForStatus(Test.FAILED))
                            if (isRetryClicked || isLSRetesting) {
                                showSpeakerPopup()
                            } /*else {
                                removeMicPlayBackTest()
                            }*/
                        }
                    }
                    val resultsPref: SharedPreferences.Editor = getSharedPreferences(getString(R.string.resultsPref), Context.MODE_PRIVATE).edit()
                    resultsPref.putString(getString(R.string.micesresultsList), Gson().toJson(amplitudeMicList))
                    resultsPref.apply()
                    maxAmplitude = 0
                    progress = 0
                    duration = 0L
                    /**
                     * In case data is populating in list more on clicking mic recycling data
                     */
                    if (amplitudeMicList.size > 0) {
                        if (tone1List.size > 0) {
                            if (tone2List.size > 0) {
                                if (tone3List.size > 0) {
                                    amplitudeMicList.clear()
                                    tone1List.clear()
                                    tone2List.clear()
                                    tone3List.clear()
                                }
                            }
                        }
                    }
                    videoMicrophoneView?.micProgressBar?.progress = progress
                    if (isSpeakerTestFailed) {
                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                        isSpeakerPlayonFailed = true
                        isSpeakerPlayonPassed = true
                    }
                    if (isSpeakerTestPassed) {
                        isSpeakerPlayonPassed = true
                        isSpeakerPlayonFailed = true
                    }
                    micText!!.visibility = View.GONE
                    if (runNext && test?.sub(Test.earphoneTestKey)?.value != Test.PASS) {
                        if (isSpeakerTestFailed) {
                            TEST_LOCK = false
                            releaseAudioRecorder()
                            resetAutoStart()
                            return
                        }
                        showSpeakerPopup()
                    } else {
                        resetAutoStart()
                    }
                    if (isManual) {
                        micText!!.visibility = View.GONE
                    }
                }
            }
        }
    }

   /* override fun removeMicPlayBackTest() {
        //super.removeMicPlayBackTest()
        val autols = Loader.instance.getByClassType(NewMicLSTest::class.java)
        if (autols != null && autols.sub(Test.loudSpeakerTestKey)?.value == Test.FAILED && (aboveCounter < 3 || BuildConfig.FLAVOR === Constants.AUDIO)) {
            val mpt = Loader.instance.getByClassType(AudioPlaybackTest::class.java)
            if (mpt != null) {
                Loader.instance.testList.remove(mpt)
            }
        }
    }*/

    private fun releaseMicBeep() {
        if (beepManager != null) {
            beepManager!!.close()
            beepManager = null
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun showSpeakerPopup() {
        val title = getString(R.string.speaker_popup)
        TEST_LOCK = false
        if (aboveCounter >= 3 && !simulateCheck.isChecked) {
            manual?.putBoolean(Constants.MANUAL_ES, false)?.apply()
            if (isSpeakerTestPassed) {
                test?.sub(Test.earphoneTestKey)?.value = Test.PASS
                earpieceStatusImageView.setImageResource(getImageForStatus(Test.PASS))
                TEST_LOCK = false
                releaseAudioRecorder()
                resetAutoStart()
                return
            }
            micText!!.visibility = View.GONE
            microphoneView?.setBackgroundResource(R.drawable.selector_row)
            videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)
            isSpeakerTestFailed = false
            isSpeakerTestPassed = true
            micText!!.visibility = View.GONE
            videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
            videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
            microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
            microphoneView?.micProgressBar?.visibility = View.INVISIBLE
            TEST_LOCK = false
            test?.sub(Test.earphoneTestKey)?.value = Test.PASS
            earpieceStatusImageView.setImageResource(getImageForStatus(Test.PASS))
            releaseAudioRecorder()
            Handler().postDelayed({ resetAutoStart() }, 1500)
        } else {
            showQuestionAlert(title, getString(R.string.did_hear_sound_speaker), object : RetryTextFieldListener {
                override fun onClick(dialog: DialogInterface, type: RetryTextFieldListener.ButtonType) {
                    val isTrue = type == RetryTextFieldListener.ButtonType.RIGHT
                    val isAutoES = type == RetryTextFieldListener.ButtonType.NEUTRAL
                    val value = if (isAutoES) Test.INIT else if (isTrue) Test.PASS else Test.FAILED
                    if (value == Test.INIT) {
                        TEST_LOCK = false
                        test?.sub(Test.earphoneTestKey)?.value = value
                        earpieceStatusImageView.setImageResource(getImageForStatus(value))
                        releaseAudioRecorder()
                        resetAutoStart()
                        Handler().postDelayed({
                            isRetryClicked = true
                            isRetryingMic = false
                            when {
                                test?.sub(Test.micESTestKey)?.value == Test.FAILED -> {
                                    if (test?.sub(Test.micESTestKey)?.value == Test.FAILED && test?.sub(Test.earphoneTestKey)?.value != Test.PASS
                                            && test?.sub(Test.videoESMicTestKey)?.value == Test.PASS) {
                                        isMicEsFailed = true
                                        /**
                                         * data is populating in list more on clicking mic recycling data
                                         */
                                        if (amplitudeMicList.size > 0) {
                                            if (tone1List.size > 0) {
                                                if (tone2List.size > 0) {
                                                    if (tone3List.size > 0) {
                                                        amplitudeMicList.clear()
                                                        tone1List.clear()
                                                        tone2List.clear()
                                                        tone3List.clear()
                                                    }
                                                }
                                            }
                                        }
                                        aboveCounter = 0
                                        playBeep()
                                    } else {
                                        /**
                                         * data is populating in list more on clicking mic recycling data
                                         */
                                        if (amplitudeMicList.size > 0) {
                                            if (tone1List.size > 0) {
                                                if (tone2List.size > 0) {
                                                    if (tone3List.size > 0) {
                                                        amplitudeMicList.clear()
                                                        tone1List.clear()
                                                        tone2List.clear()
                                                        tone3List.clear()
                                                    }
                                                }
                                            }
                                        }
                                        aboveCounter = 0
                                        playBeep()
                                    }
                                }
                                test?.sub(Test.videoESMicTestKey)?.value == Test.FAILED -> {
                                    /**
                                     * data is populating in list more on clicking mic recycling data
                                     */
                                    if (amplitudeMicList.size > 0) {
                                        if (tone1List.size > 0) {
                                            if (tone2List.size > 0) {
                                                if (tone3List.size > 0) {
                                                    amplitudeMicList.clear()
                                                    tone1List.clear()
                                                    tone2List.clear()
                                                    tone3List.clear()
                                                }
                                            }
                                        }
                                    }
                                    aboveCounter = 0
                                    videoMicTest()
                                }
                                else -> {
                                    manual?.putBoolean(Constants.MANUAL_ES, false)?.apply()
                                    isRetryingMic = true
                                    retrySpeakerTest()
                                }
                            }
                        }, 300)
                        return
                    }
                    if (isSamsung) {
                        if (value == Test.PASS) {
                            manual?.putBoolean(Constants.MANUAL_ES, true)?.apply()
                            Loader.RESULT.put(Test.earphoneTestKey + "-M", value)
                            if (isSpeakerPlayonPassed) {
                                test?.sub(Test.earphoneTestKey)?.value = value
                                earpieceStatusImageView.setImageResource(getImageForStatus(value))
                                TEST_LOCK = false
                                dialog.dismiss()
                                releaseAudioRecorder()
                                resetAutoStart()
                            }
                            micText!!.visibility = View.GONE
                            microphoneView?.setBackgroundResource(R.drawable.selector_row)
                            videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)
                            isSpeakerTestFailed = false
                            isSpeakerTestPassed = true
                            micText!!.visibility = View.GONE
                            videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                            videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                            microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                            microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                            TEST_LOCK = false
                        } else {
                            //removeMicPlayBackTest()
                            manual?.putBoolean(Constants.MANUAL_ES, true)?.apply()
                            Loader.RESULT.put(Test.earphoneTestKey + "-M", value)
                            if (isSpeakerPlayonFailed) {
                                test?.sub(Test.earphoneTestKey)?.value = value
                                earpieceStatusImageView.setImageResource(getImageForStatus(value))
                                TEST_LOCK = false
                                dialog.dismiss()
                                releaseAudioRecorder()
                                resetAutoStart()
                            }
                            isRetryingMic = false
                            isRetryClicked = false
                            isManual = true
                            isLSRetesting = false
                            if (test?.sub(Test.micESTestKey)?.value == Test.PASS) {
                                test?.sub(Test.earphoneTestKey)?.value = value
                                TEST_LOCK = false
                                earpieceStatusImageView.setImageResource(getImageForStatus(value))
                                dialog.dismiss()
                                resetAutoStart()
                                isMicFailed = false
                                videoMicTest()
                                return
                            }
                            if (isComingDirectly) {
                                isSpeakerTestFailed = true
                                isSpeakerTestPassed = false
                                micText!!.visibility = View.GONE
                                microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                microphoneView?.micProgressBar?.visibility = View.GONE
                                videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                videoMicrophoneView?.micProgressBar?.visibility = View.GONE
                            } else {
                                if (!isComingFromHeadset) {
                                    isSpeakerTestFailed = true
                                    isSpeakerTestPassed = false
                                    if (test?.sub(Test.micESTestKey)?.value == Test.FAILED) {
                                        micText!!.visibility = View.VISIBLE
                                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
                                    }
                                    if (test?.sub(Test.videoESMicTestKey)?.value == Test.FAILED) {
                                        micText!!.visibility = View.VISIBLE
                                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                        videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
                                    }
                                }
                            }
                            runNext = true
                            if (runNext) {
                                TEST_LOCK = false
                                isManualTesting = true
                                isMicClicked = true
                                isMicFailed = true
                                /**
                                 * data is populating in list more on clicking mic recycling data
                                 */
                                if (amplitudeMicList.size > 0) {
                                    if (tone1List.size > 0) {
                                        if (tone2List.size > 0) {
                                            if (tone3List.size > 0) {
                                                amplitudeMicList.clear()
                                                tone1List.clear()
                                                tone2List.clear()
                                                tone3List.clear()
                                            }
                                        }
                                    }
                                }
                                aboveCounter = 0
                                playBeep()
                            }
                        }
                    } else {
                        if (value == Test.PASS) {
                            manual?.putBoolean(Constants.MANUAL_ES, true)?.apply()
                            Loader.RESULT.put(Test.earphoneTestKey + "-M", value)
                            if (isSpeakerPlayonPassed) {
                                test?.sub(Test.earphoneTestKey)?.value = value
                                earpieceStatusImageView.setImageResource(getImageForStatus(value))
                                TEST_LOCK = false
                                dialog.dismiss()
                                releaseAudioRecorder()
                                resetAutoStart()
                            }
                            isSpeakerTestFailed = false
                            isSpeakerTestPassed = true
                            TEST_LOCK = false
                        } else {
                            manual?.putBoolean(Constants.MANUAL_ES, true)?.apply()
                            Loader.RESULT.put(Test.earphoneTestKey + "-M", value)
                            if (isSpeakerPlayonFailed) {
                                test?.sub(Test.earphoneTestKey)?.value = value
                                earpieceStatusImageView.setImageResource(getImageForStatus(value))
                                TEST_LOCK = false
                                dialog.dismiss()
                                releaseAudioRecorder()
                                resetAutoStart()
                            }
                            isRetryClicked = false
                            if (isComingDirectly) {
                                isSpeakerTestFailed = true
                                isSpeakerTestPassed = false
                            }
                        }
                    }
                    if (runNext) {
                        test?.sub(Test.earphoneTestKey)?.value = value
                        earpieceStatusImageView.setImageResource(getImageForStatus(value))
                        dialog.dismiss()
                        resetAutoStart()
                    } else {
                        test?.sub(Test.earphoneTestKey)?.value = value
                        earpieceStatusImageView.setImageResource(getImageForStatus(value))
                        dialog.dismiss()
                        releaseAudioRecorder()
                        resetAutoStart()
                    }
                }
            })
        }
    }

    /**
     * Algorithm to automatically test speaker
     */
    private fun setSpeakerResult() {
        if (tone1List.size > 0) {
            for (tone in tone1List) {
                val amplitude: Amplitude = tone
                if (amplitude.green) {
                    aboveCounter += 1
                    Log.v("Results", "passedA $aboveCounter")
                    break
                }
            }
        }
        if (tone2List.size > 0) {
            for (tone in tone2List) {
                val amplitude: Amplitude = tone
                if (amplitude.green) {
                    aboveCounter += 1
                    Log.v("Results", "passedA $aboveCounter")
                    break
                }
            }
        }
        if (tone3List.size > 0) {
            for (tone in tone3List) {
                val amplitude: Amplitude = tone
                if (amplitude.green) {
                    aboveCounter += 1
                    Log.v("Results", "passedA $aboveCounter")
                    break
                }
            }
        }
    }

    private val ttsCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            stopPlayback()
            TEST_LOCK = false
            if (intent == null || intent.action == null) {
                return
            }
            val action = intent.getStringExtra(TTSService.TTS_ACTION)
            if (action == TTSService.PLAY_HEADSET_LEFT) {
                headsetRightCode = generateNewNumber(true)
                val rightHeadsetIntent = Intent(context, TTSService::class.java)
                rightHeadsetIntent.action = TTSService.PLAY_HEADSET_RIGHT
                rightHeadsetIntent.putExtra(TTSService.RANDOM_NUMBER, headsetRightCode.toString())
                startService(rightHeadsetIntent)
            } else {
                val message = if (action == TTSService.PLAY_EARPIECE) getString(R.string.earpiece_alerttext) else getString(R.string.did_hear_sound)
                var title = action.replace("RIGHT", "").replace("PLAY_", "").replace("_", " ")
                if (title.contains("EARPIECE")) {
                    title = getString(R.string.earpiece_popup)
                } else if (title.contains("HEADSET RIGHT")) {
                    title = getString(R.string.headsetright_popup)
                } else if (title.contains("HEADSET")) {
                    title = getString(R.string.headtset_popup)
                }
                showQuestionAlert(title, message, object : AlertButtonListener {
                    override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                        val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                        val value = if (isTrue) Test.PASS else Test.FAILED
                        isHeadsetDialogClicked = false
                        isManualTesting = false
                        if (value == Test.PASS && title.equals("headset", true)) {
                            isBeepfromHeadset = true
                            if (isBeepfromHeadset) {
                                muteEarpieceSound = false
                                runNext = true
                                earpiecePlay = true
                                isBeepRunning = true
                                TEST_LOCK = false
                                if (isSamsung && isAutoStartRunning) {
                                    isComingFromHeadset = true
                                    if (isPluggedOut && !isHeadsetDialogClicked) {
                                        playBeep()
                                    }
                                }
                                handleUserSelection(action, value)
                                dialog.dismiss()
                            }
                            if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                micText!!.visibility = View.GONE
                            }
                        } else {
                            isBeepfromHeadset = false
                            if (!isBeepfromHeadset) {
                                muteEarpieceSound = false
                                runNext = true
                                earpiecePlay = true
                                isBeepRunning = true
                                TEST_LOCK = false
                                if (isSamsung && isAutoStartRunning) {
                                    isComingFromHeadset = true
                                    if (isPluggedOut && !isHeadsetDialogClicked) {
                                        playBeep()
                                    }
                                }
                                handleUserSelection(action, value)
                                dialog.dismiss()
                            }
                        }
                        if (isBeepfromHeadset && !isSamsung) {
                            resetAutoStart()
                        }
                        if (isBeepfromHeadset && isSamsung) {
                            resetAutoStart()
                            isAutoStartRunning = true
                        }
                    }
                })
            }
        }
    }

    private fun handleUserSelection(action: String?, value: Int) {
        when (action) {
            TTSService.PLAY_HEADSET_RIGHT -> {
                test?.sub(Test.headsetRightKey)?.value = value
                test?.sub(Test.headsetLeftKey)?.value = value
                test?.sub(Test.headsetPortKey)?.value = Test.PASS
                headsetStatusImageView.setImageResource(getImageForStatus(value))
            }
            TTSService.PLAY_EARPIECE -> {
                test?.sub(Test.earphoneTestKey)?.value = value
                earpieceStatusImageView.setImageResource(getImageForStatus(value))
            }
        }
    }

    /**
     * Release Audio For Further tests.
     */
    private fun releaseAudioRecorder() {
        if (beepManager != null) {
            beepManager!!.close()
            beepManager = null
        }
        if (mRecordingThreadES != null) {
            mRecordingThreadES!!.stopRecording()
            RecordingThreadES.mShouldContinue = false
            mRecordingThreadES = null
        }
        if (countDownTimerCallStarted != null) {
            countDownTimerCallStarted!!.cancel()
            countDownTimerCallStarted = null
        }
    }

    private fun playZentone() {
        /*ZenTone.getInstance().stop()
        ZenTone.getInstance().stopTone()
        ZenTone.getInstance().generate2(micFrequency, micDuration, 1f, earpiecePlay, context) {}*/
        isMicC = true
        isMicrophone = false
        beepManager = BeepManagerES(this, earpiecePlay, isSamsung, isMicrophone, isMicC)
        beepManager!!.setDevice(isSamsung)
        beepManager!!.setMicrophone(isMicrophone)
        beepManager!!.setMicC(isMicC)
        beepManager!!.updatePrefs()
        beepManager!!.playBeepSoundAndVibrate()
    }

    /**
     * resetAutoStart for further proceeds.
     */
    private fun resetAutoStart() {
        randomNumber = generateNewNumber(false)
        isAutoStartRunning = false
        pref.add(AUDIO_AUTO_ES_START_KEY, true)
        pref.save()
        testWatcher()
    }

    private fun addViews() {
        // gaplist1
        val randomNumberRange1 = 1 + Random().nextInt(6)
        val from1 = 5
        val to1 = 900
        val random1 = Random()
        val amplitudes1 = IntArray(randomNumberRange1) { random1.nextInt(to1 - from1) + from1 }
        Arrays.sort(amplitudes1)
        for (amplitude in amplitudes1) {
            gap1List.add(Amplitude(amplitude, false))
        }
        Logy.d(TAG, gap1List.toString())
        // gaplist2
        val randomNumberRange2 = 1 + Random().nextInt(6)
        val from2 = 5
        val to2 = 900
        val random2 = Random()
        val amplitudes2 = IntArray(randomNumberRange2) { random2.nextInt(to2 - from2) + from2 }
        Arrays.sort(amplitudes2)
        for (amplitude in amplitudes2) {
            gap2List.add(Amplitude(amplitude, false))
        }
        Logy.d(TAG, gap2List.toString())
    }


    override fun onStart() {
        super.onStart()
        val ttsCompleteFilter = IntentFilter(TTSService.SEND_TTS_ON_COMPLETE)
        LocalBroadcastManager.getInstance(context).registerReceiver(ttsCompleteReceiver, ttsCompleteFilter)
    }

    public override fun onResume() {
        super.onResume()

        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headSetReceiver, filter)
    }

    override fun onPause() {

        lsTapProgress.visibility = View.GONE
        RecordingThreadES.isMic = false
        RecordingThreadES.mShouldContinue = false
        runMorseCode = true
        releaseAudioRecorder()
        TEST_LOCK = false
        stopPlayback()
        resetAutoStart()
        removeTextListener()
        if (headSetReceiver != null)
            unregisterReceiver(headSetReceiver)
        super.onPause()
    }

    override fun onStop() {
        releaseAudioRecorder()
        RecordingThreadES.isMic = false
        RecordingThreadES.mShouldContinue = false
        super.onStop()
    }

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }



    override fun closeTimerTest() {
        super.closeTimerTest()
        try {

            if (BaseActivity.autoPerform) {
                if (timerTask != null) {
                    timerTask!!.cancel()
                    timerTask = null
                    MICES_SCREEN_TIME = Loader.TIME_VALUE
                    try {
                        val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                        Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_mices), -1)] =
                                RecordTest(context.getString(R.string.report_mices_test), MICES_SCREEN_TIME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Loader.RECORD_TESTS_TIME.put("MIC Ear Speaker", "${MICES_SCREEN_TIME}s")
                    Log.d(TAG, MICES_SCREEN_TIME.toString())
                    Loader.TIME_VALUE = 0
                }
            } else {
                if (Loader.RECORD_TIMER_TASK != null) {
                    Loader.RECORD_TIMER_TASK!!.cancel()
                    Loader.RECORD_TIMER_TASK = null
                    MICES_SCREEN_TIME = Loader.TIME_VALUE
                    try {
                        val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                        Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_mices), -1)] =
                                RecordTest(context.getString(R.string.report_mices_test), MICES_SCREEN_TIME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Loader.RECORD_TESTS_TIME.put("MIC Ear Speaker", "${MICES_SCREEN_TIME}s")
                    Log.d(TAG, MICES_SCREEN_TIME.toString())
                    Loader.TIME_VALUE = 0
                }
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onDestroy() {
        releaseAudioRecorder()
        RecordingThreadES.isMic = false
        RecordingThreadES.mShouldContinue = false
        super.onDestroy()
    }

    /**
     * static variables of this class.
     */
    companion object {
        val REQ = 7786
        private var AMPLITUDE_CHECKING = 1200.00
        private var MIC_AMPLITUDE_CHECKING = 1200.00
        private var VID_AMPLITUDE_CHECKING = 1200.00
        val AUDIO_AUTO_ES_START_KEY = "autoMicESCheckStart"
        val MIC_ES_AMPLITUDE_PREF = "MIC_ES_AMPLITUDE_PREF"
        val VID_ES_MIC_AMPLITUDE_PREF = "VID_ES_MICCHECK_AMPLITUDE_PREF"
    }
}