package com.upgenicsint.phonecheck.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.models.HardwareTest
import com.upgenicsint.phonecheck.test.Test
import kotlinx.android.synthetic.main.button_test_layout.view.*

/**
 * Created by Farhan on 10/18/2016.
 */

class ButtonsAdapter(val context: Context, val list: List<HardwareTest>) : RecyclerView.Adapter<ButtonsAdapter.ButtonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.button_test_layout, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bindView(list[position])
    }

    override fun getItemCount() = list.size

    class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(hardwareTest: HardwareTest) {
            itemView.nameTextView.text = hardwareTest.subTest.title.replace("Button", "")

            val statusImage: Int
            val buttonImage: Int
            if (hardwareTest.subTest.value == Test.PASS) {
                statusImage = R.drawable.blue_check
                buttonImage = hardwareTest.iconPass
            } else if (hardwareTest.subTest.value == Test.FAILED) {
                statusImage = R.drawable.not_working
                buttonImage = hardwareTest.icon
            } else {
                statusImage = R.drawable.warning
                buttonImage = hardwareTest.icon
            }

            itemView.earpieceStatusImageView.setImageResource(statusImage)
            itemView.buttonImageView.setImageResource(buttonImage)
        }
    }
}
