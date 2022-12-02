package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.Cabinet
import com.tyorikan.voicerecordingvisualizer.RecordingSampler
import com.upgenicsint.phonecheck.*
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.activities.MicLSTestActivity.Companion.audioReports
import com.upgenicsint.phonecheck.broadcastreceiver.HeadSetPlugStatusReceiver
import com.upgenicsint.phonecheck.misc.*
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.MicESTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import github.nisrulz.zentone.ZenTone
import kotlinx.android.synthetic.main.activity_mic_estest.*
import kotlinx.android.synthetic.main.audio_input_test_layout.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.jvm.internal.Intrinsics

class MicESTestActivity : BaseAudioTestActivity<MicESTest>(), HeadSetPlugCallBack {

    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var recordingSampler: RecordingSampler? = null
    private var isAutoStartRunning = false
    private var micText: TextView? = null
    private var isBeepfromHeadset: Boolean = false
    private var runNext = false //flag to stop auto start next test when user select a single test
    private var earpiecePlay = false
    private var muteEarpieceSound: Boolean = false
    private var saveHeadsetObject: SharedPreferences.Editor? = null
    var isPlaying = false
    private var isSamsung = true
    //    public var beepManager: BeepManager? = null
    //    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null

    private fun getVolumeForMode(mode: Int): Int {
        val volume: Int
        if (BuildConfig.DEBUG) {
            volume = (audioManager!!.getStreamMaxVolume(mode).toDouble() * 0.7f).toInt()
        } else {
            val audioManager = this.audioManager
            if (this.audioManager == null) {
                Intrinsics.throwNpe()
            }
            volume = (audioManager!!.getStreamMaxVolume(mode).toDouble() * 1f).toInt()
        }

        return volume
    }

    private val pref by lazy { Cabinet.open(context, R.string.mic_es) }

    private var headSetReceiver: HeadSetPlugStatusReceiver? = null

    private var countDownTimerCallStarted: CountDownTimer? = null

    // audio analytics
    private var isComingDirectly = false
    private var thresholdList: MutableList<Int> = ArrayList()
    private var micCount = 0
    private var micCountHits = 0
    private var vidmicCountHits = 0
    private var vidmicCount = 0

    private var codePlayed: String? = ""
    private var codeListened: String? = ""
    private var resultCodes: String? = ""
    private var micCodeThreshold: String? = ""
    private var vidmicCodeThreshold: String = ""

    private var micbeepTone: String? = ""
    private var vidmicbeepTone: String? = ""
    private var micbeepPlayed: String? = ""
    private var vidmicbeepPlayed: String? = ""
    private var micthresholdBeepHits: String? = ""
    private var vidmicthresholdBeepHits: String? = ""
    private var micresultBeep: String? = ""
    private var vidmicresultBeep: String? = ""
    private var miclowThresholdBeep: String? = ""
    private var michighThresholdBeep: String? = ""
    private var vidmiclowThresholdBeep: String? = ""
    private var vidmichighThresholdBeep: String? = ""

    // new testLayout
    private var isSpeakerTestFailed: Boolean = false
    private var isSpeakerTestPassed: Boolean = false
    private var isSpeakerPlayonFailed: Boolean = false
    private var isSpeakerPlayonPassed: Boolean = false
    private var isMicPasClicked: Boolean = false
    private var isBeepRunning: Boolean = false
    private var checkAfterFailedMic: Boolean = false
    private var headsetLeftCode = -1
    private var headsetRightCode = -1
    private var isPluggedOut: Boolean = false
    private var isHeadsetDialogClicked: Boolean = true
    private var isManualTesting: Boolean = false
    private var isDoneClick = false

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
                //isBeepRunning = true
                if (isSamsung && !isAutoAudioEnabled && isAutoStartRunning && !isHeadsetDialogClicked) {
                    playBeep()
                }
                if (isBeepfromHeadset) {
                    resetAutoStart()
                }
            }

            /*else {
                muteEarpieceSound = false
    //            runNext = true
                if (isBeepfromHeadset && !muteEarpieceSound) {
                    isPlaying = true
                    if (runNext) {
                        resetAutoStart()
                    } else {
                        runNext = true
                        earpiecePlay = true
                        playBeep()
    //                    microphoneTest()
                    }
                    resetAutoStart()
                }
            }
            else {
                if (runNext) {
                    playBeepForEarSpeaker()
                }
            }*/
        }
    }

    private fun writeAudioReportJson() {

        try {
            if (audioReports.exists() && audioReports.length() > 0) {
                // add audioInput in it.
                val jsonObj: JSONObject
                try {

                    jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                    val audioReport = jsonObj.getJSONArray("AudioReport")
                    for (i in 0 until audioReport.length()) {
                        try {
                            val jsonArrary = audioReport.getJSONObject(i).getJSONArray("audioOutput")
                            if (jsonArrary != null && jsonArrary.length() > 0) {
                                return
                            }
                        } catch (json: JSONException) {
                            json.printStackTrace()
                        }
                    }

                    val jsonObject = JSONObject()
                    audioReport.put(jsonObject)
                    val audioOutput = JSONArray()
                    jsonObject.put("audioOutput", audioOutput)

                    val jsonObject2 = JSONObject()
                    audioOutput.put(jsonObject2)

                    val mic = JSONArray()
                    jsonObject2.put("mic", mic)

                    val jsonObject3 = JSONObject()
                    jsonObject3.put("title", "Bottom Mic")

                    val codeattempts = JSONArray()
                    jsonObject3.put("codeAttempts", codeattempts)

                    val jsonObject4 = JSONObject()
                    codeattempts.put(jsonObject4)
                    jsonObject4.put("codePlayed", codePlayed)
                    jsonObject4.put("codeListened", codeListened)
                    jsonObject4.put("result", resultCodes)
                    jsonObject4.put("micThresholdReached", micCodeThreshold)

                    val beepAttempts = JSONArray()
                    jsonObject3.put("beepAttempts", beepAttempts)

                    val jsonObject5 = JSONObject()
                    beepAttempts.put(jsonObject5)
                    jsonObject5.put("toneFrequency", "1000")
                    jsonObject5.put("beepsPlayed", micbeepPlayed)
                    jsonObject5.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                    jsonObject5.put("thresholdReached", micthresholdBeepHits)
                    jsonObject5.put("result", micresultBeep)
                    jsonObject5.put("lowThreshold", miclowThresholdBeep)
                    jsonObject5.put("highThreshold", michighThresholdBeep)


                    // Video Mic Result
                    val jsonObject6 = JSONObject()
                    jsonObject6.put("title", "Rear Mic")

                    val codeattempts1 = JSONArray()
                    jsonObject6.put("codeAttempts", codeattempts1)

                    val jsonObject7 = JSONObject()
                    codeattempts1.put(jsonObject7)
                    jsonObject7.put("codePlayed", codePlayed)
                    jsonObject7.put("codeListened", codeListened)
                    jsonObject7.put("result", resultCodes)
                    jsonObject7.put("micThresholdReached", micCodeThreshold)

                    val beepAttempts1 = JSONArray()
                    jsonObject6.put("beepAttempts", beepAttempts1)

                    val jsonObject8 = JSONObject()
                    beepAttempts1.put(jsonObject8)
                    jsonObject8.put("toneFrequency", "1000")
                    jsonObject8.put("beepsPlayed", vidmicbeepPlayed)
                    jsonObject8.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                    jsonObject8.put("thresholdReached", vidmicthresholdBeepHits)
                    jsonObject8.put("result", vidmicresultBeep)
                    jsonObject8.put("lowThreshold", vidmiclowThresholdBeep)
                    jsonObject8.put("highThreshold", vidmichighThresholdBeep)

                    mic.put(jsonObject3)
                    mic.put(jsonObject6)

                    WriteObjectFile.getInstance().writeObject(jsonObj.toString(), "/AudioReports.json")


                    /* for (i in 0 until audioReport.length()) {
                         val jsonObject1 = audioReport.getJSONObject(i)
                         val audioOutput = jsonObject1.getJSONArray("audioOutput")
                         for (j in 0 until audioOutput.length()) {
                             val jsonObject2 = audioOutput.getJSONObject(j)
                             val mic = jsonObject2.getJSONArray("mic")
 //                                for (k in 0 until mic.length())
                             val jsonObject3 = mic.getJSONObject(0)

                             val jsonObjectBeep = JSONObject()
                             val beepAttempts: JSONArray = jsonObject3.getJSONArray("beepAttempts")
                             jsonObjectBeep.put("toneFrequency", "1000")
                             jsonObjectBeep.put("beepsPlayed", micbeepPlayed)
                             jsonObjectBeep.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                             jsonObjectBeep.put("thresholdReached", micthresholdBeepHits)
                             jsonObjectBeep.put("result", micresultBeep)
                             jsonObjectBeep.put("lowThreshold", miclowThresholdBeep)
                             jsonObjectBeep.put("highThreshold", michighThresholdBeep)
                             beepAttempts.put(jsonObjectBeep)

                             WriteObjectFile.getInstance().writeObject(jsonObj.toString(), "/AudioReports.json")
                         }
                     }*/

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                val jsonObject = JSONObject()
                // for (int i = 0; i < audioReport.length(); i++)
                val audioReport = JSONArray()
                jsonObject.put("AudioReport", audioReport)

                val jsonObject1 = JSONObject()
                audioReport.put(jsonObject1)

                val audioOutput = JSONArray()
                jsonObject1.put("audioOutput", audioOutput)

                val jsonObject2 = JSONObject()
                audioOutput.put(jsonObject2)

                val mic = JSONArray()
                jsonObject2.put("mic", mic)

                val jsonObject3 = JSONObject()
                jsonObject3.put("title", "Bottom Mic")

                val codeattempts = JSONArray()
                jsonObject3.put("codeAttempts", codeattempts)

                val jsonObject4 = JSONObject()
                codeattempts.put(jsonObject4)
                jsonObject4.put("codePlayed", codePlayed)
                jsonObject4.put("codeListened", codeListened)
                jsonObject4.put("result", resultCodes)
                jsonObject4.put("micThresholdReached", micCodeThreshold)

                val beepAttempts = JSONArray()
                jsonObject3.put("beepAttempts", beepAttempts)

                val jsonObject5 = JSONObject()
                beepAttempts.put(jsonObject5)
                jsonObject5.put("toneFrequency", "1000")
                jsonObject5.put("beepsPlayed", micbeepPlayed)
                jsonObject5.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                jsonObject5.put("thresholdReached", micthresholdBeepHits)
                jsonObject5.put("result", micresultBeep)
                jsonObject5.put("lowThreshold", miclowThresholdBeep)
                jsonObject5.put("highThreshold", michighThresholdBeep)


                // Video Mic Result
                val jsonObject6 = JSONObject()
                jsonObject6.put("title", "Rear Mic")

                val codeattempts1 = JSONArray()
                jsonObject6.put("codeAttempts", codeattempts1)

                val jsonObject7 = JSONObject()
                codeattempts1.put(jsonObject7)
                jsonObject7.put("codePlayed", codePlayed)
                jsonObject7.put("codeListened", codeListened)
                jsonObject7.put("result", resultCodes)
                jsonObject7.put("micThresholdReached", micCodeThreshold)

                val beepAttempts1 = JSONArray()
                jsonObject6.put("beepAttempts", beepAttempts1)

                val jsonObject8 = JSONObject()
                beepAttempts1.put(jsonObject8)
                jsonObject8.put("toneFrequency", "1000")
                jsonObject8.put("beepsPlayed", vidmicbeepPlayed)
                jsonObject8.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                jsonObject8.put("thresholdReached", vidmicthresholdBeepHits)
                jsonObject8.put("result", vidmicresultBeep)
                jsonObject8.put("lowThreshold", vidmiclowThresholdBeep)
                jsonObject8.put("highThreshold", vidmichighThresholdBeep)

                mic.put(jsonObject3)
                mic.put(jsonObject6)


                WriteObjectFile.getInstance().writeObject(jsonObject.toString(), "/AudioReports.json")

            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }


    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_mic_estest)

        // reset and done button declaration and set title of activity
        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("Mic Ear Speaker Test")

        Loader.TIME_VALUE = 0
        MICES_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        saveHeadsetObject = getSharedPreferences(getString(R.string.report_headset), Context.MODE_PRIVATE).edit()

        // tts = TextToSpeech(applicationContext, this)
        test = Loader.instance.getByClassType(MicESTest::class.java)

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

        if (test?.hasSubTest(Test.earphoneTestKey) == false) {
            earpieceLayout.visibility = View.GONE
        } else {
            earpieceStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.earphoneTestKey)?.value
                    ?: Test.INIT))
            earpieceLayout.setOnClickListener {
                if (!isAutoStartRunning) {
                    earpiecePlay = true
                    isPlaying = true
                    runNext = false
                    isAutoStartRunning = true
                    isComingDirectly = !isAutoAudioEnabled
                    audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)

                    /*if (!muteEarpieceSound && !isBeepfromHeadset) {
                        playBeep()
                    } else {
                        playBeepForEarSpeaker()
                    }*/
                    /*if (isBeepfromHeadset) {
                        playBeep()
                    }*/

                    playBeepForEarSpeaker()
                }
//                playSoundForSpeaker()
            }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
        audioManager!!.isSpeakerphoneOn = false

        headsetStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.headsetLeftKey)?.value
                ?: Test.INIT))

        headsetLayout.setOnClickListener {
            headsetLeftCode = generateNewNumber(true)
            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_HEADSET_LEFT
            intent.putExtra(TTSService.RANDOM_NUMBER, headsetLeftCode.toString())
            startService(intent)
            runNext = false
            isAutoStartRunning = true
        }

        if (!reportHeadset && test?.hasSubTest(Test.headsetLeftKey) == true && test?.hasSubTest(Test.headsetRightKey) == true) {
            headsetLayout.visibility = View.VISIBLE
        } else {
            headsetLayout.visibility = View.GONE
        }

        headsetLayout.isClickable = false
        headsetLayout.isEnabled = false

        val micContainer = micTestLayout as ViewGroup

        if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
            isSamsung = true
            // show text at runtime on Speaker fail.
            showMicText(micContainer)
            microphoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
            micContainer.addView(microphoneView)

            val resultsFilterMap = test?.resultsFilterMap

            if (resultsFilterMap != null) {
                if (resultsFilterMap[Test.videoESMicTestKey] == true) {
                    videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
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

        } else {
            isSamsung = false
            micTestLayoutScroll.visibility = View.GONE
            micContainer.visibility = View.GONE
            startButton.visibility = View.GONE
        }

        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlay = true
                isPlaying = true
                isMicPasClicked = true
                playBeep()
                audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
//                playSound()
            }
        }

        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                isPlaying = true
                earpiecePlay = true
                isMicPasClicked = true
                videoMicTest()
                audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
            }
        }

        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                isAutoStartRunning = true
                runNext = true
                earpiecePlay = true
                isPlaying = true
                isMicPasClicked = false
                isManualTesting = false
                checkAfterFailedMic = false
                isBeepRunning = false
                isDoneClick = false
                if (isAutoAudioEnabled) {
                    isSpeakerPlayonFailed = false
                    isSpeakerPlayonPassed = false
                    isSpeakerTestPassed = false
                    isSpeakerTestFailed = false
                    isComingDirectly = false
                    test?.sub(Test.micESTestKey)?.value = Test.INIT
                    test?.sub(Test.videoESMicTestKey)?.value = Test.INIT
                    test?.sub(Test.headsetPortKey)?.value = Test.INIT
                    Handler().postDelayed(Runnable {
                        if (!muteEarpieceSound) {
                            playBeepForEarSpeaker()
                            audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
                        }
                    }, 1000)
                } else {
                    isSpeakerPlayonFailed = false
                    isSpeakerPlayonPassed = false
                    isSpeakerTestPassed = false
                    isSpeakerTestFailed = false
                    isComingDirectly = false
                    test?.sub(Test.micESTestKey)?.value = Test.INIT
                    test?.sub(Test.videoESMicTestKey)?.value = Test.INIT
                    test?.sub(Test.headsetPortKey)?.value = Test.INIT
                    Handler().postDelayed(Runnable {
                        if (!muteEarpieceSound) {
                            if (!isBeepRunning) {
                                playBeep()
                            }
                        }
                    }, 1000)
                }
//                playSound()
            }
        }

        /*Thread(Runnable {
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                if (isSamsung && !muteEarpieceSound) {
                    val alreadyStarted = pref.getBoolean(AUDIO_AUTO_ES_START_KEY, false)
                    if (!alreadyStarted && !Loader.instance.isAutoAudioEnabled) {
                        startButton.performClick()
                    }
                }
                *//*else {
                    if (isSamsung && muteEarpieceSound) {
                        val alreadyStarted = pref.getBoolean(AUDIO_AUTO_ES_START_KEY, false)
                        if (!alreadyStarted) {
                            microphoneView?.performClick()
                        }
                    }
                }*//*
            }
        }).start()*/

        if (isSamsung && !muteEarpieceSound) {
            val alreadyStarted = pref.getBoolean(AUDIO_AUTO_ES_START_KEY, false)
            if (!alreadyStarted && !Loader.instance.isAutoAudioEnabled) {
                startButton.performClick()
            }
        }

        // used to enable/disable visualizer
        initCountdownTimer()

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
            micText!!.setTextColor(ContextCompat.getColor(this@MicESTestActivity, R.color.dark_black))
//            micText!!.setTextAppearance(R.style.TextAppearance_AppCompat_Small)
        }
        micContainer.addView(micText)
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

    fun initCountdownTimer() {

        countDownTimerCallStarted = object : CountDownTimer(5000, 100) {

            var amplitude = 0
            var progress = 0

            override fun onTick(millisUntilFinished: Long) {
                val amp = recordingSampler?.amplitude ?: 0

                if (recordingSampler?.audioSource == MediaRecorder.AudioSource.MIC) {
                    micCount++
                    micbeepPlayed = micCount.toString()
                    if (amp >= AMPLITUDE_CHECKING) {
                        micCountHits++
                        micthresholdBeepHits = micCountHits.toString()
                    }
                    progress += 105
                    microphoneView?.micProgressBar?.progress = progress
                } else if (recordingSampler?.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                    vidmicCount++
                    vidmicbeepPlayed = vidmicCount.toString()
                    if (amp >= AMPLITUDE_CHECKING) {
                        vidmicCountHits++
                        vidmicthresholdBeepHits = vidmicCountHits.toString()
                    }
                    progress += 100
                    videoMicrophoneView?.micProgressBar?.progress = progress
                }

                if (amp > amplitude) {
                    amplitude = amp
                }

                thresholdList.add(amp)
            }

            override fun onFinish() {
//                if (beepManager != null) {
//                    beepManager!!.close()
//                }
                TEST_LOCK = false
                if (!isPlaying) {
                    isPlaying = false
//                    if (beepManager != null) {
//                        beepManager!!.close()
//                    }
                    ZenTone.getInstance().stopTone()
                    return
                }

                val value = if (amplitude > AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                if (recordingSampler?.audioSource == MediaRecorder.AudioSource.MIC) {
                    test?.sub(Test.micESTestKey)?.value = value
                    microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    microphoneView?.amplitudeTextView?.text = amplitude.toString()

                    pref.add(MIC_ES_AMPLITUDE_PREF, amplitude).save()

                    micbeepTone = amplitude.toString()
                    micresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                    if (thresholdList.size > 0) {
                        thresholdList.removeAt(0)
                        miclowThresholdBeep = thresholdList.min().toString()
                        michighThresholdBeep = thresholdList.max().toString()
                        thresholdList.clear()
                    }

                    if (!codePlayed!!.isEmpty()) {
                        micresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                        micCodeThreshold = amplitude.toString()
                        micbeepTone = ""
                    }

                    amplitude = 0
                    recordingSampler?.stopRecording()
                    progress = 100
                    microphoneView?.micProgressBar?.progress = progress
                    isMicClickedTwice = false

                    // may require one more variable to handle earpiece
                    if (audioReports.exists() && audioReports.length() > 0) {
                        val jsonObj: JSONObject
                        try {
                            jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                            val audioReport = jsonObj.getJSONArray("AudioReport")
                            for (i in 0 until audioReport.length()) {
                                val jsonObject1 = audioReport.getJSONObject(i)

                                try {
                                    val audioOutput = jsonObject1.getJSONArray("audioOutput")
                                    for (j in 0 until audioOutput.length()) {
                                        val jsonObject2 = audioOutput.getJSONObject(j)
                                        val mic = jsonObject2.getJSONArray("mic")
                                        // for (k in 0 until mic.length())
                                        val jsonObject3 = mic.getJSONObject(0)

                                        val jsonObjectBeep = JSONObject()
                                        val beepAttempts: JSONArray = jsonObject3.getJSONArray("beepAttempts")
                                        jsonObjectBeep.put("toneFrequency", "1000")
                                        jsonObjectBeep.put("beepsPlayed", micbeepPlayed)
                                        jsonObjectBeep.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                                        jsonObjectBeep.put("thresholdReached", micthresholdBeepHits)
                                        jsonObjectBeep.put("result", micresultBeep)
                                        jsonObjectBeep.put("lowThreshold", miclowThresholdBeep)
                                        jsonObjectBeep.put("highThreshold", michighThresholdBeep)
                                        beepAttempts.put(jsonObjectBeep)

                                        WriteObjectFile.getInstance().writeObject(jsonObj.toString(), "/AudioReports.json")
                                    }
                                } catch (exception: JSONException) {
                                    exception.printStackTrace()
                                }


                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    if (runNext) {
                        if (videoMicrophoneView != null) {
                            if (isSpeakerTestFailed) {
                                if (test?.sub(Test.videoESMicTestKey)!!.value == Test.PASS) {
                                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                    videoMicrophoneView?.micProgressBar?.visibility = View.GONE
                                } else {
                                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                    videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
                                }
                                if (test?.sub(Test.micESTestKey)!!.value == Test.PASS) {
                                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                    microphoneView?.micProgressBar?.visibility = View.GONE
                                } else {
                                    if (checkAfterFailedMic) {
                                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                        microphoneView?.micProgressBar?.visibility = View.GONE
                                    } else {
                                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
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
                                    earpiecePlay = true
                                    TEST_LOCK = false
                                    audioManager!!.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0)
                                    videoMicTest()
                                }

                            }).start()
                        }
//                        else {
//                            showSpeakerPopup()
//
//                        }
                    }
                } else if (recordingSampler?.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                    releaseAudioRecorder()
                    test?.sub(Test.videoESMicTestKey)?.value = value
                    videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    videoMicrophoneView?.amplitudeTextView?.text = amplitude.toString()
                    pref.add(VID_ES_MIC_AMPLITUDE_PREF, amplitude).save()

                    vidmicbeepTone = amplitude.toString()
                    vidmicresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                    if (thresholdList.size > 0) {
                        thresholdList.removeAt(0)
                        vidmiclowThresholdBeep = thresholdList.min().toString()
                        vidmichighThresholdBeep = thresholdList.max().toString()
                        thresholdList.clear()
                    }

                    if (!codePlayed!!.isEmpty()) {
                        vidmicresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                        vidmicCodeThreshold = amplitude.toString()
                        vidmicbeepTone = ""
                    }

                    amplitude = 0
                    progress = 0
                    videoMicrophoneView?.micProgressBar?.progress = progress
                    isMicClickedTwice = false

                    if (audioReports.exists() && audioReports.length() > 0) {
                        val jsonObj: JSONObject
                        try {
                            jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                            val audioReport = jsonObj.getJSONArray("AudioReport")
                            for (i in 0 until audioReport.length()) {
                                val jsonObject1 = audioReport.getJSONObject(i)
                                try {
                                    val audioOutput = jsonObject1.getJSONArray("audioOutput")
                                    for (j in 0 until audioOutput.length()) {
                                        val jsonObject2 = audioOutput.getJSONObject(j)
                                        val mic = jsonObject2.getJSONArray("mic")
                                        // for (k in 1 until mic.length())
                                        val jsonObject3 = mic.getJSONObject(1)

                                        val beepAttempts: JSONArray = jsonObject3.getJSONArray("beepAttempts")
                                        val jsonObject = JSONObject()
                                        jsonObject.put("toneFrequency", "1000")
                                        jsonObject.put("beepsPlayed", vidmicbeepPlayed)
                                        jsonObject.put("defaultThreshold", AMPLITUDE_CHECKING.toString())
                                        jsonObject.put("thresholdReached", vidmicthresholdBeepHits)
                                        jsonObject.put("result", vidmicresultBeep)
                                        jsonObject.put("lowThreshold", vidmiclowThresholdBeep)
                                        jsonObject.put("highThreshold", vidmichighThresholdBeep)
                                        beepAttempts.put(jsonObject)

                                        WriteObjectFile.getInstance().writeObject(jsonObj.toString(), "/AudioReports.json")
                                    }
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    if (isSpeakerTestFailed) {
                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        //videoMicrophoneView?.micRecorder?.visibility = View.INVISIBLE
                        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        //microphoneView?.micRecorder?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                        isSpeakerPlayonFailed = true
                        isSpeakerPlayonPassed = true
                    }

                    if (isSpeakerTestPassed) {
                        isSpeakerPlayonPassed = true
                        isSpeakerPlayonFailed = true
                    }

                    if (runNext && !isAutoAudioEnabled) {
                        showSpeakerPopup()
                    }

                    if (isAutoAudioEnabled && runNext && !isComingDirectly) {
                        isComingDirectly = true
                        //writeAudioReportJson()
                        resetAutoStart()
                    }

                }
            }

        }
    }

    private fun microphoneTest() {
//        if (TEST_LOCK) {
//            return
//        }
//        TEST_LOCK = true
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.MIC, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(microphoneView?.visualizer)

        ZenTone.getInstance().stop()
        ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {}
        countDownTimerCallStarted?.start()
        recordingSampler?.startRecording()
    }

    private var isComingFromHeadset: Boolean = false

    private var isMicClickedTwice: Boolean = false

    private fun videoMicTest() {
        if (test?.sub(Test.videoESMicTestKey)?.value == Test.PASS) {
            if (isMicPasClicked && !isMicClickedTwice) {
                test?.sub(Test.videoESMicTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                isMicClickedTwice = true
                isManualTesting = false
                videoMicTest()
            }
            TEST_LOCK = false
            resetAutoStart()
            return
        }
        if (test?.sub(Test.videoESMicTestKey)?.value == Test.FAILED && runNext) {
            if (isComingFromHeadset && !isManualTesting) {
                TEST_LOCK = false
                isComingFromHeadset = false
                runNext = false
                resetAutoStart()
                return
            } else {
                if (!isMicPasClicked) {
                    micText!!.visibility = View.VISIBLE
                }
                isSpeakerTestFailed = true
                isSpeakerTestPassed = false
                videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                videoMicrophoneView?.micProgressBar?.visibility = View.GONE
            }
        }
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true
        ZenTone.getInstance().stop()
        ZenTone.getInstance().stopTone()
        releaseAudioRecorder()
        if (!isPlaying) {
            isPlaying = false
            ZenTone.getInstance().stopTone()
            return
        }

        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.CAMCORDER, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(videoMicrophoneView?.visualizer)

        Thread(Runnable {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
//            beepManager = BeepManager(this, earpiecePlay)
//            beepManager!!.updatePrefs()
//            beepManager!!.playBeepSoundAndVibrate()

            ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {}
//            speakOut()
            countDownTimerCallStarted?.start()
            recordingSampler?.startRecording()

        }).start()

//        Thread(Runnable {
//            try {
//                Thread.sleep(2500)
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//            speakOut()
//
//        }).start()
    }

    private fun playBeep() {
        if (test?.sub(Test.micESTestKey)?.value == Test.PASS) {
            if (isMicPasClicked && !isMicClickedTwice) {
                test?.sub(Test.micESTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                isMicClickedTwice = true
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
                if (!isMicPasClicked) {
                    micText!!.visibility = View.VISIBLE
                }
                isSpeakerTestFailed = true
                isSpeakerTestPassed = false
                microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                microphoneView?.micProgressBar?.visibility = View.VISIBLE
            }

        }

        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true

        Thread(Runnable {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                microphoneTest()
            }
            //ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {}

//            beepManager = BeepManager(this, earpiecePlay)
//            beepManager!!.updatePrefs()
//            beepManager!!.playBeepSoundAndVibrate()

        }).start()
    }

    private fun playSound() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true


        Thread(Runnable {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                microphoneTest()
            }
//            speakOut()

        }).start()

        Thread(Runnable {
            try {
                Thread.sleep(2500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
//            speakOut()

        }).start()
    }

    private fun playBeepForEarSpeaker() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true

//        beepManager = BeepManager(this, earpiecePlay)
//        beepManager!!.updatePrefs()
//        beepManager!!.playBeepSoundAndVibrate()

        if (isAutoAudioEnabled) {
            randomNumber = generateNewNumber(false)
            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_EARPIECE
            intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
            startService(intent)
            codePlayed = randomNumber.toString()
        } else {
            ZenTone.getInstance().generate2(800, 1, 1f, earpiecePlay, context) {}

            Thread(Runnable {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                runOnUiThread {
                    showSpeakerPopup()
                }

            }).start()
        }
    }

    private fun playSoundForSpeaker() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true

//        speakOut()

        Thread(Runnable {
            try {
                Thread.sleep(1500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                showSpeakerPopup()
            }
        }).start()
    }


    private fun showSpeakerPopup() {
        val title = getString(R.string.earpiece_popup)
//        if (beepManager != null) {
//            beepManager!!.close()
//        }
        showQuestionAlert(title, getString(R.string.earpiece_alerttext), object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                val value = if (isTrue) Test.PASS else Test.FAILED

                /*if (value == Test.PASS) {
                    if (runNext) {
                        microphoneTest()
                    }
                } else {

                }*/

                if (value == Test.PASS) {
                    if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {

                        if (isSpeakerTestPassed) {
                            test?.sub(Test.loudSpeakerTestKey)?.value = value
                            earpieceStatusImageView.setImageResource(getImageForStatus(value))
                            TEST_LOCK = false
                            dialog.dismiss()
                            if (!isAutoAudioEnabled) {
                                //writeAudioReportJson()
                                resetAutoStart()
                            }
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

                    }
                } else {
//                    if (runNext)
//                        microphoneTest()
//                     show recorder icon
                    if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                        if (isSpeakerPlayonFailed) {
                            test?.sub(Test.loudSpeakerTestKey)?.value = value
                            earpieceStatusImageView.setImageResource(getImageForStatus(value))
                            TEST_LOCK = false
                            dialog.dismiss()
                            if (!isAutoAudioEnabled) {
                                //writeAudioReportJson()
                                resetAutoStart()
                            }
                            return
                        }
                        if (test?.sub(Test.micESTestKey)?.value == Test.PASS) {
                            TEST_LOCK = false
                            earpieceStatusImageView.setImageResource(getImageForStatus(value))
                            dialog.dismiss()
                            resetAutoStart()
                            videoMicTest()
                            return
                        }

                        runNext = true
                        if (runNext && !isComingDirectly) {
                            TEST_LOCK = false
                            checkAfterFailedMic = true
                            isManualTesting = true
                            playBeep()
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
                    }
                }

                test?.sub(Test.earphoneTestKey)?.value = value
                earpieceStatusImageView.setImageResource(getImageForStatus(value))
                TEST_LOCK = false
                dialog.dismiss()
                if (!isAutoAudioEnabled && isAutoStartRunning) {
                    resetAutoStart()
                } else if (!isAutoAudioEnabled && runNext) {
                    resetAutoStart()
                }
            }
        })
    }

    /*override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "This Language is not supported")
            }
        }
        else {
            Log.d("TTSmicES", "Initialization Failed!")
        }
    }
    private fun speakOut() {
        tts!!.setSpeechRate(1f)
        val myHashRender = HashMap<String, String>()
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL.toString())
        tts!!.speak("Phone Check 1 2 3", TextToSpeech.QUEUE_ADD, myHashRender)
    }*/

    private fun resetAutoStart() {
        randomNumber = generateNewNumber(false)
        isAutoStartRunning = false
        pref.add(AUDIO_AUTO_ES_START_KEY, true)
        pref.save()
        testWatcher()
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
//        tts = TextToSpeech(applicationContext, this)

    }

    override fun onPause() {
        super.onPause()
        if (headSetReceiver != null)
            unregisterReceiver(headSetReceiver)
        ZenTone.getInstance().stopTone()
        releaseAudioRecorder()
        if (countDownTimerCallStarted != null) {
            countDownTimerCallStarted!!.cancel()
        }
        earpiecePlay = false
        isPlaying = false
        stopPlayback()
        if (true) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsCompleteReceiver)
        }
        TEST_LOCK = false
        resetAutoStart()
    }

    override fun onStop() {
        releaseAudioRecorder()
        ZenTone.getInstance().stopTone()
        ZenTone.getInstance().stop()
//        if (beepManager != null) {
//            beepManager!!.close()
//        }
        if (countDownTimerCallStarted != null) {
            countDownTimerCallStarted!!.cancel()
        }

        if (audioManager != null) {
            audioManager!!.mode = AudioManager.MODE_NORMAL
            audioManager!!.isSpeakerphoneOn = true
        }
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
                MICES_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_mices), -1)] =
                            RecordTest(context.getString(R.string.report_mices_test), MICES_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("MIC Ear Speaker", "${MICES_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onDestroy() {

        releaseAudioRecorder()
        ZenTone.getInstance().stopTone()
        ZenTone.getInstance().stop()

        if (countDownTimerCallStarted != null) {
            countDownTimerCallStarted!!.cancel()
        }

        if (audioManager != null) {
            audioManager!!.mode = AudioManager.MODE_NORMAL
            audioManager!!.isSpeakerphoneOn = true
        }
//        if (tts != null) {
//            tts!!.stop()
//            tts!!.shutdown()
//        }
        super.onDestroy()
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

            } else if (isAutoAudioEnabled) {
                var title = action.replace("PLAY_", "").replace("_", " ")
                if (title.contains("EARPIECE")) {
                    title = getString(R.string.earpiece_popup)
                } else if (title.contains("HEADSET RIGHT")) {
                    title = getString(R.string.headsetright_popup)
                } else if (title.contains("HEADSET")) {
                    title = getString(R.string.headtset_popup)
                }
                showQuestionAlert(title, object : TextFieldListener {
                    override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                        val value = if (action == TTSService.PLAY_HEADSET_RIGHT) {
                            if (isTrue && "$headsetLeftCode$headsetRightCode" == text) Test.PASS else Test.FAILED
                        } else {
                            if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                        }

                        codeListened = text
                        resultCodes = if (value == Test.PASS) "Pass" else "Fail"

                        if (value == Test.PASS && "$headsetLeftCode$headsetRightCode" != text && !muteEarpieceSound) {

                            if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                if (isSpeakerPlayonPassed) {
                                    handleUserSelection(action, value)
                                    dialog.dismiss()
                                    resetAutoStart()
                                    return
                                }
                                isSpeakerTestFailed = false
                                isSpeakerTestPassed = true
                                micText!!.visibility = View.GONE
                                videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                                microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                microphoneView?.micProgressBar?.visibility = View.INVISIBLE

                            }

                            if (audioReports.exists() && audioReports.length() > 0 && !runNext) {
                                val jsonObj: JSONObject
                                try {
                                    jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                                    val audioReport = jsonObj.getJSONArray("AudioReport")
                                    for (i in 0 until audioReport.length()) {
                                        val jsonObject1 = audioReport.getJSONObject(i)
                                        try {
                                            val audioOutput = jsonObject1.getJSONArray("audioOutput")
                                            for (j in 0 until audioOutput.length()) {
                                                val jsonObject2 = audioOutput.getJSONObject(j)
                                                val mic = jsonObject2.getJSONArray("mic")

                                                for (k in 0 until mic.length()) {
                                                    val jsonObject3 = mic.getJSONObject(k)
                                                    val jsonObjectCode = JSONObject()
                                                    val codeAttempts: JSONArray = jsonObject3.getJSONArray("codeAttempts")
                                                    jsonObjectCode.put("codePlayed", codePlayed)
                                                    jsonObjectCode.put("codeListened", codeListened)
                                                    jsonObjectCode.put("result", resultCodes)
                                                    jsonObjectCode.put("micThresholdReached", micCodeThreshold)
                                                    codeAttempts.put(jsonObjectCode)
                                                }
                                                WriteObjectFile.getInstance().writeObject(jsonObj.toString(), "/AudioReports.json")
                                            }
                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                        }
                                    }
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }

                                if (Loader.instance.isAutoAudioEnabled && Build.MANUFACTURER.containsIgnoreCase("samsung")
                                        && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                    runNext = true
                                }
                                Thread(Runnable {

                                    try {
                                        Thread.sleep(700)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                    runOnUiThread {
                                        if (runNext) {
                                            playBeep()
                                        }
                                    }

                                }).start()
                                handleUserSelection(action, value)
                                dialog.dismiss()
                                resetAutoStart()


                            } else {
                                if (Loader.instance.isAutoAudioEnabled && Build.MANUFACTURER.containsIgnoreCase("samsung")
                                        && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                    runNext = true
                                }
                                Thread(Runnable {

                                    try {
                                        Thread.sleep(700)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                    runOnUiThread {
                                        if (runNext) {
                                            playBeep()
                                        }
                                    }

                                }).start()
                                if (runNext) {
                                    handleUserSelection(action, value)
                                    dialog.dismiss()
                                } else {
                                    handleUserSelection(action, value)
                                    dialog.dismiss()
                                    resetAutoStart()
                                }
                            }

                        } else {
                            if (muteEarpieceSound) {
                                if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                    micText!!.visibility = View.GONE
                                }
                            } else {
                                if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                    if (isSpeakerPlayonFailed) {
                                        handleUserSelection(action, value)
                                        dialog.dismiss()
                                        resetAutoStart()
                                        return
                                    }

                                    if (Loader.instance.isAutoAudioEnabled) {
                                        runNext = true
                                    }

                                    Thread(Runnable {

                                        try {
                                            Thread.sleep(700)
                                        } catch (e: InterruptedException) {
                                            e.printStackTrace()
                                        }

                                        runOnUiThread {
                                            if (runNext) {
                                                playBeep()
                                            }
                                        }

                                    }).start()

                                    isSpeakerTestFailed = true
                                    isSpeakerTestPassed = false
                                    micText!!.visibility = View.VISIBLE
                                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                    microphoneView?.micProgressBar?.visibility = View.VISIBLE
                                }
                            }
                        }

                        if (value == Test.PASS && "$headsetLeftCode$headsetRightCode" == text && muteEarpieceSound) {
                            // do it later.
                            if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
                                micText!!.visibility = View.GONE
                            }
                        }

                        /*if (value == Test.PASS && "$headsetLeftCode$headsetRightCode" == text) {
                            if (runNext) {
                                microphoneTest()
                            }
                        }
                        else if (value == Test.PASS && "$headsetLeftCode$headsetRightCode" == text) {
                            if (runNext) {
                                playBeepForEarSpeaker()
                            }
                        } else {
                            // do it later.
                        }

                        else if (value == Test.PASS && "$headsetLeftCode$headsetRightCode" == text)  {
                            microphoneTest()
                        }*/

                        if (runNext) {
                            handleUserSelection(action, value)
                            dialog.dismiss()
                        } else {
                            handleUserSelection(action, value)
                            dialog.dismiss()
                            resetAutoStart()
                        }
                    }
                })

            } else if (isAutoEarPieceEnabled && action.containsIgnoreCase("PLAY_EARPIECE")) {
                var title = action.replace("PLAY_", "").replace("_", " ")
                if (title.contains("EARPIECE")) {
                    title = getString(R.string.earpiece_popup)
                } else if (title.contains("HEADSET RIGHT")) {
                    title = getString(R.string.headsetright_popup)
                } else if (title.contains("HEADSET")) {
                    title = getString(R.string.headtset_popup)
                }
                showQuestionAlert(title, object : TextFieldListener {
                    override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                        val value = if (action == TTSService.PLAY_HEADSET_RIGHT) {
                            if (isTrue && "$headsetLeftCode$headsetRightCode" == text) Test.PASS else Test.FAILED
                        } else {
                            if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                        }

//                        if (value == Test.PASS) {
//                            if (runNext) {
//                                microphoneTest()
//                            }
//                        } else {
//                            // do it later.
//                        }

                        handleUserSelection(action, value)
                        dialog.dismiss()
//                        resetAutoStart()
                        testWatcher()
                    }
                })
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
                            /*if (runNext)
                                microphoneTest()*/
                            isBeepfromHeadset = true
                            if (isBeepfromHeadset) {
                                muteEarpieceSound = false
                                runNext = true
                                earpiecePlay = true
                                isBeepRunning = true
                                TEST_LOCK = false
                                if (isSamsung && !isAutoAudioEnabled && isAutoStartRunning) {
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
                            /* if (runNext)
                                 microphoneTest()
                              show recorder icon*/
                            isBeepfromHeadset = false
                            if (!isBeepfromHeadset) {
                                muteEarpieceSound = false
                                runNext = true
                                earpiecePlay = true
                                isBeepRunning = true
                                TEST_LOCK = false
                                if (isSamsung && !isAutoAudioEnabled && isAutoStartRunning) {
                                    isComingFromHeadset = true
                                    if (isPluggedOut && !isHeadsetDialogClicked) {
                                        playBeep()
                                    }
                                }
                                handleUserSelection(action, value)
                                dialog.dismiss()
                            }

//                            if (Build.MANUFACTURER.containsIgnoreCase("samsung") && Build.MODEL.containsIgnoreCase(MictestSupportedModels.choose())) {
//                                micText!!.visibility = View.VISIBLE
//                            }
                        }
                        if (isBeepfromHeadset && !isSamsung) {
                            resetAutoStart()
                        } else if (isBeepfromHeadset && isSamsung) {
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

    private fun onAudioRecordError(sampler: RecordingSampler, e: String) {

        FirebaseUtil.addNew(FirebaseUtil.AUDIO)
                .child("Error")
                .child(if (sampler.audioSource == MediaRecorder.AudioSource.MIC) "Mic" else "Video Microphone")
                .child(FirebaseUtil.EXCEPTION)
                .setValue(e)

        FirebaseUtil.addNew(FirebaseUtil.AUDIO)
                .child("Error")
                .child(if (sampler.audioSource == MediaRecorder.AudioSource.MIC) "Mic" else "Video Microphone")
                .child("BufferSize")
                .setValue(sampler.bufferSize)

        FirebaseUtil.addNew(FirebaseUtil.AUDIO)
                .child("Error")
                .child(if (sampler.audioSource == MediaRecorder.AudioSource.MIC) "Mic" else "Video Microphone")
                .child("SampleRate")
                .setValue(sampler.recordingSampleRate)

    }

    private fun releaseAudioRecorder() {

        if (recordingSampler != null) {
            if (recordingSampler?.isRecording == true)
                recordingSampler?.stopRecording()
            recordingSampler?.release()
            recordingSampler = null
        }
    }

    companion object {
        val REQ = 55
        private var AMPLITUDE_CHECKING = 1200.00
        val AUDIO_AUTO_ES_START_KEY = "autoMicESCheckStart"
        val MIC_ES_AMPLITUDE_PREF = "MIC_ES_AMPLITUDE_PREF"
        val VID_ES_MIC_AMPLITUDE_PREF = "VID_ES_MICCHECK_AMPLITUDE_PREF"
        var MICES_SCREEN_TIME = 0
        var isSpeakerWorking = false
        /*@JvmStatic
        val headsetReport = File("${Loader.baseFile}/HeadsetReport.json")*/
    }
}