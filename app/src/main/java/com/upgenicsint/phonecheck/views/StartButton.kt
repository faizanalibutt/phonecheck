package com.upgenicsint.phonecheck.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView

import com.upgenicsint.phonecheck.R

/**
 * Created by Farhan on 12/15/2016.
 */

class StartButton : ImageView {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onFinishInflate() {
        super.onFinishInflate()
        resetAnimation()
    }


    fun resetAnimation() {
        val scale = AnimationUtils.loadAnimation(context, R.anim.scale)
        scale.duration = 1500
        scale.repeatMode = Animation.REVERSE
        scale.repeatCount = 1000
        animation = scale
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> setImageResource(R.drawable.start_pressed)
            //MotionEvent.ACTION_UP -> setImageResource(R.drawable.start)
        }
        return super.onTouchEvent(event)
    }
}
