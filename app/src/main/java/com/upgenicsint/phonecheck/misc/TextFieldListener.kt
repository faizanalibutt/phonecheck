package com.upgenicsint.phonecheck.misc

import android.content.DialogInterface

/**
 * Created by farhanahmed on 01/10/2017.
 */
interface TextFieldListener {
    fun onClick(dialog: DialogInterface,text:String,isTrue:Boolean)
}