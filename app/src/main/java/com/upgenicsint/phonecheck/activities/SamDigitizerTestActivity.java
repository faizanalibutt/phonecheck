package com.upgenicsint.phonecheck.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.Touch;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.misc.AlertButtonListener;
import com.upgenicsint.phonecheck.models.RecordTest;
import com.upgenicsint.phonecheck.test.Test;
import com.upgenicsint.phonecheck.test.hardware.DigitizerTestAdvance;
import com.upgenicsint.phonecheck.utils.DialogUtils;

import org.json.JSONException;

import java.util.TimerTask;

import static com.upgenicsint.phonecheck.activities.DigitizerActivity.DIGITIZER_SCREEN_TIME;

public class SamDigitizerTestActivity extends DeviceTestableActivity<DigitizerTestAdvance> implements View.OnTouchListener {

    private static boolean isSamPassed = false;
    private TouchTestView touchTestView;
    static DigitizerTestAdvance test;
    public static int REQ = 1510;
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
            public void onClick(DialogInterface dialog, AlertButtonListener.ButtonType type) {

                if (type == ButtonType.RIGHT) {
                    test.setStatus(Test.FAILED);
                    checkTest();
                    setResult(Activity.RESULT_OK);
                    finish();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.sam_digitizer);
        onCreateNav();
        TouchTestView.rectPathFilled.reset();
        touchTestView = findViewById(R.id.touch_view);
        //TouchTestView touch = new TouchTestView(this, getApplicationContext());

        Loader.TIME_VALUE = 0;
        DIGITIZER_SCREEN_TIME = 0;
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

        test = Loader.getInstance().getByClassType(DigitizerTestAdvance.class);

        touchTestView.setOnTouchListener(this);
        Log.v("Cycle:", "onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        isDialogShow = true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        Log.v("Cycle", "(x,y) = (" + x + "," + y + ")");
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                TouchTestView.linePath.moveTo(event.getX(), event.getY());
                TouchTestView.setRectValues(x, y);
                touchTestView.invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                TouchTestView.linePath.lineTo(event.getX(), event.getY());
                TouchTestView.setRectValues(x, y);
                touchTestView.invalidate();
                break;
            default:
                return false;
        }
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

    public void endTest(int totalRect, int totalFilled) {
        if (test != null) {
            if (totalRect == totalFilled) {
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

    @Override
    protected void finalizeTest() {
        super.finalizeTest();
        closeTimerTest();
    }

    @Override
    protected void closeTimerTest() throws IllegalArgumentException {
        super.closeTimerTest();
        if (Loader.RECORD_TIMER_TASK != null) {
            Loader.RECORD_TIMER_TASK.cancel();
            Loader.RECORD_TIMER_TASK = null;
            DIGITIZER_SCREEN_TIME = Loader.TIME_VALUE;
            try {
                SharedPreferences recordPrefs = getSharedPreferences(getResources().getString(R.string.record_tests), Context.MODE_PRIVATE);
                Loader.getInstance().getRecordList().set(recordPrefs.getInt(getString(R.string.record_digi), -1), new RecordTest(getString(R.string.report_digitizer_test), DIGITIZER_SCREEN_TIME));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Loader.RECORD_TESTS_TIME.put("Digitizer", DIGITIZER_SCREEN_TIME + "s");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Loader.TIME_VALUE = 0;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDialogShow = false;
        isTimerStarted = false;
        countDownTimer.cancel();
    }
}
