package com.upgenicsint.phonecheck.activities

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.GestureDetector
import android.view.KeyCharacterMap
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.widget.Toast
import co.balrampandey.logy.Logy
import com.farhanahmed.cabinet.Cabinet
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.*
import com.upgenicsint.phonecheck.adapter.TestListAdapter
import com.upgenicsint.phonecheck.misc.*
import com.upgenicsint.phonecheck.misc.Constants.DETAIL
import com.upgenicsint.phonecheck.services.FingerprintHideService
import com.upgenicsint.phonecheck.services.PowerListenerService
import com.upgenicsint.phonecheck.services.TTSService
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.*
import com.upgenicsint.phonecheck.test.hardware.BatteryTest
import com.upgenicsint.phonecheck.test.hardware.CallTest
import com.upgenicsint.phonecheck.test.hardware.DualCallTest
import com.upgenicsint.phonecheck.test.hardware.LCDTest
import com.upgenicsint.phonecheck.test.sensor.AccelerometerTest
import com.upgenicsint.phonecheck.test.sensor.AutoVibrationTest
import com.upgenicsint.phonecheck.test.sensor.ProximityTest
import com.upgenicsint.phonecheck.test.sensor.VibrationTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import com.upgenicsint.phonecheck.utils.Tools
import com.upgenicsint.phonecheck.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_auto_start.*
import org.json.JSONException
import org.json.JSONObject
import pl.tajchert.nammu.Nammu
import radonsoft.net.rta.RTA
import java.lang.Exception
import java.util.*

class MainActivity : BaseActivity(), Permissionable, SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var alreadyPassed: Boolean = false
    private var xpositive: Boolean = false
    private var ypositive: Boolean = false
    private var xnegative: Boolean = false
    private var ynegative: Boolean = false

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && !alreadyPassed) {
            if (alreadyPassed) {
                return
            }
            getAccelerometer(event)
        }
    }

    private fun getAccelerometer(event: SensorEvent) {
        val values: FloatArray = event.values
        val x: Float = values[0]
        val y: Float = values[1]
        val z: Float = values[2]
        if (x >= 2f && !xpositive) {
            xpositive = true
            Log.d("Sensor", "x: $x y: $y z: $z")
        }
        if (y >= 2f && !ypositive) {
            ypositive = true
            Log.d("Sensor", "x: $x y: $y z: $z")
        }
        if (y <= -2f && !ynegative) {
            ynegative = true
            Log.d("Sensor", "x: $x y: $y z: $z")
        }
        if (x <= -2f && !xnegative) {
            xnegative = true
            Log.d("Sensor", "x: $x y: $y z: $z")
        }
        if ((xpositive && ypositive && xnegative) || (ypositive && xpositive && ynegative) ||
                (ypositive && xnegative && ynegative) || (xpositive && ynegative && xnegative)) {
            alreadyPassed = true
            if (isAccelerometerLoaded && Loader.instance.isAutoAccelEnabled) {
                testAccelerometer()
            } else if (isAccelerometerLoaded && Loader.instance.isAccelEnabled) {
                Log.d(TAG, "Go to Accelerometer test screen to perform this test")
            } else {
                testAccelerometer()
            }
        }
    }

    private fun testAccelerometer() {
        val test = Loader.instance.getByClassType(AccelerometerTest::class.java)
        if (test != null) {
            test.sub(Test.accelerometerTestKey)?.value = Test.PASS
            test.sub(Test.gyroTestKey)?.value = Test.PASS
            test.sub(Test.screenRotationTestKey)?.value = Test.PASS
            test.status = Test.PASS
            test.reviewTest()
            testListAdapter!!.notifyDataSetChanged()
        }
    }

    internal var testListAdapter: TestListAdapter? = null
    internal lateinit var testList: List<Test>
    internal var currentActivityTest: Test? = null
    internal var currentTestPosition = 0
    var receiver: Boolean = false

    private var bixlightServiceId: String? = null

    private lateinit var storeStateOperation: StoreOperation
    private lateinit var storeFilterOperation: StoreOperation
    private var isProximityPass = false
    private var isAccelerometerLoaded = false
    //HashMap<String, Boolean> completedList = new HashMap<>();
    var showVibrationHint = false
    var showAutoVibrationHint = false
    private var powerServiceIntent: Intent? = null

    private val permissionList: Array<String>
        get() {
            val perm = ArrayList<String>()
            perm.add(Manifest.permission.READ_PHONE_STATE)
            perm.add(Manifest.permission.GET_ACCOUNTS)
            perm.add(Manifest.permission.RECORD_AUDIO)
            perm.add(Manifest.permission.CAMERA)
            perm.add(Manifest.permission.CALL_PHONE)
            perm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            perm.add(Manifest.permission.ACCESS_FINE_LOCATION)
            perm.add(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                perm.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                perm.add(Manifest.permission.USE_FINGERPRINT)
            }

            return perm.toTypedArray()
        }

    private val allowedTest = listOf(WifiTest::class.java, GPSTest::class.java, BatteryTest::class.java, BluetoothTest::class.java, BluetoothTest2::class.java, VibrationTest::class.java)

    override val backPress: Boolean
        get() = true

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fadeout, R.anim.fadeout)
        //BUILD_VERSION = getString(R.string.build_version)
        startService(Intent(this, TTSService::class.java))
        powerServiceIntent = Intent(context, PowerListenerService::class.java)
        startService(powerServiceIntent)
        setContentView(R.layout.activity_main)
        Logy.setEnable(BuildConfig.DEBUG)
        Nammu.init(applicationContext)


        /*  //Clear Firebase Database
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference ref = firebaseDatabase.getReferenceFromUrl("https://phonecheck-edcc0.firebaseio.com/");
        ref.removeValue();*/


        /*homeTextView.setOnLongClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Test List")
            builder.setMessage("By Default Test List Filter is enabled you can temporarily enable or disable Test List.\n(Press Reset Button to reload tests.)")
            builder.setPositiveButton("On") { dialog, which ->
                Loader.enableLoadFilter = true
                dialog.dismiss()
            }
            builder.setNegativeButton("Off") { dialog, which ->
                Loader.enableLoadFilter = false
                dialog.dismiss()
            }
            builder.setNeutralButton("Cancel") { dialog, which -> dialog.dismiss() }
            builder.create().show()
            Logy.i(packageName, "rhkg38yw4w-" + Loader.RESULT.toString().trim { it <= ' ' } + "-4rhjg7x9gw")
            true
        }*/

        //logBatteryInformation();

        storeStateOperation = Cabinet.open(context, R.string.device_results)
        storeFilterOperation = Cabinet.open(context, R.string.device_filter)
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        testRecyclerView.setLayoutManager(layoutManager)
        testRecyclerView.setItemAnimator(null)
        testRecyclerView.addItemDecoration(SpaceItemDivider(context))
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        checkMissingPermission()

        @SuppressLint("ClickableViewAccessibility")
        val phoneCheckVersion = findViewById(R.id.buildTextView) as TextView
        val gestureDetector = GestureDetector(this@MainActivity,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        val intent = Intent(this@MainActivity, TestResultsActivity::class.java)
                        intent.putExtra(DETAIL, Constants.TEST)
                        startActivity(intent)
                        return true
                    }

                    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                        return true
                    }

                    override fun onDown(e: MotionEvent): Boolean {
                        return true
                    }
                })
        phoneCheckVersion.performClick()
        phoneCheckVersion.setOnTouchListener { v, m -> gestureDetector.onTouchEvent(m) }

        val phoneCheckResults = findViewById<TextView>(R.id.homeTextView)
        val gestureDetector1 = GestureDetector(this@MainActivity,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        val intent = Intent(this@MainActivity, TestResultsActivity::class.java);
                        intent.putExtra(DETAIL, Constants.CHECK)
                        startActivity(intent)
                        return true
                    }

                    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
                        return true
                    }

                    override fun onDown(e: MotionEvent?): Boolean {
                        return true
                    }
                })
        phoneCheckResults.performClick()
        phoneCheckResults.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, m: MotionEvent?): Boolean {
                return gestureDetector1.onTouchEvent(m)
            }
        })

    }

//    override fun attachBaseContext(base: Context) {
//        super.attachBaseContext(LocaleHelper.onAttach(base))
//    }

    private fun checkMissingPermission() {
        val perm = permissionList

        if (checkForPermissions(perm)) {
            mainSetup()
        } else {
            Nammu.askForPermission(this, perm, PermissionCallBackAdapter(this))
        }
    }


    override fun onPermissionGranted() {
        mainSetup()
    }

    override fun onPermissionRefused() {
        Toast.makeText(context, "Permission refused", Toast.LENGTH_SHORT).show()
        //Loader.enableLoadFilter = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mainSetup()
        } else {
            startActivity(Intent(context, PermissionActivity::class.java))
            this@MainActivity.finish()
        }
    }


    private fun checkForPermissions(perms: Array<String>): Boolean {
        for (s in perms) {
            if (!Nammu.checkPermission(s)) {
                return false
            }
        }
        return true
    }

    private var isActivityCreated: Boolean = false

    /**
     * Method for main setup when permission check is completed
     */
    private fun mainSetup() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) {
                val fingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager?
                if (fingerprintManager != null) {
                    Loader.instance.fingerPrintSupport = fingerprintManager.isHardwareDetected
                }
            }
        }

        val accelSupported = sensorManager.registerListener(this, sensorManager.getDefaultSensor
        (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        if (!accelSupported) {
            // no accelerometer on this device
            sensorManager.unregisterListener(this)
        }

        Loader.writeDeviceInfoFile(context)

        Loader.instance.loadTest(context)

        testList = Loader.instance.testList
        /*for (Test test : testList) {
            completedList.put(test.getTitle(), true);
        }*/
        setupAdapter(testList)

        FirebaseUtil
                .addNew(FirebaseUtil.SENSOR).child("Proximity")
                .setValue(if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY)) "Yes" else "No")

        FirebaseUtil.addNew(FirebaseUtil.TELEPHONE).child("Telephony")
                .setValue(if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) "Yes" else "No")
        FirebaseUtil.addNew(FirebaseUtil.TELEPHONE).child("Telephony_GSM")
                .setValue(if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM)) "Yes" else "No")
        FirebaseUtil.addNew(FirebaseUtil.TELEPHONE).child("Telephony_CDMA")
                .setValue(if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) "Yes" else "No")

        if (Loader.instance.filterContains(Test.autoVibrationTestKey)) {
            showVibrationHint = Loader.instance.getByClassType(AutoVibrationTest::class.java) != null
        } else {
            showVibrationHint = Loader.instance.getByClassType(VibrationTest::class.java) != null
        }

        isAccelerometerLoaded = Loader.instance.getByClassType(AccelerometerTest::class.java) != null


//        isBixEnabled()

        // method to start the test automatically without pressing start button
        val customizations = Loader.instance.clientCustomization
        if (customizations != null && customizations.isAutoStartBatteryDrain && !isActivityCreated) {
            isActivityCreated = true
            when {
                /*customizations.isCommentsAdded && !BatteryDiagnosticActivity.isConnected(this@MainActivity) -> {
                    val intent = Intent(context, SaveComments::class.java)
                    activity.startActivity(intent)
                }*/
                customizations.isAutoStartBatteryDrain -> {
                    val intent = Intent(context, BatteryDiagnosticActivity::class.java)
                    activity.startActivity(intent)
                }
                else -> {
                }
            }
        } else {
            if (!autostart) {
                startTest()
            }
        }
    }

    private fun isBixEnabled() {
        if (Build.MANUFACTURER.containsIgnoreCase("samsung")) {
            if (KeyCharacterMap.deviceHasKey(1082)) {
                var ret = false
                val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                if (accessibilityManager != null) {
                    val runningServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    for (service in runningServices) {
                        if (service.id.equals(bixlightServiceId)) {
                            ret = true
                            break
                        }
                    }
                    if (ret == false) {
                        Toast.makeText(this, "Please enable bixby service", Toast.LENGTH_SHORT).show()
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun setupAdapter(testList: List<Test>) {
        if (testRecyclerView != null) {
            testListAdapter = TestListAdapter(context, testList) { position, view ->
                if (BaseActivity.isSoftBackPressed && currentActivityTest !is VibrationTest) {
                    BaseActivity.isSoftBackPressed = false
                }
                if (BaseActivity.isSoftBackPressed && currentActivityTest !is AutoVibrationTest) {
                    BaseActivity.isSoftBackPressed = false
                }
                performTest(testRecyclerView, position, view)
            }

            if (testList.size == 0) {
                testRecyclerView.visibility = View.GONE
                appBar.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                emptyView.visibility = View.GONE
                testRecyclerView.visibility = View.VISIBLE
                appBar.visibility = View.VISIBLE
            }

            testRecyclerView.setAdapter(testListAdapter)
        }
        if (testList.size <= 0)
            emptyRetryButton.setOnClickListener({ mainSetup() })

        /*if (ALLOW_SHOWCASE)
            showAutoPerformShowCase();*/

    }

    /*private void showAutoPerformShowCase() {
        boolean check = Cabinet.open(getContext(), R.string.showcase_pref).getBoolean("autoperform", false);
        if (check) {
            return;
        }
        ViewTarget target = new ViewTarget(R.id.autoPerformButton, this);
        sv = new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setTarget(target)
                .setContentTitle("Auto Perform")
                .setStyle(R.style.CustomShowcaseTheme2)
                .setOnClickListener(this)
                .setContentText("You can automatically perform all tests.")
                .build();
        sv.setTag("AUTOPERFORM");
    }

    private void showResetShowCase() {
        boolean check = Cabinet.open(getContext(), R.string.showcase_pref).getBoolean("reset", false);
        if (check) {
            return;
        }
        ViewTarget target = new ViewTarget(R.id.resetButton, this);
        sv = new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setTarget(target)
                .setContentTitle("Reset")
                .setStyle(R.style.CustomShowcaseTheme2)
                .setOnClickListener(this)
                .setContentText("Reset all test and their result.")
                .build();
        sv.setTag("RESET");

    }

    private void showListRowShowCase() {
        boolean check = Cabinet.open(getContext(), R.string.showcase_pref).getBoolean("list_row", false);
        if (check) {
            return;
        }
        TestListAdapter.TestViewHolder holder = (TestListAdapter.TestViewHolder) testRecyclerView.findViewHolderForAdapterPosition(0);
        ViewTarget target = new ViewTarget(holder.iconImageView);
        sv = new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setTarget(target)
                .setContentTitle("How to perform test?")
                .setStyle(R.style.CustomShowcaseTheme2)
                .setOnClickListener(this)
                .setContentText("Tests are performed when select test from ListSelector.")
                .build();
        sv.setTag("LIST_ROW");

    }*/

    private var selectView: View? = null

    /**
     * Method for performing test when selected from RecyclerView*
     *
     * @param recyclerView ListSelector of loaded test.
     * @param position     the current of selected test.
     * @param v            row view from RecyclerView which clicked
     */
    private fun performTest(recyclerView: RecyclerView, position: Int, v: View?) {
        if (position < 0 || position >= testList.size) {
            return
        }
        currentTestPosition = position
        selectView = v

        /*val currentTest = currentActivityTest;
        if (!isProximityPass && currentTest is ProximityTest) {
            isProximityPass = true

            //currentActivityTest.isClear = false

            currentTest.unSetAtWarning()
            try {
                currentTest.onSaveFilter(storeFilterOperation)
                currentTest.onSaveState(storeStateOperation)

                testListAdapter?.let { adapter ->
                    adapter.notifyItemChanged(adapter.getPosition(currentTest))
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }

            currentTest.onFinish()
            testWatcher()
        }*/


        currentActivityTest = testList[position]

        currentActivityTest?.let { currentActivityTest ->


            Logy.d(TAG, "Running " + currentActivityTest.title ?: "")

            testListAdapter?.resetItem(position)
            testListAdapter?.setLastSelectedItemPosition(position)


            currentActivityTest.completeState = Test.TOUCHED
            if (currentActivityTest.showHintMessage()) {
                changeHeaderText(currentActivityTest.detail)
            }


            /**
             * test which require a activity to open, pass fail status in calculated on OnActivityResult method
             * */

            if (currentActivityTest.requireActivity() && !currentActivityTest.requireUserInteraction()) {
                currentActivityTest.perform(context, autoPerform)

            } else if (currentActivityTest.requireUserInteraction()) {

                currentActivityTest.testListener = object : Test.Listener {
                    override fun onPerformDone() {
                        currentActivityTest.performUserInteraction()
                    }

                    override fun onUserInteractionDone(shouldGoNext: Boolean) {
                        if (currentActivityTest is ProximityTest) {
                            isProximityPass = true
                        }
                        if (!shouldGoNext) {
                            return
                        }
                        if (currentActivityTest.status == Test.PASS) {
                            testSuccess(currentActivityTest, recyclerView, v, position, true)
                            if (autoPerform) {
                                if (currentTestPosition + 1 < Loader.instance.testList.size &&
                                        Loader.instance.testList[currentTestPosition + 1] is AccelerometerTest && alreadyPassed) {
                                    if (isAccelerometerLoaded && Loader.instance.isAutoAccelEnabled) {
                                        autoNextTest(position + 1)
                                    } else if (isAccelerometerLoaded && Loader.instance.isAccelEnabled) {
                                        autoNextTest(position)
                                    } else {
                                        autoNextTest(position + 1)
                                    }
                                } else {
                                    autoNextTest(position)
                                }
                            }
                        } else {
                            testFailed("onUserInteractionDone", currentActivityTest, recyclerView, v, position, false)
                            if (autoPerform && isAllowedToContinue(currentActivityTest)) {
                                if (currentTestPosition + 1 < Loader.instance.testList.size &&
                                        Loader.instance.testList[currentTestPosition + 1] is AccelerometerTest && alreadyPassed) {
                                    if (isAccelerometerLoaded && Loader.instance.isAutoAccelEnabled) {
                                        autoNextTest(position + 1)
                                    } else if (isAccelerometerLoaded && Loader.instance.isAccelEnabled) {
                                        autoNextTest(position)
                                    } else {
                                        autoNextTest(position + 1)
                                    }
                                } else {
                                    autoNextTest(position)
                                }
                            }
                        }
                        //currentActivityTest.isClear = false
                        currentActivityTest.onFinish()
                        currentActivityTest.testListener = null
                    }

                    override fun onUserInteractionCancel(shouldGoNext: Boolean) {
                        if (currentActivityTest is ProximityTest) {
                            isProximityPass = false
                        }

                        if (!shouldGoNext) {
                            return
                        }
                        //currentActivityTest.isClear = false
                        currentActivityTest.status = Test.FAILED
                        testFailed("onUserInteractionCancel", currentActivityTest, recyclerView, v, position, false)
                        if (autoPerform && isAllowedToContinue(currentActivityTest)) {
                            if (currentActivityTest is AccelerometerTest && currentActivityTest.status == Test.PASS) {
                                autoNextTest(position + 1)
                            } else {
                                autoNextTest(position)
                            }
                        }

                        currentActivityTest.testListener = null
                    }
                }
                currentActivityTest.perform(context, autoPerform)
            } else {
                /*
                * test which simple and return status when perform is called*/
                if (currentActivityTest.perform(context, false) == Test.PASS) {
                    //currentActivityTest.isClear = false
                    testSuccess(currentActivityTest, recyclerView, v, position, true)
                    if (autoPerform) {
                        if (currentTestPosition + 1 < Loader.instance.testList.size
                                && Loader.instance.testList[currentTestPosition + 1] is ProximityTest
                                && Devices.isS10Available()) {
                            autoNextTest(position + 1)
                        } else {
                            autoNextTest(position)
                        }
                        //autoNextTest(position)
                    } else {
                    }
                } else {
                    //currentActivityTest.isClear = false
                    testFailed("Simple Test ", currentActivityTest, recyclerView, v, position, false)
                    if (autoPerform && isAllowedToContinue(currentActivityTest)) {
                        if (currentTestPosition + 1 < Loader.instance.testList.size
                                && Loader.instance.testList[currentTestPosition + 1] is ProximityTest
                                && Devices.isS10Available()) {
                            autoNextTest(position + 1)
                        } else {
                            autoNextTest(position)
                        }
                        //autoNextTest(position)
                    } else {
                    }
                }
            }/*
            * case 1:test which require simple input from user
            * case 2:User click on test and test requires permission popup;
            * */
        }

    }

    /*
    * Method for incrementing test running count and checking currently running test is last on the ListSelector.
    * if count is equal to total test and currently running test also last one then save tests result on
    * TestResults.json file and open TestCompleteActivity.*/
    private fun testWatcher() {
        Cabinet.open(context, R.string.settings_pref).add("isStarted", true).save()
        if (!autoPerform) {
            return
        }

        /*completedList.remove(currentActivityTest.getTitle());

        Logy.d(TAG, currentActivityTest.getTitle() + " isRemoved " + completedList.containsKey(currentActivityTest.getTitle()));

        FirebaseUtil.addNew("testWatcher")
                .child(currentActivityTest.getTitle())
                .setValue(completedList.containsKey(currentActivityTest.getTitle()));*/

        //Log.d("TEST COUNT","Size "+completedList.size() + " Test "+currentActivityTest.getClass().getSimpleName());

        //Logy.d(TAG, "completedList " + completedList.size() + "");

        if (currentTestPosition == testList.size - 1) {
            //FirebaseUtil.addNew("testWatcher").child("isCompleted").setValue(true);
            Cabinet.open(context, R.string.settings_pref).add("isCompleted", true).save()
            //completedList = new HashMap<>();
            resetHeaderText()

            autoPerformButtonStateReset()

            dumpTestResultToFile()
            dumpCosmeticTestResultToFile()

            if (autoPerform) {
                autoPerform = false
                BaseActivity.isAutoPerformRunning = false

                val customizations = Loader.instance.clientCustomization
                if (customizations != null) {
                    when {
                        customizations.isCommentsAdded && !BatteryDiagnosticActivity.isConnected(this@MainActivity) -> {
                            val intent = Intent(context, SaveComments::class.java)
                            activity.startActivity(intent)
                        }
                        customizations.isAutoBatteryDrain -> {
                            val intent = Intent(context, BatteryDiagnosticActivity::class.java)
                            activity.startActivity(intent)
                        }
                        customizations.isAutoStartBatteryDrain -> {
                            val intent = Intent(context, BatteryDiagnosticActivity::class.java)
                            activity.startActivity(intent)
                        }
                        else -> {
                            val intent = Intent(context, TestCompletionActivity::class.java)
                            activity.startActivity(intent)
                        }
                    }
                } else {
                    val intent = Intent(context, TestCompletionActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }
        /*if (completedList.size() == 0) {
            FirebaseUtil.addNew("testWatcher").child("isCompleted").setValue(true);
            Cabinet.open(getContext(), R.string.settings_pref).add("isCompleted", true).save();
            completedList = new HashMap<>();
            resetHeaderText();

            autoPerformButtonStateReset();

            dumpTestResultToFile();
            if (autoPerform) {
                autoPerform = false;
                isAutoPerformRunning = false;

                Intent intent = new Intent(getContext(), TestCompletionActivity.class);
                startActivity(intent);
            }

        }*/
    }

    private fun autoPerformButtonStateReset() {
        autoPerformButton.setImageResource(R.drawable.start)
        if (autoPerformButton.getAnimation() == null) {
            autoPerformButton.resetAnimation()
        }
        if (autoPerformButton.getAnimation() != null)
            autoPerformButton.getAnimation().start()

    }

    private fun dumpTestResultToFile() {

        for (test in Loader.instance.testList) {
            if (!test.title.containsIgnoreCase("Cosmetics Test")) {
                test.createResult()
            }
        }

        Loader.instance.dumpResultToFile(this)
        val sharedPreferences = this.getSharedPreferences("Barcode Result", Context.MODE_PRIVATE)
        var barcodeResult: String? = null
        barcodeResult = sharedPreferences.getString("BarcodeScanned", "")
        Log.i(packageName, Loader.RESULT_BARCODE_START_PREFIX + barcodeResult + Loader.RESULT_BARCODE_END_PREFIX)
        Log.i(packageName, Loader.RESULT_START_PREFIX + Loader.RESULT.toString() + Loader.RESULT_END_PREFIX)
    }

    fun dumpCosmeticTestResultToFile() {
        for (test in testList) {
            if (test.title.containsIgnoreCase("Cosmetics Test")) {
                test.createCosmeticResult()
            }
        }
        Loader.instance.dumpCosmeticsResultToFile()
        Log.i(packageName, Loader.RESULT_COSMETICS_START_PREFIX + Loader.RESULTCOSMETICS.toString() + Loader.RESULT_COSMETICS_END_PREFIX)
    }


    /*
    * @params caller caller function name*/
    private fun testFailed(caller: String, test: Test, recyclerView: RecyclerView, v: View?, position: Int, animate: Boolean) {
        test.completeState = Test.COMPLETED
        Logy.d(TAG, caller + " " + currentActivityTest?.javaClass?.simpleName + " Failed")
        resetHeaderText()
        try {
            if (test.title.containsIgnoreCase("Cosmetics Test")) {
                test.onSaveStateCosmetics(storeStateOperation)
            } else {
                test.onSaveState(storeStateOperation)
            }
            test.onSaveFilter(storeFilterOperation)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        animateStatus(recyclerView, position)
        if (v != null && animate) {
            Handler(mainLooper).postDelayed({
                if (!BaseActivity.isSoftBackPressed)
                    recyclerView.smoothScrollBy(v.x.toInt() + v.width, v.y.toInt() + v.height)
            }, 600)
        }
        testWatcher()

    }

    private fun isAllowedToContinue(test: Test): Boolean {
        val allowedList = allowedTest
        for (c in allowedList) {
            if (c == test.javaClass) {
                return true
            }
        }
        return false
    }

    private fun testSuccess(test: Test, recyclerView: RecyclerView, v: View?, position: Int, animate: Boolean) {
        test.completeState = Test.COMPLETED
        resetHeaderText()
        try {
            if (test.title.containsIgnoreCase("Cosmetics Test")) {
                test.onSaveStateCosmetics(storeStateOperation)
            } else {
                test.onSaveState(storeStateOperation)
            }
            test.onSaveFilter(storeFilterOperation)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        animateStatus(recyclerView, position)
        if (v != null && animate) {
            Handler(mainLooper).postDelayed({
                if (!BaseActivity.isSoftBackPressed)
                    recyclerView.smoothScrollBy(v.x.toInt() + v.width, v.y.toInt() + v.height)
            }, 600)
        }
        testWatcher()
    }

    private fun resetHeaderText() {
        if (headerCurrentTestTextView != null) {
            headerCurrentTestTextView.setText("")
        }
    }

    private fun changeHeaderText(text: String) {
        if (headerCurrentTestTextView == null) {
            return
        }
        Tools.flipView(headerCurrentTestTextView)
        headerCurrentTestTextView.setText(text)
    }

    private fun animateStatus(recyclerView: RecyclerView, position: Int) {
        /*Run this method code in Main Thread */
        runOnUiThread {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) as TestListAdapter.TestViewHolder?

            if (holder != null) {
                val test = testList[position]

                if (test.status == Test.INIT) {
                    Utils.loadImage(R.drawable.warning, holder.statusImageView)
                } else if (test.isPassed) {
                    Utils.loadImage(R.drawable.blue_check, holder.statusImageView)
                } else {
                    Utils.loadImage(R.drawable.not_working, holder.statusImageView)
                }
                Tools.scaleUp(holder.statusImageView)

                testListAdapter?.notifyItemChanged(position)
            } else {
                Logy.e(TAG, "Holder found null @ animateStatus")
            }
        }
    }

    /*fun changePhoneCheckTextColor() {

    }*/

    fun routePhoneCheck(view: View) {
        startActivity(Intent(this@MainActivity, PhoneCheckWebsiteActivity::class.java))
    }

    @SuppressLint("ApplySharedPref")
    fun resetClick(view: View) {
        autoPerformButtonStateReset()
        //completedList = new HashMap<>();
        autoPerformButton.isEnabled = true
        autoPerformButton.setImageResource(R.drawable.start)
        Loader.instance.clientCustomization = null
        currentTestPosition = 0
        isSoftBackPressed = false
        auto_start_mode = false
        shouldMoveToNextTest = true
        autoPerform = false
        AudioInputTestActivity.isSpeakerWorking = false
        isProximityPass = false
        isAutoPerformRunning = false
        Loader.RESULT = JSONObject()
        Loader.RESULT_TIME = JSONObject()
        Loader.RECORD_TESTS_TIME = JSONObject()
        Loader.TOTAL_SCREEN_TIME = 0
        Loader.TIME_VALUE = 0
        Loader.RECORD_TIMER_TASK = null
        try {
            removeAllReprotPreferences(this@MainActivity)
            Loader.instance.recordList.clear()
        } catch (e: Exception) {
            Log.d(TAG, "record list is empty")
        }
        getDefaultScreenTimeValues()
        if (testRecyclerView != null) {
            testRecyclerView.smoothScrollToPosition(0)
        }
        resetHeaderText()

//        val pref2 by lazy { Cabinet.open(context, R.string.speech_pref) }
//        pref2.add(SpeechRecognization.SPEECH_AUTO_START_KEY, false)
//        pref2.save()

        val pref = applicationContext.getSharedPreferences(RTA.AUTO_START_MIC_QUALITY, Context.MODE_PRIVATE)
        val barcodePref = applicationContext.getSharedPreferences("Barcode Result", Context.MODE_PRIVATE)
        val editor = pref.edit()
        val barcodeEditor = barcodePref.edit()
        barcodeEditor.putString("BarcodeScanned", null)
        barcodeEditor.clear()
        barcodeEditor.commit()
        editor.putBoolean(RTA.AUTO_MIC_QUALITY, false)
        editor.putInt(RTA.MIC_QUALITY_TEST_STATUS, 2)
        editor.putInt(RTA.VIDMIC_QUALITY_TEST_STATUS, 2)
        editor.clear()
        editor.commit()

        Cabinet.open(context, R.string.settings_pref).add("isCompleted", false).save()
        Cabinet.open(context, R.string.settings_pref).add("isStarted", false).save()
        Cabinet.open(context, R.string.settings_pref).add(AudioInputTestActivity.AUDIO_AUTO_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add(MicCheckTestActivity.AUDIO_AUTO_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add(MicLSTestActivity.AUDIO_AUTO_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add(NewMicLSTestActivity.AUDIO_AUTO_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add(AudioPlaybackTestActivity.MIC_PLAYBACK, false).save()
        Cabinet.open(context, R.string.settings_pref).add(MicESTestActivity.AUDIO_AUTO_ES_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add(NewMicESTestActivity.AUDIO_AUTO_ES_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add(SpeechRecognization.SPEECH_AUTO_START_KEY, false).save()
        Cabinet.open(context, R.string.settings_pref).add("currentTestPosition", 0).save()
        Cabinet.open(context, getString(R.string.settings_pref)).add(WirelessChargingActivity.WIRELESS_STATUS, false).save()

        getSharedPreferences(getString(R.string.device_results), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.device_filter), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.spen_buttons_pref), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.audio_pref), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.speech_pref), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.mic_check), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.mic_ls), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.newmic_ls), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.mic_es), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.newmic_es), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.cosmetics_key), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.resultsPref), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.save_grade), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.manual), Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(getString(R.string.wireless_battery_charging), Context.MODE_PRIVATE).edit().clear().commit()
        //getSharedPreferences(getString(R.string.report_headset), Context.MODE_PRIVATE).edit().clear().commit()

        when {
            MicLSTestActivity.audioReports.exists() -> MicLSTestActivity.audioReports.delete()
            BatteryDiagnosticActivity.batteryStats.exists() -> BatteryDiagnosticActivity.batteryStats.delete()
            SaveComments.commentsFile.exists() -> SaveComments.commentsFile.delete()
            GradingsActivity.gradingResults.exists() -> GradingsActivity.gradingResults.delete()
            Loader.timeTestTakenFile.exists() -> Loader.timeTestTakenFile.delete()
            //MicESTestActivity.headsetReport.exists() -> MicESTestActivity.headsetReport.delete()
            /**
             * androiddeviceconfiguration.json
             * batteryapi.json
             * batteryResults.json
             * batterystat.json
             * gradeconfig.json
             * deviceinfo.json
             * config.json
             */
        }

        alreadyPassed = false
        sensorManager.unregisterListener(this)
        xpositive = false
        xnegative = false
        ypositive = false
        ynegative = false
        isAccelerometerLoaded = false
        mainSetup()
    }

    override fun onPause() {
        super.onPause()
        currentActivityTest?.onFinish()
        //sensorManager.unregisterListener(this)
    }

    override fun onStop() {
        super.onStop()
        currentActivityTest?.onFinish()
    }

    /**
     * #result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADMIN_REQ_CODE) {
            return
        }
        if (currentActivityTest == null || testListAdapter == null) {
            Logy.e(TAG, "currentActivityTest or adapter is null at onActivityResult")
            return
        }

        // settings RTA activity result here
        if (requestCode == 77) {

            /*if (RTA.receiver){
                if (RTA.micQuality){
                    Loader.instance.getByClassType(ReceiverAndMic::class.java)!!.sub(Test.micQualityReceiverTest)!!.value = Test.PASS
                }
                else {
                    Loader.instance.getByClassType(ReceiverAndMic::class.java)!!.sub(Test.micQualityReceiverTest)!!.value = Test.FAILED
                }
                if (RTA.vidMicQuality){
                    Loader.instance.getByClassType(ReceiverAndMic::class.java)!!.sub(Test.videoMicQualityReceiverTestKey)!!.value = Test.PASS
                }
                else {
                    Loader.instance.getByClassType(ReceiverAndMic::class.java)!!.sub(Test.videoMicQualityReceiverTestKey)!!.value = Test.FAILED
                }
                if (RTA.micQuality && RTA.vidMicQuality){
                    Loader.instance.getByClassType(ReceiverAndMic::class.java)!!.status = Test.PASS
                }
                else {
                    Loader.instance.getByClassType(ReceiverAndMic::class.java)!!.status = Test.FAILED
                }
            }
            else {*/
            if (RTA.micQuality || RTA.testStatus == 0) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.PASS
            }
            if (RTA.vidMicQuality || RTA.testStatusVid == 0) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.PASS
            }
            if ((RTA.micQuality && RTA.vidMicQuality) || (RTA.testStatus == 0 && RTA.testStatusVid == 0)) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.PASS
            }
            if (!RTA.micQuality && RTA.testStatus == 1) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.FAILED
            }
            if (!RTA.vidMicQuality && RTA.testStatusVid == 1) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.FAILED
            }
            if (RTA.testStatus == 0 && RTA.testStatusVid == 0) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.PASS
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.PASS
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.PASS
            } else if ((!RTA.micQuality && !RTA.vidMicQuality) && (RTA.testStatus == 1 && RTA.testStatusVid == 1)) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.FAILED
            } else if (RTA.testStatus == 2 && RTA.testStatusVid == 2) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.FAILED
            } else if (RTA.testStatus == 0 && RTA.testStatusVid == 2) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.PASS
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.FAILED
            } else if (RTA.testStatus == 0 && RTA.testStatusVid == 1) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.PASS
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.FAILED
            } else if (RTA.testStatus == 1 && RTA.testStatusVid == 0) {
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.FAILED
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.PASS
                Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.FAILED
            }
//            else {
//                if ((!RTA.micQuality && !RTA.vidMicQuality) || (RTA.testStatus == 1 && RTA.testStatusVid == 1)) {
//                    Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.micQualityTest)!!.value = Test.FAILED
//                    Loader.instance.getByClassType(MicQualityTest::class.java)!!.sub(Test.videoMicQualityTestKey)!!.value = Test.FAILED
//                    Loader.instance.getByClassType(MicQualityTest::class.java)!!.status = Test.FAILED
//                }
//            }
            //           }
        } else if (requestCode == 1786) {
            testListAdapter!!.notifyDataSetChanged()
        } else if (requestCode == FingerPrintActivity.REQ) {
            //stopService(Intent(this@MainActivity, FingerprintHideService::class.java))
        } else if ((requestCode == CallTest.REQ || requestCode == DualCallTest.REQ) && Devices.isS10Available()) {
            val test1 = Loader.instance.getByClassType(ProximityTest::class.java)
            if (test1 != null) {
                test1.status = if (test1.status != Test.PASS) Test.FAILED else test1.status
            }
            testListAdapter?.notifyDataSetChanged()
        } else if (requestCode == NewMicLSTestActivity.REQ || requestCode == NewMicESTestActivity.REQ) {
            testListAdapter!!.notifyDataSetChanged()
        } else if (requestCode == AudioPlaybackTestActivity.REQ) {
            testListAdapter!!.notifyDataSetChanged()
        }

        currentActivityTest?.let { currentActivityTest ->
            //currentActivityTest.isClear = false
            var moveToNext = true
            //Toast.makeText(getContext(), "onActivityResult for " + currentActivityTest.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            Logy.d(TAG, "requestCode " + requestCode + " resultCode " + if (resultCode == Activity.RESULT_OK) "RESULT_OK" else "RESULT_CANCEL")
            /*if (isSoftBackPressed) {
                for (Map.Entry<String, SubTest> entry : currentActivityTest.subTests.entrySet()) {
                    if (!entry.getValue().isPass()) {
                        currentActivityTest.setClear(true);
                        currentActivityTest.setRunning(false);
                        int p = testList.indexOf(currentActivityTest);
                        if (testListAdapter != null) {
                            testListAdapter.notifyItemChanged(p);
                            if (testRecyclerView != null)
                                testRecyclerView.smoothScrollToPosition(p);
                        }
                        return;

                    }
                }
                isSoftBackPressed = false;
            }*/
            if (testListAdapter == null) {
                Logy.e(TAG, "currentActivityTest or adapter is null at onActivityResult")
                return
            }

            currentActivityTest.isRunning = false

            if (currentActivityTest is WifiTest) {
                val test = currentActivityTest as WifiTest
                moveToNext = test.isWifiEnabled
                test.perform(context, autoPerform)
                return
            }

            if (currentActivityTest is LCDTest) {
                BaseActivity.shouldMoveToNextTest = true
            }

            if (!BaseActivity.shouldMoveToNextTest) {
                BaseActivity.shouldMoveToNextTest = currentActivityTest.status == Test.PASS
            }

            if (!BaseActivity.shouldMoveToNextTest && autoPerform) {
                if (currentActivityTest.hasSubTest) {
                    currentActivityTest.reviewTest()
                }


                if (currentActivityTest.status == Test.PASS) {
                    testSuccess(currentActivityTest, testRecyclerView, selectView, currentTestPosition, BaseActivity.shouldMoveToNextTest)
                } else {
                    testFailed("OnActivityResult", currentActivityTest, testRecyclerView, selectView, currentTestPosition, true)
                }

                BaseActivity.shouldMoveToNextTest = true
                return
            }


            if (moveToNext) {
                if (currentActivityTest.hasSubTest) {
                    currentActivityTest.reviewTest()
                }


                if (currentActivityTest.status == Test.PASS) {
                    testSuccess(currentActivityTest, testRecyclerView, selectView, currentTestPosition, false)
                } else {
                    testFailed("OnActivityResult", currentActivityTest, testRecyclerView, selectView, currentTestPosition, false)
                }

                testRecyclerView.adapter!!.notifyItemChanged(testList.indexOf(currentActivityTest))

                if (autoPerform) {
                    if (currentTestPosition + 1 < Loader.instance.testList.size
                            && Loader.instance.testList[currentTestPosition + 1] is ProximityTest
                            && Devices.isS10Available()) {
                        autoNextTest(currentTestPosition + 1)
                    } else if (currentTestPosition + 1 < Loader.instance.testList.size && Loader.instance.testList[currentTestPosition + 1] is AccelerometerTest
                            && alreadyPassed) {
                        if (isAccelerometerLoaded && Loader.instance.isAutoAccelEnabled) {
                            autoNextTest(currentTestPosition + 1)
                        } else if (isAccelerometerLoaded && Loader.instance.isAccelEnabled) {
                            autoNextTest(currentTestPosition)
                        } else {
                            autoNextTest(currentTestPosition + 1)
                        }
                    } else {
                        autoNextTest(currentTestPosition)
                    }
                }
            }
        }


    }

    private fun autoNextTest(pos: Int) {

        currentTestPosition = pos + 1

        Cabinet.open(context, R.string.settings_pref).add("currentTestPosition", currentTestPosition).save()


        if (!autoPerform || currentTestPosition < 0 || currentTestPosition >= testList.size)
            return


        if (BaseActivity.isSoftBackPressed && testRecyclerView != null) {

            testRecyclerView.smoothScrollToPosition(currentTestPosition)
            Toast.makeText(context, "Auto Perform paused", Toast.LENGTH_LONG).show()
            return
        }

        /*currentActivityTest?.let { currentActivityTest ->
            if (currentActivityTest is ProximityTest && showVibrationHint) {
                if (!(currentActivityTest.status == Test.PASS || currentActivityTest.status == Test.INIT)) {
                    val p = testList.indexOf(currentActivityTest)
                    if (testListAdapter != null) {
                        currentActivityTest.status = Test.FAILED
                        testListAdapter?.notifyItemChanged(p)
                    }
                }
                changeHeaderText(getString(R.string.tap_on_vib))
                if (testRecyclerView != null)
                    testRecyclerView.scrollToPosition(currentTestPosition)
                return
            }
        }*/
        Logy.d(TAG, "====================================")

        /*
        * find test which was skiped*/
        var lastSkipped = -1
        for (i in pos downTo 0) {
            if (i < testList.size) {
                val skippedTest = testList[i]
                if (skippedTest.completeState == Test.NOT_TOUCHED) {
                    lastSkipped = i
                }
            }
        }
        /*
        * get skiped */
        if (lastSkipped > -1 && lastSkipped < testList.size) {
            val skippedTest = testList[lastSkipped]
            if (testRecyclerView != null && skippedTest is VibrationTest) {
                testRecyclerView.smoothScrollToPosition(lastSkipped)
                changeHeaderText("Tap " + skippedTest.title + " test to proceed")
                Logy.d(TAG, skippedTest.javaClass.simpleName + "Test was skipped")
                return
            } else if (testRecyclerView != null && skippedTest is AutoVibrationTest) {
                testRecyclerView.smoothScrollToPosition(lastSkipped)
                changeHeaderText("Tap " + skippedTest.title + " test to proceed")
                Logy.d(TAG, skippedTest.javaClass.simpleName + "Test was skipped")
                return
            }
        }

        if (currentTestPosition < testList.size && testRecyclerView != null) {

            testRecyclerView.scrollToPosition(currentTestPosition + 1)

            val holder = testRecyclerView.findViewHolderForAdapterPosition(currentTestPosition)

            if (holder != null) {
                Handler().postDelayed({ holder.itemView.callOnClick() }, 200)
            } else {
                performTest(testRecyclerView, currentTestPosition, null)
            }
        }
    }

    fun doneClick(view: View) {
        /*if (Cabinet.open(getContext(), R.string.settings_pref).getBoolean("isStarted", false)) {
        } else {
            Toast.makeText(getContext(), "Tests are not yet performed", Toast.LENGTH_SHORT).show();
        }*/

        startCompletionActivity()

    }

    fun autoPerformClick(view: View) {
        startTest()
    }

    private fun startTest() {
        autoPerformButton.isEnabled = false
        autoPerformButton.setImageResource(R.drawable.start_pressed)
        if (autoPerformButton.getAnimation() != null) {
            autoPerformButton.getAnimation().cancel()
        }
        autoPerformButton.setScaleX(1f)
        autoPerformButton.setScaleY(1f)
        val isCompleted = Cabinet.open(context, R.string.settings_pref).getBoolean("isCompleted", false)
        if (isCompleted) {
            startCompletionActivity()
            return
        }
        autoPerform = true

        if (testRecyclerView != null) {

            autostart = true
            auto_start_mode = true

            currentTestPosition = Cabinet.open(context, R.string.settings_pref).getInt("currentTestPosition", 0)

            testRecyclerView.scrollToPosition(currentTestPosition)

            Handler().postDelayed({
                val holder = testRecyclerView.findViewHolderForAdapterPosition(currentTestPosition)
                if (holder != null) {
                    holder.itemView.callOnClick()
                } else {
                    Toast.makeText(context, "Error while performing test no " + currentTestPosition, Toast.LENGTH_SHORT).show()
                }
            }, 200)
        }
    }

    private fun startCompletionActivity() {
        autoPerformButtonStateReset()
        dumpTestResultToFile()
        dumpCosmeticTestResultToFile()
        val customizations = Loader.instance.clientCustomization
        if (customizations != null) {
            when {
                customizations.isCommentsAdded && !BatteryDiagnosticActivity.isConnected(this@MainActivity) -> {
                    val intent = Intent(context, SaveComments::class.java)
                    activity.startActivity(intent)
                }
                customizations.isAutoBatteryDrain -> {
                    val intent = Intent(context, BatteryDiagnosticActivity::class.java)
                    activity.startActivity(intent)
                }
                customizations.isAutoStartBatteryDrain -> {
                    val intent = Intent(context, BatteryDiagnosticActivity::class.java)
                    activity.startActivity(intent)
                }
                else -> {
                    val intent = Intent(context, TestCompletionActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        } else {
            val intent = Intent(context, TestCompletionActivity::class.java)
            activity.startActivity(intent)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNavBackClick() {}

    /*@Override
    public void onClick(View v) {
        if (ALLOW_SHOWCASE) {
            if (sv == null) {
                return;
            }
            sv.hide();
            if (sv.getTag().equals("AUTOPERFORM")) {
                Cabinet.open(getContext(), R.string.showcase_pref).add("autoperform", true).save();
                showResetShowCase();
            } else if (sv.getTag().equals("RESET")) {
                Cabinet.open(getContext(), R.string.showcase_pref).add("reset", true).save();
                showListRowShowCase();
            } else if (sv.getTag().equals("LIST_ROW")) {
                Cabinet.open(getContext(), R.string.showcase_pref).add("list_row", true).save();
            }
        }

    }*/

    fun debugBtnClick(view: View) {
        startActivity(Intent(this, DualCallActivity::class.java))
    }

    override fun onDestroy() {
        stopService(Intent(this, TTSService::class.java))
        if (powerServiceIntent != null) {
            stopService(powerServiceIntent)
        }
        sensorManager.unregisterListener(this)
        isActivityCreated = false

        super.onDestroy()

        try {
            Process.killProcess(Process.myPid())
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val ALLOW_SHOWCASE = false
        var autostart = false
        var auto_start_mode = false
        private val ADMIN_REQ_CODE = 9898
        var connectedNetwork = false
        var BUILD_VERSION = ""
    }


}
