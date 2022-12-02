package com.upgenicsint.phonecheck.activities

import android.content.Context
import android.graphics.*
import android.hardware.SensorListener
import android.hardware.SensorManager
import android.hardware.SensorManager.DATA_X
import android.hardware.SensorManager.DATA_Y
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.sensor.AccelerometerTest
import com.upgenicsint.phonecheck.views.BouncingBallModel
import java.util.*
import java.util.concurrent.TimeUnit

class AccelerometerActivity : DeviceTestableActivity<AccelerometerTest>(), SurfaceHolder.Callback {
    private var holder: SurfaceHolder? = null
    private lateinit var model: BouncingBallModel
    private var gameLoop: GameLoop? = null
    private var backgroundPaint = Paint().apply { color = Color.WHITE }
    private var isBallTouchAllSide = false
    private val sensorMgr by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var lastSensorUpdate: Long = -1
    internal var ball: Bitmap? = null
    private var isSurfaceLive = true

    private var sensorListener: SensorListener? = object : SensorListener {
        override fun onSensorChanged(sensor: Int, values: FloatArray) {
            if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
                val curTime = System.currentTimeMillis()
                // only allow one update every 50ms, otherwise updates
                // come way too fast
                if (lastSensorUpdate == -1L || curTime - lastSensorUpdate > 50) {
                    lastSensorUpdate = curTime

                    model.setAccel(values[DATA_X], values[DATA_Y])
                }
            }
        }

        override fun onAccuracyChanged(sensor: Int, accuracy: Int) {

        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        test = Loader.instance.getByClassType(AccelerometerTest::class.java)
        BALL_RADIUS = resources.getDimensionPixelSize(R.dimen.ball_size)
        model = BouncingBallModel(BALL_RADIUS)
        setContentView(R.layout.activity_accelerometer)
        onCreateNav()
        recordUserTestCameToScreen = true
        Loader.TIME_VALUE = 0
        ACCEL_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        val surface = findViewById<View>(R.id.bouncing_ball_surface) as SurfaceView
        holder = surface.holder
        surface.holder.addCallback(this)

        val ballPaint = Paint()
        ballPaint.color = Color.BLUE
        ballPaint.isAntiAlias = true
        ball = BitmapFactory.decodeResource(resources, R.drawable.ball)

        model.onTouchAllSideListener = object : BouncingBallModel.OnTouchAllSideListener {
            override fun onDone() {
                runOnUiThread {
                    val test = test
                    if (test != null) {
                        isBallTouchAllSide = true
                        test.sub(Test.accelerometerTestKey)?.value = Test.PASS
                        test.sub(Test.gyroTestKey)?.value = Test.PASS
                        test.sub(Test.screenRotationTestKey)?.value = Test.PASS
                        test.status = Test.PASS
                        finalizeTest()
                        model.onTouchAllSideListener = null
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorListener = null
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
                ACCEL_SCREEN_TIME = Loader.TIME_VALUE
                if (test != null) {
                    try {
                        val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                        Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_accel), -1)] =
                                RecordTest(context.getString(R.string.report_accelerometer_test), ACCEL_SCREEN_TIME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Loader.RECORD_TESTS_TIME.put(getString(R.string.report_accelerometer_test), "${ACCEL_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    override fun onResume() {
        super.onResume()

        val accelSupported = sensorMgr.registerListener(sensorListener,
                SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_NORMAL)

        if (!accelSupported) {
            // on accelerometer on this device
            sensorMgr.unregisterListener(sensorListener, SensorManager.SENSOR_ACCELEROMETER)
            // TODO show an error
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterSensor()

    }

    private fun unregisterSensor() {
        sensorMgr.unregisterListener(sensorListener, SensorManager.SENSOR_ACCELEROMETER)
        model.setAccel(0f, 0f)
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        model.setSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceLive = true
        gameLoop = GameLoop()
        gameLoop?.start()
    }

    private fun draw() {
        // thread safety - the SurfaceView could go away while we are drawing

        var c: Canvas? = null
        if (!isSurfaceLive) {
            return
        }
        try {
            // NOTE: in the LunarLander they don't have any synchronization here,
            // so I guess this is OK. It will return null if the holder is not ready
            c = holder?.lockCanvas()

            // this needs to synchronize on something
            if (c != null) {
                doDraw(c)
            }
        } finally {
            if (c != null) {
                try {
                    holder?.unlockCanvasAndPost(c)
                } catch (ignored: Exception) {

                }

            }
        }
    }

    private fun doDraw(c: Canvas) {
        val width = c.width
        val height = c.height
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)


        synchronized(model.LOCK) {
            val ballX = model.ballPixelX
            val ballY = model.ballPixelY
            //c.drawCircle(ballX, ballY, BALL_RADIUS, ballPaint);
            ball = Bitmap.createScaledBitmap(ball, BALL_RADIUS, BALL_RADIUS, true)
            c.drawBitmap(ball, ballX, ballY, null)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

        isSurfaceLive = false

        try {
            model.setSize(0, 0)
            gameLoop?.safeStop()
        } finally {
            gameLoop = null
        }
    }

    private inner class GameLoop : Thread() {
        @Volatile private var running = true

        override fun run() {
            while (running) {
                try {
                    //  don't like this hardcoding
                    TimeUnit.MILLISECONDS.sleep(5)

                    draw()
                    model.updatePhysics()

                } catch (ie: InterruptedException) {
                    running = false
                }

            }
        }

        internal fun safeStop() {
            running = false
            interrupt()
        }
    }

    companion object {
        private var BALL_RADIUS = 120
        var ACCEL_SCREEN_TIME = 0
        var recordUserTestCameToScreen = false
        val REQ = 9
    }

}
