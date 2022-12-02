package com.upgenicsint.phonecheck.activities

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.eraser.EraserThread
import com.upgenicsint.phonecheck.eraser.SecureDeleteExtensions
import com.upgenicsint.phonecheck.misc.D
import kotlinx.android.synthetic.main.activity_eraser.*
import org.json.JSONException
import org.json.JSONObject
import pl.tajchert.nammu.Nammu
import java.io.File

class EraserActivity3 : BaseActivity() {

    private val devicePolicyManager by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val cn by lazy { ComponentName(this, D::class.java) }


    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return state != null && Environment.MEDIA_MOUNTED == state
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eraser)
        Nammu.init(this)
        devicePolicyManager.resetPassword("0000", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)

        cancelButton.setOnClickListener {
            finish()
        }

        handleIntent(intent)


    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.getStringExtra(OP_ACTION)

            when (action) {
                FINISH -> finish()
                ERASE -> startEraser()
                ASK_FACTORY_RESET_PERMISSION -> {
                    if (checkWipePermission()) {
                        printLog(ASK_FACTORY_RESET_PERMISSION, OP_SUCCESS)
                    } else {
                        askWipePermission()
                    }
                }
                HAS_FACTORY_RESET_PERMISSION -> {
                    printLog(HAS_FACTORY_RESET_PERMISSION, if (checkWipePermission()) OP_SUCCESS else OP_FAILED)
                }
                FACTORY_RESET -> wipeNow()

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //private boolean wipeCheckAgain = true;

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADMIN_REQ_CODE) {
            if (checkWipePermission()) {
                printLog(ASK_FACTORY_RESET_PERMISSION, OP_SUCCESS)
            } else {
                printLog(ASK_FACTORY_RESET_PERMISSION, OP_FAILED)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    private fun startEraser() {
        if (!isExternalStorageWritable) {
            printLog(ERASE, "Failed:Not Writable")
        }
        val externalPath = Environment.getExternalStorageDirectory()

        if (externalPath != null) {
            val eraserAlgo = intent.getStringExtra("ALGO_ID")
            val eraserThread = EraserThread(eraserAlgo ?: "0", externalPath)
            eraserThread.progressListener = object : SecureDeleteExtensions.OnProgressListener {
                override fun onResponse(file: File?, position: Int, total: Int) {
                    //Thread.sleep(3 * 60 * 1000)
                    runOnUiThread {
                        if (total <= 0) {
                            printLog(ERASE, OP_SUCCESS)
                        }
                        try {
                            val per = (position.toFloat() / total.toFloat() * 100f).toInt()
                            if (file != null) {
                                statusTextView.text = "Erasing Internal ${file.absolutePath}"
                            }
                            if (per >= 100 || position == total) {
                                printLog(ERASE, OP_SUCCESS)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            eraserThread.start()
        }
    }


    private fun printLog(operation: String, status: String) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("operation", operation)
            jsonObject.put("status", status)
            Log.i(packageName, LOG_START_HASH + jsonObject.toString() + LOG_END_HASH)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun checkWipePermission() = devicePolicyManager.isAdminActive(cn)

    private fun askWipePermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation))
        startActivityForResult(intent, ADMIN_REQ_CODE)
    }

    private fun wipeNow() {

        try {
            if (checkWipePermission()) {
                printLog(FACTORY_RESET, OP_SUCCESS)
                devicePolicyManager.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)
            } else {
                printLog(FACTORY_RESET, OP_FAILED)
            }
        } catch (e: Exception) {
            printLog(FACTORY_RESET, OP_FAILED)
            e.printStackTrace()
        }

    }

    companion object {

        val LOG_START_HASH = "UGhvbmVDaGVja0VyYXNlclN0YXJ0"
        val LOG_END_HASH = "UGhvbmVDaGVja0VyYXNlckVuZA=="
        private val OP_ACTION = "OP_ACTION"
        private val ADMIN_REQ_CODE = 99
        private val OP_SUCCESS = "complete"
        private val OP_FAILED = "failed"

        val TAG = "EraserActivity2"
        val HAS_FACTORY_RESET_PERMISSION = "HAS_FACTORY_RESET_PERMISSION"
        val ERASE = "ERASE"
        val ASK_FACTORY_RESET_PERMISSION = "ASK_FACTORY_RESET_PERMISSION"
        val FACTORY_RESET = "FACTORY_RESET"
        val FINISH = "FINISH"
    }
}