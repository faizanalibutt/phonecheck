package com.upgenicsint.phonecheck.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class LineDrawView extends View {

    private Paint paintPath;
    private static int X;
    private static int Y;
    private Context context;
    public static Path linePath, rectPathFilled, rectPathUnfilled;

    public LineDrawView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public LineDrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineDrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public static void setRectValues(int x, int y) {
        X = x;
        Y = y;
    }

    private void init() {

        X = 0;
        Y = 0;

        linePath = new Path();
        rectPathFilled = new Path();
        rectPathUnfilled = new Path();

        paintPath = new Paint();
        paintPath.setAntiAlias(true);
        paintPath.setColor(Color.BLACK);
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setStrokeWidth(5f);
        paintPath.setStrokeJoin(Paint.Join.ROUND);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(linePath, paintPath);
    }
}
