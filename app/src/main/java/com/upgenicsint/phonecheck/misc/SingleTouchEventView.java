package com.upgenicsint.phonecheck.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.upgenicsint.phonecheck.R;

public class SingleTouchEventView extends View {

	private Paint paint = new Paint();
	private Path path = new Path();
	private int strokeWidth = getResources().getDimensionPixelSize(R.dimen.stroke_size);

	public SingleTouchEventView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(strokeWidth);
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
	}

	@Override
	protected void onDraw(Canvas canvas) {
//		Path path = new Path();
//		boolean first = true;
//		for(Point point : points){
//			if(first){
//				first = false;
//				path.moveTo(point.x, point.y);
//			}
//			else{
//				path.lineTo(point.x, point.y);
//			}
//		}
		canvas.drawPath(path, paint);
	}


	public void draw(MotionEvent event) {
		float eventX = event.getX();
		float eventY = event.getY();

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				path.moveTo(eventX, eventY);
				return ;
			case MotionEvent.ACTION_MOVE:
				path.lineTo(eventX, eventY);
				break;
			case MotionEvent.ACTION_UP:
				// nothing to do
				break;
			default:
				return;
		}

		// Schedules a repaint.
		invalidate();
	}
}