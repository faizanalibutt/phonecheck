package com.upgenicsint.phonecheck.activities

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.NfcTest
import java.util.*


class NFCActivity : DeviceTestableActivity<NfcTest>() {

    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        Loader.TIME_VALUE = 0
        NFC_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        onCreateNav()
        Logy.setEnable(BuildConfig.DEBUG)
        setNavTitle("NFC Test")
        test = Loader.instance.getByClassType(NfcTest::class.java)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this@NFCActivity)
        mPendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, this.javaClass)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    /**
     * Enable NFC
     */
    private fun showNFCSettings() {
        Toast.makeText(this, "Enable NFC", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        startActivity(intent)
    }

    /**
     * Start NFC
     */
    override fun onResume() {
        super.onResume()
        if (mNfcAdapter != null) {
            if (!mNfcAdapter!!.isEnabled)
                showNFCSettings()
            mNfcAdapter!!.enableForegroundDispatch(this, mPendingIntent, null, null)
        }
    }

    override fun onNavDoneClick(v: View) {
        super.onNavDoneClick(v)
        if (test != null && test!!.status != Test.PASS) {
            test!!.status = Test.FAILED
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent != null) {
            getTagInfo(intent)
        }
    }

    private fun getTagInfo(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        /**
         * Finally pass NFC
         */
        if ( tag != null) {
            test!!.status = Test.PASS
            finalizeTest()
        }
    }

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }

    override fun closeTimerTest() {
        super.closeTimerTest()
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                NFC_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_nfc), -1)] =
                            RecordTest(context.getString(R.string.report_nfc_test), NFC_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("NFC", "${NFC_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    companion object {
        var NFC_SCREEN_TIME = 0
        val REQ = 4786
    }
}
