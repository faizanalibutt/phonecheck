package com.upgenicsint.phonecheck.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.view.Gravity
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.WindowManager
import android.widget.*
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.containsIgnoreCase

class FingerPrintHideService : Service() {

    private var windowManager: WindowManager? = null
    private var hideAutomation: ProgressBar? = null
    private var pleaseMe: TextView? = null
    private var parent: RelativeLayout? = null
    private var parentParams: RelativeLayout.LayoutParams? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //return super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        if (action == "com.phonecheck.showautomation") {
            try {

                windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

                parent = RelativeLayout(this@FingerPrintHideService)
                parentParams = RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                parentParams!!.addRule(RelativeLayout.CENTER_IN_PARENT)
                parent!!.setBackgroundResource(R.color.white_color)

                hideAutomation = ProgressBar(this@FingerPrintHideService, null, android.R.attr.progressBarStyle)
                hideAutomation!!.isIndeterminate = true
                hideAutomation!!.visibility = View.VISIBLE
                hideAutomation!!.id = ViewCompat.generateViewId()
                parent!!.addView(hideAutomation, parentParams)

                pleaseMe = TextView(this@FingerPrintHideService)
                pleaseMe!!.text = "Please wait..."
                pleaseMe!!.setTextColor(ContextCompat.getColor(this@FingerPrintHideService, R.color.dark_black))
                val parentParam = RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                parentParam.addRule(RelativeLayout.BELOW, hideAutomation!!.id)
                parentParam.addRule(RelativeLayout.CENTER_HORIZONTAL)
                parent!!.addView(pleaseMe, parentParam)

                params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT)
                } else {
//                    WindowManager.LayoutParams(
//                            WindowManager.LayoutParams.MATCH_PARENT,
//                            WindowManager.LayoutParams.MATCH_PARENT,
//                            WindowManager.LayoutParams.TYPE_PHONE,
//                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                            PixelFormat.TRANSLUCENT)

                    WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                            PixelFormat.TRANSLUCENT)
                }

                params!!.gravity = Gravity.CENTER
                params!!.x = 0 //Gravity.CENTER
                params!!.y = 100 //Gravity.CENTER

                //windowManager!!.updateViewLayout(parent, params)
                windowManager!!.addView(parent, params)

                if (Build.MODEL.containsIgnoreCase("Pixel") || Build.MODEL.containsIgnoreCase("Mi A1")) {
                    Handler().postDelayed({
                        if (parent != null)
                            try {
                                windowManager!!.removeView(parent)
                                parent = null
                            } catch (ignored: Exception) {
                            }
                    }, 10000)
                } else {
                    Handler().postDelayed({
                        if (parent != null)
                            try {
                                windowManager!!.removeView(parent)
                                parent = null
                            } catch (ignored: Exception) {
                            }
                    }, 10000)
                }

            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        } else {
            if (parent != null)
                try {
                    windowManager!!.removeView(parent)
                    parent = null
                } catch (ignored: Exception) {
                }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (parent != null)
            try {
                windowManager!!.removeView(parent)
                parent = null
            } catch (ignored: Exception) {
            }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}