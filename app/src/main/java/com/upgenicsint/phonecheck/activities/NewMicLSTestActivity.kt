package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.Cabinet
import com.google.gson.Gson
import com.upgenicsint.phonecheck.activities.MicLSTestActivity.Companion.MICLS_SCREEN_TIME
import com.upgenicsint.phonecheck.barcode.BeepManager
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.media.AudioDataReceivedListener
import com.upgenicsint.phonecheck.media.RecordingThread
import com.upgenicsint.phonecheck.media.audio.calculators.AudioCalculator
import com.upgenicsint.phonecheck.misc.RetryTextFieldListener
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.misc.TextFieldListener
import com.upgenicsint.phonecheck.models.Amplitude
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AudioPlaybackTest
import com.upgenicsint.phonecheck.test.chip.NewMicESTest
import com.upgenicsint.phonecheck.test.chip.NewMicLSTest
import kotlinx.android.synthetic.main.audio_new_input_test_layout.view.*
import kotlinx.android.synthetic.main.activity_new_mic_lstest.*
import java.util.*
import kotlin.collections.ArrayList

class NewMicLSTestActivity : BaseAudioTestActivity<NewMicLSTest>() {

    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var micText: TextView? = null
    private var resultsView: ImageView? = null
    private val pref by lazy { Cabinet.open(context, R.string.newmic_ls) }
    /*private var resultsPref: SharedPreferences.Editor? = null*/
    private var isAutoStartRunning = false
    private var runNext = false //flag to stop auto start next test when user select a single testm
    private var earpiecePlay = false // handle test?.sub(Test.loudSpeakerTestKey)?.value resource
    private var isPlaying = false
    private var isBeepPassed: Boolean = false
    private var isRetryClicked: Boolean = false
    private var isMicClicked: Boolean = false
    private var progressMaxValue: Long = 0
    // new test Layout
    private var isMicPasClicked: Boolean = false
    private var isSpeakerTestFailed: Boolean = false
    private var isSpeakerTestPassed: Boolean = false
    private var isSpeakerPlayonFailed: Boolean = false
    private var isSpeakerPlayonPassed: Boolean = false
    private var isRetryingMic: Boolean = false
    private var isManual: Boolean = false
    private var isLSRetesting: Boolean = false
    private var countDownTimerCallStarted: CountDownTimer? = null // used for recording 5 sec
    private var countDownTimerCallStarted1: CountDownTimer? = null // used for screen overlay 3 sec
    private var mRecordingThread: RecordingThread? = null
    private var beepManager: BeepManager? = null
    private var audioCalculator: AudioCalculator? = null
    private var amplitude: Int = 0
    private var amplitudeMicList: MutableList<Amplitude> = ArrayList()
    private var aboveCounter = 0
    // get Duration from media player and put to text
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "AutoLS"
    private var tone1List: MutableList<Amplitude> = ArrayList()
    private var tone2List: MutableList<Amplitude> = ArrayList()
    private var tone3List: MutableList<Amplitude> = ArrayList()

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_new_mic_lstest)
        // setting Nav Bar adding test
        onCreateNav()

        Loader.TIME_VALUE = 0
        MICLS_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("Mic Loud Speaker Test")
        test = Loader.instance.getByClassType(NewMicLSTest::class.java)
        audioCalculator = AudioCalculator()
        /*Toast.makeText(this, "$AMPLITUDE_CHECKING_VID", Toast.LENGTH_SHORT).show()*/
        // adding views like Microphone VideoMicrophone
        val micContainer = micTestLayout1 as ViewGroup
        speakerStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.loudSpeakerTestKey)?.value
                ?: Test.INIT))
        showMicText(micContainer) // show text at runtime on Speaker fail.
        microphoneView = LayoutInflater.from(context).inflate(R.layout.audio_new_input_test_layout, null)
        micContainer.addView(microphoneView)
        val resultsFilterMap = test?.resultsFilterMap
        if (resultsFilterMap != null) {
            if (resultsFilterMap[Test.videoMicTestKey] == true) {
                videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.audio_new_input_test_layout, null)
                micContainer.addView(videoMicrophoneView)
            }
        }
        microphoneView?.let { microphoneView ->
            microphoneView.nameTextView.text = context.getString(R.string.microphone)
            microphoneView.imageView.setImageResource(R.drawable.microphone)
            microphoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.micTestKey)?.value
                    ?: Test.INIT))
            microphoneView.amplitudeTextView.text = "${pref.getInt(MIC_AMPLITUDE_PREF, 0)}"
        }
        videoMicrophoneView?.let { videoMicrophoneView ->
            videoMicrophoneView.nameTextView.text = context.getString(R.string.video_microphone)
            videoMicrophoneView.imageView.setImageResource(R.drawable.microphone)
            videoMicrophoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.videoMicTestKey)?.value
                    ?: Test.INIT))
            videoMicrophoneView.amplitudeTextView.text = "${pref.getInt(VID_MIC_AMPLITUDE_PREF, 0)}"
        }
        showResultView()
        if (BuildConfig.FLAVOR === Constants.AUDIO) handleAudioSimulation()
        microphoneView?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)
        // implement click listeners on Views
        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlay = false
                isPlaying = true
                isMicClicked = true
                isMicPasClicked = true
                isRetryingMic = false
                isRetryClicked = false
                isLSRetesting = false
                playBeep()
            }
        }
        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlay = false
                isPlaying = true
                isMicClicked = true
                isMicPasClicked = true
                isRetryClicked = false
                isLSRetesting = false
                videoMicTest()
            }
        }
        speakerLayout.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                isPlaying = true
                earpiecePlay = false
                isLSRetesting = true
                isRetryClicked = false
                isManual = false
                isMicClicked = false
                lsTapProgress.visibility = View.VISIBLE
                playBeepForSpeaker()
            }
        }
        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                resetTest()
                if (BuildConfig.FLAVOR != Constants.AUDIO) {
                    playBeep()
                }
            }
        }
        // auto start tests when activity created and Manual Prefs
        manual = getSharedPreferences(getString(R.string.manual), Context.MODE_PRIVATE).edit()
        val alreadyStarted = pref.getBoolean(AUDIO_AUTO_START_KEY, false)
        if (!alreadyStarted) {
            autoSuite.visibility = View.VISIBLE
            countDownTimerCallStarted1 = object : CountDownTimer(3000, 3000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    autoSuite.visibility = View.GONE
                }
            }
            countDownTimerCallStarted1?.start()
            startButton.performClick()
        }
    }

    private fun resetTest() {
        isAutoStartRunning = true
        runNext = true
        isPlaying = true
        earpiecePlay = false
        isMicClicked = false
        isMicPasClicked = false
        isRetryClicked = false
        isSpeakerTestFailed = false
        isSpeakerPlayonFailed = false
        isSpeakerPlayonPassed = false
        isSpeakerTestPassed = false
        isRetryingMic = false
        isManual = false
        isLSRetesting = false
        test?.sub(Test.micTestKey)?.value = Test.INIT
        test?.sub(Test.videoMicTestKey)?.value = Test.INIT
        micText!!.visibility = View.GONE
        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
        microphoneView?.statusImageView?.setImageResource(getImageForStatus(Test.INIT))
        videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(Test.INIT))
        speakerStatusImageView.setImageResource(getImageForStatus(Test.INIT))
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
            micText!!.setTextColor(ContextCompat.getColor(this@NewMicLSTestActivity, R.color.dark_black))
        }
        micContainer.addView(micText)
    }

    private fun showResultView() {
        microphoneView?.setOnLongClickListener {
            showQuestionAlert("Enter Pin", object : TextFieldListener {
                val pin = 5612
                override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                    try {
                        if (isTrue && pin == text.toInt()) {
                            startActivity(Intent(this@NewMicLSTestActivity,
                                    AmplitudeResultsDetail::class.java).putExtra(Constants.LS_MIC, true))
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this@NewMicLSTestActivity, "Please Input Correct Pin", Toast.LENGTH_SHORT).show()
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


    private val simulateTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            try {
                MIC_AMPLITUDE_CHECKING = s.toString().toDouble()
            } catch (e: Exception) {
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
                VID_AMPLITUDE_CHECKING = 1200.00
                e.printStackTrace()
            }
        }
    }

    private fun handleAudioSimulation() {
        microphoneView?.simulateMic?.visibility = View.VISIBLE
        videoMicrophoneView?.simulateMic?.visibility = View.VISIBLE
        simulateCheck?.visibility = View.VISIBLE
        videoMicrophoneView?.simulateMic?.addTextChangedListener(simulateTextWatcher1)
        microphoneView?.simulateMic?.addTextChangedListener(simulateTextWatcher)
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

    private fun playBeepForSpeaker() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        releaseAudioRecorder()
        progressMaxValue = 8000
        aboveCounter = 0
        initCountdownTimer()
        Thread(Runnable {
            try {
                Thread.sleep(700)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
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
            RecordingThread.isMic = true
            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()
            mRecordingThread = RecordingThread(AudioDataReceivedListener { data ->
                if (audioCalculator != null && mRecordingThread != null && data != null && data.isNotEmpty()) {
                    try {
                        audioCalculator!!.setBytes(RecordingThread.short2byte(data))
                        amplitude = audioCalculator!!.amplitude
                    } catch (nullp: NullPointerException) {
                        nullp.printStackTrace()
                    }
                }
            })
            // start recording for 8 sec
            countDownTimerCallStarted?.start()
            if (!mRecordingThread!!.recording()) mRecordingThread!!.startRecording()
        }).start()
    }

    /**
     * update duration
     */
    /*private val updateDuration: Runnable? = object : Runnable {
        override fun run() {
            if (mediaPlayer != null) {
                val duration = mediaPlayer!!.currentPosition
                Logy.d(TAG, "Current Duration is: " + duration.toString())
                handlerDuration.postDelayed(this, 10)
                if (duration >= 1200 || duration <= 2000) {

                }
                if (duration >= 2500 || duration <= 4000) {

                }
                if (duration >= 5300 || duration <= 6500) {

                }
            }
        }
    }*/
    /**
     * update duration closed
     */

    private fun playBeep() {
        if (test?.sub(Test.micTestKey)?.value == Test.PASS) {
            if (isMicPasClicked) {
                test?.sub(Test.micTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                playBeep()
                return
            }
            videoMicTest()
            return
        }
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        if (isMicClicked) {
            progressMaxValue = 4000
            microphoneView?.micProgressBar!!.max = 4000
        } else {
            progressMaxValue = 8000
            microphoneView?.micProgressBar!!.max = 8000
        }

        releaseAudioRecorder()
        // used to enable/disable visualizer with the help of CountDownTimer.
        initCountdownTimer()
        Thread(Runnable {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                microphoneTest()
            }
            RecordingThread.isMic = true
            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.setTone(isMicClicked)
            beepManager!!.checkSpeaker(isSpeakerTestFailed)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()
            /***
             * extra use it when required. right now no usage
             */
            mediaPlayer = beepManager!!.mediaPlayer
        }).start()
    }

    private fun microphoneTest() {
        mRecordingThread = RecordingThread(AudioDataReceivedListener { data ->
            if (audioCalculator != null && mRecordingThread != null && data != null && data.isNotEmpty() && data.size > 0) {
                try {
                    microphoneView?.visualizer!!.samples = data
                    audioCalculator!!.setBytes(RecordingThread.short2byte(data))
                    amplitude = audioCalculator!!.amplitude
                } catch (nullp: NullPointerException) {
                    nullp.printStackTrace()
                }
            }
        })
        // start recording for 8 sec
        countDownTimerCallStarted?.start()
        if (!mRecordingThread!!.recording()) mRecordingThread!!.startRecording()
    }

    private fun videoMicTest() {
        if (test?.sub(Test.videoMicTestKey)?.value == Test.PASS) {
            if (isMicPasClicked) {
                test?.sub(Test.videoMicTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                videoMicTest()
            }
            TEST_LOCK = false
            isBeepPassed = false
            resetAutoStart()
            return
        }
        if (test?.sub(Test.videoMicTestKey)?.value == Test.FAILED && runNext) {
            if (!isMicPasClicked && !isRetryClicked) {
                micText!!.visibility = View.VISIBLE
            }
            isSpeakerTestFailed = true
            isSpeakerTestPassed = false
            if (isRetryClicked) {
                videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
            } else {
                videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
            }
        }
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        if (isMicClicked) {
            progressMaxValue = 4000
            microphoneView?.micProgressBar!!.max = 4000
        } else {
            progressMaxValue = 4000
            microphoneView?.micProgressBar!!.max = 4000
        }
        releaseAudioRecorder()
        // used to enable/disable visualizer with the help of CountDownTimer.
        initCountdownTimer()
        Thread(Runnable {
            try {
                Thread.sleep(700)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            RecordingThread.isMic = false
            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.setTone(isMicClicked)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()
            runOnUiThread {
                mRecordingThread = RecordingThread(AudioDataReceivedListener { data ->
                    if (audioCalculator != null && mRecordingThread != null && data != null && data.isNotEmpty()) {
                        try {
                            videoMicrophoneView?.visualizer!!.samples = data
                            audioCalculator!!.setBytes(RecordingThread.short2byte(data))
                            amplitude = audioCalculator!!.getAmplitude()
                        } catch (nullp: NullPointerException) {
                            nullp.printStackTrace()
                        }
                    }
                })
                // start recording for 4 sec
                countDownTimerCallStarted?.start()
                if (!mRecordingThread!!.recording()) mRecordingThread!!.startRecording()
            }
        }).start()
    }

    private var isRetrying: Boolean = false

    private fun retrySpeakerTest() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        releaseAudioRecorder()
        if (test?.sub(Test.videoMicTestKey)?.value == Test.PASS && test?.sub(Test.micTestKey)?.value == Test.PASS && test?.sub(Test.loudSpeakerTestKey)?.value != Test.PASS) {
            lsTapProgress!!.visibility = View.VISIBLE
        }
        progressMaxValue = 8000
        aboveCounter = 0
        isMicClicked = false
        isManual = false
        initCountdownTimer()
        Thread(Runnable {
            try {
                Thread.sleep(700)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
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
            RecordingThread.isMic = true
            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()
            mRecordingThread = RecordingThread(AudioDataReceivedListener { data ->
                if (audioCalculator != null && mRecordingThread != null && data != null && data.isNotEmpty()) {
                    try {
                        if (test?.sub(Test.micTestKey)?.value == Test.FAILED && !isLSRetesting) {
                            microphoneView?.visualizer!!.samples = data
                        }
                        if (test?.sub(Test.micTestKey)?.value == Test.FAILED && isLSRetesting && isRetryClicked) {
                            microphoneView?.visualizer!!.samples = data
                        }
                        audioCalculator!!.setBytes(RecordingThread.short2byte(data))
                        amplitude = audioCalculator!!.amplitude
                    } catch (nullp: NullPointerException) {
                        nullp.printStackTrace()
                    }
                }
            })
            // start recording for 8 sec
            countDownTimerCallStarted?.start()
            if (!mRecordingThread!!.recording()) mRecordingThread!!.startRecording()
        }).start()
    }

    @SuppressLint("CommitPrefEdits")
    private fun showSpeakerPopup() {
        val title = getString(R.string.speaker_popup)
        TEST_LOCK = false
        if (aboveCounter >= 3 && !simulateCheck.isChecked) {
            if (isSpeakerTestPassed) {
                test?.sub(Test.loudSpeakerTestKey)?.value = Test.PASS
                speakerStatusImageView.setImageResource(getImageForStatus(Test.PASS))
                TEST_LOCK = false
                isBeepPassed = false
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
            isBeepPassed = false
            test?.sub(Test.loudSpeakerTestKey)?.value = Test.PASS
            speakerStatusImageView.setImageResource(getImageForStatus(Test.PASS))
            releaseAudioRecorder()
            Handler().postDelayed({ resetAutoStart() }, 3000)
        } else {
            showQuestionAlert(title, getString(R.string.did_hear_sound_speaker), object : RetryTextFieldListener {
                override fun onClick(dialog: DialogInterface, type: RetryTextFieldListener.ButtonType) {
                    val isTrue = type == RetryTextFieldListener.ButtonType.RIGHT
                    val isAutoLS = type == RetryTextFieldListener.ButtonType.NEUTRAL
                    val value = if (isAutoLS) Test.INIT else if (isTrue) Test.PASS else Test.FAILED
                    if (value == Test.INIT) {
                        TEST_LOCK = false
                        handleUserSelection(value)
                        isBeepPassed = false
                        releaseAudioRecorder()
                        resetAutoStart()
                        Handler().postDelayed({
                            isRetryClicked = true
                            isRetryingMic = false
                            when {
                                test?.sub(Test.micTestKey)?.value == Test.FAILED -> {
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
                                test?.sub(Test.videoMicTestKey)?.value == Test.FAILED -> videoMicTest()
                                else -> {
                                    manual?.putBoolean(Constants.MANUAL_LS, false)?.apply()
                                    isRetryingMic = true
                                    retrySpeakerTest()
                                }
                            }
                        }, 300)
                        return
                    }
                    if (value == Test.PASS) {
                        manual?.putBoolean(Constants.MANUAL_LS, true)?.apply()
                        Loader.RESULT.put(Test.loudSpeakerTestKey + "-M", value)
                        if (isSpeakerTestPassed) {
                            test?.sub(Test.loudSpeakerTestKey)?.value = value
                            speakerStatusImageView.setImageResource(getImageForStatus(value))
                            TEST_LOCK = false
                            dialog.dismiss()
                            isBeepPassed = false
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
                        isBeepPassed = false
                    } else {
                        removeMicPlayBackTest()
                        manual?.putBoolean(Constants.MANUAL_LS, true)?.apply()
                        Loader.RESULT.put(Test.loudSpeakerTestKey + "-M", value)
                        if (isSpeakerPlayonFailed) {
                            test?.sub(Test.loudSpeakerTestKey)?.value = value
                            speakerStatusImageView.setImageResource(getImageForStatus(value))
                            TEST_LOCK = false
                            dialog.dismiss()
                            isBeepPassed = false
                            releaseAudioRecorder()
                            resetAutoStart()
                        }
                        isRetryingMic = false
                        isRetryClicked = false
                        isManual = true
                        isLSRetesting = false
                        if (test?.sub(Test.micTestKey)?.value == Test.PASS) {
                            test?.sub(Test.loudSpeakerTestKey)?.value = value
                            isBeepPassed = false
                            TEST_LOCK = false
                            speakerStatusImageView.setImageResource(getImageForStatus(value))
                            dialog.dismiss()
                            resetAutoStart()
                            videoMicTest()
                            return
                        }
                        runNext = true
                        if (runNext) {
                            TEST_LOCK = false
                            isBeepPassed = true
                            isMicClicked = true
                            playBeep()
                        }
                        isSpeakerTestFailed = true
                        isSpeakerTestPassed = false
                        micText!!.visibility = View.VISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
                    }
                    if (runNext) {
                        test?.sub(Test.loudSpeakerTestKey)?.value = value
                        speakerStatusImageView.setImageResource(getImageForStatus(value))
                        dialog.dismiss()
                        resetAutoStart()
                    } else {
                        test?.sub(Test.loudSpeakerTestKey)?.value = value
                        speakerStatusImageView.setImageResource(getImageForStatus(value))
                        dialog.dismiss()
                        releaseAudioRecorder()
                        resetAutoStart()
                    }
                }
            })
        }
    }

    /**
     * Algorithm to automatically test test?.sub(Test.loudSpeakerTestKey)?.value
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


    private fun initCountdownTimer() {
        countDownTimerCallStarted = object : CountDownTimer(progressMaxValue, 100) {
            var amplitude1 = 0
            var progress = 0
            var duration = 0L
            override fun onTick(interval: Long) {
                if (RecordingThread.isMic) {
                    progress += 105
                    microphoneView?.micProgressBar?.progress = progress
                    /**
                     * what was done in a big list now divided into 3 lists
                     */
                    duration += 100L
                    Logy.d("Results", "Duration: $duration , Amplitude: $amplitude")
                    if (duration in 1400..2500) {
                        if (amplitude >= 1500) {
                            tone1List.add(Amplitude(amplitude, true))
                        } else {
                            tone1List.add(Amplitude(amplitude, false))
                        }
                    }
                    if (duration in 3300..4500) {
                        if (amplitude >= 1500) {
                            tone2List.add(Amplitude(amplitude, true))
                        } else {
                            tone2List.add(Amplitude(amplitude, false))
                        }
                    }
                    if (duration in 5000..7000) {
                        if (amplitude >= 1500) {
                            tone3List.add(Amplitude(amplitude, true))
                        } else {
                            tone3List.add(Amplitude(amplitude, false))
                        }
                    }
                    if (isLSRetesting && !isManual) {
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                    }
                } else {
                    if (isManual && !isMicClicked) {
                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                        videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
                    }
                    progress += 105
                    /*duration += 100L
                    if (amplitude1 >= AMPLITUDE_CHECKING && duration >= 2000L) {
                        releaseMicBeep()
                        countDownTimerCallStarted!!.onFinish()
                    }*/
                    videoMicrophoneView?.micProgressBar?.progress = progress
                }
                if (amplitude > amplitude1) {
                    amplitude1 = amplitude
                }
            }

            override fun onFinish() {
                TEST_LOCK = false
                val value: Int
                if (RecordingThread.isMic) {
                    if (BuildConfig.FLAVOR === Constants.AUDIO) {
                        value = if (amplitude1 >= MIC_AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    } else {
                        value = if (amplitude1 >= AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    }
                    setSpeakerResult()
                    amplitudeMicList.addAll(tone1List)
                    amplitudeMicList.addAll(tone2List)
                    amplitudeMicList.addAll(tone3List)
                    releaseAudioRecorder()
                    if (!isRetryingMic && !isLSRetesting) {
                        isRetryingMic = true
                        test?.sub(Test.micTestKey)?.value = value
                        microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        microphoneView?.amplitudeTextView?.text = amplitude1.toString()
                        pref.add(MIC_AMPLITUDE_PREF, amplitude1).save()
                    }
                    if (isLSRetesting && isRetryClicked) {
                        test?.sub(Test.micTestKey)?.value = value
                        microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        microphoneView?.amplitudeTextView?.text = amplitude1.toString()
                        pref.add(MIC_AMPLITUDE_PREF, amplitude1).save()
                    }
                    if (!isMicClicked) {
                        if (aboveCounter >= 3 && !simulateCheck.isChecked) {
                            test?.sub(Test.loudSpeakerTestKey)?.value = Test.PASS
                            Loader.RESULT.put(Test.loudSpeakerTestKey, Test.PASS)
                            manual?.putBoolean(Constants.MANUAL_LS, false)?.apply()
                            speakerStatusImageView.setImageResource(getImageForStatus(Test.PASS))
                        } else {

                            //removeMicPlayBackTest()

                            test?.sub(Test.loudSpeakerTestKey)?.value = Test.FAILED
                            Loader.RESULT.put(Test.loudSpeakerTestKey, Test.FAILED)
                            manual?.putBoolean(Constants.MANUAL_LS, false)?.apply()
                            speakerStatusImageView.setImageResource(getImageForStatus(Test.FAILED))
                            if (isLSRetesting && !isRetryClicked) {
                                showSpeakerPopup()
                            } else if (isRetryClicked && test?.sub(Test.videoMicTestKey)?.value == Test.PASS) {
                                showSpeakerPopup()
                            } /*else {
                                removeMicPlayBackTest()
                            }*/
                        }
                    }
                    amplitude1 = 0
                    progress = 100
                    duration = 0L
                    microphoneView?.micProgressBar?.progress = progress
                    val resultsPref: SharedPreferences.Editor = getSharedPreferences(getString(R.string.resultsPref), Context.MODE_PRIVATE).edit()
                    resultsPref.putString(getString(R.string.micresultsList), Gson().toJson(amplitudeMicList))
                    resultsPref.apply()
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
                    if (runNext || (isLSRetesting && isRetryClicked)) {
                        if (videoMicrophoneView != null) {
                            if (isSpeakerTestFailed) {
                                if (test?.sub(Test.videoMicTestKey)?.value == Test.PASS) {
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
                                if (test?.sub(Test.micTestKey)?.value == Test.PASS) {
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
                    if (isManual && test?.sub(Test.videoMicTestKey)?.value == Test.PASS) {
                        micText!!.visibility = View.GONE
                        microphoneView?.micProgressBar!!.setBackgroundResource(R.drawable.selector_row)
                        microphoneView?.micProgressBar!!.visibility = View.INVISIBLE
                    }
                } else {
                    if (BuildConfig.FLAVOR === Constants.AUDIO) {
                        value = if (amplitude1 >= VID_AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    } else {
                        value = if (amplitude1 >= AMPLITUDE_CHECKING_VID) Test.PASS else Test.FAILED
                    }
                    releaseAudioRecorder()
                    test?.sub(Test.videoMicTestKey)?.value = value
                    videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    videoMicrophoneView?.amplitudeTextView?.text = amplitude1.toString()
                    pref.add(VID_MIC_AMPLITUDE_PREF, amplitude1).save()
                    amplitude1 = 0
                    progress = 0
                    videoMicrophoneView?.micProgressBar?.progress = progress
                    isBeepPassed = false
                    resetAutoStart()
                    if (runNext && !isManual) {
                        showSpeakerPopup()
                    }
                    if (isRetryClicked) {
                        showSpeakerPopup()
                    }
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
                    if (isManual) {
                        micText!!.visibility = View.GONE
                    }
                }
                lsTapProgress.visibility = View.GONE
            }
        }
    }

    override fun removeMicPlayBackTest() {
        super.removeMicPlayBackTest()
        /*val autoes = Loader.instance.getByClassType(NewMicESTest::class.java)
        if (autoes != null && autoes.sub(Test.earphoneTestKey)?.value == Test.FAILED && (aboveCounter < 3 || BuildConfig.FLAVOR === Constants.AUDIO)) {
            val mpt = Loader.instance.getByClassType(AudioPlaybackTest::class.java)
            if (mpt != null) {
                Loader.instance.testList.remove(mpt)
            }
        }*/
        if (aboveCounter < 3 || BuildConfig.FLAVOR === Constants.AUDIO) {
            val mpt = Loader.instance.getByClassType(AudioPlaybackTest::class.java)
            if (mpt != null) {
                Loader.instance.testList.remove(mpt)
            }
        }
    }

    private fun releaseAudioRecorder() {
        if (beepManager != null) {
            beepManager!!.close()
            beepManager = null
        }
        if (mRecordingThread != null) {
            mRecordingThread!!.stopRecording()
            RecordingThread.mShouldContinue = false
            mRecordingThread = null
        }
        if (countDownTimerCallStarted != null) {
            countDownTimerCallStarted!!.cancel()
            countDownTimerCallStarted = null
        }
    }

    private fun releaseMicBeep() {
        if (beepManager != null) {
            beepManager!!.close()
            beepManager = null
        }
    }

    private fun resetAutoStart() {
        isAutoStartRunning = isBeepPassed
        pref.add(AUDIO_AUTO_START_KEY, true)
        pref.save()
        testWatcher()
    }

    private fun handleUserSelection(value: Int) {
        test?.sub(Test.loudSpeakerTestKey)?.value = value
        if (value == Test.PASS) {
            AudioInputTestActivity.isSpeakerWorking = true
        }
        speakerStatusImageView.setImageResource(getImageForStatus(value))
    }

    override fun onPause() {
        lsTapProgress.visibility = View.GONE
        releaseAudioRecorder()
        RecordingThread.isMic = false
        RecordingThread.mShouldContinue = false
        TEST_LOCK = false
        isBeepPassed = false
        resetAutoStart()
        removeTextListener()
        if (countDownTimerCallStarted1 != null) {
            countDownTimerCallStarted1?.cancel()
            countDownTimerCallStarted1 = null
            autoSuite.visibility = View.GONE
        }
        super.onPause()
    }

    override fun onStop() {
        releaseAudioRecorder()
        RecordingThread.isMic = false
        RecordingThread.mShouldContinue = false
        super.onStop()
    }

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }

    override fun closeTimerTest() {
        super.closeTimerTest()
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                MICLS_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_micls), -1)] =
                            RecordTest(context.getString(R.string.report_micls_test), MICLS_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("MIC Loud Speaker", "${MICLS_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {
            ignored.printStackTrace()
        }
    }

    override fun onDestroy() {
        releaseAudioRecorder()
        RecordingThread.isMic = false
        RecordingThread.mShouldContinue = false
        super.onDestroy()
    }

    companion object {
        val REQ = 1786
        private var AMPLITUDE_CHECKING = when {
            Build.MODEL.containsIgnoreCase("SM-N900") -> 2000.00
            else -> 1500.00
        }
        private var AMPLITUDE_CHECKING_VID = when {
            Build.MODEL.containsIgnoreCase("SM-N900") -> 2000.00
            Devices.chooseThreshold() -> 1000.00
            else -> 1500.00
        }

        private var MIC_AMPLITUDE_CHECKING = 1200.00
        private var VID_AMPLITUDE_CHECKING = 1200.00
        val AUDIO_AUTO_START_KEY = "autoMicCheckStart"
        val MIC_AMPLITUDE_PREF = "MICCHECK_AMPLITUDE_PREF"
        val VID_MIC_AMPLITUDE_PREF = "VID_MICCHECK_AMPLITUDE_PREF"
    }

}