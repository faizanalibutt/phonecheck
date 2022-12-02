package com.upgenicsint.phonecheck.misc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View

import com.upgenicsint.phonecheck.R

/**
 * Created by Farhan on 10/16/2016.
 */

class BottomDividerDecorator(context: Context) : RecyclerView.ItemDecoration() {
    private var offset = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.alert_divider_color)
        offset = context.resources.getDimensionPixelSize(R.dimen.list_divider_size)
        paint.strokeWidth = offset.toFloat()
    }


    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val layoutManager = parent.layoutManager

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            c.drawRect(layoutManager!!.getDecoratedLeft(child).toFloat(),
                    layoutManager.getDecoratedTop(child).toFloat(),
                    layoutManager.getDecoratedRight(child).toFloat(),
                    layoutManager.getDecoratedBottom(child).toFloat(),
                    paint)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) == 0) {
            return
        }
        outRect.top = offset
    }

}
