package com.upgenicsint.phonecheck.misc

import android.content.DialogInterface

/**
 * Created by farhanahmed on 01/10/2017.
 */
interface RetryTextFieldListener {
    enum class ButtonType
    {
        LEFT,RIGHT,NEUTRAL
    }
    fun onClick(dialog: DialogInterface, type: ButtonType)
}