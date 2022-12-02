package com.upgenicsint.phonecheck.activities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.upgenicsint.phonecheck.misc.Devices;

import java.util.ArrayList;

public class MultiTouchView extends View {
    private int cirleR;
    private final int[] colorAry = new int[]{-65536, -16711681, -65281, -16776961, -16711936, -7829368};
    private final boolean debugMode = false;
    final float density = getContext().getResources().getDisplayMetrics().density;
    private boolean mCurDown;
    private int mCurNumPointers = 0;
    private int mMaxNumPointers = 0;
    private Paint mPaint;
    private Paint mPaint2;
    private final ArrayList<PointerState> mPointers = new ArrayList();
    private Paint mTextPaint;
    private TextView points;
    private int textSize;
    private boolean alreadyPassed;
    private boolean timerStatus = false;

    private static class PointerState {
        private int color;
        private String indexStr;
        private boolean mCurDown;
        private int mCurX;
        private int mCurY;

        private PointerState() {
        }
    }

    public MultiTouchView(Context context) {
        super(context);
        initView();
    }

    public MultiTouchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MultiTouchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void initView() {
        setFocusable(true);
        cirleR = (int) (55.0f * density);
        textSize = (int) (35.0f * density);
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize((float) textSize);
        mTextPaint.setARGB(255, 0, 0, 0);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setARGB(255, 0, 0, 0);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(7.0f * density);
        mPaint2 = new Paint();
        mPaint2.setAntiAlias(true);
        mPaint2.setStyle(Style.STROKE);
        mPaint2.setStrokeWidth(14.0f * density);
        int p = 0;
        while (p < 20) {
            PointerState ps = new PointerState();
            mPointers.add(ps);
            ps.indexStr = String.valueOf(mPointers.size());
            ps.color = p < 5 ? colorAry[p] : colorAry[5];
            p++;
        }
    }

    protected void onDraw(Canvas canvas) {
        if (mCurDown) {
//            canvas.drawARGB(150, 0, 0, 0);
            mCurNumPointers = 0;
            for (int p = 0; p < mMaxNumPointers; p++) {
                PointerState ps = mPointers.get(p);
                if (mCurDown && ps.mCurDown) {
                    mPaint2.setColor(ps.color);
                    mPaint2.setAlpha(130);
                    canvas.drawCircle((float) ps.mCurX, (float) ps.mCurY, ((float) cirleR) + (7.0f * density), mPaint2);
                    canvas.drawCircle((float) ps.mCurX, (float) ps.mCurY, (float) cirleR, mPaint);
                    canvas.drawText(ps.indexStr, (float) (ps.mCurX - cirleR), (float) (ps.mCurY - cirleR), mTextPaint);
                    mCurNumPointers++;
                }
            }
            points.setText(String.valueOf(mCurNumPointers));
            if (mCurNumPointers >= 3 && !alreadyPassed) {
                alreadyPassed = true;
                ((MultiTouchTestActivity)getContext()).endTest(mCurNumPointers);
            } else if (Devices.INSTANCE.isTwoTouchSensor() && mCurNumPointers >= 2 && !alreadyPassed) {
                alreadyPassed = true;
                ((MultiTouchTestActivity)getContext()).endTest(mCurNumPointers);
            }
            return;
        }
        points.setText(String.valueOf(0));
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean z;
        int action = event.getAction();
        int POINTER_COUNT = event.getPointerCount();
//        if (mMaxNumPointers < POINTER_COUNT) {
            mMaxNumPointers = POINTER_COUNT;
//        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            z = false;
        } else {
            z = true;
        }
        mCurDown = z;
        PointerState ps = mPointers.get(event.getPointerId((65280 & action) >> 8));
        switch (action & 255) {
            case 0:
                ps.mCurDown = true;
                break;
            /*case AnalyticsGmsCoreClient.BIND_FAILED *//*1*//*:
                ps.mCurDown = false;
                break;*/
            case 5:
                ps.mCurDown = true;
                break;
            case 6:
                ps.mCurDown = false;
                break;
        }
        for (int i = 0; i < POINTER_COUNT; i++) {
            PointerState ps_tmp = mPointers.get(event.getPointerId(i));
            ps_tmp.mCurX = (int) event.getX(i);
            ps_tmp.mCurY = (int) event.getY(i);
        }
        //((MultiTouchTestActivity)getContext()).getTimerStatus(timerStatus);
        postInvalidate();
        return true;
    }

    public void setTextView(TextView pointsTv, int count) {
        points = pointsTv;
        mMaxNumPointers = count;
    }

    public int getMMaxNumPointers() {
        return mMaxNumPointers;
    }

    public void settimerStatus(boolean timerStatus) {
        this.timerStatus = timerStatus;
    }
}
