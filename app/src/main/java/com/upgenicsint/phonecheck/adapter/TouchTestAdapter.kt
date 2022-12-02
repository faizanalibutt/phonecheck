package com.upgenicsint.phonecheck.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.upgenicsint.phonecheck.R
import kotlinx.android.synthetic.main.bubble_item.view.*

/**
 * Created by Farhan on 10/13/2016.
 */

class TouchTestAdapter(var count: Int, var bubbleHeight: Int, var bubbleWidth: Int, val context: Context) : RecyclerView.Adapter<TouchTestAdapter.BubbleViewHolder>() {

    private val bubblePopStatusArray = BooleanArray(count)
    var totalPopped = 0
        private set

    val isAllPopped get() = totalPopped == count


    init {
        fillStatusArray()
    }

    private fun fillStatusArray() {
        bubblePopStatusArray.fill(true)
        /*for (i in 0 until count) {
            bubblePopStatusArray[i] = true
        }*/
    }

    fun reset() {
        fillStatusArray()
        totalPopped = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.bubble_item, parent, false)
        return BubbleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BubbleViewHolder, position: Int) {
        holder.bindView()
    }

    fun popBubble(position: Int) {
        if (position >= 0) {
            bubblePopStatusArray[position] = false
            totalPopped++
            notifyItemChanged(position)
        }

    }

    override fun getItemCount() = count

    fun isItemPopped(position: Int) = if (position < 0) false else !bubblePopStatusArray[position]

    inner class BubbleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            if (bubbleWidth != -1 && bubbleHeight != -1) {
                itemView.bubbleImageView.layoutParams.width = bubbleWidth
                itemView.bubbleImageView.layoutParams.height = bubbleHeight
                itemView.bubbleImageView.layoutParams = itemView.bubbleImageView.layoutParams
            }
        }

        fun bindView()
        {
            Glide.with(context)
                    .load(if (bubblePopStatusArray[adapterPosition]) R.drawable.bubble else R.drawable.dashed_circle)
                    .dontAnimate()
                    .into(itemView.bubbleImageView)
        }
    }
}
