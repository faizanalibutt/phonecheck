package com.upgenicsint.phonecheck.activities

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import com.upgenicsint.phonecheck.R
import java.lang.ref.WeakReference

class SplashActivity : BaseActivity() {

    private var delayHandler: Handler? = null
    private var splashDelayRunnable: SplashDelayRunnable? = null
    internal var currentApiVersion = android.os.Build.VERSION.SDK_INT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)


        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

            window.decorView.systemUiVisibility = flags

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView = window.decorView
            decorView
                    .setOnSystemUiVisibilityChangeListener { visibility ->
                        if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                            decorView.systemUiVisibility = flags
                        }
                    }
        }

        setContentView(R.layout.activity_splash)
        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        manager.isWifiEnabled = true
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.enable()




        startSplash()

    }

    private fun showTimeoutActivity() {
        val intent = Intent(context, TimeoutActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startSplash() {
        delayHandler = Handler()
        splashDelayRunnable = SplashDelayRunnable(this)
        delayHandler?.postDelayed(splashDelayRunnable, 1000)
        /*View logoImageView = findViewById(R.id.splashLogo);
        try {
            AnimatorSet set = new AnimatorSet();
            set.setDuration(450);
            set.playTogether(
                    ObjectAnimator.ofFloat(logoImageView, View.SCALE_Y, 0.2f, 1),
                    ObjectAnimator.ofFloat(logoImageView, View.SCALE_X, 0.2f, 1));

            set.start();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }


    override fun onDestroy() {
        super.onDestroy()
        if (delayHandler != null) {
            delayHandler?.removeCallbacks(splashDelayRunnable)
            splashDelayRunnable = null
            delayHandler = null
        }
    }

    private class SplashDelayRunnable(activity: SplashActivity) : Runnable {
        private var weakReference = WeakReference(activity)


        override fun run() {
            val ref = weakReference.get()
            if (ref != null) {
                ref.onSplashDone()
            }
        }
    }

    private fun onSplashDone() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onNavBackClick() {

    }

    override fun onNavDoneClick(v: View) {

    }

}
