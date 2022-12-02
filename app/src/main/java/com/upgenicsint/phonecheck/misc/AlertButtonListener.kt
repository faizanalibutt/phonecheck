package com.upgenicsint.phonecheck.misc

import android.content.DialogInterface

/**
 * Created by farhanahmed on 12/09/2017.
 */

interface AlertButtonListener {
    enum class ButtonType
    {
        LEFT,RIGHT
    }
    fun onClick(dialog: DialogInterface, type: ButtonType)
}
