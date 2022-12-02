package com.upgenicsint.phonecheck.adapter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test
import kotlinx.android.synthetic.main.completion_list_layout.view.*

/**
 * Created by Farhan on 10/31/2016.
 */

class TestCompletionAdapter(private val testList: List<Test>, val context: Context) : RecyclerView.Adapter<TestCompletionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.completion_list_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(testList[position])
    }

    override fun getItemCount() = testList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(test:Test)
        {
            itemView.testNameTextView.text = test.title

            if (test.isPassed) {
                itemView.earpieceStatusImageView.setImageResource(R.drawable.blue_check)

            } else if (test.status == Test.INIT) {
                itemView.earpieceStatusImageView.setImageResource(R.drawable.warning)

            } else {
                itemView.earpieceStatusImageView.setImageResource(R.drawable.not_working)
            }
            val animatorSet = AnimatorSet()
            val scale1 = ObjectAnimator.ofFloat(itemView.earpieceStatusImageView, View.SCALE_X, 0f, 1f)
            val scale2 = ObjectAnimator.ofFloat(itemView.earpieceStatusImageView, View.SCALE_Y, 0f, 1f)
            animatorSet.playTogether(scale1, scale2)
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.duration = 500
            animatorSet.start()
        }
    }
}

