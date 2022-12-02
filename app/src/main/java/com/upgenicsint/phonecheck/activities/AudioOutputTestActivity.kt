package com.upgenicsint.phonecheck.activities

import android.content.*
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.broadcastreceiver.HeadSetPlugStatusReceiver
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.misc.HeadSetPlugCallBack
import com.upgenicsint.phonecheck.misc.TextFieldListener
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AudioOutputTest
import com.upgenicsint.phonecheck.utils.DialogUtils
import kotlinx.android.synthetic.main.activity_audio_output_test.*
import java.util.*

class AudioOutputTestActivity : BaseAudioTestActivity<AudioOutputTest>(), HeadSetPlugCallBack {

    private val TAG = AudioOutputTestActivity::class.java.simpleName
    private var headSetReceiver: HeadSetPlugStatusReceiver? = null
    override fun onHeadSetAttachment(isAttached: Boolean) {

        earpieceLayout.isClickable = !isAttached
        earpieceLayout.isEnabled = !isAttached

        headsetLayout.isClickable = isAttached
        headsetLayout.isEnabled = isAttached


        headsetLayout.alpha = if (isAttached) 1f else 0.5f
        earpieceLayout.alpha = if (!isAttached) 1f else 0.5f

        if (isAttached) {
            headsetLayout.performClick()
        }


    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_output_test)

        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle(getString(R.string.audioOpt_nav_title))

        test = Loader.instance.getByClassType(AudioOutputTest::class.java)


        if (test?.hasSubTest(Test.earphoneTestKey) == false) {
            earpieceLayout.visibility = View.GONE
        } else {
            earpieceStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.earphoneTestKey)?.value ?: Test.INIT))
            earpieceLayout.setOnClickListener {
                randomNumber = generateNewNumber(false)
                val intent = Intent(this, TTSService::class.java)
                intent.action = TTSService.PLAY_EARPIECE
                intent.putExtra(TTSService.RANDOM_NUMBER, randomNumber.toString())
                startService(intent)
            }
        }

        headsetStatusImageView.setImageResource(getImageForStatus(test?.sub(Test.headsetLeftKey)?.value ?: Test.INIT))



        headsetLayout.setOnClickListener {
            headsetLeftCode = generateNewNumber(true)
            val intent = Intent(this, TTSService::class.java)
            intent.action = TTSService.PLAY_HEADSET_LEFT
            intent.putExtra(TTSService.RANDOM_NUMBER, headsetLeftCode.toString())
            startService(intent)
        }



        headSetReceiver = HeadSetPlugStatusReceiver(this)

        if (test?.hasSubTest(Test.headsetLeftKey) == true && test?.hasSubTest(Test.headsetRightKey) == true) {
            headsetLayout.visibility = View.VISIBLE

        } else {
            headsetLayout.visibility = View.GONE
        }

        headsetLayout.isClickable = false
        headsetLayout.isEnabled = false
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

        stopPlayback()


        if (ttsCompleteReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(ttsCompleteReceiver)
        }

    }

    override fun onStart() {
        super.onStart()
        val ttsCompleteFilter = IntentFilter(TTSService.SEND_TTS_ON_COMPLETE)
        LocalBroadcastManager.getInstance(context).registerReceiver(ttsCompleteReceiver, ttsCompleteFilter)
    }

    private var headsetLeftCode = -1;
    private var headsetRightCode = -1;
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

            }
            else if (isAutoAudioEnabled) {
                var title = action.replace("PLAY_", "").replace("_", " ")
                if(title.contains("EARPIECE")){
                    title = getString(R.string.earpiece_popup)
                }
                else if(title.contains("HEADSET RIGHT")){
                    title = getString(R.string.headsetright_popup)
                }
                else if(title.contains("HEADSET")){
                    title = getString(R.string.headtset_popup)
                }
                showQuestionAlert(title, object : TextFieldListener {
                    override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                        val value = if (action == TTSService.PLAY_HEADSET_RIGHT) {
                            if (isTrue && "$headsetLeftCode$headsetRightCode" == text) Test.PASS else Test.FAILED
                        } else {
                            if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                        }

                        handleUserSelection(action, value)
                        dialog.dismiss()

                        testWatcher()
                    }
                })

            }
            else if (isAutoEarPieceEnabled && action.containsIgnoreCase("PLAY_EARPIECE")){
                var title = action.replace("PLAY_", "").replace("_", " ")
                if(title.contains("EARPIECE")){
                    title = getString(R.string.earpiece_popup)
                }
                else if(title.contains("HEADSET RIGHT")){
                    title = getString(R.string.headsetright_popup)
                }
                else if(title.contains("HEADSET")){
                    title = getString(R.string.headtset_popup)
                }
                showQuestionAlert(title, object : TextFieldListener {
                    override fun onClick(dialog: DialogInterface, text: String, isTrue: Boolean) {
                        val value = if (action == TTSService.PLAY_HEADSET_RIGHT) {
                            if (isTrue && "$headsetLeftCode$headsetRightCode" == text) Test.PASS else Test.FAILED
                        }
                        else {
                            if (isTrue && checkAnswer(text)) Test.PASS else Test.FAILED
                        }

                        handleUserSelection(action, value)
                        dialog.dismiss()

                        testWatcher()
                    }
                })
            }
            else {
                val message = if (action == TTSService.PLAY_EARPIECE) getString(R.string.earpiece_alerttext) else getString(R.string.did_hear_sound)
                var title = action.replace("RIGHT", "").replace("PLAY_", "").replace("_", " ")
                if(title.contains("EARPIECE")){
                    title = getString(R.string.earpiece_popup)
                }
                else if(title.contains("HEADSET RIGHT")){
                    title = getString(R.string.headsetright_popup)
                }
                else if(title.contains("HEADSET")){
                    title = getString(R.string.headtset_popup)
                }
                showQuestionAlert(title, message, object : AlertButtonListener {
                    override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                        val isTrue = type == AlertButtonListener.ButtonType.RIGHT
                        val value = if (isTrue) Test.PASS else Test.FAILED
                        handleUserSelection(action, value)
                        dialog.dismiss()

                        testWatcher()
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


    companion object {
        val REQ = 17
    }
}
