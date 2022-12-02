package com.upgenicsint.phonecheck.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.content.LocalBroadcastManager
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.AudioInputTestActivity
import com.upgenicsint.phonecheck.activities.MicLSTestActivity
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import java.io.File
import java.util.*

class TTSService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = MediaPlayer()
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val audioPath = "${Loader.baseFile}/audio_test.wav"
    private val audioFile = File(audioPath)
    var intent_action = "PLAY_EARPIECE"
    private val isLGAndroidKitkatDevice = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT && Build.BRAND.equals("LGE", ignoreCase = true)

    private fun getVolumeForMode(mode: Int) : Int
    {
        return AudioManagerWrapper.getVolumeForMode(mode, audioManager)
    }

    val context: Context
        get() = this

    override fun onCreate() {
        super.onCreate()
        ttsSetup()
    }

    private fun ttsSetup() {
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onBind(intent: Intent) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) {
            return Service.START_STICKY
        }
        when (intent.action) {
            PLAY_MIC, PLAY_VIDEO_MIC -> {
                loudSpeakerTest()
                playerSetup(1, intent, "mic")
            }
            PLAY_SPEAKER -> {
                loudSpeakerTest()
                playerSetup(0, intent, context.getString(R.string.speaker_tts))
            }
            PLAY_EARPIECE -> {
                earphoneTest()
                playerSetup(0, intent, context.getString(R.string.earpiece_tts))
            }
            PLAY_HEADSET_LEFT -> {
                setHeadSetLeftSettings()
                playerSetup(0, intent, context.getString(R.string.headset_left_tts))
            }
            PLAY_HEADSET_RIGHT -> {
                setHeadSetRightSettings()
                playerSetup(0, intent, context.getString(R.string.headset_right_tts))
            }
            STOP_PLAYBACK -> {
                player?.stop()
            }
        }

        return Service.START_NOT_STICKY
    }

    private fun playerSetup(i: Int, intent: Intent, from: String) {

        val number = intent.getStringExtra(TTSService.RANDOM_NUMBER)
        playTestSound(intent, number)
    }

    private fun sendErrorBroadCast(from: String) {
        val onErrorIntent = Intent(TTSService.SEND_TTS_ON_ERROR)
        onErrorIntent.putExtra(TTSService.ERROR_TYPE, from)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(onErrorIntent)
    }


    private val onPreparedListener: (Intent) -> Unit = {
        val onPrepIntent = Intent(TTSService.SEND_TTS_ON_PREPARED)
        onPrepIntent.putExtra(TTSService.TTS_ACTION, it.action)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(onPrepIntent)
    }
    private val onCompletionListener: (MediaPlayer, Intent) -> Unit = { mp, intent ->
        val onComIntent = Intent(TTSService.SEND_TTS_ON_COMPLETE)
        onComIntent.putExtra(TTSService.TTS_ACTION, intent.action)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(onComIntent)
    }

    private val onErrorListener: (Intent, String) -> Boolean = { intent, from ->
        sendErrorBroadCast(from)
        false
    }

    private fun playTestSound(intent: Intent, speakTextTxt: String) {
        val tts = tts
        if (tts != null) {
            try {
                tts.language = Locale.ENGLISH
            } catch (e: Exception) {
                tts.language = Locale.getDefault()
            }

            tts.setSpeechRate(1f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bundle = Bundle()
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, speakTextTxt)
                tts.synthesizeToFile(speakTextTxt, bundle, audioFile, speakTextTxt)
            } else {
                val myHashRender = HashMap<String, String>()
                myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, speakTextTxt)
                tts.synthesizeToFile(speakTextTxt, myHashRender, audioPath)
            }

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(p0: String?) {

                }

                override fun onDone(utteranceId: String) {
                    onTTSDone(intent)
                }

                override fun onError(utteranceId: String) {
                    sendErrorBroadCast("Utterance_OnError")
                }
            })
        }
    }

    private fun onTTSDone(intent: Intent) {
        var playBackStarted = false
        var playBackCompleted = false
        if (audioFile.exists()) {
            player?.let { player ->
                try {
                    player.reset()
                    val clientCustomization = Loader.instance.clientCustomization

                    //val isMic = intent.action == PLAY_MIC || intent.action == PLAY_VIDEO_MIC
                    /* In Manual mode Play Beep sound if enabled in customization and dont play it on mic*/

                    if (Loader.instance.isAutoAudioEnabled) {
                        player.setDataSource(audioPath)
                    } else if (Loader.instance.isAutoEarPieceEnabled && intent.action.containsIgnoreCase("PLAY_EARPIECE")) {
                        player.setDataSource(audioPath)
                    } else {
                        val descriptor = assets.openFd("audio/beep_test.wav")
                        player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    }

                    player.setOnErrorListener { mp, i, i2 -> onErrorListener(intent, "") }
                    player.setOnPreparedListener { mp ->
                        mp.start()
                        if (!playBackStarted) {
                            onPreparedListener(intent)
                            playBackStarted = true
                        }
                    }

                    player.setOnCompletionListener { mp ->
                        mp.stop()
                        if (!playBackCompleted) {
                            onCompletionListener(player, intent)
                            playBackCompleted = true
                        }
                    }
                    player.prepareAsync()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        } else {
            sendErrorBroadCast("Utterance_OnDone")
        }
    }


    private fun loudSpeakerTest() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getVolumeForMode(AudioManager.STREAM_MUSIC), 0)
        audioManager.isSpeakerphoneOn = true
        val mode = AudioManager.STREAM_MUSIC
        player?.setAudioStreamType(mode)
        player?.setVolume(1f, 1f)
    }

    private val headSetStreamVolume = if (Build.MANUFACTURER.containsIgnoreCase("samsung")) 9 else 8

    private fun setHeadSetRightSettings() {
        val hacker = AudioHacker()


        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, headSetStreamVolume, 0)
        try {
            hacker.forceRouteHeadset(true)
        } catch (e: Exception) {
            val mode = AudioManager.STREAM_MUSIC
            player?.setAudioStreamType(mode)
            audioManager.isSpeakerphoneOn = false
        }

        player?.setVolume(0f, 1f)
    }

    private fun setHeadSetLeftSettings() {
        val hacker = AudioHacker()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, headSetStreamVolume, 0)
        try {
            hacker.forceRouteHeadset(true)
        } catch (e: Exception) {
            val mode = AudioManager.STREAM_MUSIC
            player?.setAudioStreamType(mode)
            audioManager.isSpeakerphoneOn = false
        }

        player?.setVolume(1f, 0f)
    }


    private fun earphoneTest() {
        val mode = if (!isLGAndroidKitkatDevice) AudioManager.STREAM_VOICE_CALL else AudioManager.MODE_NORMAL
        audioManager.setStreamVolume(mode, AudioManagerWrapper.getVolumeForMode(AudioManager.STREAM_MUSIC,audioManager), 0)
        //audioManager.setMode(mode)
        audioManager.isSpeakerphoneOn = false
        player?.setAudioStreamType(mode)
        player?.setVolume(1f, 1f)

    }

    override fun onDestroy() {
        super.onDestroy()
        val tts = tts
        if (tts != null) {
            tts.setOnUtteranceProgressListener(null)
            tts.stop()
            tts.shutdown()
        }
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true

        val player = player
        if (player != null) {
            try {
                player.reset()
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onInit(status: Int) {
        ttsStatus = status
        if (status != TextToSpeech.SUCCESS) {
            sendErrorBroadCast("TTS INIT")
        }
        FirebaseUtil.addNew(FirebaseUtil.AUDIO).child("TTS").child("status").setValue(status)
    }

    class AudioHacker {
        private val TAG = "AudioTest"

        /* force route function through AudioSystem */
        @Throws(Exception::class)
        private fun setDeviceConnectionState(device: Int, state: Int, address: String) {
            val audioSystem = Class.forName("android.media.AudioSystem")
            val setDeviceConnectionState = audioSystem.getMethod(
                    "setDeviceConnectionState", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            setDeviceConnectionState.invoke(audioSystem, device, state, address)
        }

        @Throws(Exception::class)
        fun forceRouteHeadset(enable: Boolean) {
            if (enable) {
                Logy.i(TAG, "force route to Headset")
                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "")
                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "")
            } else {
                Logy.i(TAG, "force route to Earpirce")
                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "")
                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "")
                setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "")
            }
        }

        companion object {
            // Constants copied from AudioSystem
            private val DEVICE_IN_WIRED_HEADSET = 0x400000
            private val DEVICE_OUT_EARPIECE = 0x1
            private val DEVICE_OUT_WIRED_HEADSET = 0x4
            private val DEVICE_STATE_UNAVAILABLE = 0
            private val DEVICE_STATE_AVAILABLE = 1
        }
    }

    companion object {
        @JvmField
        val RANDOM_NUMBER = "RANDOM_NUMBER"
        @JvmField
        val SPEECH_RECOG = "SPEECH_RECOG"
        @JvmField
        val TTS_ACTION = "TTS_ACTION"
        @JvmField
        val SEND_TTS_ON_ERROR = "SEND_TTS_ON_ERROR"
        @JvmField
        val ERROR_TYPE = "ERROR_TYPE"
        @JvmField
        var SEND_TTS_ON_COMPLETE = "SEND_TTS_ON_COMPLETE"
        @JvmField
        var SEND_TTS_ON_PREPARED = "SEND_TTS_ON_PREPARED"
        @JvmField
        val TAG = TTSService::class.java.simpleName
        @JvmField
        val PLAY_HEADSET_LEFT = "PLAY_HEADSET_LEFT"
        @JvmField
        val PLAY_HEADSET_RIGHT = "PLAY_HEADSET_RIGHT"
        @JvmField
        val STOP_PLAYBACK = "STOP_PLAYBACK"
        @JvmField
        val PLAY_SPEAKER = "PLAY_SPEAKER"
        @JvmField
        val PLAY_EARPIECE = "PLAY_EARPIECE"
        @JvmField
        val PLAY_MIC = "PLAY_MIC"
        @JvmField
        val PLAY_VIDEO_MIC = "PLAY_VIDEO_MIC"
        @JvmField
        var ttsStatus = 101010
    }

}