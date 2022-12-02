package com.upgenicsint.phonecheck.adapter

import android.app.Application
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.models.TestStatusInfo
import com.upgenicsint.phonecheck.test.Test
import kotlinx.android.synthetic.main.sub_test_row_layout.view.*

/**
 * Created by Farhan on 10/15/2016.
 */

class SubTestAdapter(public val context: Context, private val testStatusInfos: List<TestStatusInfo>) : RecyclerView.Adapter<SubTestAdapter.SubTestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubTestViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sub_test_row_layout, parent, false)
        return SubTestViewHolder(context,view)
    }

    override fun onBindViewHolder(holder: SubTestViewHolder, position: Int) {
        holder.bindView(testStatusInfos[position])
    }

    override fun getItemCount() = testStatusInfos.size

    class SubTestViewHolder( val context: Context,itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(testStatusInfo: TestStatusInfo) {
            itemView.titleTextView.text = testStatusInfo.title
            if (testStatusInfo.statusText != null) {
                itemView.statusTextView.text = testStatusInfo.statusText
            } else {
//                itemView.statusTextView.text = if (testStatusInfo.status == Test.PASS) "Passed" else "Failed"
                itemView.statusTextView.text = if (testStatusInfo.status == Test.PASS) context.getString(R.string.passed) else context.getString(R.string.failed)
            }
        }
    }
}
