package com.upgenicsint.phonecheck.fragments;

import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.misc.AlertButtonListener;
import com.upgenicsint.phonecheck.misc.SingleTouchEventView;
import com.upgenicsint.phonecheck.models.ClientCustomization;
import com.upgenicsint.phonecheck.utils.DialogUtils;

import co.balrampandey.logy.Logy;

public class GuidedDigitizerFragment extends TestFragment {

    public static final int REQ = 4;
    private static final int TIME_OUT_IN_SEC = 5;
    private static final String POP_ON_TOUCH_KEY = "POP_ON_TOUCH_KEY";
    private static final String TAG = "Guided";
    private ClientCustomization customizations = Loader.getInstance().clientCustomization;
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
    private boolean popByStylus;
    private float dX = 0.0f, dY = 0.0f;
    private ImageView start_logo, finish_icon;
    private TextView start_text;

    public static GuidedDigitizerFragment newInstance(boolean popByStylus) {
        GuidedDigitizerFragment fragment = new GuidedDigitizerFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(POP_ON_TOUCH_KEY, popByStylus);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        start_logo = view.findViewById(R.id.start_logo);
        finish_icon = view.findViewById(R.id.finish_icon);
        start_text = view.findViewById(R.id.start_text);

        return view;
    }

    private void bubbleListSetup() {

        if (getActivity() == null) {
            return;
        }

        if (customizations != null && customizations.getDigiTizerBoost()) {
            bubbleHeight = getResources().getDimensionPixelSize(R.dimen.bubble_image_size);
            bubbleWidth = getResources().getDimensionPixelSize(R.dimen.bubble_image_size);
        } else {
            bubbleHeight = getResources().getDimensionPixelSize(R.dimen.bubble_image_size);
            bubbleWidth = getResources().getDimensionPixelSize(R.dimen.bubble_image_size);
        }

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

                    if (i == 0 && j != 0) {
                        addBubble(i, j, R.drawable.left);
                    } else if (j == 0 && i == 0) {
                        addBubble(i, j, R.drawable.bubble);
                    } else if (i == bubbleRow - 1 && j != bubbleColumn - 1) {
                        addBubble(i, j, R.drawable.right);
                    } else if (j == bubbleColumn - 1 && i == bubbleRow - 1) {
                        addBubble(i, j, R.drawable.top);
                    } else if (j == bubbleColumn - 1 && i != bubbleRow - 1) {
                        addBubble(i, j, R.drawable.top);
                    } else if (j != bubbleColumn - 1 && i != bubbleRow - 1 && (i == 1 || i == 2)) {
                        addBubble(i, j, R.drawable.bubble);
                        if (j != bubbleColumn - 1 && i != bubbleRow - 1 && i == 1) {
                            start_logo.setVisibility(View.VISIBLE);
                            start_logo.setX((float) j * bubbleWidth);
                            start_logo.setY((float) i * bubbleHeight);
                        }
                    } else if (j != bubbleColumn - 1 && i != bubbleRow - 1) {
                        addBubble(i, j, R.drawable.down);
                    }

                }
                if (j == 0) {
                    ImageView imageView = new ImageView(getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
                    imageView.setLayoutParams(params);
                    imageView.setImageResource(R.drawable.down);
                    totalAdded++;
                    diagonal.addView(imageView);
                    imageView.setX((float) j * bubbleWidth);
                    imageView.setY((float) i * bubbleHeight);
                    if (i == 1) {
                        start_text.setVisibility(View.VISIBLE);
                        start_text.setX((float)(( j * bubbleWidth) + (bubbleHeight)));
                        start_text.setY((float)((i * bubbleHeight)));
                    }
                }
                if (j == 0) {
                    ImageView imageView = new ImageView(getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
                    imageView.setLayoutParams(params);
                    imageView.setImageResource(R.drawable.top);
                    totalAdded++;
                    diagonal2.addView(imageView);
                    imageView.setX((float) j * bubbleWidth);
                    imageView.setY((float) i * bubbleHeight);
                    if (i == 1) {
                        diagonal2.removeView(imageView);
                        finish_icon.setLayoutParams(params);
                        finish_icon.setVisibility(View.VISIBLE);
                        finish_icon.setX((float) j * bubbleWidth);
                        finish_icon.setY((float) i * bubbleHeight);
                    }
                }
            }
        }

        //dX = start_logo.getX();
        //dY = start_logo.getY();

    }

    private void addBubble(int i, int j, int bubbleType) {
        ImageView imageView = new ImageView(getActivity());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
        imageView.setLayoutParams(params);
        imageView.setImageResource(bubbleType);
        totalAdded++;
        mainLayout.addView(imageView);
        imageView.setX((float) j * bubbleWidth);
        imageView.setY((float) i * bubbleHeight);
    }

    private void addBubble(float i, float j, ViewGroup parent) {
        ImageView imageView1 = new ImageView(getActivity());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(bubbleWidth, bubbleHeight);
        imageView1.setLayoutParams(params);
        imageView1.setImageResource(R.drawable.green);
        imageView1.setVisibility(View.VISIBLE);
        parent.addView(imageView1);
        imageView1.setX(i);
        imageView1.setY(j);
    }


    public boolean onTouchEvent(MotionEvent event) {

        View view;
        if ((view = findViewAtPosition(mainLayout, event.getX(), event.getY())) != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                totalPop++;
                view.setVisibility(View.INVISIBLE);
                addBubble(view.getX(), view.getY(), mainLayout);
            }

        }
        if ((view = findViewAtPosition(diagonal, event.getX(), event.getY())) != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                totalPop++;
                view.setVisibility(View.INVISIBLE);
                addBubble(view.getX(), view.getY(), diagonal);
            }
        }

        if ((view = findViewAtPosition(diagonal2, event.getX(), event.getY())) != null) {

            if (view.getVisibility() != View.INVISIBLE) {
                totalPop++;
                view.setVisibility(View.INVISIBLE);
                addBubble(view.getX(), view.getY(), diagonal2);
            }

        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_MOVE:

                /*
                start_logo.animate()
                        .x(event.getRawX())
                        .y(event.getRawY())
                        .setDuration(0)
                        .start();
                dX = start_logo.getX() - event.getRawX();
                dY = start_logo.getY() - event.getRawY();
                //Toast.makeText(getActivity(), "show Dialog", Toast.LENGTH_SHORT).show();
                */

                start_logo.setX(event.getRawX());
                start_logo.setY(event.getRawY());
                Log.d(TAG, "Action Move Called");

                break;

            case MotionEvent.ACTION_UP:

                if (totalPop == totalAdded && getTestListener() != null) {
                    endTest(true);
                    break;
                }

                start_text.setVisibility(View.INVISIBLE);
                start_logo.setX(event.getRawX());
                start_logo.setY(event.getRawY());
                showTimeoutAlert();
                Log.d(TAG, "Action Up called");

                /*if ((start_logo = (ImageView) findViewAtPosition(mainLayout, event.getX(), event.getY())) != null) {
                    start_text.setVisibility(View.INVISIBLE);
                } else if ((start_logo = (ImageView) findViewAtPosition(diagonal, event.getX(), event.getY())) != null) {
                    start_text.setVisibility(View.INVISIBLE);
                } else if ((start_logo = (ImageView) findViewAtPosition(diagonal2, event.getX(), event.getY())) != null) {
                    start_text.setVisibility(View.INVISIBLE);
                }*/

                break;

            /*case DragEvent.ACTION_DRAG_STARTED:
                // do nothing
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                mainLayout.setBackgroundResource(R.drawable.blue_check);
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                mainLayout.setBackgroundResource(R.drawable.warning);
                break;
            case DragEvent.ACTION_DROP:
                // Dropped, reassign View to ViewGroup
                mainLayout.setBackgroundResource(R.drawable.not_working);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                mainLayout.setBackgroundResource(R.drawable.drawable_circle_dark_blue);*/

            default:
                return false;
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
            getTestListener().onDone(GuidedDigitizerFragment.this, b);
        }
    }

    private void showTimeoutAlert() {

        if (getActivity() != null && !getActivity().isFinishing()) {
            AlertDialog alertDialog = DialogUtils.createConfirmationAlert(getContext(), getString(R.string.no_touch_digi), getString(R.string.digi_msg), getContext().getString(R.string.continuous), getContext().getString(R.string.fail), new AlertButtonListener() {
                @Override
                public void onClick(@NonNull DialogInterface dialog, @NonNull AlertButtonListener.ButtonType type) {

                    if (getTestListener() != null && type == ButtonType.RIGHT) {
                        endTest(false);
                    }
                    dialog.dismiss();
                }
            });
            if (!alertDialog.isShowing() && getActivity() != null && !getActivity().isFinishing()) {
                alertDialog.show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainLayout.setOnTouchListener(null);
    }


}
