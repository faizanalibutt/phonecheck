package com.upgenicsint.phonecheck.activities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.upgenicsint.phonecheck.views.CustomRect;

import java.util.ArrayList;

public class TouchTestView extends View {

    private Paint paintUnfilled, paintFilled, paintPath;
    private static float totalWidth, totalHeight;
    private static int leftRect, rightRect, topRect, bottomRect;
    private static int X;
    private static int Y;
    private Context context;
    private float numOfHorizontalRect, numOfVerticalRect, numOfDiagonalRect, numOfDiagonalRectDefault;
    private float rectWidth, rectHeight, diagonalRectHeight;
    private float diagonalSize;
    public static Path linePath, rectPathFilled, rectPathUnfilled;
    private float top, left, j, k;
    private int totalRect, totalFilled, rectPosition;
    private ArrayList<CustomRect> rectsFilled = new ArrayList<>();
    private ArrayList<CustomRect> rectsUnfilled = new ArrayList<>();
    private boolean alreadyPassed;
    SamDigitizerTestActivity sam;

    public TouchTestView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public TouchTestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchTestView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TouchTestView(SamDigitizerTestActivity samDigitizerTestActivity, Context context) {
        super(context);
        this.context = context;
        this.sam = samDigitizerTestActivity;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TouchTestView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    public static void setRectValues(int x, int y) {
        X = x;
        Y = y;
    }

    private void init() {

        X = 0;
        Y = 0;
        paintUnfilled = new Paint();
        paintUnfilled.setColor(Color.GRAY);
        paintUnfilled.setStyle(Paint.Style.STROKE);
        paintUnfilled.setStrokeWidth(2);

        paintFilled = new Paint();
        paintFilled.setColor(Color.GREEN);
        paintFilled.setStyle(Paint.Style.FILL);
        paintFilled.setStrokeWidth(2);

        linePath = new Path();
        rectPathFilled = new Path();
        rectPathUnfilled = new Path();

        paintPath = new Paint();
        paintPath.setAntiAlias(true);
        paintPath.setColor(Color.BLACK);
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setStrokeWidth(5f);
        paintPath.setStrokeJoin(Paint.Join.ROUND);

        for (int i = 0; i < 184; i++) {
            rectsUnfilled.add(new CustomRect(0, 0, 0, 0, false));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        totalWidth = getMeasuredWidth();
        totalHeight = getMeasuredHeight();

//        numOfHorizontalRect = Math.round(totalWidth / 100);
//        numOfVerticalRect = Math.round(totalHeight / 100);
        numOfHorizontalRect = 15;
        numOfVerticalRect = 25;

        rectWidth = totalWidth / numOfHorizontalRect;
        rectHeight = totalHeight / numOfVerticalRect;

        diagonalSize = (float) getDiagonalSize(totalWidth - rectWidth, totalHeight - 2 * rectHeight);
//        numOfDiagonalRect = Math.round(diagonalSize / 50);
//        numOfDiagonalRect = Math.round(diagonalSize / 50);
//        numOfDiagonalRect = 52;
        numOfDiagonalRectDefault = 52;
        diagonalRectHeight = diagonalSize / 60;

        CustomRect rect;
        //OnTouchDrawRects
//        Log.v("Time:", "Start Time" + startTime);
        for (int i = 0; i < rectsFilled.size(); i++) {
            rect = rectsFilled.get(i);
            canvas.drawRect(rect, paintFilled);
            totalFilled = rectsFilled.size();
            Log.v("Size", "rectFilled.size() = " + rectsFilled.size());
        }

        int total = 0;
        for (int i = 0; i < rectsFilled.size(); i++) {
            rect = rectsFilled.get(i);
            if (rect.isGreen())
                ++total;
//            canvas.drawRect(rect, paintFilled);
//            Log.v("Size","rectFilled.size() = " + rectsFilled.size());
        }
        totalFilled = total;
        //Left Column
        top = 0;

        int numOfColumn = 0, numOfRows = 50, numOfDiagonals = 80;
        for (int i = 0; i < numOfVerticalRect; i++) {
            //Left Column
//            RectF rect = new RectF(0, top, rectWidth, top + rectHeight);
//            CustomRect rect = new CustomRect(0, top, rectWidth, top + rectHeight, false);
            rect = rectsUnfilled.get(numOfColumn);
            rect.left = 0;
            rect.top = top;
            rect.right = rectWidth;
            rect.bottom = top + rectHeight;
            canvas.drawRect(rect, paintUnfilled);
            if (X != 0 && Y != 0) {
                if (rect.contains(X, Y)) {
//                    if (!rectsFilled.contains(rect))
//                        rectPathFilled.addRect(rect, Path.Direction.CW);
//                        ++totalFilled;
                    if (!rect.isGreen()) {
                        rect.setGreen(true);
                        rectsFilled.add(rect);
                    }
                }
            }
            ++numOfColumn;
            //Right Column
//            rect = new RectF(totalWidth - rectWidth, top, totalWidth, top + rectHeight);
//            CustomRect rect = new CustomRect(totalWidth - rectWidth, top, totalWidth, top + rectHeight, false);
            rect = rectsUnfilled.get(numOfColumn);
            rect.left = totalWidth - rectWidth;
            rect.top = top;
            rect.right = totalWidth;
            rect.bottom = top + rectHeight;
            canvas.drawRect(rect, paintUnfilled);
            if (X != 0 && Y != 0) {
                if (rect.contains(X, Y)) {
//                    if (!rectsFilled.contains(rect))
//                        rectPathFilled.addRect(rect, Path.Direction.CW);
//                        ++totalFilled;
                    if (!rect.isGreen()) {
                        rect.setGreen(true);
                        rectsFilled.add(rect);
                        Log.v("Rect:", rect.toShortString() + "(X,Y):" + X + "," + Y);
                    }
                }
            }
            top += rectHeight;
            ++numOfColumn;
        }
        left = 0;
        for (int i = 0; i < numOfHorizontalRect; i++) {
            //Top Row
//            RectF rect = new RectF(left, 0, left + rectWidth, rectHeight);
//            CustomRect rect = new CustomRect(left, 0, left + rectWidth, rectHeight, false);
            rect = rectsUnfilled.get(numOfRows);
            rect.left = left;
            rect.top = 0;
            rect.right = left + rectWidth;
            rect.bottom = rectHeight;
            canvas.drawRect(rect, paintUnfilled);
            if (X != 0 && Y != 0) {
                if (rect.contains(X, Y)) {
//                    if (!rectsFilled.contains(rect))
//                        rectPathFilled.addRect(rect, Path.Direction.CW);
//                        ++totalFilled;
                    if (!rect.isGreen()) {
                        rect.setGreen(true);
                        rectsFilled.add(rect);
                        Log.v("Rect:", rect.toShortString() + "(X,Y):" + X + "," + Y);
                    }
                }
            }
            ++numOfRows;
            //Bottom Row
//            rect = new RectF(left, totalHeight - rectHeight, left + rectWidth, totalHeight);
//            rect = new CustomRect(left, totalHeight - rectHeight, left + rectWidth, totalHeight, false);
            rect = rectsUnfilled.get(numOfRows);
            rect.left = left;
            rect.top = totalHeight - rectHeight;
            rect.right = left + rectWidth;
            rect.bottom = totalHeight;
            canvas.drawRect(rect, paintUnfilled);
            if (X != 0 && Y != 0) {
                if (rect.contains(X, Y)) {
//                    if (!rectsFilled.contains(rect))
//                        rectPathFilled.addRect(rect, Path.Direction.CW);
//                        ++totalFilled;
                    if (!rect.isGreen()) {
                        rect.setGreen(true);
                        rectsFilled.add(rect);
                        Log.v("Rect:", rect.toShortString() + "(X,Y):" + X + "," + Y);
                    }
                }
            }
            left += rectWidth;
            ++numOfRows;
        }

        j = rectWidth / 2;
        k = totalWidth - rectWidth - rectWidth / 2;
        top = rectHeight;
        numOfDiagonalRect = 0;
        for (int i = 0; i < numOfDiagonalRectDefault; i++) {
            if (top >= totalHeight - rectHeight)
                break;
            numOfDiagonalRect++;
            //Left Diagonal
//            RectF rect = new RectF(j, top, j + rectWidth, top + diagonalRectHeight);
//            CustomRect rect = new CustomRect(j, top, j + rectWidth, top + diagonalRectHeight, false);
            rect = rectsUnfilled.get(numOfDiagonals);
            rect.left = j;
            rect.top = top;
            rect.right = j + rectWidth;
            rect.bottom = top + diagonalRectHeight;
            canvas.drawRect(rect, paintUnfilled);
            if (X != 0 && Y != 0) {
                if (rect.contains(X, Y)) {
//                    rectPathFilled.addRect(rect, Path.Direction.CW);
//                    ++totalFilled;
                    if (!rect.isGreen()) {
                        rect.setGreen(true);
                        rectsFilled.add(rect);
                        Log.v("Rect:", rect.toShortString() + "(X,Y):" + X + "," + Y);
                    }
                }
            }
            Log.v("Diagonal: ", rect.toShortString() + X + " , " + Y);

            j += rectWidth / 4;
            ++numOfDiagonals;

            //Right Diagonal
//            rect = new RectF(k, top, k + rectWidth, top + diagonalRectHeight);
//            rect = new CustomRect(k, top, k + rectWidth, top + diagonalRectHeight, false);
            rect = rectsUnfilled.get(numOfDiagonals);

            rect.left = k;
            rect.top = top;
            rect.right = k + rectWidth;
            rect.bottom = top + diagonalRectHeight;
            canvas.drawRect(rect, paintUnfilled);
            if (X != 0 && Y != 0) {
                if (rect.contains(X, Y)) {
//                    rectPathFilled.addRect(rect, Path.Direction.CW);
//                    ++totalFilled;
                    if (!rect.isGreen()) {
                        rect.setGreen(true);
                        rectsFilled.add(rect);
                        Log.v("Rect:", rect.toShortString() + "(X,Y):" + X + "," + Y);
                    }
                }
            }
            top += diagonalRectHeight;
            k -= rectWidth / 4;
            ++numOfDiagonals;
        }

//        canvas.drawPath(rectPathFilled,paintFilled);

        //multiplied by 2 for both top and bottom, left and right
        totalRect = (int) (2 * numOfHorizontalRect + 2 * numOfVerticalRect + 2 * numOfDiagonalRect);
        Log.v("Total:", "total=" + totalRect + "filled=" + totalFilled);

        if (totalRect == totalFilled && !alreadyPassed) {
            alreadyPassed = true;
            ((SamDigitizerTestActivity)getContext()).endTest(totalRect, totalFilled);
        }
        canvas.drawPath(linePath, paintPath);
    }

    public double getDiagonalSize(float width, float height) {
        double x = Math.pow(width, 2);
        double y = Math.pow(height, 2);
        return Math.sqrt(x + y);
    }
}
