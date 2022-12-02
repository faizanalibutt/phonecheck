package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.BuildConfig
import com.farhanahmed.cabinet.Cabinet
import com.tyorikan.voicerecordingvisualizer.RecordingSampler
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.barcode.BeepManager
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.SpeechToTextTest
import com.upgenicsint.phonecheck.toast
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_audio_input_test.*
import kotlinx.android.synthetic.main.audio_input_test_layout.view.*
import java.util.*

class SpeechRecognization : BaseAudioTestActivity<SpeechToTextTest>(), RecognitionListener, TextToSpeech.OnInitListener {

    private var microphoneView: View? = null
    private var isAutoStartRunning = false
    private val REQ_CODE_SPEECH_INPUT = 100
    private var phrase = "hello"
    private var speechResult = null
//    public var beepManager: BeepManager? = null
    private var tts: TextToSpeech? = null
    private var speech: SpeechRecognizer? = null
    private var tone: ToneGenerator? = null
    private val LOG_TAG = "VoiceRecognition"
    private var recognizerIntent: Intent? = null
    private var recordingSampler: RecordingSampler? = null
    private var countDownTimerCallStarted: CountDownTimer? = null
    private var progressBar: ProgressBar? = null
    private var audioManager: AudioManager? = null
    private val pref by lazy { Cabinet.open(context, R.string.speech_pref) }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_recognization)
//        progressBar = findViewById(R.id.progressBar1) as ProgressBar
        onCreateNav()

        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle(getString(R.string.speech_recog_nav_title))

        Loader.TIME_VALUE = 0
        SPEECH_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        test = Loader.instance.getByClassType(SpeechToTextTest::class.java)

        tts = TextToSpeech(this, this)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)

//        progressBar!!.setVisibility(View.VISIBLE)
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech!!.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName())
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 7000)
//        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 7000)
//        recognizerIntent!!.putExtra("android.speech.extra.DICTATION_MODE", true)
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)

//        beepManager = BeepManager(this)
//        beepManager!!.updatePrefs()

        val micContainer = micTestLayout as ViewGroup

        microphoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
        micContainer.addView(microphoneView)

        val tv: TextView = findViewById(R.id.nameTextViewSpeech)

        microphoneView?.let { microphoneView ->
            microphoneView.nameTextView.text = context.getString(R.string.microphone)
            microphoneView.imageView.setImageResource(R.drawable.microphone)
//            microphoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.micQuality)?.value ?: Test.INIT))
            microphoneView.statusImageView.setImageResource(getImageForStatus(test!!.status))
            microphoneView.progressBar1.setVisibility(View.VISIBLE)
        }
        microphoneView?.setBackgroundResource(R.drawable.selector_row)

        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning){
                if(TEST_LOCK)
                {
                    return@setOnClickListener
                }
                TEST_LOCK = true

                //audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                speech = SpeechRecognizer.createSpeechRecognizer(this)
                speech!!.setRecognitionListener(this)
                microphoneView!!.progressBar1.setVisibility(View.VISIBLE)
                microphoneView!!.progressBar1.setIndeterminate(true)
                if(AudioInputTestActivity.isSpeakerWorking == true){
                    tv.setVisibility(View.INVISIBLE)
//                    beepManager = BeepManager(this)
//                    beepManager!!.updatePrefs()
                    startWithBeep()
                }
                else{
                    tv.setVisibility(View.VISIBLE)
                    startWithoutBeep()
                }
            }

        }

        startButton.setOnClickListener {
            if (!isAutoStartRunning){
                isAutoStartRunning = true
                if(TEST_LOCK)
                {
                    return@setOnClickListener
                }
                TEST_LOCK = true

                //audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                speech = SpeechRecognizer.createSpeechRecognizer(this)
                speech!!.setRecognitionListener(this)

                microphoneView!!.progressBar1.setVisibility(View.VISIBLE)
                microphoneView!!.progressBar1.setIndeterminate(true)
                if(AudioInputTestActivity.isSpeakerWorking == true) {
                    tv.setVisibility(View.INVISIBLE)
//                    beepManager = BeepManager(this)
//                    beepManager!!.updatePrefs()
                    startWithBeep()
                }
                else {
                    tv.setVisibility(View.VISIBLE)
                    startWithoutBeep()
                }
            }
        }

        val alreadyStarted2 = pref.getBoolean(SPEECH_AUTO_START_KEY, false)

        if (!alreadyStarted2){
            startButton.performClick()
            /*if(AudioInputTestActivity.isSpeakerWorking == true){
                if(TEST_LOCK)
                {
                    return
                }
                TEST_LOCK = true
                tv.setVisibility(View.INVISIBLE)
                beepManager = BeepManager(this)
                beepManager!!.updatePrefs()
                startWithBeep()
            }
            else{
                if(TEST_LOCK)
                {
                    return
                }
                TEST_LOCK = true
                tv.setVisibility(View.VISIBLE)
                startWithoutBeep()
            }*/
        }
    }

    private fun startWithoutBeep() {
        countDownTimerCallStarted = object : CountDownTimer(7000, 100){
            override fun onFinish() {
                speech!!.stopListening()
//                beepManager!!.close()
                microphoneView!!.progressBar1.setIndeterminate(false)
                microphoneView!!.progressBar1.setVisibility(View.INVISIBLE)
            }

            override fun onTick(millisUntilFinished: Long) {

            }

        }
        speech!!.startListening(recognizerIntent)
        countDownTimerCallStarted?.start()
    }

    private fun startWithBeep() {
        val handler = Handler()
        handler.postDelayed(Runnable {
            speakOut()
        }, 1000)

        countDownTimerCallStarted = object : CountDownTimer(7000, 100){
            override fun onFinish() {
                speech!!.stopListening()
//                beepManager!!.close()
                microphoneView!!.progressBar1.setIndeterminate(false)
                microphoneView!!.progressBar1.setVisibility(View.INVISIBLE)
            }

            override fun onTick(millisUntilFinished: Long) {

            }

        }
        speech!!.startListening(recognizerIntent)
        countDownTimerCallStarted?.start()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public override fun onResume(){
        super.onResume()
//        beepManager!!.updatePrefs()
        tts = TextToSpeech(this, this)
    }

    public override fun onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
//        beepManager!!.close()
//        releaseAudioRecorder()
        if (speech != null) {
            speech!!.destroy()
            audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            Log.d(LOG_TAG, "destroy")
        }
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
                SPEECH_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_speech), -1)] =
                            RecordTest(context.getString(R.string.report_speech_test), SPEECH_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("SpeechRecognition", "${SPEECH_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    public override fun onPause() {
        super.onPause()
//        beepManager!!.close()
//        releaseAudioRecorder()
        if (speech != null) {
            speech!!.destroy()
            audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            Log.d(LOG_TAG, "destroy")
        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "This Language is not supported")
            }

        } else {
            Log.d("TTS", "Initilization Failed!")
        }

    }

    private fun speakOut() {
        tts!!.speak("Please Say Hello", TextToSpeech.QUEUE_FLUSH, null)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(LOG_TAG, "onReadyForSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.d(LOG_TAG, "onRmsChanged: " + rmsdB)
//        progressBar!!.setProgress(rmsdB.toInt())
        microphoneView!!.progressBar1.setProgress(rmsdB.toInt())
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.d(LOG_TAG, "onBufferReceived: " + buffer);
    }

    override fun onPartialResults(partialResults: Bundle?) {
        Log.d(LOG_TAG, "onPartialResults")
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(LOG_TAG, "onEvent")
    }

    override fun onBeginningOfSpeech() {
        Log.d(LOG_TAG, "onBeginningOfSpeech")
//        progressBar!!.setIndeterminate(false)
//        progressBar!!.setMax(10)
        microphoneView!!.progressBar1.setIndeterminate(false)
        microphoneView!!.progressBar1.setMax(12)
    }

    override fun onEndOfSpeech() {
        Log.d(LOG_TAG, "onEndOfSpeech")
        microphoneView!!.progressBar1.setIndeterminate(false)
        microphoneView!!.progressBar1.setVisibility(View.INVISIBLE)
//        progressBar!!.setIndeterminate(true)
//        progressBar!!.setIndeterminate(false)
//        progressBar!!.setVisibility(View.INVISIBLE)
        speech!!.stopListening()
//        beepManager!!.close()
    }

    override fun onError(error: Int) {
        val errorMessage = getErrorText(error)
        Log.d(LOG_TAG, "FAILED " + errorMessage)
        microphoneView!!.progressBar1.setIndeterminate(false)
        microphoneView!!.progressBar1.setVisibility(View.INVISIBLE)
//        progressBar!!.setIndeterminate(false)
//        progressBar!!.setVisibility(View.INVISIBLE)
        speech!!.stopListening()
        if (speech != null) {
            speech!!.destroy()
            Log.d(LOG_TAG, "destroy")
        }
//        beepManager!!.close()
        if(errorMessage.equals("No speech input")){
            test?.status = Test.FAILED
            test?.sub(Test.micQuality)?.value = Test.FAILED
            microphoneView?.statusImageView?.setImageResource(getImageForStatus(Test.FAILED))
        }
        else if(errorMessage.equals("Didn't understand, please try again")){
            test?.status = Test.PASS
            test?.sub(Test.micQuality)?.value = Test.PASS
            microphoneView?.statusImageView?.setImageResource(getImageForStatus(Test.PASS))
        }
        else{
            test?.status = Test.PASS
            test?.sub(Test.micQuality)?.value = Test.FAILED
            microphoneView?.statusImageView?.setImageResource(getImageForStatus(Test.PASS))
        }
        TEST_LOCK = false
        resetAutoStart()
    }

    override fun onResults(results: Bundle?) {
        Log.i(LOG_TAG, "onResults")
        /*val matches = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        var text = ""
        for (result in matches)
            text += result + "\n"
        Toast.makeText(this, "Result are: " +text, Toast.LENGTH_SHORT).show()*/
        speech!!.stopListening()
//        beepManager!!.close()
        test?.status = Test.PASS
        test?.sub(Test.micQuality)?.value = Test.FAILED
        microphoneView?.statusImageView?.setImageResource(getImageForStatus(Test.PASS))
        TEST_LOCK = false
        resetAutoStart()
    }

    fun getErrorText(errorCode: Int): String {
        val message: String
        when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> message = "Audio recording error"
            //SpeechRecognizer.ERROR_CLIENT -> message = "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> message = "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> message = "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> message = "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> message = "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> message = "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> message = "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> message = "No speech input"
            else -> message = "Didn't understand, please try again."
        }
        return message
    }

    private fun resetAutoStart() {
        isAutoStartRunning = false
        pref.add(SpeechRecognization.SPEECH_AUTO_START_KEY, true)
        pref.save()
        testWatcher()
    }

    companion object {
        var REQ = 16
        private val AMPLITUDE_CHECKING = 2500.00
        val SPEECH_AUTO_START_KEY = "autoSpeechStart"
        val MIC_AMPLITUDE_PREF = "MIC_AMPLITUDE"
        var SPEECH_SCREEN_TIME = 0
    }
}
