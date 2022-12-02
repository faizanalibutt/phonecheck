package com.upgenicsint.phonecheck.activities

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.util.Log
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.SamDigitizerTestActivity.test
import com.upgenicsint.phonecheck.test.Test
import java.io.File
import java.util.*

class StockCameraActivity : DeviceTestableActivity<Test>() {
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_camera)

        openCameraForPic()
    }

    private fun openCameraForPic() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory("PhoneCheck").toString())
        try {
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        imageUri = FileProvider.getUriForFile(this,
                this.applicationContext.packageName + ".provider",
                File(mediaStorageDir.path + File.separator +
                        UUID.randomUUID().toString() + ".jpg"))
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == 2) {
                    test?.sub(Test.rearCameraQualityTestKey)?.value = Test.PASS
                    test?.sub(Test.stockCameraTestKey)?.value = Test.PASS
                    test?.sub(Test.rearCameraFlashTestKey)?.value = Test.PASS
                    finalizeTest()
                }
            }
        } catch (E: Exception) {

        }

    }

    companion object {
        val REQ = 202
        private val TAG = "Camera Activity"
        val CAMERA_TYPE_KEY = "CAMERA_TYPE_KEY"
        var FRONT_CAMERA_SCREEN_TIME = 0
        var REAR_CAMERA_SCREEN_TIME = 0
    }
}
