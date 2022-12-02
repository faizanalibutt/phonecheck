package com.upgenicsint.phonecheck.test.chip

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.CameraTestActivity
import com.upgenicsint.phonecheck.misc.CameraFace
import com.upgenicsint.phonecheck.misc.CameraMethodChooser
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.Tools
import org.json.JSONException
import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback

/**
 * Created by Farhan on 10/17/2016.
 */

class FrontCameraTest(context: Context) : Test(context) {

    companion object {

        @JvmField var hasFrontFlash: Boolean = false
    }

    override val jsonKey: String
        get() = Test.cameraFrontTestKey


    override val title: String
        get() = context.getString(R.string.frontCam_title)

    override val detail: String
        get() = context.getString(R.string.frontCam_picDesc)+ ".\n" + context.getString(R.string.frontCam_flashDesc)

    override val iconResource: Int
        get() = R.drawable.camera

    override val hasSubTest: Boolean
        get() = true

    override val activityRequestCode: Int
        get() = CameraTestActivity.REQ_FRONT

    init {

        try {
            val cameraMethod = CameraMethodChooser.choose()

            if (cameraMethod == 0 || cameraMethod == 1) {
                val ci = Camera.CameraInfo()
                for (i in 0 until Camera.getNumberOfCameras()) {
                    Camera.getCameraInfo(i, ci)
                    if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

                        val c = Camera.open(i)

                        hasFrontFlash = Tools.checkHasFlash(c)

                        if (c != null) {
                            try {
                                c.release()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    for (cameraId in manager.cameraIdList) {
                        val chars = manager.getCameraCharacteristics(cameraId)
                        val facing = chars.get(CameraCharacteristics.LENS_FACING)
                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                            if (hasFlash != null) {
                                hasFrontFlash = hasFlash
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        subTests.put(Test.frontCameraTestKey, SubTest(context.getString(R.string.frontCam_sub)))
        subTests.put(Test.frontCameraQualityTestKey, SubTest(context.getString(R.string.frontCam_quality)))
        if (hasFrontFlash) {
            subTests.put(Test.frontCameraFlashTestKey, SubTest(context.getString(R.string.front_flash)))
        }
        resultsFilterMap.put(Test.frontCameraFlashTestKey, hasFrontFlash)
        resultsFilterMap.put(Test.frontCameraTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))

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
            startIntent(context, CameraTestActivity::class.java, CameraTestActivity.REQ)

        } else {
            Nammu.askForPermission(context as Activity, Manifest.permission.CAMERA, object : PermissionCallback {
                override fun permissionGranted() {
                    startIntent(context, CameraTestActivity::class.java, CameraTestActivity.REQ)
                }

                override fun permissionRefused() {
                    //isClear = false
                }
            })
        }
    }

    override fun startIntent(context: Context, target: Class<out Activity>, req: Int): Intent {
        val intent = Intent(context, target)
        intent.putExtra(CameraTestActivity.CAMERA_TYPE_KEY, CameraFace.FACE_FRONT.value)
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
        storeOperation.add(Test.cameraFrontTestKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(Test.cameraFrontTestKey, status)

        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction(): Boolean {
        return false
    }

    override fun requireActivity(): Boolean {
        return true
    }




}
