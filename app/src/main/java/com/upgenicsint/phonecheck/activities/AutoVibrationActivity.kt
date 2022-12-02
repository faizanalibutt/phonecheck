package com.upgenicsint.phonecheck.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.media.AudioDataReceivedListener
import com.upgenicsint.phonecheck.media.RecordingThread
import com.upgenicsint.phonecheck.media.audio.calculators.AudioCalculator
import com.upgenicsint.phonecheck.misc.Constants
import com.upgenicsint.phonecheck.misc.Devices
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.sensor.AutoVibratorTest
import kotlinx.android.synthetic.main.activity_auto_vibrations.*
import java.util.*
import kotlin.collections.ArrayList

class AutoVibrationActivity : DeviceTestableActivity<AutoVibratorTest>() {

    private var audioCalculator: AudioCalculator? = null
    private var mRecordingThread: RecordingThread? = null
    private var amplitude: Int = 0
    private var frequency: Double = 0.0
    private var amplitudeMicList: MutableList<Int> = ArrayList()
    private var frequencyMicList: MutableList<Double> = ArrayList()
    private var mixedMicList: MutableList<String> = ArrayList()
    private var belowCounter = 0
    private var aboveCounter = 0
    private var countDownTimerCallStarted: CountDownTimer? = null // used for recording 5sec
    private var vibrator: Vibrator? = null
    private val dot: Long = 200
    private val dash: Long = 500
    private val pattern: LongArray = longArrayOf(1000, dot, dot, dash, dot, dash, dash, dot)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContentView(R.layout.activity_auto_vibrations)
        // setting Nav Bar adding test
        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("Vibration Test")
        audioCalculator = AudioCalculator()
        startVibrator.setOnLongClickListener {
            startActivity(Intent(this@AutoVibrationActivity, AmplitudeResultsDetail::class.java).putExtra(Constants.AUTO_VIBRATOR, true))
            return@setOnLongClickListener true
        }
        test = Loader.instance.getByClassType(AutoVibratorTest::class.java)
        /*Handler().postDelayed({
            releaseAudioRecorder()
            initCountDownTimer()
            RecordingThread.isMic = true
            mRecordingThread = RecordingThread(AudioDataReceivedListener { data ->
                if (audioCalculator != null && mRecordingThread != null && data != null && data.isNotEmpty() && data.size > 0) {
                    try {
                        visualizer!!.samples = data
                        audioCalculator!!.setBytes(RecordingThread.short2byte(data))
                        amplitude = audioCalculator!!.getAmplitude()
                    } catch (nullp: NullPointerException) {
                        nullp.printStackTrace()
                    }
                }
            })
            // start recording for 8 sec
            countDownTimerCallStarted?.start()
            if (!mRecordingThread!!.recording()) mRecordingThread!!.startRecording()
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 1))
                } else {
                    vibrator?.vibrate(pattern, 1)
                }
            }
        }, 2000)*/
    }

    fun startVibration(view: View) {
        releaseAudioRecorder()
        initCountDownTimer()
        RecordingThread.isMic = true
        mRecordingThread = RecordingThread(AudioDataReceivedListener { data ->
            if (audioCalculator != null && mRecordingThread != null && data != null && data.isNotEmpty() && data.size > 0) {
                try {
                    visualizer!!.samples = data
                    audioCalculator!!.setBytes(RecordingThread.short2byte(data))
                    amplitude = audioCalculator!!.amplitude
                    frequency = audioCalculator!!.frequency
                } catch (nullp: NullPointerException) {
                    nullp.printStackTrace()
                }
            }
        })
        // start recording for 8 sec
        countDownTimerCallStarted?.start()
        if (!mRecordingThread!!.recording()) mRecordingThread!!.startRecording()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    fun showResults(view: View) {
        startActivity(Intent(this@AutoVibrationActivity, AmplitudeResultsDetail::class.java).putExtra(Constants.AUTO_VIBRATOR, true))
    }

    private fun initCountDownTimer() {
        countDownTimerCallStarted = object : CountDownTimer(4000, 100) {
            override fun onTick(p0: Long) {
                liveThreshold.text = amplitude.toString()
                liveFrequency.text = frequency.toString()
                amplitudeMicList.add(amplitude)
                frequencyMicList.add(frequency)
                mixedMicList.add(amplitude.toString() + "\t\t\t\t" + frequency.toString())
            }

            override fun onFinish() {
                releaseAudioRecorder()
                setSpeakerResult()
                if (aboveCounter >= 3 && Devices.isMicrophoneSensitive()) {
                    amplitudeText.text = Collections.max(amplitudeMicList).toString()
                    status.setImageResource(R.drawable.blue_check)
                    test!!.status = Test.PASS
                } else if (aboveCounter >= 3 && belowCounter >= 3) {
                    amplitudeText.text = Collections.max(amplitudeMicList).toString()
                    status.setImageResource(R.drawable.blue_check)
                    test!!.status = Test.PASS
                } else if (aboveCounter >= 3) {
                    amplitudeText.text = Collections.max(amplitudeMicList).toString()
                    status.setImageResource(R.drawable.blue_check)
                    test!!.status = Test.PASS
                } else {
                    amplitudeText.text = Collections.max(amplitudeMicList).toString()
                    status.setImageResource(R.drawable.not_working)
                    test!!.status = Test.FAILED
                }
                //finalizeTest()
                val resultsPref: SharedPreferences.Editor = getSharedPreferences(getString(R.string.resultsPref), Context.MODE_PRIVATE).edit()
                resultsPref.putString(getString(R.string.autovibratorresultsList), mixedMicList.toString())
                resultsPref.apply()
                belowCounter = 0
                aboveCounter = 0
                if (amplitudeMicList.size > 0) amplitudeMicList.clear()
                if (frequencyMicList.size > 0) frequencyMicList.clear()
                if (mixedMicList.size > 0) mixedMicList.clear()
            }
        }
    }

    /**
     * Algorithm to automatically test vibrator
     */
    private fun setSpeakerResult() {
        val thresholdSize = 10
        val thresholdStart = 0
        val thresholdEnd: Int = Math.min(thresholdStart + thresholdSize, amplitudeMicList.size)
        val thresholdSublist: MutableList<Int> = amplitudeMicList.subList(thresholdStart, thresholdEnd)
        var threshold: Long = 0
        for (i: Int in thresholdSublist.indices) {
            try {
                threshold += thresholdSublist.get(i)
            } catch (excep: Exception) {
                excep.printStackTrace()
            }
        }
        threshold /= thresholdSublist.size
        Log.d("Results", threshold.toString())
        val size = 10
        var isCounterAbove = false
        var isCounterBelow = false
        var isTone1 = false
        var isTone2 = false
        var isTone3 = false
        var start = 10
        if (amplitudeMicList.size > 0) {
            while (start < amplitudeMicList.size) {
                val end: Int = Math.min(start + size, amplitudeMicList.size)
                val sublist: MutableList<Int> = amplitudeMicList.subList(start, end)

                if (aboveCounter == 3 || belowCounter == 3) {
                    break
                }

                if (Collections.min(sublist) < threshold) {
                    belowCounter += 1
                    Log.v("Results", "passedB $belowCounter")
                    isCounterBelow = true
                }

                if (Collections.max(sublist) >= threshold) {
                    aboveCounter += 1
                    Log.v("Results", "passedA $aboveCounter")
                    isCounterAbove = true
                }

                if (isCounterAbove && aboveCounter == 1 && isCounterBelow && belowCounter == 1) {
                    Log.v("Results", "Mic reached threshold 1")
                    isTone1 = true
                    isCounterAbove = false
                    isCounterBelow = false
                } else {
                    Log.v("Results", "Mic not reached threshold 1")
                }

                if (isCounterAbove && aboveCounter == 1) {
                    isCounterAbove = false
                }

                if (isCounterBelow && belowCounter == 1) {
                    isCounterBelow = false
                }

                if (isCounterAbove && aboveCounter == 2 && isCounterBelow && belowCounter == 2) {
                    Log.v("Results", "Mic reached threshold 2")
                    isTone2 = true
                    isCounterAbove = false
                    isCounterBelow = false
                } else {
                    Log.v("Results", "Mic not reached threshold 2")
                }

                if (isCounterAbove && aboveCounter == 2) {
                    isCounterAbove = false
                }

                if (isCounterBelow && belowCounter == 2) {
                    isCounterBelow = false
                }

                if (isCounterAbove && aboveCounter == 3 && isCounterBelow && belowCounter == 3) {
                    Log.v("Results", "Mic reached threshold 3")
                    isTone3 = true
                    isCounterAbove = false
                    isCounterBelow = false
                } else {
                    Log.v("Results", "Mic not reached threshold 3")
                }

                if (isCounterAbove && aboveCounter == 3) {
                    isCounterAbove = false
                }

                if (isCounterBelow && belowCounter == 3) {
                    isCounterBelow = false
                }

                Log.v("Results", sublist.toString())
                start += size
            }
        }
    }

    private fun releaseAudioRecorder() {
        if (vibrator != null) {
            vibrator?.cancel()
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

    override fun onPause() {
        super.onPause()
        releaseAudioRecorder()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAudioRecorder()
    }

    companion object {
        val REQ = 23
    }

}
