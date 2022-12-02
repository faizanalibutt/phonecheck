package com.upgenicsint.phonecheck.activities

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.annotation.IdRes
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import co.balrampandey.logy.Logy
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.utils.Tools

/**
 * Created by Farhan on 10/14/2016.
 */

open class BaseActivity : AppCompatActivity() {

    val context: Context
        get() = this

    val activity: AppCompatActivity
        get() = this

    open val backPress: Boolean
        get() = true

    protected fun applyFont(@IdRes res: Int) {
        val textView = findViewById<View>(res) as TextView
        textView.typeface = Tools.getJunctionFontRegular(context)
    }

    var newWakeLock: PowerManager.WakeLock? = null
    val pm by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logy.setEnable(BuildConfig.DEBUG)

        //val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        //val lock = keyguardManager.newKeyguardLock("PhoneCheck_Keyguard")
        //lock.disableKeyguard()
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON )

    }

    override fun onResume() {
        super.onResume()
        newWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "PhoneCheck")
        if (newWakeLock?.isHeld == true) {
            newWakeLock?.release()
        }
        try {
            newWakeLock?.acquire()
        }
        catch (e:RuntimeException){
            e.printStackTrace()
        }
    }


    override fun onPause() {
        super.onPause()
        newWakeLock?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON )
    }

    open fun onNavBackClick() {}

    open fun onNavDoneClick(v: View) {}

    fun onCreateNav() {
        val navBackButton = findViewById<View>(R.id.nav_back)
        val navDoneButton = findViewById<View>(R.id.nav_done)

        if (navBackButton == null || navDoneButton == null)
            return

        navBackButton.setOnClickListener {
            isSoftBackPressed = true
            shouldMoveToNextTest = false
            onNavBackClick()
        }

        navDoneButton.setOnClickListener { v ->
            isSoftBackPressed = false
            shouldMoveToNextTest = true
            onNavDoneClick(v)
        }
    }

    fun setNavTitle(title: String) {
        val navTitleButton = findViewById<Button>(R.id.nav_title)
        if (navTitleButton != null) {
            navTitleButton.text = title
        }
    }


    override fun onBackPressed() {
        isSoftBackPressed = true
        onNavBackClick()
        if (backPress)
            super.onBackPressed()
    }


    companion object {

        @JvmField
        var autoPerform = false
        @JvmField
        var isAutoPerformRunning = false
        @JvmField
        var isSoftBackPressed = false
        @JvmField
        var shouldMoveToNextTest = false

        @JvmStatic
        fun clearLightStatusBar(view: View) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var flags = view.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                view.systemUiVisibility = flags
            }
        }
    }
}
