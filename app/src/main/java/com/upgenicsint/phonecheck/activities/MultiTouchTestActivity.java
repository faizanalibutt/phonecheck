package com.upgenicsint.phonecheck.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.misc.AlertButtonListener;
import com.upgenicsint.phonecheck.misc.Devices;
import com.upgenicsint.phonecheck.models.RecordTest;
import com.upgenicsint.phonecheck.test.Test;
import com.upgenicsint.phonecheck.test.hardware.MultiTouchTest;
import com.upgenicsint.phonecheck.utils.DialogUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.TimerTask;

public class MultiTouchTestActivity extends DeviceTestableActivity<MultiTouchTest> implements View.OnTouchListener {

    protected static final int MENU_FACEBOOK = 2;
    protected static final int MENU_Quit = 3;
    protected static final int MENU_RESET = 1;
    private SharedPreferences settings;
    private MultiTouchView multiTouchView;
    private MultiTouchTest test;
    public static int REQ = 1520;
    private boolean isDialogShow = false;
    private boolean isTimerStarted = false;

    CountDownTimer countDownTimer = new CountDownTimer(5000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (isDialogShow) {
                showTimeoutAlert();
            }
        }
    };

    private void showTimeoutAlert() {
        AlertDialog alertDialog = DialogUtils.createConfirmationAlert(getContext(), getString(R.string.no_touch_digi), getString(R.string.digi_msg), getContext().getString(R.string.no), getContext().getString(R.string.yes), new AlertButtonListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, @NonNull AlertButtonListener.ButtonType type) {

                if (type == ButtonType.RIGHT) {
                    test.setStatus(Test.FAILED);
                    finalizeTest();
                }
                dialog.dismiss();
                isTimerStarted = false;
                countDownTimer.cancel();
            }
        });

        if (isTimerStarted && !alertDialog.isShowing() && !getActivity().isFinishing()) {
            alertDialog.show();
        }
    }

    public void endTest(int totalTouch){
        if (test != null) {
            if (totalTouch >= 3) {
                test.setStatus(Test.PASS);
                finalizeTest();
            } else if(Devices.INSTANCE.isTwoTouchSensor() && totalTouch >= 2) {
                test.setStatus(Test.PASS);
                finalizeTest();
            } else {
                test.setStatus(Test.FAILED);
                finalizeTest();
            }
        }
        isTimerStarted = false;
        countDownTimer.cancel();
    }

   /* public void getTimerStatus(boolean isTimerStarted) {
        this.isTimerStarted = isTimerStarted;
        if (this.isTimerStarted) {
            countDownTimer.cancel();
            countDownTimer.start();
        }
        if (!this.isTimerStarted) {
            countDownTimer.start();
            this.isTimerStarted = true;
            multiTouchView.settimerStatus(true);
        }
    }*/

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        multiTouchView.onTouchEvent(motionEvent);
        if (isTimerStarted) {
            countDownTimer.cancel();
            countDownTimer.start();
        }
        if (!isTimerStarted) {
            countDownTimer.start();
            isTimerStarted = true;
        }
        return true;
    }

   /* @Override
    protected int checkTest() {
        if (test.getStatus() != Test.PASS) {
            test.setStatus(Test.FAILED);
        }
        return test.getStatus();
    }*/

    @Override
    public void onNavDoneClick(@NotNull View v) {
        super.onNavDoneClick(v);
        if (test != null && test.getStatus() != Test.PASS) {
            test.setStatus(Test.FAILED);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

//        requestWindowFeature(1);
//        getWindow().setFlags(1024, 1024);
//        settings = getSharedPreferences("PREF", 0);
//        int count = settings.getInt("MAX_POINTS", 0);
        setContentView(R.layout.activity_multi_touch_test);

        Loader.TIME_VALUE = 0;
        MULTI_TOUCH_SCREEN_TIME = 0;
        Loader.RECORD_TIMER_TASK = new TimerTask() {

            @Override
            public void run() {
                Loader.RECORD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        Loader.TIME_VALUE++;
                    }
                });
            }
        };
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000);

        onCreateNav();
        setNavTitle("Multi Touch Test");
        test = Loader.getInstance().getByClassType(MultiTouchTest.class);
        int count = 0;
        multiTouchView = findViewById(R.id.touchView);
        multiTouchView.setTextView((TextView) findViewById(R.id.points), count);
        multiTouchView.setOnTouchListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        isDialogShow = true;
    }

    protected void onPause() {
        super.onPause();
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt("MAX_POINTS", touchView.getMMaxNumPointers());
//        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDialogShow = false;
        isTimerStarted = false;
        countDownTimer.cancel();
    }

    @Override
    protected void finalizeTest() {
        super.finalizeTest();
        closeTimerTest();
    }

    public static int MULTI_TOUCH_SCREEN_TIME = 0;
    @Override
    protected void closeTimerTest() throws IllegalArgumentException {
        super.closeTimerTest();
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK.cancel();
                Loader.RECORD_TIMER_TASK = null;
                MULTI_TOUCH_SCREEN_TIME = Loader.TIME_VALUE;
                try {
                    SharedPreferences recordPrefs = getSharedPreferences(getResources().getString(R.string.record_tests), Context.MODE_PRIVATE);
                    Loader.getInstance().getRecordList().set(recordPrefs.getInt(getString(R.string.record_mutlitouch), -1), new RecordTest(getString(R.string.report_multitouch_test), MULTI_TOUCH_SCREEN_TIME));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Loader.RECORD_TESTS_TIME.put("MultiTouch" , MULTI_TOUCH_SCREEN_TIME + "s");
                Loader.TIME_VALUE = 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
