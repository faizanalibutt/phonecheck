package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Typeface
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.speech.tts.TextToSpeech
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
import com.upgenicsint.phonecheck.barcode.BeepManager
import com.upgenicsint.phonecheck.misc.*
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.MicLSTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_mic_lstest.*
import kotlinx.android.synthetic.main.audio_input_test_layout.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class MicLSTestActivity : BaseAudioTestActivity<MicLSTest>(), TextToSpeech.OnInitListener {

    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var micText: TextView? = null
    private var recordingSampler: RecordingSampler? = null
    private var isAutoStartRunning = false
    private var checkAutoAudioTest: Boolean = false
    private var runNext = false //flag to stop auto start next test when user select a single testm,,
    private var earpiecePlay = false
    var beepManager: BeepManager? = null
    private var tts: TextToSpeech? = null
    private var isPlaying = false
    private var isSpeakerTouched = false
    private var isBeepPassed: Boolean = false
    private val pref by lazy { Cabinet.open(context, R.string.mic_ls) }

    // audio analytics
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


    private var countDownTimerCallStarted: CountDownTimer? = null

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
                            val jsonArrary = audioReport.getJSONObject(i).getJSONArray("audioInput")
                            if (jsonArrary != null && jsonArrary.length() > 0) {
                                return
                            }
                        } catch (json: JSONException) {
                            json.printStackTrace()
                        }
                    }

                    val jsonObject = JSONObject()
                    audioReport.put(jsonObject)
                    val audioInput = JSONArray()
                    jsonObject.put("audioInput", audioInput)

                    val jsonObject2 = JSONObject()
                    audioInput.put(jsonObject2)

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
                         val audioInput = jsonObject1.getJSONArray("audioInput")
                         for (j in 0 until audioInput.length()) {
                             val jsonObject2 = audioInput.getJSONObject(j)
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

                /*            for (int i = 0; i < audioReport.length(); i++) {}
                   */

                val audioReport = JSONArray()
                jsonObject.put("AudioReport", audioReport)

                val jsonObject1 = JSONObject()
                audioReport.put(jsonObject1)

                val audioInput = JSONArray()
                jsonObject1.put("audioInput", audioInput)

                val jsonObject2 = JSONObject()
                audioInput.put(jsonObject2)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_mic_lstest)

        onCreateNav()
        //audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        //initSpeech()

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
        test = Loader.instance.getByClassType(MicLSTest::class.java)
        randomNumber = generateNewNumber(false)

        val micContainer = micTestLayout1 as ViewGroup

        // show text at runtime on Speaker fail.
        showMicText(micContainer)

        microphoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
        micContainer.addView(microphoneView)

        val resultsFilterMap = test?.resultsFilterMap

        if (resultsFilterMap != null) {
            if (resultsFilterMap[Test.videoMicTestKey] == true) {
                videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
                micContainer.addView(videoMicrophoneView)
            }
        }

        // test either auto audio is added or not.
        if (Loader.instance.filterContains(Test.autoAudioFilterKey)) {
            checkAutoAudioTest = true
        }

        speakerStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.loudSpeakerTestKey)?.value ?: Test.INIT))

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

        microphoneView?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)

        /* micRecorder?.setOnLongClickListener { }*/

        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlay = false
                isPlaying = true
                playBeep()
//                playSound()
            }
        }

        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                isPlaying = true
                videoMicTest()
            }
        }

        speakerLayout.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                isPlaying = true
                isSpeakerTouched = true
                //auto ls test
                /*if (isAutoAudioEnabled && isAutoLSEnabled) {
                    initSpeech()
                    startSpeech()
                }*/

                Handler().postDelayed({ playBeepForSpeaker() }, 500)
                //playSoundForSpeaker()
            }
        }

        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                if (isAutoAudioEnabled) {
                    //auto ls test
                    /*if (isAutoLSEnabled) {
                        isLoopThroughResult = false
                        tryFirstSpeech = true
                        speechValue = false
                        startSpeech()
                    }*/
                    resetTest()
                    Handler().postDelayed({ playBeepForSpeaker() }, 1000)

                } else {
                    /*isMicClicked = false
                    isMicPasClicked = false*/
                    resetTest()
                    playBeep()
                }

//                playSound()
//                microphoneTest()
            }
        }

        // auto start tests when activity created
        val alreadyStarted = pref.getBoolean(AUDIO_AUTO_START_KEY, false)
        if (!alreadyStarted) {
            startButton.performClick()
        }

        // used to enable/disable visualizer.
        initCountdownTimer()
    }

    private fun resetTest() {
        isAutoStartRunning = true
        runNext = true
        isPlaying = true
        earpiecePlay = false
        isSpeakerTouched = false
        isSpeakerTestFailed = false
        isSpeakerPlayonFailed = false
        isSpeakerPlayonPassed = false
        isSpeakerTestPassed = false
        isSpeakerTestFailed = false
        test?.sub(Test.micTestKey)?.value = Test.INIT
        test?.sub(Test.videoMicTestKey)?.value = Test.INIT
        micText!!.visibility = View.GONE
        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
    }


    // countdowntimer is used to disable audio visualizer and stop test
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

                // old amp logic
                if (amp > amplitude) {
                    amplitude = amp
                }

                thresholdList.add(amp)

            }

            override fun onFinish() {
                if (beepManager != null) {
                    beepManager!!.close()
                }
                TEST_LOCK = false
                if (!isPlaying) {
                    isPlaying = false
                    if (beepManager != null) {
                        beepManager!!.close()
                    }
//                    ZenTone.getInstance().stopTone()
                    return
                }

                val value: Int
                if (recordingSampler?.audioSource == MediaRecorder.AudioSource.MIC) {
                    value = if (amplitude > AMPLITUDE_CHECKING) Test.PASS else Test.FAILED
                    test?.sub(Test.micTestKey)?.value = value
                    microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    microphoneView?.amplitudeTextView?.text = amplitude.toString()
                    pref.add(MIC_AMPLITUDE_PREF, amplitude).save()

                    micbeepTone = amplitude.toString()
                    micresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                    if (thresholdList.size != 0) {
                        thresholdList.removeAt(0)
                    }
                    miclowThresholdBeep = thresholdList.min().toString()
                    michighThresholdBeep = thresholdList.max().toString()
                    thresholdList.clear()

                    if (!codePlayed!!.isEmpty()) {
                        micresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                        micCodeThreshold = amplitude.toString()
                        micbeepTone = ""
                    }

                    amplitude = 0
                    progress = 100
                    microphoneView?.micProgressBar?.progress = progress
                    releaseAudioRecorder()

                    if (audioReports.exists() && audioReports.length() > 0) {
                        val jsonObj: JSONObject
                        try {
                            jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                            val audioReport = jsonObj.getJSONArray("AudioReport")
                            for (i in 0 until audioReport.length()) {
                                val jsonObject1 = audioReport.getJSONObject(i)

                                try {
                                    val audioInput = jsonObject1.getJSONArray("audioInput")
                                    for (j in 0 until audioInput.length()) {
                                        val jsonObject2 = audioInput.getJSONObject(j)
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
                                if (test?.sub(Test.videoMicTestKey)!!.value == Test.PASS) {
                                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                    videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                                } else {
                                    videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                    videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
                                }
                                if (test?.sub(Test.micTestKey)!!.value == Test.PASS) {
                                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                                    microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                                } else {
                                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                                    microphoneView?.micProgressBar?.visibility = View.VISIBLE
                                    microphoneView?.micProgressBar?.progress = 0
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
//                        else {
//                            showSpeakerPopup()
//                        }
                    }
                } else if (recordingSampler?.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                    value = if (amplitude > AMPLITUDE_CHECKING_VID) Test.PASS else Test.FAILED
                    releaseAudioRecorder()
                    test?.sub(Test.videoMicTestKey)?.value = value
                    videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    videoMicrophoneView?.amplitudeTextView?.text = amplitude.toString()
                    pref.add(VID_MIC_AMPLITUDE_PREF, amplitude).save()

                    vidmicbeepTone = amplitude.toString()
                    vidmicresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                    if (thresholdList.size != 0) {
                        thresholdList.removeAt(0)
                    }
                    vidmiclowThresholdBeep = thresholdList.min().toString()
                    vidmichighThresholdBeep = thresholdList.max().toString()
                    thresholdList.clear()

                    if (!codePlayed!!.isEmpty()) {
                        vidmicresultBeep = if (value == Test.PASS) "Pass" else "Fail"
                        vidmicCodeThreshold = amplitude.toString()
                        vidmicbeepTone = ""
                    }

                    amplitude = 0
                    progress = 0
                    videoMicrophoneView?.micProgressBar?.progress = progress

                    if (audioReports.exists() && audioReports.length() > 0) {
                        val jsonObj: JSONObject
                        try {
                            jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                            val audioReport = jsonObj.getJSONArray("AudioReport")
                            for (i in 0 until audioReport.length()) {
                                val jsonObject1 = audioReport.getJSONObject(i)
                                try {
                                    val audioInput = jsonObject1.getJSONArray("audioInput")
                                    for (j in 0 until audioInput.length()) {
                                        val jsonObject2 = audioInput.getJSONObject(j)
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

                    if (runNext && !isAutoAudioEnabled) {
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

                    if (isAutoAudioEnabled && runNext && !isSpeakerTouched) {
                        isBeepPassed = false
                        resetAutoStart()
                        writeAudioReportJson()
                    }

                }
            }

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
            micText!!.setTextColor(ContextCompat.getColor(MicLSTestActivity@ this, R.color.dark_black))
//            micText!!.setTextAppearance(R.style.TextAppearance_AppCompat_Small)
        }
        micContainer.addView(micText)
    }

    private fun microphoneTest() {

        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.MIC, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(microphoneView?.visualizer)

        /*beepManager = BeepManager(this, earpiecePlay)
        beepManager!!.updatePrefs()
        beepManager!!.playBeepSoundAndVibrate()*/

        countDownTimerCallStarted?.start()
        recordingSampler?.startRecording()

    }

    private fun videoMicTest() {
        if (test?.sub(Test.videoMicTestKey)?.value == Test.PASS) {
            /*if (isMicPasClicked) {
                test?.sub(Test.videoMicTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                videoMicTest()
            }*/
            TEST_LOCK = false
            isBeepPassed = false
            resetAutoStart()
            return
        }
        if (TEST_LOCK) {
            return
        }
        if (test?.sub(Test.videoMicTestKey)?.value == Test.FAILED && runNext) {
            /*if (!isMicPasClicked) {
                micText!!.visibility = View.VISIBLE
            }*/
            isSpeakerTestFailed = true
            isSpeakerTestPassed = false
            videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
            videoMicrophoneView?.micProgressBar?.visibility = View.VISIBLE
        }
        TEST_LOCK = true

        /* if (!isPlaying) {
             isPlaying = false
             ZenTone.getInstance().stopTone()
             return
         }

         ZenTone.getInstance().stop()*/

        Thread(Runnable {
            try {
                Thread.sleep(700)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            releaseAudioRecorder()
            recordingSampler = RecordingSampler(MediaRecorder.AudioSource.CAMCORDER, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
            recordingSampler?.setSamplingInterval(100)
            recordingSampler?.link(videoMicrophoneView?.visualizer)

            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()

            /*ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {
            }
            speakOut()*/

            countDownTimerCallStarted?.start()
            recordingSampler?.startRecording()

        }).start()

        /*if (Loader.instance.filterContains(Test.autoAudioFilterKey)) {
            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_VIDEO_MIC
            intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
            startService(intent)
        } else {
            Thread(Runnable {
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                beepManager = BeepManager(this, earpiecePlay)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()

//            ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {
//            }
//            speakOut()
                countDownTimerCallStarted?.start()
                recordingSampler?.startRecording()

            }).start()
        }*/

        /*Thread(Runnable {
            try {
                Thread.sleep(2500)

            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            speakOut()
        }).start()*/

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

    private fun playBeep() {
        if (test?.sub(Test.micTestKey)?.value == Test.PASS) {
            /*if (isMicPasClicked) {
                test?.sub(Test.micTestKey)?.value = Test.INIT
                TEST_LOCK = false
                isMicPasClicked = false
                playBeep()
                return
            }*/
            videoMicTest()
            return
        }
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true

//        ZenTone.getInstance().stop()

        Thread(Runnable {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                microphoneTest()
            }
//            ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {}
            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()

        }).start()

        /*if (Loader.instance.filterContains(Test.autoAudioFilterKey)) {

            if (TEST_LOCK) {
                return
            }
            TEST_LOCK = true
            releaseAudioRecorder()
            recordingSampler = RecordingSampler(MediaRecorder.AudioSource.LS_MIC, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
            recordingSampler?.setSamplingInterval(100)
            recordingSampler?.link(microphoneView?.visualizer)


            countDownTimerCallStarted = object : CountDownTimer(5000, 100) {

                var amplitude = 0
                override fun onTick(millisUntilFinished: Long) {

                    val amp = recordingSampler?.amplitude ?: 0

                    if (amp > amplitude) {
                        amplitude = amp
                    }

                }

                override fun onFinish() {
                    TEST_LOCK = false
                    val value = if (amplitude > AMPLITUDE_CHECKING) Test.PASS else Test.FAILED

                    if (recordingSampler?.audioSource == MediaRecorder.AudioSource.LS_MIC) {
                        test?.sub(Test.micTestKey)?.value = value
                        microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        microphoneView?.amplitudeTextView?.text = amplitude.toString()

                        pref.add(AudioInputTestActivity.MIC_AMPLITUDE_PREF, amplitude).save()

                        amplitude = 0
                        recordingSampler?.stopRecording()
                        if (runNext) {
                            if (videoMicrophoneView != null) {

                                videoMicTest()
                            }
//                            else {
//                                showSpeakerPopup()
//
//                            }
                        }

                    } else if (recordingSampler?.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                        releaseAudioRecorder()
                        test?.sub(Test.videoMicTestKey)?.value = value
                        videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        videoMicrophoneView?.amplitudeTextView?.text = amplitude.toString()
                        pref.add(AudioInputTestActivity.VID_MIC_AMPLITUDE_PREF, amplitude).save()
                        amplitude = 0
                        if (runNext) {
                            showSpeakerPopup()
                        }

                    }

                }

            }

            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_MIC
            intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
            startService(intent)
        } else {
            Thread(Runnable {
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                runOnUiThread {
                    microphoneTest()
                }
//            ZenTone.getInstance().generate2(2500, 4, 1f, earpiecePlay, context) {}
                beepManager = BeepManager(this, earpiecePlay)
                beepManager!!.updatePrefs()
                beepManager!!.playBeepSoundAndVibrate()

            }).start()
        }*/
    }

    private fun playBeepForSpeaker() {
        if (TEST_LOCK) {
            return
        }
        TEST_LOCK = true

        //ZenTone.getInstance().generate2(800, 1, 0.7f, earpiecePlay, context) {}

        if (checkAutoAudioTest) {
            if (isSpeakerTouched) {
                randomNumber = generateNewNumber(false)
            }
            /*if (isAutoLSEnabled) {

                val temp1: CharArray = randomNumber.toString().toCharArray()
                if (!temp1.isEmpty()) {
                    if (temp1.size == 1) {
                        val aTemp: String = temp1[0] + "     " + " 0 "
                        val intent = Intent(this, TTSService::class.java)
                        intent.action = TTSService.PLAY_SPEAKER
                        intent.putExtra(TTSService.RANDOM_NUMBER, aTemp)
                        startService(intent)
                    } else {
                        val aTemp: String = temp1[0] + "     " + temp1[1]
                        val intent = Intent(this, TTSService::class.java)
                        intent.action = TTSService.PLAY_SPEAKER
                        intent.putExtra(TTSService.RANDOM_NUMBER, aTemp)
                        startService(intent)
                    }
                }
            } else {
                val intent = Intent(this, TTSService::class.java)
                intent.action = TTSService.PLAY_SPEAKER
                intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
                startService(intent)
            }*/

            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_SPEAKER
            intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
            startService(intent)
            codePlayed = randomNumber.toString()

        } else {
            beepManager = BeepManager(this, earpiecePlay)
            beepManager!!.updatePrefs()
            beepManager!!.playBeepSoundAndVibrate()

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
        val title = getString(R.string.speaker_popup)

        if (beepManager != null) {
            beepManager!!.close()
        }

        showQuestionAlert(title, getString(R.string.did_hear_sound_speaker), object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                val value = if (isTrue) Test.PASS else Test.FAILED

                if (value == Test.PASS) {
                    /*if (runNext)
                        microphoneTest()*/
                    if (isSpeakerTestPassed) {
                        test?.sub(Test.loudSpeakerTestKey)?.value = value
                        speakerStatusImageView.setImageResource(getImageForStatus(value))
                        TEST_LOCK = false
                        isBeepPassed = false
                        dialog.dismiss()
                        if (!isAutoAudioEnabled) {
                            resetAutoStart()
                            writeAudioReportJson()
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
                    TEST_LOCK = false
                    isBeepPassed = false
                } else {

                    if (isSpeakerPlayonFailed) {
                        test?.sub(Test.loudSpeakerTestKey)?.value = value
                        speakerStatusImageView.setImageResource(getImageForStatus(value))
                        TEST_LOCK = false
                        isBeepPassed = false
                        dialog.dismiss()
                        if (!isAutoAudioEnabled) {
                            resetAutoStart()
                            writeAudioReportJson()
                        }
                        return
                    }

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
                        playBeep()
                    }

                    isSpeakerTestFailed = true
                    isSpeakerTestPassed = false
                    micText!!.visibility = View.VISIBLE
                    microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                    microphoneView?.micProgressBar?.visibility = View.VISIBLE
                }

                test?.sub(Test.loudSpeakerTestKey)?.value = value
                speakerStatusImageView.setImageResource(getImageForStatus(value))
                dialog.dismiss()
                if (!isAutoAudioEnabled) {
                    resetAutoStart()
                    writeAudioReportJson()
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "This Language is not supported")
            }

        } else {
            Log.d("TTSmicLS", "Initialization Failed!")
        }
    }

    private fun speakOut() {
        tts!!.setSpeechRate(1f)
        tts!!.speak("Phone Check 1 2 3", TextToSpeech.QUEUE_FLUSH, null)
    }


    private fun resetAutoStart() {
        isAutoStartRunning = isBeepPassed
        pref.add(AUDIO_AUTO_START_KEY, true)
        pref.save()
        randomNumber = generateNewNumber(false)
        testWatcher()
    }

    public override fun onResume() {
        super.onResume()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStop() {
//        ZenTone.getInstance().stopTone()
//        ZenTone.getInstance().stop()

        if (ttsCompleteReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsCompleteReceiver)
        }

        if (ttsPreparedReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsPreparedReceiver)
        }
        if (ttsErrorReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsErrorReceiver)
        }

        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        val ttsCompleteFilter = IntentFilter(TTSService.SEND_TTS_ON_COMPLETE)
        LocalBroadcastManager.getInstance(context).registerReceiver(ttsCompleteReceiver, ttsCompleteFilter)

        val ttsPreparedFilter = IntentFilter(TTSService.SEND_TTS_ON_PREPARED)
        LocalBroadcastManager.getInstance(context).registerReceiver(ttsPreparedReceiver, ttsPreparedFilter)

        val ttsErrorFilter = IntentFilter(TTSService.SEND_TTS_ON_ERROR)
        LocalBroadcastManager.getInstance(context).registerReceiver(ttsErrorReceiver, ttsErrorFilter)
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
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onDestroy() {
        //ZenTone.getInstance().stopTone()
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
            tts = null
        }
        releaseAudioRecorder()
        if (countDownTimerCallStarted != null) {
            countDownTimerCallStarted!!.cancel()
            countDownTimerCallStarted = null
        }
        //cancelSpeech()
        //unmuteSound()
        earpiecePlay = false
        isPlaying = false
        TEST_LOCK = false
        isBeepPassed = false
        runNext = false
        resetAutoStart()

        super.onDestroy()
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
            RecordingSampler.isRecording = false
            recordingSampler = null
            if (RecordingSampler.mAudioRecord != null) {
                RecordingSampler.mAudioRecord.stop()
                RecordingSampler.mAudioRecord.release()
                RecordingSampler.mAudioRecord = null
            }

        }
    }

    private val ttsPreparedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || intent.action == null) {
                return
            }

            val action = intent.getStringExtra(TTSService.TTS_ACTION)

            if (action == TTSService.PLAY_MIC || action == TTSService.PLAY_VIDEO_MIC) {
                countDownTimerCallStarted?.start()
                recordingSampler?.startRecording() ?: toast("Error recordingSampler null")
            }

        }
    }

    private val ttsErrorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                FirebaseUtil.addNew(FirebaseUtil.AUDIO).child("errorReceiver")
                        .child(intent.getStringExtra(TTSService.ERROR_TYPE))
                        .setValue(TEST_LOCK)
            } catch (ignored: Exception) {
            }

            TEST_LOCK = false
        }
    }

    private val ttsCompleteReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || intent.action == null) {
                return
            }

            val action = intent.getStringExtra(TTSService.TTS_ACTION)

            //unmuteSound()

            if (action == TTSService.PLAY_SPEAKER) {
                TEST_LOCK = false
                if (isAutoAudioEnabled) {

                    var title = action.replace("PLAY_", "").replace("_", " ")
                    if (title.contains("SPEAKER")) {
                        title = getString(R.string.speaker_popup)
                        //speechTitle = title
                    }

                    /*if (isAutoLSEnabled) {
                        speechAction = action
                        TEST_LOCK = false
                        isBeepPassed = false
                        isAutoStartRunning = isBeepPassed
                        return
                    }*/

                    showDialog(title, action)

                }

                /*else {
                    var title = action.replace("RIGHT", "").replace("PLAY_", "").replace("_", " ")
                    if(title.contains("SPEAKER")){
                        title = getString(R.string.speaker_popup)
                    }
                    showQuestionAlert(title, getString(R.string.did_hear_sound_speaker), object : AlertButtonListener {
                        override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                            val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                            val value = if (isTrue) Test.PASS else Test.FAILED
                            handleUserSelection(action, value)
                            dialog.dismiss()

                            resetAutoStart()
                        }
                    })
                }*/

            }

        }
    }

    private fun showDialog(title: String, action: String) {
        showQuestionAlert(title, object : TextFieldListener {
            override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                val value = if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED

                codeListened = text
                resultCodes = if (value == Test.PASS) "Pass" else "Fail"
                /*if (value == Test.PASS) speechValue = true*/

                if (audioReports.exists() && audioReports.length() > 0 && !runNext && isSpeakerTouched) {
                    val jsonObj: JSONObject
                    try {
                        jsonObj = JSONObject(ReadTestJsonFile.getInstance().returnNewObject(audioReports))
                        val audioReport = jsonObj.getJSONArray("AudioReport")
                        for (i in 0 until audioReport.length()) {
                            val jsonObject1 = audioReport.getJSONObject(i)
                            try {
                                val audioInput = jsonObject1.getJSONArray("audioInput")
                                for (j in 0 until audioInput.length()) {
                                    val jsonObject2 = audioInput.getJSONObject(j)
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

                    if (value == Test.PASS) {
                        if (isSpeakerPlayonPassed) {
                            handleUserSelection(action, value)
                            isBeepPassed = false
                            TEST_LOCK = false
                            dialog.dismiss()
                            resetAutoStart()
                            return
                        }
                        if (Loader.instance.isAutoAudioEnabled) {
                            runNext = true
                        }

                        if (runNext) {
                            playBeep()
                        }

                        isSpeakerTestFailed = false
                        isSpeakerTestPassed = true
                        micText!!.visibility = View.GONE
                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        //videoMicrophoneView?.micRecorder?.visibility = View.INVISIBLE
                        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        //microphoneView?.micRecorder?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                        TEST_LOCK = false
                        isBeepPassed = true
                    } else {
                        if (isSpeakerPlayonFailed) {
                            handleUserSelection(action, value)
                            isBeepPassed = false
                            TEST_LOCK = false
                            dialog.dismiss()
                            resetAutoStart()
                            return
                        }

                        if (Loader.instance.isAutoAudioEnabled) {
                            runNext = true
                        }

                        Thread(Runnable {

                            try {
                                Thread.sleep(1000)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                            runOnUiThread {
                                if (runNext) {
                                    TEST_LOCK = false
                                    playBeep()
                                }
                            }

                        }).start()

                        isSpeakerTestFailed = true
                        isSpeakerTestPassed = false
                        micText!!.visibility = View.VISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                        //microphoneView?.micRecorder?.visibility = View.VISIBLE
                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
                        isBeepPassed = true
                    }
                    handleUserSelection(action, value)
                    dialog.dismiss()
                    resetAutoStart()
                } else {
                    if (value == Test.PASS) {
                        if (isSpeakerPlayonPassed) {
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

                        isSpeakerTestFailed = false
                        isSpeakerTestPassed = true
                        micText!!.visibility = View.GONE
                        videoMicrophoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        //videoMicrophoneView?.micRecorder?.visibility = View.INVISIBLE
                        videoMicrophoneView?.micProgressBar?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.selector_row)
                        //microphoneView?.micRecorder?.visibility = View.INVISIBLE
                        microphoneView?.micProgressBar?.visibility = View.INVISIBLE
                        TEST_LOCK = false
                        isBeepPassed = true
                    } else {
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
                                    TEST_LOCK = false
                                    playBeep()
                                }
                            }

                        }).start()

                        isSpeakerTestFailed = true
                        isSpeakerTestPassed = false
                        micText!!.visibility = View.VISIBLE
                        microphoneView?.micProgressBar?.setBackgroundResource(R.drawable.mictest_bg)
                        //microphoneView?.micRecorder?.visibility = View.VISIBLE
                        microphoneView?.micProgressBar?.visibility = View.VISIBLE
                        isBeepPassed = true
                    }
                    handleUserSelection(action, value)
                    dialog.dismiss()
                    resetAutoStart()
                }
            }

        })
    }

    private fun handleUserSelection(action: String, value: Int) {
        when (action) {
            TTSService.PLAY_SPEAKER -> {
                test?.sub(Test.loudSpeakerTestKey)?.value = value
                if (value == Test.PASS) {
                    AudioInputTestActivity.isSpeakerWorking = true
                }
                speakerStatusImageView.setImageResource(getImageForStatus(value))
            }
        }
    }

    companion object {
        val REQ = 54
        private var AMPLITUDE_CHECKING = when {
            Build.MODEL.containsIgnoreCase("SM-N900") -> 2000.00
            else -> 1500.00
        }
        private var AMPLITUDE_CHECKING_VID = when {
            Build.MODEL.containsIgnoreCase("SM-N900") -> 2000.00
            Devices.chooseThreshold() -> 1000.00
            else -> 1500.00
        }
        val AUDIO_AUTO_START_KEY = "autoMicCheckStart"
        val MIC_AMPLITUDE_PREF = "MICCHECK_AMPLITUDE_PREF"
        val VID_MIC_AMPLITUDE_PREF = "VID_MICCHECK_AMPLITUDE_PREF"
        var MICLS_SCREEN_TIME = 0
        @JvmStatic
        val audioReports = File("${Loader.baseFile}/AudioReports.json")
    }

}
