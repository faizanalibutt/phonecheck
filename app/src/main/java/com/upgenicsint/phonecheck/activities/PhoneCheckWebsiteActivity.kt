package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.app.IntentService
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.chip.WifiTest
import kotlinx.android.synthetic.main.activity_phone_check_website.*

class PhoneCheckWebsiteActivity : AppCompatActivity() {

    val wifiTest = WifiTest(this@PhoneCheckWebsiteActivity)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_check_website)
        showWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showWebView() {
        webview.webViewClient = MyBrowser()
        webview.webChromeClient = MyBrowser2()
        webview.settings.loadsImagesAutomatically = true
        webview.settings.javaScriptEnabled = true
        webview.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webview.loadUrl(getString(R.string.phonecheck_weblink))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webview.canGoBack()) {
            webview.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private inner class MyBrowser : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (wifiTest.isAlreadyConnected) {
                setProgressBar(View.VISIBLE)
            } else {
                setProgressBar(View.GONE)
                showToastMsg(0)
            }
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            setProgressBar(View.GONE)
            super.onPageFinished(view, url)
        }

    }

    private fun setProgressBar(show: Int) {
        progressBar.visibility = show
    }

    private inner class MyBrowser2 : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress >= PROGRESS_HIDE) {
                setProgressBar(View.GONE)
            }
            if (!wifiTest.isConnected) {
                setProgressBar(View.GONE)
                showToastMsg(0)
            }
        }
    }

    private fun showToastMsg(length: Int) {
        Toast.makeText(this@PhoneCheckWebsiteActivity, "Internet not Available",
                length).show()
    }

    override fun onPause() {
        super.onPause()
        setProgressBar(View.GONE)
    }

    /*override fun onRestart() {
        super.onRestart()
        if (wifiTest.isAlreadyConnected) {
            webview.reload()
            setProgressBar(View.VISIBLE)
        } else {
            if (!showToast) {
                showToastMsg(0)
                showToast = true
            }
            webview.stopLoading()
            setProgressBar(View.GONE)
        }
    }*/

    companion object {
        val PROGRESS_HIDE = 70
    }
}
