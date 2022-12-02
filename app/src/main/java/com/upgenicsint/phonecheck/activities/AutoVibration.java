package com.upgenicsint.phonecheck.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.barcode.CaptureActivity;
import com.upgenicsint.phonecheck.test.Test;
import com.upgenicsint.phonecheck.test.chip.AutofocusTest;
import com.upgenicsint.phonecheck.test.chip.SpeechToTextTest;
import com.upgenicsint.phonecheck.test.sensor.AutoVibrationTest;
import com.upgenicsint.phonecheck.test.sensor.autoVibration.AccelData;

import java.util.ArrayList;
import java.util.Collections;

public class AutoVibration extends DeviceTestableActivity<AutoVibrationTest>  implements SensorEventListener,
        View.OnClickListener {
    private SensorManager sensorManager;
    private Button btnStart, btnStop, btnwithout, nextActivity, nextButton;
    private boolean started = false;
    private ArrayList<AccelData> sensorData;
    private ImageView testStatus;
    private ArrayList sensorDataVibration;
    private LinearLayout layout;
    private View mChart;
    private TextView x_values, y_vaues, z_values, minZ, maxZ;
    double x = 0, y = 0, z = 0;
    private boolean isStopped = false;
    Handler handler;
    Runnable runnable;
    private boolean isStart = false;
    private int counter = 0;
    public Vibrator vibrator;
    private ArrayList<AccelData> dataBeforVib;
    private ArrayList zAxisBeforVib;
    boolean vibrationStarted = false;
    boolean isPaused = false;
    boolean testStarted = false;
    ProgressDialog progressDialog;
    AutoVibrationTest test;

    public static int REQ = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_vibration);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorData = new ArrayList<AccelData>();
        dataBeforVib = new ArrayList<AccelData>();

        test = Loader.getInstance().getByClassType(AutoVibrationTest.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vibration Test");
        progressDialog.setMessage(getString(R.string.flashTst_msg));

//		initialize vibration
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        // Thread for updating the text view
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update TextView here!
                                updateTextViews();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();
    }
    private void updateTextViews() {
//        x_values.setText(String.valueOf(x));
//        y_vaues.setText(String.valueOf(y));
//        z_values.setText(String.valueOf(z));
    }
    @Override
    public void onResume(){
        super.onResume();
        (new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.show();
                        progressDialog.setCanceledOnTouchOutside(false);
                    }
                });
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startVibrating();
            }
        })).start();
    }

    private void startVibrating() {
        sensorData = new ArrayList<AccelData>();
        dataBeforVib = new ArrayList<AccelData>();
        started = true;
        isStopped = false;
        isPaused = false;
        testStarted = true;
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                vibrateCellPhone();
            }
        })).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (started == true) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] gravity = new float[3];
        float[] accelerometer_data = new float[3];

        if (started) {

            final double alpha = 0.8;

            // without any filter
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            long timestamp = System.currentTimeMillis();
            final AccelData data = new AccelData(timestamp, x, y, z);
            sensorData.add(data);

            if(!isPaused) {
                new CountDownTimer(1000, 500) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        dataBeforVib.add(data);
                    }

                    @Override
                    public void onFinish() {
                        isPaused = true;
                    }
                }.start();
            }

            // Thread for pausing the vibration and updating the views
            if(counter == 3 && !isStopped){
                isStopped = true;
                handler.removeCallbacks(runnable);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sensorManager.unregisterListener(AutoVibration.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                started = false;
                                checkTestVibration();
                            }
                        });
                    }
                }, 2000);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {

    }
    private void vibrateCellPhone() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isStart = true;
                vibrationStarted = true;
                handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        vibrator.vibrate(850);
                        handler.postDelayed(runnable,2000);
                        counter++;
                    }
                };
                handler.post(runnable);
            }
        });
    }
    private void checkTestVibration() {
        vibrationStarted = false;

        sensorDataVibration = new ArrayList();
        zAxisBeforVib = new ArrayList();


        if (sensorData != null || sensorData.size() > 0) {
            long t = sensorData.get(0).getTimestamp();

            for (AccelData data : sensorData) {
                sensorDataVibration.add(data.getZ());
            }

            for (AccelData data2 : dataBeforVib){
                zAxisBeforVib.add(data2.getZ());
            }

            Collections.sort(sensorDataVibration);
            Collections.sort(zAxisBeforVib);

            String maxWithouVib = String.valueOf(Collections.max(zAxisBeforVib));
            String minWithouVib = String.valueOf(Collections.min(zAxisBeforVib));

            String max = String.valueOf(Collections.max(sensorDataVibration));
            String min = String.valueOf(Collections.min(sensorDataVibration));

            double maxValue = Double.parseDouble(max);
            double minValue = Double.parseDouble(min);

            double maxWithoutVib = Double.parseDouble(maxWithouVib);
            double minWithoutVib = Double.parseDouble(minWithouVib);

            progressDialog.dismiss();
            if(testStarted) {
                if (maxValue > maxWithoutVib || minValue < minWithoutVib) {
                    if (test != null){
                        test.setStatus(Test.PASS);
                    }
                }
                else {
                    if (test != null){
                        test.setStatus(Test.FAILED);
                    }
                }
            }
            else{
                if (maxValue > maxWithoutVib && minValue < minWithoutVib) {
                    if (test != null){
                        test.setStatus(Test.PASS);
                    }
                }
                else {
                    if (test != null){
                        test.setStatus(Test.FAILED);
                    }
                }
            }
//            if (checkTest() == Test.PASS) {
//                finalizeTest();
//            }
            finalizeTest();
        }
        dataBeforVib.clear();
        sensorData.clear();
        sensorData = null;
        dataBeforVib = null;
        testStarted = false;
        counter = 0;
    }
}
