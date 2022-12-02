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
import com.upgenicsint.phonecheck.barcode.CaptureActivity
import com.upgenicsint.phonecheck.misc.CameraFace
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

/**
 * Created by zohai on 12/14/2017.
 */
class AutofocusTest(context: Context) : Test(context) {


    override val jsonKey: String
        get() = Test.QRtest


    override val title: String
        get() = context.getString(R.string.rearCamera_title)

    override val detail: String
        get() = context.getString(R.string.barcode_desc)

    override val iconResource: Int
        get() = R.drawable.camera

    override val hasSubTest: Boolean
        get() = false

    override val activityRequestCode: Int
        get() = CaptureActivity.REQ

//    init {
//        subTests.put(Test.rearCameraTestKey, SubTest(context.getString(R.string.rearCamera_sub)))
//        subTests.put(Test.rearCameraQualityTestKey, SubTest(context.getString(R.string.camera_quality)))
//        subTests.put(Test.cameraAutoFocusKey, SubTest(context.getString(R.string.autofocus_subtest)))
//        subTests.put(Test.rearCameraFlashTestKey, SubTest(context.getString(R.string.rear_flash)))
//
//        resultsFilterMap.put(Test.rearCameraTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
//        resultsFilterMap.put(Test.rearCameraFlashTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
//        //resultsFilterMap.put(Test.cameraAutoFocusKey, true)
//
//    }


    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        isRunning = false
        this.context = context

        startMethod2Activity()

        return status
    }


    private fun startMethod2Activity() {
        if (Nammu.checkPermission(Manifest.permission.CAMERA)) {
            startIntent(context, CaptureActivity::class.java, CaptureActivity.REQ)

        } else {
            Nammu.askForPermission(context as Activity, Manifest.permission.CAMERA, object : PermissionCallback {
                override fun permissionGranted() {
                    startIntent(context, CaptureActivity::class.java, CaptureActivity.REQ)
                }

                override fun permissionRefused() {
                    // isClear = false
                }
            })
        }
    }

    override fun startIntent(context: Context, target: Class<out Activity>, req: Int): Intent {
        val intent = Intent(context, target)

        intent.putExtra(CaptureActivity.CAMERA_TYPE_KEY, CameraFace.FACE_BACK.value)
        try {
            val activity = context as Activity
//            activity.startActivity(intent)
            activity.startActivityForResult(intent, req)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return intent
    }

    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        //Loader.RESULT.put(cameraTestKey, toJsonStatus());
        storeOperation.add(Test.QRtest, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.QRtest, status)
        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction() = false

    override fun requireActivity() = true
}
