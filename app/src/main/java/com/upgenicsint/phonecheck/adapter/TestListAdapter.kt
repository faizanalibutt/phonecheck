package com.upgenicsint.phonecheck.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.rohit.recycleritemclicksupport.RecyclerItemClickSupport
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.test.Test
import kotlinx.android.synthetic.main.test_row_layout.view.*

/**
 * Created by Farhan on 10/15/2016.
 */

class TestListAdapter(context: Context, private val testList: List<Test>, val itemClick: (Int, View) -> Unit) : BaseRVAdapter<TestListAdapter.TestViewHolder>(context) {

    private var lastSelectedItemPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.test_row_layout, parent, false)
        return TestViewHolder(view)
    }

    fun setLastSelectedItemPosition(lastSelectedItemPosition: Int) {
        this.lastSelectedItemPosition = lastSelectedItemPosition
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        holder.bindView(testList[position], itemClick)
    }

    fun resetItem(position: Int) {
        if (position >= 0 && position < testList.size) {
            //testList[position].isClear = false
            notifyItemChanged(position)
        }
    }

    fun getItem(position: Int): Test = testList[position]

    fun getPosition(test: Test): Int = testList.indexOf(test)

    override fun getItemCount(): Int = testList.size

    class TestViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val statusImageView: ImageView = itemView.earpieceStatusImageView

        init {
            itemView.subTestListView.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            itemView.subTestListView.layoutManager = layoutManager
        }

        fun bindView(test: Test, itemClick: (Int, View) -> Unit) {

            itemView.titleTextView.text = test.title
            itemView.iconImageView.setImageResource(test.iconResource)

            if (test.detail == null) {
                itemView.detailTextView.visibility = View.INVISIBLE
            } else {
                itemView.detailTextView.visibility = View.VISIBLE
                itemView.detailTextView.text = test.detail
            }
            if (test.status == Test.INIT) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.background_light))
                itemView.earpieceStatusImageView.setImageResource(R.drawable.warning)
            } else if (test.status == Test.PASS) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.list_select_color))
                itemView.earpieceStatusImageView.setImageResource(R.drawable.blue_check)

            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.list_select_color))
                itemView.earpieceStatusImageView.setImageResource(R.drawable.not_working)
            }

            if (test.status != Test.INIT) {
                val subListAdapter = test.adapter
                if (test.hasSubTest && subListAdapter != null && subListAdapter.itemCount > 0) {
                    itemView.detailTextView.visibility = View.GONE
                    itemView.subTestListView.visibility = View.VISIBLE
                    itemView.subTestListView.adapter = subListAdapter

                } else {
                    itemView.detailTextView.visibility = View.VISIBLE
                    itemView.subTestListView.visibility = View.GONE
                }
            } else {
                itemView.detailTextView.visibility = View.VISIBLE
                itemView.subTestListView.visibility = View.GONE
            }
            val clickListener = View.OnClickListener {
                itemClick.invoke(adapterPosition, itemView)
            }
            RecyclerItemClickSupport.addTo(itemView.subTestListView).setOnItemClickListener { recyclerView, p, v ->
                itemClick.invoke(adapterPosition, itemView)
            }
            itemView.setOnClickListener(clickListener)
        }
    }
}
