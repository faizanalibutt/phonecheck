package com.upgenicsint.phonecheck.utils

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.support.annotation.DrawableRes
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import com.upgenicsint.phonecheck.containsIgnoreCase

import com.upgenicsint.phonecheck.test.hardware.ButtonTest

/**
 * Created by Farhan on 11/4/2016.
 */

object Utils {

    /*
    * method for return valid keycode for samsung phones which have active key
    *
    * @return keycode
    */
    @JvmStatic val samsungActiveModelCode: Int
        get() {
            val model = Build.MODEL
            var key = -1
            if (Build.MANUFACTURER.containsIgnoreCase("samsung")) {
                if (model.containsIgnoreCase("SM-G890") || model.containsIgnoreCase("SM-G870")) {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        if (KeyCharacterMap.deviceHasKey(ButtonTest.ACTIVE_KEYCODE_238)) {
                            key = ButtonTest.ACTIVE_KEYCODE_238
                            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Active Key").child("238 keycode").setValue("Yes")
                        }
                    } else {
                        if (KeyCharacterMap.deviceHasKey(ButtonTest.ACTIVE_KEYCODE_275)) {
                            key = ButtonTest.ACTIVE_KEYCODE_275
                            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Active Key").child("275 keycode").setValue("Yes")
                        } else if (KeyCharacterMap.deviceHasKey(ButtonTest.ACTIVE_KEYCODE_1015)) {
                            key = ButtonTest.ACTIVE_KEYCODE_1015
                            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Active Key").child("1015 keycode").setValue("Yes")
                        }
                    }

                }
                if (key == -1) {
                    FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Active Key").child("No active key").setValue("Yes")
                }
            }

            return key
        }

    @JvmStatic val samsungBixbyModelCode: Int
        get() {
            val model = Build.MODEL
            var key = -1
            if (Build.MANUFACTURER.containsIgnoreCase("samsung")) {
                if (KeyCharacterMap.deviceHasKey(ButtonTest.BIXBY_KEYCODE_1082)) {
                    key = ButtonTest.BIXBY_KEYCODE_1082
                }
                if (key == -1) {
                    FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Active Key").child("No active key").setValue("Yes")
                }
            }
            return key
        }

    //galaxy s4
    @JvmStatic val isS4Model: Boolean
        get() {
            val method2Models = arrayOf("SCH-I545", "SGH-I337", "SPH-L720", "SGH-M919", "SCH-R970")

            val model = Build.MODEL.toLowerCase()
            for (method2Model in method2Models) {
                val modelFromList = method2Model.toLowerCase()
                Log.d("CameraTest", model + " : " + model.startsWith(modelFromList) + " : " + model.contains(modelFromList))
                if (model.startsWith(modelFromList) || model.contains(modelFromList)) {
                    return true
                }
            }
            return false
        }

    //galaxy s5
    @JvmStatic val isS5Model: Boolean
        get() {
            val method2Models = arrayOf("SM-G900", "SM-G901", "SM-G903", "SM-G870")

            val model = Build.MODEL.toLowerCase()
            for (method2Model in method2Models) {
                val modelFromList = method2Model.toLowerCase()
                if (model.startsWith(modelFromList) || model.contains(modelFromList)) {
                    return true
                }
            }
            return false
        }

    @JvmStatic val isSonyXperia: Boolean
        get() = Build.MANUFACTURER.containsIgnoreCase( "sony") && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_CAMERA)

    @JvmStatic fun loadImage(@DrawableRes res: Int, imageView: ImageView?) {
        if (imageView == null) {
            return
        }
        imageView.setImageResource(res)

    }

    @JvmStatic fun hasMenuButton(context: Context): Boolean {
        var hasMenu = false
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_MENU)) {
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("KEYCODE_MENU").setValue("Yes")
            hasMenu = true
        }
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_APP_SWITCH)) {
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("KEYCODE_APP_SWITCH").setValue("Yes")
            hasMenu = true
        }
        if (hasNavBar(context.resources)) {
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("hasNavBar()").setValue("Yes")
        }
        if (ViewConfiguration.get(context).hasPermanentMenuKey()) {
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("hasPermanentMenuKey()").setValue("Yes")
        }
        if (Build.BRAND.equals("samsung", ignoreCase = true) || Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("BRAND()").setValue("Yes")
        }
        if (Build.BRAND.equals("motorola", ignoreCase = true) || Build.MANUFACTURER.equals("motorola", ignoreCase = true)){
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("BRAND()").setValue("Yes")
        }
        if (Build.MODEL.equals("XT1254", ignoreCase = true) || Build.MANUFACTURER.equals("motorola", ignoreCase = true)){
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("MODEL()").setValue("Yes")
        }
        if (Build.MODEL.equals("HTC-6525lvw", ignoreCase = true) || Build.MANUFACTURER.equals("htc", ignoreCase = true)){
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("MODEL() ").setValue("Yes")
        }
        if (Build.MODEL.equals("TCL-5056N", ignoreCase = true)){
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("MODEL() ").setValue("Yes")
        }
        if (Build.MODEL.containsIgnoreCase("LG H700") || Build.MANUFACTURER.equals("lg", ignoreCase = true) || Build.BRAND.equals("lg", ignoreCase = true)){
            hasMenu = true
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("MODEL() ").setValue("Yes")
        }
        if (Build.MODEL.containsIgnoreCase("LG-H700")) {
            hasMenu = true
        }
        if (!hasMenu) {
            FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu").child("No Menu Button").setValue("Yes")
        }
        return hasMenu
    }

    @JvmStatic fun hasNavBar(resources: Resources): Boolean {
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }

    @JvmStatic fun isDevice(make: String, model: String) =
            Build.MANUFACTURER.containsIgnoreCase(make) && Build.MODEL.containsIgnoreCase( model)

    @JvmStatic fun isDeviceHasKeyCode(make: String, model: String, keycode: Int) =
            KeyCharacterMap.deviceHasKey(keycode) && Build.MANUFACTURER.containsIgnoreCase( make) && Build.MODEL.containsIgnoreCase( model)

    @JvmStatic fun isDeviceHasKeyCode(model: String, keycode: Int) =
            KeyCharacterMap.deviceHasKey(keycode) && Build.MODEL.containsIgnoreCase( model)
}
