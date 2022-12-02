package com.upgenicsint.phonecheck.misc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import com.upgenicsint.phonecheck.R

/**
 * Created by farhanahmed on 08/08/2017.
 */

class SpaceItemDivider(context: Context) : RecyclerView.ItemDecoration() {

    internal var height: Int = 0

    init {
        height = context.resources.getDimension(R.dimen.item_divider_height).toInt()
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        if (parent.getChildViewHolder(view).adapterPosition == parent.adapter!!.itemCount - 1) {
            outRect.set(height, height, height, height)
        } else {
            outRect.set(height, height, height, 0)

        }
    }
}
