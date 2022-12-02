package com.upgenicsint.phonecheck.activities

import android.content.Context
import android.content.DialogInterface
import android.media.*
import android.media.AudioTrack.MODE_STREAM
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AudioPlaybackTest
import kotlinx.android.synthetic.main.activity_audio_playback_test.*
import kotlinx.android.synthetic.main.mic_playback_test_layout.*
import kotlinx.android.synthetic.main.mic_playback_test_layout.view.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import android.os.Environment.getExternalStorageDirectory
import android.os.Handler
import android.widget.ImageView
import com.farhanahmed.cabinet.Cabinet
import com.upgenicsint.phonecheck.Loader.Companion.baseFile
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.models.RecordTest
import kotlinx.android.synthetic.main.activity_main.view.*
import java.util.*
import kotlin.jvm.internal.Intrinsics


class AudioPlaybackTestActivity : BaseAudioTestActivity<AudioPlaybackTest>() {

    private var microphoneView: View? = null
    private var videoMicrophoneView: View? = null
    private var microphonePlay: ImageView? = null
    private var videoMicrophonePlay: ImageView? = null
    private var isAutoStartRunning = false
    private var runNext = false //flag to stop auto start next test when user select a single test

    private val RECORDER_SAMPLERATE = 44100
    private val RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val pref by lazy { Cabinet.open(context, R.string.mic_ls) }

    val fileMicrophone = "${Loader.baseFile}/microphoneTemp.pcm"
    val fileVidMicrophone = "${Loader.baseFile}/videoMicrophoneTemp.pcm"

    var micClicked = false
    var vidMicClicked = false

    internal var audioManager: AudioManager? = null

    private fun getVolumeForMode(mode: Int): Int {
        val volume: Int
        if (BuildConfig.DEBUG) {
            volume = (audioManager!!.getStreamMaxVolume(mode).toDouble() * 1f).toInt()
        } else {
            val audioManager = this.audioManager
            if (this.audioManager == null) {
                Intrinsics.throwNpe()
            }
            volume = (audioManager!!.getStreamMaxVolume(mode).toDouble() * 1f).toInt()
        }

        return volume
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_playback_test)
        onCreateNav()

        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle(getString(R.string.audio_playback_title))

        Loader.TIME_VALUE = 0
        MIC_PLAYBACK_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        test = Loader.instance.getByClassType(AudioPlaybackTest::class.java)

        audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, getVolumeForMode(AudioManager.STREAM_MUSIC), 0)

        val micContainer = micTestLayout as ViewGroup

        microphoneView = LayoutInflater.from(context).inflate(R.layout.mic_playback_test_layout, null)
        micContainer.addView(microphoneView)

        val resultsFilterMap = test?.resultsFilterMap

        if (resultsFilterMap != null) {
            if (resultsFilterMap[Test.vidMicPlaybackTestKey] == true) {
                videoMicrophoneView = LayoutInflater.from(context).inflate(R.layout.mic_playback_test_layout, null)
                micContainer.addView(videoMicrophoneView)
            }
        }

        microphoneView?.let { microphoneView ->
            microphoneView.nameTextView.text = context.getString(R.string.microphone)
            microphoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.micPlaybackTestKey)?.value
                    ?: Test.INIT))
        }

        videoMicrophoneView?.let { videoMicrophoneView ->
            videoMicrophoneView.nameTextView.text = context.getString(R.string.video_microphone)
            videoMicrophoneView.statusImageView.setImageResource(getImageForStatus(test?.sub(Test.vidMicPlaybackTestKey)?.value
                    ?: Test.INIT))

        }

        microphoneView?.setBackgroundResource(R.drawable.selector_row)
        videoMicrophoneView?.setBackgroundResource(R.drawable.selector_row)

        microphonePlay = microphoneView?.findViewById(R.id.micPlay)
        videoMicrophonePlay = videoMicrophoneView?.findViewById(R.id.micPlay)

        microphoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                if (micFile.exists()) {
                    microphonePlay?.setImageResource(R.drawable.pause)
                    micClicked = true
                    (Thread(Runnable {
                        try {
                            Thread.sleep(500)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (activity != null) {
                            activity!!.runOnUiThread {
                                playMicRecording(fileMicrophone)
                            }}
                    })).start()
                }
            }
        }

        videoMicrophoneView?.setOnClickListener {
            if (!isAutoStartRunning) {
                runNext = false
                if (vidMicFile.exists()) {
                    videoMicrophonePlay?.setImageResource(R.drawable.pause)
                    vidMicClicked = true
                    (Thread(Runnable {
                        try {
                            Thread.sleep(500)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (activity != null) {
                            activity!!.runOnUiThread {
                                playMicRecording(fileVidMicrophone)
                            }}
                    })).start()
                }
            }
        }

        startButton.setOnClickListener {
            if (!isAutoStartRunning) {
                isAutoStartRunning = true
                runNext = true
                if (micFile.exists()) {
                    microphonePlay?.setImageResource(R.drawable.pause)
                    micClicked = true
                    (Thread(Runnable {
                        try {
                            Thread.sleep(500)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (activity != null) {
                            activity!!.runOnUiThread {
                                playMicRecording(fileMicrophone)
                            }}
                    })).start()
                }
            }
        }

        val alreadyStarted = pref.getBoolean(MIC_PLAYBACK, false)
        if (!alreadyStarted && MainActivity.auto_start_mode) {
            Handler().postDelayed({
                startButton.performClick()
            }, 700)
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
                MIC_PLAYBACK_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_micplayback), -1)] =
                            RecordTest(context.getString(R.string.report_micplayback_test), MIC_PLAYBACK_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("MicPlayBack", "${MIC_PLAYBACK_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    private fun playMicRecording(filePath: String) {
        if (filePath == null)
            return

        val file = File(filePath)
        var time: Int
        var byteData = ByteArray(0)

        if (micClicked && (file.length().toInt() / RECORDER_SAMPLERATE / 2) > 5) {
            time = (file.length().toInt() / RECORDER_SAMPLERATE / 2) - 5
            Log.d(TAG, time.toString())
            time *= (RECORDER_SAMPLERATE * 2)
            byteData = ByteArray(time)
        } else if (micClicked && (file.length().toInt() / RECORDER_SAMPLERATE / 2) > 0) {
            byteData = ByteArray(file.length().toInt())
        }
//        byteData = ByteArray(file.length().toInt())

        if (vidMicClicked && (file.length().toInt() / RECORDER_SAMPLERATE / 2) > 4)  {
            time = (file.length().toInt() / RECORDER_SAMPLERATE / 2) - 2
            Log.d(TAG, time.toString())
            time *= (RECORDER_SAMPLERATE * 2)
            byteData = ByteArray(time)
        } else if (vidMicClicked && (file.length().toInt() / RECORDER_SAMPLERATE / 2) > 0) {
            time = (file.length().toInt() / RECORDER_SAMPLERATE / 2) - 1
            Log.d(TAG, time.toString())
            time *= (RECORDER_SAMPLERATE * 2)
            byteData = ByteArray(time)
        }

        var input: FileInputStream? = null
        try {
            input = FileInputStream(file)
            input!!.read(byteData)
            input.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        // Set and push to audio track..
        val intSize = android.media.AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING)

        val at = AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, intSize, MODE_STREAM)
        if (at != null) {
            at!!.setPlaybackRate(44100)
            at!!.play()
            // Write the byte array to the track
            at!!.write(byteData, 0, byteData.size)

            at!!.stop()
            at!!.release()
        }
        Thread(Runnable {
            try {
                Thread.sleep(800)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            if (activity != null) {
                activity!!.runOnUiThread {
                    showSpeakerPopup()
                }}
        }).start()
    }

    private fun showSpeakerPopup() {
        var title = "Recording Playback"

        showQuestionAlert(title, getString(R.string.did_hear_sound_speaker), object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                val value = if (isTrue) Test.PASS else Test.FAILED

                if (micClicked) {
                    micClicked = false
                    dialog.dismiss()
                    test?.sub(Test.micPlaybackTestKey)?.value = value
                    microphoneView!!.statusImageView.setImageResource(getImageForStatus(value))
                    microphonePlay?.setImageResource(R.drawable.play)
                    if (runNext) {
                        if (vidMicFile.exists()) {
                            (Thread(Runnable {
                                try {
                                    Thread.sleep(1300)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                vidMicClicked = true
                                if (activity != null) {
                                    activity!!.runOnUiThread {
                                        videoMicrophonePlay?.setImageResource(R.drawable.pause)
                                        playMicRecording(fileVidMicrophone)
                                    }
                                }
                            })).start()
                        }
                    }
                } else if (vidMicClicked) {
                    vidMicClicked = false
                    dialog.dismiss()
                    test?.sub(Test.vidMicPlaybackTestKey)?.value = value
                    videoMicrophoneView!!.statusImageView.setImageResource(getImageForStatus(value))
                    videoMicrophonePlay?.setImageResource(R.drawable.play)
                    TEST_LOCK = false
                    resetAutoStart()
                }

                /*(Thread(Runnable {
                    try {
                        Thread.sleep(1000)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    runOnUiThread {

                    }
                })).start()*/

            }
        })
    }

    private fun resetAutoStart() {
        isAutoStartRunning = false
        pref.add(MIC_PLAYBACK, true)
        pref.save()
        testWatcher()
    }

    companion object {
        val REQ = 777
        val MIC_PLAYBACK = "autoMicPlayback"
        var MIC_PLAYBACK_SCREEN_TIME = 0
        val TAG = "FUCK"
        @JvmStatic
        val micFile = File("$baseFile/microphoneTemp.pcm")
        @JvmStatic
        val vidMicFile = File("$baseFile/videoMicrophoneTemp.pcm")
    }

}