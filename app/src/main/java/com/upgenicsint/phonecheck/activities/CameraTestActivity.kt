package com.upgenicsint.phonecheck.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.zxing.PlanarYUVLuminanceSource
import com.ragnarok.rxcamera.RxCameraData
import com.upgenicsint.phonecheck.BuildConfig
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.barcode.CaptureActivityHandler
import com.upgenicsint.phonecheck.barcode.ViewfinderView
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.fragments.Camera1APIImpl1Fragment
import com.upgenicsint.phonecheck.fragments.Camera1APIImpl2Fragment
import com.upgenicsint.phonecheck.fragments.Camera2APIFragment
import com.upgenicsint.phonecheck.fragments.CameraFragment
import com.upgenicsint.phonecheck.misc.*
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.BackCameraTest
import com.upgenicsint.phonecheck.test.chip.FrontCameraTest
import com.upgenicsint.phonecheck.utils.DialogUtils
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import com.upgenicsint.phonecheck.utils.Tools
import com.upgenicsint.phonecheck.utils.Utils
import kotlinx.android.synthetic.main.activity_camera_test.*
import kotlinx.android.synthetic.main.navi_buttons.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*


class CameraTestActivity : DeviceTestableActivity<Test>() {

    // abstract camera fragment object
    protected var cameraFragment: CameraFragment<*>? = null
    /*private var handler: CaptureActivityHandler? = null*/

    // Enum CameraFace is checking either its rear or front
    var currentFace = CameraFace.FACE_FRONT
    private var cameraMethod: Int = 1

    private var progress: AlertDialog? = null
    private val isAutoFocusPassed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)

        Tools.setBrightness(window, 1f)
        nav_buildTextView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        onCreateNav()

        progress = ProgressBarUtil.get(getString(R.string.loading_image), context)
        if (intent.hasExtra(CAMERA_TYPE_KEY)) {
            val type = intent.getIntExtra(CAMERA_TYPE_KEY, -1)
            when (type) {
                0 -> {
                    currentFace = CameraFace.FACE_BACK
                    try {
                        test = Loader.instance.getByClassType(BackCameraTest::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                1 -> {
                    currentFace = CameraFace.FACE_FRONT
                    try {
                        test = Loader.instance.getByClassType(FrontCameraTest::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Loader.TIME_VALUE = 0
        if (currentFace == CameraFace.FACE_FRONT) {
            CameraTestActivity.FRONT_CAMERA_SCREEN_TIME = 0
        } else {
            CameraTestActivity.REAR_CAMERA_SCREEN_TIME = 0
        }
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        val test = test

        if (test == null) {
            finalizeTest()
            return
        }

        setNavTitle(test.title)

        Logy.setEnable(BuildConfig.DEBUG)
        nav_title.setTextColor(ContextCompat.getColor(context, R.color.main_header_text_color))


        //cameraPictureImageView.setMinimumScaleType(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);

        test.resultsFilterMap.put(Test.rearCameraTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        test.resultsFilterMap.put(Test.frontCameraTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
        test.resultsFilterMap.put(Test.rearCameraFlashTestKey, context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))


        FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                .child("hasBackCamera")
                .setValue(test.resultsFilterMap[Test.rearCameraTestKey] ?: false)
        FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                .child("hasFrontCamera")
                .setValue(test.resultsFilterMap[Test.frontCameraTestKey] ?: false)

        FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                .child("hasBackFlash")
                .setValue(test.resultsFilterMap[Test.rearCameraFlashTestKey] ?: false)
        FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                .child("hasFrontFlash")
                .setValue(test.resultsFilterMap[Test.frontCameraFlashTestKey] ?: false)

        reloadFragmentFor(currentFace)

    }

   /* fun getHandler(): CaptureActivityHandler? {
        return handler
    }*/


    private fun cameraOpenCheck() {
        val test = test ?: return
        if (currentFace === CameraFace.FACE_FRONT) {
            test.sub(Test.frontCameraTestKey)?.value = Test.PASS
        }
        if (currentFace === CameraFace.FACE_BACK) {
            test.sub(Test.rearCameraTestKey)?.value = Test.PASS

        }
    }


    /**
     * Method for taking picture from camera.
     */
    private fun requestTakePicture() {
        if (cameraFragment == null) {
            endTestByFailure("cameraFragment is null")
            return
        }

        if (cameraTakeBtn.tag != null) {
            return
        }

        cameraFragment?.takePicture()
        cameraTakeBtn.tag = "CLICKED"
    }

    private fun checkCamera() = cameraFragment != null


    /**
     * end the test and show failure toast
     */
    private fun endTestByFailure(s: String) {
        FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                .child("endTestByFailure")
                .child(s)
                .setValue(s)
        finalizeTest()
    }


    private fun reloadFragmentFor(face: CameraFace) {
        val test = test ?: return
        if (Build.MANUFACTURER.containsIgnoreCase("sony")) {
            cameraFragment = Camera1APIImpl2Fragment()
        } else {
            cameraMethod = CameraMethodChooser.choose()
            when (cameraMethod) {
                0 -> cameraFragment = Camera1APIImpl1Fragment()
                1 -> cameraFragment = Camera1APIImpl2Fragment()
                2 -> {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    cameraFragment = Camera2APIFragment()
                }
            }
            //Toast.makeText(this@CameraTestActivity, cameraMethod.toString(), Toast.LENGTH_LONG).show()
        }
        /*cameraMethodTextView.text = "Camera code $cameraMethod"*/
        val cameraFragment = cameraFragment
        if (cameraFragment == null) {
            showErrorMessage(getString(R.string.camera_instance_nt_found))
            return
        }
        cameraFragment.currentFace = face


        cameraFragment.onCameraOpen = { camera ->
            cameraTakeBtn.tag = null

            FirebaseUtil.addNew(FirebaseUtil.CAMERA)
                    .child("Method")
                    .setValue(cameraMethod)
            if (cameraFragment.currentFace === CameraFace.FACE_FRONT) {
                test.resultsFilterMap.put(Test.frontCameraFlashTestKey, FrontCameraTest.hasFrontFlash)
            }

            /*Take Picture*/
            cameraPictureImageView.visibility = View.GONE
            cameraTakeBtn.visibility = View.VISIBLE
            cameraTakeBtn.setOnClickListener { requestTakePicture() }
            val listener = CameraTestClickListener()
            failBtn.setOnClickListener(listener)
            passBtn.setOnClickListener(listener)

        }


        cameraFragment.onAutoFocusCompleted = {
            if (test.hasSubTest(Test.cameraAutoFocusKey) == true) {
                test.sub(Test.cameraAutoFocusKey)?.value = Test.PASS
            }
        }

        cameraFragment.onPictureTaken = { data ->
            onPictureTaken(data)
        }

        cameraFragment.onCameraError = { e ->
            e.printStackTrace()
            showErrorMessage(e.message)
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, cameraFragment).commit()
    }


    private var isPictureTaken = false

    private fun onPictureTaken(data: Any) {
        isPictureTaken = true
        runOnUiThread {

            val progress = progress
            if (progress != null) {
                if (!activity.isFinishing && !progress.isShowing) {
                    progress.show()
                }
            }

            try {
                supportFragmentManager.beginTransaction().remove(cameraFragment!!).commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cameraFragment?.onCloseCamera()
            cameraOpenCheck()
            cameraTakeBtn.tag = null


            cameraPictureImageView.visibility = View.VISIBLE
            hideCameraTakeButton(true)
            hideCameraQualityButtons(false)

            progress?.show()
            if (data is File) {
                val load = Glide.with(activity)
                        .load(data)
                        .override(800, 1200)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .listener(object : RequestListener<File, GlideDrawable> {
                            override fun onResourceReady(resource: GlideDrawable?, model: File?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                                data.delete()

                                progress?.let {
                                    if (it.isShowing) {
                                        it.cancel()
                                    }
                                }
                                return false
                            }

                            override fun onException(e: Exception?, model: File?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                                data.delete()
                                progress?.let {
                                    if (it.isShowing) {
                                        it.cancel()
                                    }
                                }
                                showErrorMessage(e?.message)
                                return false
                            }

                        })
                val cameraFragment = cameraFragment
                if (cameraFragment != null) {
                    if (currentFace === CameraFace.FACE_BACK) {
                        load.transform(RotateTransformation(activity, 90F))
                    } else {
                        if (Build.MODEL.containsIgnoreCase("Nexus 6")) {
                            load.transform(RotateTransformation(activity, 90F))
                        } else {
                            load.transform(RotateTransformation(activity, 270F))
                        }
                    }
                }
                load.into(cameraPictureImageView)
                showDidFlashMessage()

            } else {
                ImageLoader(this).execute(data)
            }
        }
    }

    class ImageLoader(activity: CameraTestActivity) : AsyncTask<Any, Bitmap?, Bitmap?>() {
        val activity = WeakReference<CameraTestActivity>(activity)
        override fun doInBackground(vararg p0: Any?): Bitmap? {

            val ref = activity.get()
            val data = p0[0]
            if (ref != null) {
                if (ref.cameraFragment is Camera2APIFragment) {
                    return ref.getBitmapForCamera2API(data as ByteArray)
                } else if (data is ByteArray) {
                    return ref.getBitmapForCameraAPI1(data)
                } else if (data is RxCameraData) {
                    return ref.getBitmapForRXCamera(data)
                }
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            val ref = activity.get()
            if (ref != null) {
                if (result != null) {
                    ref.onBitmapReady(result)
                } else {
                    ref.onBitmapFailed()
                }

            }
        }
    }

    private fun showErrorMessage(message: String?) {
        runOnUiThread {
            val errorBuilder = AlertDialog.Builder(context)
            errorBuilder.setCancelable(false)
            errorBuilder.setTitle("Error")
            if (message == null) {
                errorBuilder.setMessage(getString(R.string.camer_err))
            } else {
                errorBuilder.setMessage(message)
            }
            errorBuilder.setPositiveButton(context.getString(R.string.ok_id)) { dialog, which ->
                dialog.dismiss()
                finish()
            }
            errorBuilder.create().show()
        }
    }

    fun getBitmapForCamera2API(data: ByteArray): Bitmap? {
        try {

            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = false
            opts.inPreferredConfig = Bitmap.Config.RGB_565
            opts.inDither = true

            var bitmap: Bitmap? = null

            var width = 0
            var height = 0

            if (cameraFragment?.currentFace === CameraFace.FACE_FRONT) {
                bitmap = Glide.with(context)
                        .load(data)
                        .asBitmap()
                        .transform(RotateTransformation(context, 90f))
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                if (bitmap != null) {
                    width = bitmap.width
                    height = bitmap.height
                }
            } else if (cameraFragment?.currentFace === CameraFace.FACE_BACK) {
                bitmap = Glide.with(context)
                        .load(data)
                        .asBitmap()
                        .transform(RotateTransformation(context, 90f))
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                if (bitmap != null) {
                    width = bitmap.width
                    height = bitmap.height
                }

//                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, opts)
//                if (bitmap != null) {
//                    width = bitmap.width / 3
//                    height = bitmap.height / 3
//                    if (width < 480) {
//                        width = bitmap.width / 2
//                        if (width < 480) {
//                            width = bitmap.width
//                        }
//                    }
//                    if (height < 640) {
//                        height = bitmap.height / 2
//                        if (height < 640) {
//                            height = bitmap.height
//                        }
//                    }
//                }
            }

            if (bitmap != null) {
                Log.d(CameraTestActivity.TAG, "Before Width " + bitmap.width + " Height " + bitmap.height)
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

                Log.d(CameraTestActivity.TAG, "after Width $width Height $height")
                return scaledBitmap;

            }

            return null


        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null

    }

    private fun getBitmapForRXCamera(rxCameraData: RxCameraData): Bitmap? {

        val cameraFragment = cameraFragment

        if (cameraFragment != null) {
            if (cameraFragment.currentFace === CameraFace.FACE_BACK) {
                rxCameraData.rotateMatrix.setRotate(90f)
            }

            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = false
            if (Build.MODEL.contains("LG-H918")) {
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888

            } else {
                opts.inPreferredConfig = Bitmap.Config.RGB_565
            }
            opts.inDither = true
            val bitmap = BitmapFactory.decodeByteArray(rxCameraData.cameraData, 0, rxCameraData.cameraData.size, opts)


            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rxCameraData.rotateMatrix, false)

        }
        return null

    }

    private fun getBitmapForCameraAPI1(data: ByteArray): Bitmap? {
        try {
            val rotateMatrix = Matrix()

            if (cameraFragment?.currentFace === CameraFace.FACE_FRONT) {
                rotateMatrix.postRotate(270F, 0.5f, 0.5f)
            } else {
                rotateMatrix.postRotate(90F, 0.5f, 0.5f)
            }

            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = false
            opts.inPreferredConfig = Bitmap.Config.RGB_565
            opts.inDither = true

            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, opts)

            if (bitmap != null) {
                var width = bitmap.width / 3
                var height = bitmap.height / 3

                if (cameraFragment?.currentFace === CameraFace.FACE_BACK) {

                    if (width < 480) {
                        width = bitmap.width / 2
                        if (width < 480) {
                            width = bitmap.width
                        }
                    }
                    if (height < 640) {
                        height = bitmap.height / 2
                        if (height < 640) {
                            height = bitmap.height
                        }
                    }
                }

                Log.d(CameraTestActivity.TAG, "Before Width " + bitmap.width + " Height " + bitmap.height)
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

                Log.d(CameraTestActivity.TAG, "after Width $width Height $height")

                return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, rotateMatrix, true)
            }


        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }

        return null
    }


    fun onBitmapFailed() {
        progress?.let {
            if (it.isShowing) {
                it.cancel()
            }
        }
        showErrorMessage(getString(R.string.err_loading_img))
    }

    fun onBitmapReady(bitmap: Bitmap) {
        val test = test ?: return
        val cameraFragment = cameraFragment ?: return

        cameraPictureImageView.visibility = View.VISIBLE
        cameraPictureImageView.setImageBitmap(bitmap)


        showDidFlashMessage()

        progress?.let {
            if (it.isShowing) {
                it.cancel()
            }
        }
    }

    fun showDidFlashMessage() {
        cameraFragment?.let { cameraFragment ->
            test?.let { test ->
                if (currentFace === CameraFace.FACE_FRONT && test.resultsFilterMap[Test.frontCameraFlashTestKey] ?: false || currentFace === CameraFace.FACE_BACK && test.resultsFilterMap[Test.rearCameraFlashTestKey] ?: false) {
                    showFlashMessage()
                }
            }

        }

    }

    override fun onDestroy() {
        progress?.let {
            if (it.isShowing) {
                it.cancel()
            }
        }
        super.onDestroy()
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
                if (currentFace == CameraFace.FACE_FRONT) {
                    FRONT_CAMERA_SCREEN_TIME = Loader.TIME_VALUE
                    try {
                        val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                        Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_frontcam), -1)] =
                                RecordTest(context.getString(R.string.report_frontcam_test), FRONT_CAMERA_SCREEN_TIME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Loader.RECORD_TESTS_TIME.put("Front Camera", "${FRONT_CAMERA_SCREEN_TIME}s")
                } else if (currentFace == CameraFace.FACE_BACK) {
                    REAR_CAMERA_SCREEN_TIME = Loader.TIME_VALUE
                    try {
                        val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                        Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_rearcam), -1)] =
                                RecordTest(context.getString(R.string.report_rear_test), REAR_CAMERA_SCREEN_TIME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Loader.RECORD_TESTS_TIME.put("Rear Camera", "${REAR_CAMERA_SCREEN_TIME}s")
                }
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    private inner class CameraTestClickListener : View.OnClickListener {

        override fun onClick(v: View) {
            val test = test ?: return

            if (v.id == R.id.passBtn) {
                if (currentFace === CameraFace.FACE_FRONT) {
                    test.sub(Test.frontCameraQualityTestKey)?.value = Test.PASS
                } else {
                    test.sub(Test.rearCameraQualityTestKey)?.value = Test.PASS
                }
            } else if (v.id == R.id.failBtn) {
                if (currentFace === CameraFace.FACE_FRONT) {
                    test.sub(Test.frontCameraQualityTestKey)?.value = Test.FAILED
                } else {
                    test.sub(Test.rearCameraQualityTestKey)?.value = Test.FAILED
                }
            }

            finalizeTest()
        }
    }


    private inner class AlertClickListener : AlertButtonListener {

        override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
            val test = test ?: return
            if (type == AlertButtonListener.ButtonType.RIGHT) {
                if (currentFace === CameraFace.FACE_BACK) {
                    /*rearBarcodeText.visibility = View.VISIBLE*/
                    test.sub(Test.rearCameraFlashTestKey)?.value = Test.PASS
                } else
                    test.sub(Test.frontCameraFlashTestKey)?.value = Test.PASS
            } else {
                if (currentFace === CameraFace.FACE_BACK) {
                    test.sub(Test.rearCameraFlashTestKey)?.value = Test.FAILED
                    /*rearBarcodeText.visibility = View.VISIBLE*/
                } else
                    test.sub(Test.frontCameraFlashTestKey)?.value = Test.FAILED
            }
            dialog.dismiss()

            failBtn.visibility = View.VISIBLE
            passBtn.visibility = View.VISIBLE
            /*if (camera.getConfig().isFaceCamera) {
                switchCamera();
            } else if (!camera.getConfig().isFaceCamera && autoPerform) {
                if (autoPerform) {
                    endTest();
                }
            }*/
        }
    }

    private fun hideCameraQualityButtons(hide: Boolean) {
        pictureTakenLayout.visibility = if (hide) View.INVISIBLE else View.VISIBLE
    }

    private fun hideCameraTakeButton(hide: Boolean) {
        cameraTakeBtn.visibility = if (hide) View.INVISIBLE else View.VISIBLE
    }

    protected fun showFlashMessage() {
        val alertDialog = DialogUtils.createConfirmationAlert(context, R.string.flash_test, R.string.did_flash_fire, context.getString(R.string.no), context.getString(R.string.yes), AlertClickListener())
        if (!(context as Activity).isFinishing()) {
            alertDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPictureTaken) {
            cameraFragment?.onCloseCamera()
            cameraFragment?.openCamera()
        }
        if (Build.MODEL.containsIgnoreCase(Constants.S3)) {
            cameraFragment?.onCloseCamera()
            cameraFragment?.openCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.MODEL.containsIgnoreCase(Constants.S3)) {
            return
        }
        cameraFragment?.onCloseCamera()
    }


    companion object {
        val REQ = 2
        val REQ_FRONT = 202
        private val TAG = "Camera Activity"
        val CAMERA_TYPE_KEY = "CAMERA_TYPE_KEY"
        var FRONT_CAMERA_SCREEN_TIME = 0
        var REAR_CAMERA_SCREEN_TIME = 0
    }

}
