package com.upgenicsint.phonecheck.media

import android.speech.tts.UtteranceProgressListener

/**
 * Created by Farhan on 10/30/2016.
 */

abstract class UtteranceProgressAdapter : UtteranceProgressListener() {

    override fun onStart(utteranceId: String) {

    }

    override fun onDone(utteranceId: String) {

    }

    override fun onError(utteranceId: String) {

    }
}