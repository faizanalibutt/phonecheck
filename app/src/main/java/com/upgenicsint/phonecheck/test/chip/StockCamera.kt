package com.upgenicsint.phonecheck.test.chip

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.CameraTestActivity
import com.upgenicsint.phonecheck.activities.StockCameraActivity
import com.upgenicsint.phonecheck.misc.CameraFace
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

class StockCamera(context: Context) : Test(context) {


    override val jsonKey: String
        get() = Test.cameraBackTestKey


    override val title: String
        get() = "Stock Camera Test"

    override val detail: String
        get() = context.getString(R.string.backCamera_picDesc)+ ".\n" + context.getString(R.string.backCamera_flashDesc)

    override val iconResource: Int
        get() = R.drawable.camera

    override val hasSubTest: Boolean
        get() = true

    override val activityRequestCode: Int
        get() = CameraTestActivity.REQ

    init {

        subTests.put(Test.rearCameraTestKey, SubTest(context.getString(R.string.rearCamera_sub)))
        subTests.put(Test.rearCameraQualityTestKey, SubTest(context.getString(R.string.camera_quality)))
        subTests.put(Test.rearCameraFlashTestKey, SubTest(context.getString(R.string.rear_flash)))
//        subTests.put(Test.cameraAutoFocusKey, SubTest("Auto Focus"))


        resultsFilterMap.put(Test.rearCameraTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        resultsFilterMap.put(Test.rearCameraFlashTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        //resultsFilterMap.put(Test.cameraAutoFocusKey, true)

    }


    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        isRunning = false
        this.context = context

        startMethod2Activity()

        return status
    }


    private fun startMethod2Activity() {
        if (Nammu.checkPermission(Manifest.permission.CAMERA)) {
            startIntent(context, StockCameraActivity::class.java, StockCameraActivity.REQ)

        } else {
            Nammu.askForPermission(context as Activity, Manifest.permission.CAMERA, object : PermissionCallback {
                override fun permissionGranted() {
                    startIntent(context, CameraTestActivity::class.java, StockCameraActivity.REQ)
                }

                override fun permissionRefused() {
                    // isClear = false
                }
            })
        }
    }

    override fun startIntent(context: Context, target: Class<out Activity>, req: Int): Intent {
        val intent = Intent(context, target)
        intent.putExtra(CameraTestActivity.CAMERA_TYPE_KEY, CameraFace.FACE_BACK.value)
        try {
            val activity = context as Activity
            activity.startActivityForResult(intent, req)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return intent
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        //Loader.RESULT.put(cameraTestKey, toJsonStatus());
        storeOperation.add(Test.cameraBackTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.cameraBackTestKey, status)
        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction() = false

    override fun requireActivity() = true
}

