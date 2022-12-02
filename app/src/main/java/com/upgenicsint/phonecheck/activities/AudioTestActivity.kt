package com.upgenicsint.phonecheck.activities

import android.content.*
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.support.v4.content.LocalBroadcastManager
import android.text.InputType
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.tyorikan.voicerecordingvisualizer.RecordingSampler
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.broadcastreceiver.HeadSetPlugStatusReceiver
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.HeadSetPlugCallBack
import com.upgenicsint.phonecheck.misc.TextFieldListener
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AudioTest
import com.upgenicsint.phonecheck.utils.DialogUtils
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import com.upgenicsint.phonecheck.utils.Tools
import kotlinx.android.synthetic.main.activity_audio_test.*
import java.util.*

class AudioTestActivity : DeviceTestableActivity<AudioTest>(), HeadSetPlugCallBack {
    override fun onHeadSetAttachment(isAttached: Boolean) {

        if (isAttached) {
            isHeadsetPlugged = true
            test?.sub(Test.headsetPortKey)?.value = Test.PASS
            headSetStatusImageView.setImageResource(R.drawable.blue_check)
            testLayout.isClickable = false
            earphoneLayout.isClickable = false
            loudSpeakerLayout.isClickable = false
            microphoneLayout.isClickable = false

            headsetLayout.isClickable = true
            headsetLayout.alpha = 1f

            testLayout.alpha = 0.5f
            testLayout.isClickable = false
            headsetTest()
        }else{
            isHeadsetPlugged = false

            testLayout.alpha = 1f

            testLayout.isClickable = true
            headsetLayout.isClickable = false
            headsetLayout.alpha = 0.5f

            microphoneLayout.isClickable = true
            loudSpeakerLayout.isClickable = true
            earphoneLayout.isClickable = true
        }
    }

    private var headSetReceiver: HeadSetPlugStatusReceiver? = null
    var isHeadsetPlugged: Boolean = false
    private var countDownTimerCallStarted: CountDownTimer? = null
    private var randomNumber: Int = 0
    private var recordingSampler: RecordingSampler? = null
    private var isAutoAudioEnabled = Loader.instance.isAutoAudioEnabled
    private var TEST_LOCK = false
    private val TAG = AudioTestActivity::class.java.simpleName

    private val ttsPreparedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || intent.action == null) {
                return
            }
            when (intent.getStringExtra(TTSService.TTS_ACTION)) {
                TTSService.PLAY_MIC -> if (recordingSampler != null && countDownTimerCallStarted != null) {
                    countDownTimerCallStarted?.start()
                    recordingSampler?.startRecording()
                }
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
            TEST_LOCK = false
            if (intent == null || intent.action == null) {
                return
            }
            val action = intent.getStringExtra(TTSService.TTS_ACTION)
            if (action == TTSService.PLAY_MIC) {
                /*
                * if test was mic dont show any pop from here*/
                return
            }

            if (isAutoAudioEnabled) {
                val test = test ?: return
                showQuestionAlert(action.replace("PLAY_", "").replace("_", " "), object : TextFieldListener {
                    override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                        when (action) {
                            TTSService.PLAY_SPEAKER -> {
                                test.sub(Test.loudSpeakerTestKey)?.value = if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.loudSpeakerTestKey), speakerStatusImageView)
                                changeIconState(speakerImageView, R.drawable.speaker)
                            }
                            TTSService.PLAY_EARPIECE -> {
                                test.sub(Test.earphoneTestKey)?.value = if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.earphoneTestKey), earpieceStatusImageView)
                                changeIconState(earpieceImageView, R.drawable.ear_speaker)
                            }
                            TTSService.PLAY_HEADSET_LEFT -> {
                                test.sub(Test.headsetLeftKey)?.value = if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.headsetLeftKey), leftHeadSetStatusImageView)

                                //start service for right headset test
                                changeIconState(leftHeadSetImageView, R.drawable.left_headset)

                                changeIconState(rightHeadSetImageView, R.drawable.right_headset_pass)

                                val rightHeadsetIntent = Intent(context, TTSService::class.java)
                                rightHeadsetIntent.action = TTSService.PLAY_HEADSET_RIGHT
                                rightHeadsetIntent.putExtra(TTSService.RANDOM_NUMBER, singleDigitRandomText)
                                startService(rightHeadsetIntent)
                            }
                            TTSService.PLAY_HEADSET_RIGHT -> {
                                changeIconState(rightHeadSetImageView, R.drawable.right_headset)

                                test.sub(Test.headsetRightKey)?.value = if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.headsetRightKey), rightHeadSetStatusImageView)
                            }
                        }
                        testWatcher()
                        dialog.dismiss()
                    }

                })

            } else if (action == TTSService.PLAY_HEADSET_LEFT) {

                val rightHeadsetIntent = Intent(context, TTSService::class.java)
                rightHeadsetIntent.action = TTSService.PLAY_HEADSET_RIGHT
                rightHeadsetIntent.putExtra(TTSService.RANDOM_NUMBER, singleDigitRandomText)
                startService(rightHeadsetIntent)

            } else {
                showQuestionAlert(action.replace("RIGHT", "").replace("PLAY_", "").replace("_", " "), object : AlertButtonListener {
                    override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                        val test = test ?: return
                        val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                        when (action) {
                            TTSService.PLAY_SPEAKER -> {
                                test.sub(Test.loudSpeakerTestKey)?.value = if (isTrue) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.loudSpeakerTestKey), speakerStatusImageView)
                                changeIconState(speakerImageView, R.drawable.speaker)
                            }
                            TTSService.PLAY_EARPIECE -> {
                                test.sub(Test.earphoneTestKey)?.value = if (isTrue) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.earphoneTestKey), earpieceStatusImageView)
                                changeIconState(earpieceImageView, R.drawable.ear_speaker)
                            }
                            TTSService.PLAY_HEADSET_RIGHT -> {
                                changeIconState(rightHeadSetImageView, R.drawable.right_headset_pass)

                                test.sub(Test.headsetRightKey)?.value = if (isTrue) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.headsetRightKey), rightHeadSetStatusImageView)

                                changeIconState(leftHeadSetImageView, R.drawable.left_headset_pass)


                                test.sub(Test.headsetLeftKey)?.value = if (isTrue) Test.PASS else Test.FAILED
                                setImage(test.sub(Test.headsetLeftKey), leftHeadSetStatusImageView)
                            }
                        }
                        testWatcher()
                        dialog.dismiss()

                    }
                })
            }
        }
    }

    private val randomText: String
        get() {
            randomNumber = 1 + Random().nextInt(99)
            return randomNumber.toString()
        }

    private val singleDigitRandomText: String
        get() {
            randomNumber = 1 + Random().nextInt(9)
            return randomNumber.toString()
        }
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_test)
        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("Audio Test")
        test = Loader.instance.getByClassType(AudioTest::class.java)
        if (isAutoAudioEnabled) {
            audioPlayBackTextView.text = getString(R.string.playback_auto_mode)
        } else {
            audioPlayBackTextView.text = getString(R.string.playback_manual_mode)
        }

        if (TTSService.ttsStatus == TextToSpeech.SUCCESS) {
            mainView.visibility = View.VISIBLE
            loadingView.visibility = View.GONE
        } else {
            mainView.visibility = View.GONE
            loadingView.visibility = View.VISIBLE
        }
        FirebaseUtil.addNew(FirebaseUtil.AUDIO).child("isAutoAudioEnabled").setValue(isAutoAudioEnabled)

        ttsSetup()


        val onClick: (View) -> Unit = { view ->
            if (!TEST_LOCK) {
                TEST_LOCK = true
                when (view.id) {
                    R.id.loudSpeakerLayout -> if (!isHeadsetPlugged)
                        loudSpeakerTest()
                    else
                        TEST_LOCK = false
                    R.id.earphoneLayout -> if (!isHeadsetPlugged) {
                        earphoneTest()
                    } else
                        TEST_LOCK = false
                    R.id.microphoneLayout -> if (!isHeadsetPlugged) {
                        createAudioRecorder()
                        microphoneTest()
                    } else
                        TEST_LOCK = false
                    R.id.headsetLayout -> if (isHeadsetPlugged)
                        headsetTest()
                    else {
                        Toast.makeText(context, "Plug HeadSet", Toast.LENGTH_SHORT).show()
                        TEST_LOCK = false
                    }
                }
            } else {
                Toast.makeText(context, "Please Wait generating sound", Toast.LENGTH_LONG).show()
                Logy.d(TAG, "Test Lock " + TEST_LOCK)
            }
        }
        loudSpeakerLayout.setOnClickListener(onClick)
        earphoneLayout.setOnClickListener(onClick)
        microphoneLayout.setOnClickListener(onClick)
        headsetLayout.setOnClickListener(onClick)
    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//
//    }

    private fun ttsSetup() {

        val test = test
        if (test == null) {
            //test is null right now so finish this activity.
            finalizeTest()
            return
        }
        headSetReceiver = HeadSetPlugStatusReceiver(this)

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY) && packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Logy.d("AudioTestActivity", "Earpiece FEATURE_SENSOR_PROXIMITY")

            earphoneLayout.visibility = View.VISIBLE
        } else {
            earphoneLayout.visibility = View.GONE
        }

        FirebaseUtil.addNew(FirebaseUtil.AUDIO).child("earpiece").setValue(if (test.hasSubTest(Test.earphoneTestKey)) "Yes" else "No")

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            videoMicLayout.visibility = View.VISIBLE
        } else {
            videoMicLayout.visibility = View.GONE
        }

        FirebaseUtil.addNew(FirebaseUtil.AUDIO).child("videoMic").setValue(if (test.hasSubTest(Test.videoMicTestKey)) "Yes" else "No")


        setImage(test.sub(Test.loudSpeakerTestKey), speakerStatusImageView)
        setImage(test.sub(Test.micTestKey), micStatusImageView)

        if (test.hasSubTest(Test.earphoneTestKey)) {
            setImage(test.sub(Test.earphoneTestKey), earpieceStatusImageView)
        }

        if (test.hasSubTest(Test.videoMicTestKey)) {
            setImage(test.sub(Test.videoMicTestKey), videoMicStatusImageView)
        }

        if (test.resultsFilterMap[Test.headsetLeftKey] == true && test.resultsFilterMap[Test.headsetRightKey] == true) {
            setImage(test.sub(Test.headsetRightKey), rightHeadSetStatusImageView)
            setImage(test.sub(Test.headsetLeftKey), leftHeadSetStatusImageView)
        } else {
            headsetLayout.visibility = View.INVISIBLE
        }
    }


    private fun onAudioRecordError(sampler: RecordingSampler, e: String) {
        //Toast.makeText(getContext(),sampler.getAudioSource()+"",Toast.LENGTH_LONG).show();

        emptyVisualizerTextView.visibility = View.VISIBLE
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

    private fun createAudioRecorder() {
        emptyVisualizerTextView.visibility = View.GONE
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.MIC, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })


        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(visualizer)

    }

    private fun createVideoAudioRecorder() {
        emptyVisualizerTextView.visibility = View.GONE
        releaseAudioRecorder()
        recordingSampler = RecordingSampler(MediaRecorder.AudioSource.CAMCORDER, RecordingSampler.Listener { sampler, e -> onAudioRecordError(sampler, e) })
        recordingSampler?.setSamplingInterval(100)
        recordingSampler?.link(visualizer)
    }

    private fun releaseAudioRecorder() {
        val recordingSampler = recordingSampler
        if (recordingSampler != null) {
            if (recordingSampler.isRecording)
                recordingSampler.stopRecording()
            recordingSampler.release()
        }
    }

    private fun setImage(i: SubTest?, view: ImageView) {
        if (i != null) {

            if (i.value == Test.INIT) {
                view.setImageResource(R.drawable.warning)
            } else {
                view.setImageResource(if (i.value == Test.PASS) R.drawable.blue_check else R.drawable.not_working)
            }
        }

    }


    fun headsetTest() {
        changeIconState(leftHeadSetImageView, R.drawable.left_headset_pass)
        val intent = Intent(this, TTSService::class.java)
        intent.action = TTSService.PLAY_HEADSET_LEFT
        intent.putExtra(TTSService.RANDOM_NUMBER, singleDigitRandomText)
        startService(intent)
    }

    private fun microphoneTest() {
        changeIconState(micImageView, R.drawable.mic_pass)

        countDownTimerCallStarted = object : CountDownTimer(5000, 1000) {
            internal var completed = false

            override fun onTick(millisUntilFinished: Long) {
                val test = test ?: return
                timeTextView.visibility = View.VISIBLE
                timeTextView.text = String.format(Locale.getDefault(), "Complete in %1d sec", millisUntilFinished / 1000)
                val recordingSampler = recordingSampler
                if (visualizer != null && recordingSampler != null) {
                    micMiniLayout.visibility = View.VISIBLE
                    val amplitude: Double = recordingSampler.amplitude.toDouble()
                    Logy.d(TAG, "Amplitude " + amplitude)
                    if (amplitude > AMPLITUDE_CHECKING) {
                        micMiniLayout.visibility = View.VISIBLE
                        Logy.d(TAG, "Passed Amplitude : " + amplitude)
                        if (true) {
                            recordingSampler.stopRecording()
                            if (recordingSampler.audioSource == MediaRecorder.AudioSource.MIC) {
                                test.sub(Test.micTestKey)?.value = Test.PASS
                                micStatusTextView.text = amplitude.toString()
                                micStatusImageView.setImageResource(R.drawable.blue_check)
                                videoMicTest()
                            } else if (recordingSampler.audioSource == MediaRecorder.AudioSource.CAMCORDER) {
                                completed = true
                                videoMicStatusTextView.text = amplitude.toString()
                                test.sub(Test.videoMicTestKey)?.value = Test.PASS
                                videoMicStatusImageView.setImageResource(R.drawable.blue_check)
                                changeIconState(micImageView, R.drawable.mic)

                            }
                        }
                        testWatcher()

                        timeTextView.visibility = View.GONE

                        cancel()

                    }
                }

            }


            override fun onFinish() {
                micMiniLayout.visibility = View.VISIBLE
                timeTextView.visibility = View.GONE
                if (recordingSampler != null && !completed) {
                    if (recordingSampler?.audioSource == MediaRecorder.AudioSource.MIC) {
                        micStatusImageView.setImageResource(R.drawable.not_working)
                        videoMicTest()
                    } else {
                        videoMicStatusImageView.setImageResource(R.drawable.not_working)
                    }
                }
                testWatcher()
            }
        }

        val test = test
        if (recordingSampler != null && test != null) {

            if (test.sub(Test.loudSpeakerTestKey)?.value == Test.FAILED) {

                val confirmationAlert = DialogUtils.createConfirmationAlert(context, R.string.speaker_test_failed, R.string.say_hello_loud, "Cancel", "OK", object : AlertButtonListener {
                    override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                        if (type == AlertButtonListener.ButtonType.LEFT) {
                            if (recordingSampler != null) {
                                val intent = Intent(context, TTSService::class.java)
                                intent.action = TTSService.PLAY_MIC
                                intent.putExtra(TTSService.RANDOM_NUMBER, MICROPHONE_PHRASE)
                                startService(intent)
                                micMiniLayout.visibility = View.VISIBLE

                            }

                        } else {
                            TEST_LOCK = false
                            test.sub(Test.micTestKey)?.value = Test.FAILED
                            test.sub(Test.videoMicTestKey)?.value = Test.FAILED

                            setImage(test.sub(Test.micTestKey), micStatusImageView)
                            setImage(test.sub(Test.videoMicTestKey), videoMicStatusImageView)
                            changeIconState(micImageView, R.drawable.mic)
                        }
                        dialog.dismiss()
                    }
                })
                if (!isFinishing)
                    confirmationAlert.show()

            } else {
                if (recordingSampler != null) {
                    micMiniLayout.visibility = View.VISIBLE
                    val intent = Intent(context, TTSService::class.java)
                    intent.action = TTSService.PLAY_MIC
                    intent.putExtra(TTSService.RANDOM_NUMBER, MICROPHONE_PHRASE)
                    startService(intent)

                }
            }
        }

    }

    private fun videoMicTest() {
        val test = test ?: return

        if (test.hasSubTest(Test.videoMicTestKey)) {
            Handler().postDelayed({
                createVideoAudioRecorder()
                val intent = Intent(context, TTSService::class.java)
                intent.action = TTSService.PLAY_MIC
                intent.putExtra(TTSService.RANDOM_NUMBER, MICROPHONE_PHRASE)
                startService(intent)
                countDownTimerCallStarted?.cancel()
            }, 500)
        }
    }


    private fun testWatcher() {
        if (BaseActivity.autoPerform && checkTest() == Test.PASS) {
            finalizeTest()
        }

    }

    private fun earphoneTest() {
        val intent = Intent(this, TTSService::class.java)
        intent.action = TTSService.PLAY_EARPIECE
        intent.putExtra(TTSService.RANDOM_NUMBER, randomText)
        startService(intent)
        changeIconState(earpieceImageView, R.drawable.ear_speaker_pass)

    }

    private fun checkAnswer(s: String) = !(s.isEmpty() || !s.equals(randomNumber.toString(), ignoreCase = true))


    private fun showQuestionAlert(title: String, textFieldListener: TextFieldListener?) {

        val createTextFieldDialog = DialogUtils.createTextFieldDialog(context, title, "Cancel", "Enter", InputType.TYPE_CLASS_NUMBER, textFieldListener)

        if (!isFinishing)
            createTextFieldDialog.show()

    }

    private fun showQuestionAlert(title: String, textFieldListener: AlertButtonListener?) {

        //sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        val alertDialog = DialogUtils.createConfirmationAlert(context, title, getString(R.string.did_hear_sound), "Fail", "Pass", object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                //overide this becasue of Bad implementation from ios app
                textFieldListener?.onClick(dialog, type)
            }
        })
        if (!isFinishing)
            alertDialog.show()
    }

    private fun loudSpeakerTest() {

        val intent = Intent(this, TTSService::class.java)
        intent.action = TTSService.PLAY_SPEAKER
        intent.putExtra(TTSService.RANDOM_NUMBER, randomText)
        startService(intent)

        changeIconState(speakerImageView, R.drawable.speaker_pass)
    }

    private fun changeIconState(imageView: ImageView, res: Int) {
        Tools.flipView(imageView)
        imageView.setImageResource(res)
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

    public override fun onResume() {
        super.onResume()
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headSetReceiver, filter)

    }

    override fun onPause() {
        super.onPause()
        if (headSetReceiver != null)
            unregisterReceiver(headSetReceiver)
    }

    override fun onStop() {
        super.onStop()
        val recordingSampler = recordingSampler
        if (recordingSampler != null) {
            if (recordingSampler.isRecording)
                recordingSampler.stopRecording()
            recordingSampler.release()
        }

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

    companion object {
        val REQ = 10
        private val TTS_DATA_CHECK_CODE = 99
        private val AMPLITUDE_CHECKING = 500.00
        val MICROPHONE_PHRASE = "Testing one two three Testing"
    }
}
