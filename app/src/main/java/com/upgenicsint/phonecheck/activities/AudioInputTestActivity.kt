package com.upgenicsint.phonecheck.activities

import android.content.*
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.Cabinet
import com.tyorikan.voicerecordingvisualizer.RecordingSampler
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.TextFieldListener
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AudioInputTest
import com.upgenicsint.phonecheck.toast
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_audio_input_test.*
import kotlinx.android.synthetic.main.audio_input_test_layout.view.*

class AudioInputTestActivity : BaseAudioTestActivity<AudioInputTest>() {

    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var recordingSampler: RecordingSampler? = null
    private var isAutoStartRunning = false
    private var runNext = false //flag to stop auto start next test when user select a single test
    private val pref by lazy { Cabinet.open(context, R.string.audio_pref) }

    var context2: Context? = null
        get() = this

    private var countDownTimerCallStarted: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_input_test)
        onCreateNav()

        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle(getString(R.string.audiInp_nav_title))
        test = Loader.instance.getByClassType(AudioInputTest::class.java)

        randomNumber = generateNewNumber(false)

        val micContainer = micTestLayout as ViewGroup

        microphoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
        micContainer.addView(microphoneView)

        val resultsFilterMap = test?.resultsFilterMap

        if (resultsFilterMap != null) {
            if (resultsFilterMap[Test.videoMicTestKey] == true) {
                videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.audio_input_test_layout, null)
                micContainer.addView(videoMicrophoneView)
            }
        }

        speakerStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.loudSpeakerTestKey)?.value ?: Test.INIT))

        microphoneView?.let { microphoneView ->
            microphoneView.nameTextView.text = context.getString(R.string.microphone)
            microphoneView.imageView.setImageResource(R.drawable.microphone)
            microphoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.micTestKey)?.value ?: Test.INIT))
            microphoneView.amplitudeTextView.text = "${pref.getInt(MIC_AMPLITUDE_PREF,0)}"
        }

        videoMicrophoneView?.let { videoMicrophoneView ->
            videoMicrophoneView.nameTextView.text = context.getString(R.string.video_microphone)
            videoMicrophoneView.imageView.setImageResource(R.drawable.microphone)
            videoMicrophoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.videoMicTestKey)?.value ?: Test.INIT))
            videoMicrophoneView.amplitudeTextView.text = "${pref.getInt(VID_MIC_AMPLITUDE_PREF,0)}"

        }


        microphoneView?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)

        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                microphoneTest()
            }
        }

        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                videoMicTest()
            }
        }

        speakerLayout.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                loudSpeakerTest()
            }
        }

        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                isAutoStartRunning = true
                runNext = true
                microphoneTest()
            }

        }


        val alreadyStarted = pref.getBoolean(AUDIO_AUTO_START_KEY, false)

        if (!alreadyStarted) {
            startButton.performClick()
        }
    }

    private fun microphoneTest() {
        if(TEST_LOCK)
        {
            return
        }
        TEST_LOCK = true
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.MIC, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(microphoneView?.visualizer)


        countDownTimerCallStarted = object : CountDownTimer(5000, 100) {

            var amplitude = 0
            override fun onTick(millisUntilFinished: Long) {

                val amp = recordingSampler?.amplitude ?: 0

                if (amp > amplitude)
                {
                    amplitude = amp
                }

            }

            override fun onFinish() {
                TEST_LOCK = false
                val value = if (amplitude > AMPLITUDE_CHECKING) Test.PASS else Test.FAILED

                if (recordingSampler?.audioSource == MediaRecorder.AudioSource.MIC) {
                    test?.sub(Test.micTestKey)?.value = value
                    microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    microphoneView?.amplitudeTextView?.text = amplitude.toString()

                    pref.add(MIC_AMPLITUDE_PREF,amplitude).save()

                    amplitude = 0
                    recordingSampler?.stopRecording()
                    if (runNext) {
                        if (videoMicrophoneView != null) {

                            videoMicTest()
                        } else {
                            showSpeakerPopup()

                        }
                    }

                }
                else if (recordingSampler?.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                    releaseAudioRecorder()
                    test?.sub(Test.videoMicTestKey)?.value = value
                    videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                    videoMicrophoneView?.amplitudeTextView?.text = amplitude.toString()
                    pref.add(VID_MIC_AMPLITUDE_PREF,amplitude).save()
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


    }

    private fun videoMicTest() {

        if(TEST_LOCK)
        {
            return
        }
        TEST_LOCK = true
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.CAMCORDER, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(videoMicrophoneView?.visualizer)


        val intent = Intent(this, TTSService::class.java)
        intent.action = TTSService.PLAY_VIDEO_MIC
        intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
        startService(intent)
    }

    private fun loudSpeakerTest() {

        if(TEST_LOCK)
        {
            return
        }
        TEST_LOCK = true
        val intent = Intent(this, TTSService::class.java)
        intent.action = TTSService.PLAY_SPEAKER
        intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
        startService(intent)
    }

    override fun onStop() {
        super.onStop()


        if (ttsCompleteReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsCompleteReceiver)
        }

        if (ttsPreparedReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsPreparedReceiver)
        }
        if (ttsErrorReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsErrorReceiver)
        }


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

            if (action == TTSService.PLAY_SPEAKER) {
                TEST_LOCK = false
                if (isAutoAudioEnabled) {
                    var title = action.replace("PLAY_", "").replace("_", " ")
                    if(title.contains("SPEAKER")){
                        title = getString(R.string.speaker_popup)
                    }
                    showQuestionAlert(title, object : TextFieldListener {
                        override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                            val value = if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                            handleUserSelection(action, value)
                            dialog.dismiss()

                            resetAutoStart()
                        }

                    })

                }
                else {
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
                }
            }

        }
    }


    private fun showSpeakerPopup() {
        val speakerIntent = Intent(TTSService.PLAY_SPEAKER)
        speakerIntent.putExtra(TTSService.TTS_ACTION, TTSService.PLAY_SPEAKER)
        ttsCompleteReceiver.onReceive(activity, speakerIntent)
    }

    private fun resetAutoStart() {
        isAutoStartRunning = false
        pref.add(AUDIO_AUTO_START_KEY, true)
        pref.save()
        randomNumber = generateNewNumber(false)


        testWatcher()
    }

    private fun handleUserSelection(action: String, value: Int) {
        when (action) {
            TTSService.PLAY_SPEAKER -> {
                test?.sub(Test.loudSpeakerTestKey)?.value = value
                if(value == Test.PASS){
                    isSpeakerWorking = true
                }
                speakerStatusImageView.setImageResource(getImageForStatus(value))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAudioRecorder()
    }

    private fun releaseAudioRecorder() {

        if (recordingSampler != null) {
            if (recordingSampler?.isRecording == true)
                recordingSampler?.stopRecording()
            recordingSampler?.release()
            recordingSampler = null
        }
    }

    private fun onAudioRecordError(sampler: RecordingSampler, e: String) {
        //Toast.makeText(getContext(),sampler.getAudioSource()+"",Toast.LENGTH_LONG).show()

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


    companion object {
        val REQ = 16
        private val AMPLITUDE_CHECKING = 500.00
        val AUDIO_AUTO_START_KEY = "autoAudioStart"
        val MIC_AMPLITUDE_PREF = "MIC_AMPLITUDE_PREF"
        val VID_MIC_AMPLITUDE_PREF = "VID_MIC_AMPLITUDE_PREF"
        var isSpeakerWorking = false
    }
}
