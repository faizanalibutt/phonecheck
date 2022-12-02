package com.upgenicsint.phonecheck.activities

import android.content.*
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.BuildConfig
import com.farhanahmed.cabinet.Cabinet
import com.tyorikan.voicerecordingvisualizer.RecordingSampler

import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.MicCheckTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import github.nisrulz.zentone.ZenTone
import kotlinx.android.synthetic.main.activity_mic_check.*
import kotlinx.android.synthetic.main.mic_input_test_layout.view.*

class MicCheckTestActivity : BaseAudioTestActivity<MicCheckTest>(){
    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var recordingSampler: RecordingSampler? = null
    private var isAutoStartRunning = false
    private var runNext = false //flag to stop auto start next test when user select a single test
    private var earpiecePlay = false
    private var earpiecePlayVidMic = false
    private var beepPlaying = false
    var micCode: Int = 0
    var vidCode: Int = 0
    private var audioCounter = 0
    var isPlaying = false
    var ampSpeaker: Int = 0
    var ampEar: Int = 0
    var ampVidSpeaker:Int = 0
    var ampVidEar:Int = 0
    private var audioManager: AudioManager? = null
    private var audioManager2: AudioManager? = null
    private fun getVolumeForMode(mode: Int) = if (BuildConfig.DEBUG ) 5  else (audioManager!!.getStreamMaxVolume(mode) * 0.70).toInt()
    private val pref by lazy { Cabinet.open(context, R.string.mic_check) }

    var context2: Context? = null
        get() = this

    private var countDownTimerCallStarted: CountDownTimer? = null

    private var isButtonDisabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_mic_check)

        onCreateNav()

        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle(getString(R.string.mic_check_title))
        test = Loader.instance.getByClassType(MicCheckTest::class.java)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager2 = getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, getVolumeForMode(AudioManager.STREAM_MUSIC), 0)
        audioManager2!!.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager2!!.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),0)

        val micContainer = micTestLayout as ViewGroup

        microphoneView = LayoutInflater.from(context).inflate(R.layout.mic_input_test_layout, null)
        micContainer.addView(microphoneView)

        val resultsFilterMap = test?.resultsFilterMap

        if (resultsFilterMap != null) {
            if (resultsFilterMap[Test.videoMicTestKey] == true) {
                videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.mic_input_test_layout, null)
                micContainer.addView(videoMicrophoneView)
            }
        }
//        speakerStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.loudSpeakerTestKey)?.value ?: Test.INIT))

        microphoneView?.let { microphoneView ->
            microphoneView.nameTextView.text = context.getString(R.string.microphone)
            microphoneView.imageView.setImageResource(R.drawable.microphone)
            microphoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.micTestKey)?.value ?: Test.INIT))
            microphoneView.amplitudeTextView.text = "${pref.getInt(MIC_AMPLITUDE_PREF,0)}"
            microphoneView.amplitudeTextView2.text = "${pref.getInt(MIC_EAR_AMPLITUDE_PREF,0)}"
        }

        videoMicrophoneView?.let { videoMicrophoneView ->
            videoMicrophoneView.nameTextView.text = context.getString(R.string.video_microphone)
            videoMicrophoneView.imageView.setImageResource(R.drawable.microphone)
            videoMicrophoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.videoMicTestKey)?.value ?: Test.INIT))
            videoMicrophoneView.amplitudeTextView.text = "${pref.getInt(VID_MIC_AMPLITUDE_PREF,0)}"
            videoMicrophoneView.amplitudeTextView2.text = "${pref.getInt(VID_MIC_EAR_AMPLITUDE_PREF,0)}"

        }


        microphoneView?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)

        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                playBeep()
                runNext = false
                earpiecePlayVidMic = false
                earpiecePlay = false
            }
        }

        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                earpiecePlayVidMic = false
                earpiecePlay = false
                playBeepVid()
            }
        }

//        speakerLayout.setOnClickListener {
//            if (!isAutoStartRunning) {
//                runNext = false
//            }
//        }

        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                isAutoStartRunning = true
                runNext = true
                earpiecePlayVidMic = false
                earpiecePlay = false
                playBeep()
            }
        }
        val alreadyStarted = pref.getBoolean(AUDIO_AUTO_START_KEY, false)

        if (!alreadyStarted) {
            startButton.performClick()
            isButtonDisabled = true
        }
    }

    private fun microphoneTest() {
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.MIC, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
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
                if (recordingSampler?.audioSource == MediaRecorder.AudioSource.MIC) {
                    recordingSampler?.stopRecording()

                    if (!earpiecePlay) {
                        earpiecePlay = true
                        ampSpeaker = amplitude
                        microphoneView?.amplitudeTextView?.text = amplitude.toString()
                        amplitude = 0
                        playBeep()
                    }

                    else if (earpiecePlay){
                        earpiecePlay = false
                        ampEar = amplitude
                        microphoneView?.amplitudeTextView2?.text = amplitude.toString()
                        val value = if ((ampSpeaker > AMPLITUDE_CHECKING) && (ampEar > AMPLITUDE_CHECKING)) Test.PASS else Test.FAILED
                        test?.sub(Test.micTestKey)?.value = value
                        microphoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        pref.add(MIC_AMPLITUDE_PREF,ampSpeaker).save()
                        pref.add(MIC_EAR_AMPLITUDE_PREF,ampEar).save()
                        amplitude = 0
                        ampEar = 0
                        ampSpeaker = 0

                        if (runNext) {
                            if (videoMicrophoneView != null) {

                                playBeepVid()
                            }
                            else {
                                resetAutoStart()

                            }
                        }
                    }
                }
            }

        }
        countDownTimerCallStarted?.start()
        recordingSampler?.startRecording()
    }

    private fun playBeep() {
        if(TEST_LOCK)
        {
            return
        }
        TEST_LOCK = true

        ZenTone.getInstance().stop()

        Thread(Runnable {
            try {
                Thread.sleep(1500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                microphoneTest()
            }
            ZenTone.getInstance().generate2(1500, 4, 1f, earpiecePlay, context) {
            }

        }).start()
    }

    private fun playBeepVid(){
        if(TEST_LOCK)
        {
            return
        }
        TEST_LOCK = true

        ZenTone.getInstance().stop()

        Thread(Runnable {
            try {
                Thread.sleep(1500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                videoMicTest()
            }
            ZenTone.getInstance().generate2(1500, 4, 1f, earpiecePlayVidMic, context) {
            }

        }).start()
    }

    private fun videoMicTest() {
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.CAMCORDER, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(videoMicrophoneView?.visualizer)


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
                if (recordingSampler?.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                    recordingSampler?.stopRecording()

                    if (!earpiecePlayVidMic){
                        earpiecePlayVidMic = true
                        ampVidSpeaker = amplitude
                        videoMicrophoneView?.amplitudeTextView?.text = amplitude.toString()
                        amplitude = 0
                        playBeepVid()
                    }
                    else if (earpiecePlayVidMic){
                        ampVidEar = amplitude
                        videoMicrophoneView?.amplitudeTextView2?.text = amplitude.toString()
                        val value = if ((ampVidSpeaker > AMPLITUDE_CHECKING) && (ampVidEar > AMPLITUDE_CHECKING)) Test.PASS else Test.FAILED
                        test?.sub(Test.videoMicTestKey)?.value = value
                        videoMicrophoneView?.statusImageView?.setImageResource(getImageForStatus(value))
                        pref.add(VID_MIC_AMPLITUDE_PREF,ampVidSpeaker).save()
                        pref.add(VID_MIC_EAR_AMPLITUDE_PREF,ampVidEar).save()
                        amplitude = 0
                        ampVidEar = 0
                        ampVidSpeaker = 0

                        if (runNext){
                            resetAutoStart()
                        }
                    }
                }

            }

        }
        countDownTimerCallStarted?.start()
        recordingSampler?.startRecording()

    }

    private fun resetAutoStart() {
        isAutoStartRunning = false
        pref.add(AUDIO_AUTO_START_KEY, true)
        pref.save()
        testWatcher()
    }

    override fun onStop() {
        ZenTone.getInstance().stop()
        earpiecePlay = false
        earpiecePlayVidMic = false
        super.onStop()
    }

    override fun onDestroy() {
        ZenTone.getInstance().stop()
        countDownTimerCallStarted?.cancel()
        releaseAudioRecorder()
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
            recordingSampler = null
        }
    }
    companion object {
        val REQ = 37
        private val AMPLITUDE_CHECKING = 1500.00
        val AUDIO_AUTO_START_KEY = "autoMicCheckStart"
        val MIC_AMPLITUDE_PREF = "MICCHECK_AMPLITUDE_PREF"
        val VID_MIC_AMPLITUDE_PREF = "VID_MICCHECK_AMPLITUDE_PREF"
        val MIC_EAR_AMPLITUDE_PREF = "MICCHECK_EAR_AMPLITUDE_PREF"
        val VID_MIC_EAR_AMPLITUDE_PREF = "VID_MICCHECK_EAR_AMPLITUDE_PREF"
        var isSpeakerWorking = false
    }
}
