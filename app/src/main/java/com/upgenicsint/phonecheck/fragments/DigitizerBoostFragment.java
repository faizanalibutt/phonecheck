package com.upgenicsint.phonecheck.fragments;

import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.misc.AlertButtonListener;
import com.upgenicsint.phonecheck.misc.SingleTouchEventView;
import com.upgenicsint.phonecheck.utils.DialogUtils;

import co.balrampandey.logy.Logy;

/**
 * Created by Farhan on 10/20/2016.
 */

public class DigitizerBoostFragment extends TestFragment {
    public static final int REQ = 4;
    private static final int TIME_OUT_IN_SEC = 5;
    private static final String POP_ON_TOUCH_KEY = "POP_ON_TOUCH_KEY";
    int bubbleHeight = 0;
    int bubbleWidth = 0;
    int bubbleColumn = 0;
    int bubbleRow = 0;
    int layoutWidth, layoutHeight;
    int estimatedHeightOfList = 0;
    SingleTouchEventView singleTouchEventView;
    ViewGroup mainLayout;
    ViewGroup diagonal;
    ViewGroup diagonal2;
    ViewGroup lineDraw;

    private int totalAdded;
    private int estimatedWidthOfList;
    private int totalPop;
    int total = 0;
    private boolean isTimerStarted = false;
    private boolean popByStylus;
    private boolean isDialogShow = false;
    /*SoundPool sounds;
    Timer popTimer;*/

    public static DigitizerBoostFragment newInstance(boolean popByStylus) {
        DigitizerBoostFragment fragment = new DigitizerBoostFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(POP_ON_TOUCH_KEY, popByStylus);
        fragment.setArguments(bundle);
        return fragment;
    }

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
            public void onClick(DialogInterface dialog, ButtonType type) {

                if (getTestListener() != null && type == ButtonType.RIGHT) {
                    endTest(false);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*createSoundPool();
        popTimer = new Timer();*/
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.digitizer_fragment, container, false);
        mainLayout = view.findViewById(R.id.mainLayout);
        diagonal = view.findViewById(R.id.diagonal);
        diagonal2 = view.findViewById(R.id.diagonal2);
        //lineDraw = view.findViewById(R.id.lineDraw);
        mainLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mainLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                layoutHeight = mainLayout.getHeight();
                layoutWidth = mainLayout.getWidth();
                bubbleListSetup();
                return false;
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            popByStylus = bundle.getBoolean(POP_ON_TOUCH_KEY);
        }

        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (popByStylus) {
                    if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                        onTouchEvent(event);
                    }
                } else {
                    onTouchEvent(event);
                }

                return true;
            }
        });

        return view;
    }

    private void bubbleListSetup() {

        if (getActivity() == null) {
            return;
        }
        bubbleHeight = getResources().getDimensionPixelSize(R.dimen.bubble_image_size1);
        bubbleWidth = getResources().getDimensionPixelSize(R.dimen.bubble_image_size1);

        Logy.d("bubbleSize", bubbleWidth + " / " + bubbleHeight);

        bubbleColumn = layoutWidth / bubbleWidth;
        bubbleRow = layoutHeight / bubbleHeight;

        Logy.d("with", layoutWidth + "");
        Logy.d("heightPixels", layoutHeight + "");

        Logy.d("bubbleColumn", bubbleColumn + "");
        Logy.d("bubbleRow", bubbleRow + "");

        estimatedHeightOfList = bubbleRow * bubbleHeight;
        estimatedWidthOfList = bubbleColumn * bubbleWidth;

        int remainingHeightSpace = layoutHeight - estimatedHeightOfList;
        int remainingWidthSpace = layoutWidth - estimatedWidthOfList;

        total = bubbleRow * bubbleColumn;

        if (estimatedHeightOfList < layoutHeight) {
            int extra = remainingHeightSpace / bubbleRow;
            bubbleHeight += extra;
        }

        if (estimatedWidthOfList < layoutWidth) {
            int extra = remainingWidthSpace / bubbleColumn;
            bubbleWidth += extra;
        }

        if (layoutHeight > 0 && layoutWidth > 0) {
            float angle = (float) Math.toDegrees(Math.atan((double) layoutWidth / (double) layoutHeight));
            diagonal2.setRotation(angle);
            diagonal.setRotation(-angle);
        }

        Logy.d("Grid", "BubbleColumn " + bubbleColumn);
        Logy.d("Grid", "BubbleRow " + bubbleRow);

        for (int i = 0; i < bubbleRow; i++) {
            for (int j = 0; j < bubbleColumn; j++) {
                if (i == 0 || j == bubbleColumn - 1 || j == 0 || i == bubbleRow - 1) {
                    ImageView imageView = new ImageView(getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
                    imageView.setLayoutParams(params);
                    imageView.setImageResource(R.drawable.bubble);
                    totalAdded++;
                    mainLayout.addView(imageView);
                    imageView.setX((float) j * bubbleWidth);
                    imageView.setY((float) i * bubbleHeight);
                }
                if (j == 0) {
                    ImageView imageView = new ImageView(getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
                    imageView.setLayoutParams(params);
                    imageView.setImageResource(R.drawable.bubble);
                    totalAdded++;
                    diagonal.addView(imageView);
                    imageView.setX((float) j * bubbleWidth);
                    imageView.setY((float) i * bubbleHeight);
                }
                if (j == 0) {
                    ImageView imageView = new ImageView(getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
                    imageView.setLayoutParams(params);
                    imageView.setImageResource(R.drawable.bubble);
                    totalAdded++;
                    diagonal2.addView(imageView);
                    imageView.setX((float) j * bubbleWidth);
                    imageView.setY((float) i * bubbleHeight);
                }
            }
        }
//        singleTouchEventView = new SingleTouchEventView(getActivity(), null);
//        lineDraw.addView(singleTouchEventView);
//        singleTouchEventView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        isDialogShow = true;
    }

    public boolean onTouchEvent(MotionEvent event) {

//        if (singleTouchEventView != null) {
//            singleTouchEventView.draw(event);
//        }

        View view;
        if ((view = findViewAtPosition(mainLayout, event.getX(), event.getY())) != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                totalPop++;
                view.setVisibility(View.INVISIBLE);
                //playPopSound();
            }
        }
        if ((view = findViewAtPosition(diagonal, event.getX(), event.getY())) != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                totalPop++;
                view.setVisibility(View.INVISIBLE);
                //playPopSound();
            }
        }
        if ((view = findViewAtPosition(diagonal2, event.getX(), event.getY())) != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                totalPop++;
                view.setVisibility(View.INVISIBLE);
                //playPopSound();
            }
        }
        if (isTimerStarted) {
            countDownTimer.cancel();
            countDownTimer.start();
        }
        if (!isTimerStarted) {
            countDownTimer.start();
            isTimerStarted = true;
        }
        if (totalPop == totalAdded && getTestListener() != null) {
            endTest(true);
        }
        return true;
    }


    private View findViewAtPosition(View parent, float x, float y) {
        if (parent != null) {
            if (parent instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) parent;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    //Rect rect = new Rect();
                    View child = viewGroup.getChildAt(i);
                    View viewAtPosition = findViewAtPosition(child, x, y);

//                    child.getHitRect(rect);
//                    rect.top -= getResources().getDimensionPixelSize(R.dimen.rect_baloon_size);    // increase top hit area
//                    rect.left -= getResources().getDimensionPixelSize(R.dimen.rect_baloon_size);   // increase left hit area
//                    rect.bottom += getResources().getDimensionPixelSize(R.dimen.rect_baloon_size); // increase bottom hit area
//                    rect.right += getResources().getDimensionPixelSize(R.dimen.rect_baloon_size);  // increase right hit area
//                    parent.setTouchDelegate( new TouchDelegate(rect , child));

                    if (viewAtPosition != null) {
                        return viewAtPosition;
                    }
                }
            } else {
                Rect rect = new Rect();
                parent.getGlobalVisibleRect(rect);
                if (rect.contains((int) x, (int) y)) {
                    return parent;
                }
            }
        }
        return null;
    }

    private void endTest(boolean b) {
        if (getTestListener() != null) {
            getTestListener().onDone(DigitizerBoostFragment.this, b);
        }
        countDownTimer.cancel();
        isTimerStarted = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDialogShow = false;
        isTimerStarted = false;
        countDownTimer.cancel();
        mainLayout.setOnTouchListener(null);
    }


}
