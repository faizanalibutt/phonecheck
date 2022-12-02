package com.upgenicsint.phonecheck.eraser

import java.io.File

/**
 * Created by farhanahmed on 20/06/2017.
 */

class EraserThread(val eraserAlgo: String, private val path: File) : Thread() {
    var progressListener: SecureDeleteExtensions.OnProgressListener? = null

    override fun run() {
        var secureDeleteExtensions: SecureDeleteExtensions? = null
        when (eraserAlgo) {
            "0" -> secureDeleteExtensions = SecureDeleteExtensions(path, OverwriteAlgorithm.DOD_3)
            "1" -> secureDeleteExtensions = SecureDeleteExtensions(path, OverwriteAlgorithm.Random)
            "2" -> secureDeleteExtensions = SecureDeleteExtensions(path, OverwriteAlgorithm.Quick)
        }

        if (secureDeleteExtensions != null) {
            if (progressListener != null) {
                secureDeleteExtensions.listener = progressListener
                secureDeleteExtensions.deleteAll(path)
            }

        }
    }
}