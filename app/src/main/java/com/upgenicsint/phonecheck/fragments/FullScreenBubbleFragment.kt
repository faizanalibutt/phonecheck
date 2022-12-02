package com.upgenicsint.phonecheck.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.TouchTestAdapter
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.DialogUtils
import kotlinx.android.synthetic.main.full_screen_fragment.*
import java.lang.Exception

/**
 * Created by farhanahmed on 31/10/2017.
 */
class FullScreenBubbleFragment : TestFragment() {

    private var test: Test? = null
    private var bubbleSize: Int = 0
    private var bubbleColumn: Int = 0
    private var bubbleRow: Int = 0
    private var estimatedWidthOfList: Int = 0
    private var bubbleHeight = 0
    private var bubbleWidth = 0
    private var estimatedHeightOfList: Int = 0
    private var total: Int = 0
    private var isTimerStarted: Boolean = false
    private val customizations = Loader.instance.clientCustomization
    internal var countDownTimer: CountDownTimer = object : CountDownTimer(5000, 1000) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            showTimeoutAlert()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.full_screen_fragment, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view?.viewTreeObserver?.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                onViewDimensionsAvailable()
                return false
            }
        })

    }

    fun onViewDimensionsAvailable() {

        bubbleListSetup(view as ViewGroup)

    }

    private fun bubbleListSetup(viewGroup: View) {
        bubbleSize = resources.getDimensionPixelSize(R.dimen.bubble_image_size)
        Log.d("bubbleSize", bubbleSize.toString() + "")
        viewGroup.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewGroup.viewTreeObserver.removeOnPreDrawListener(this)
                bubbleColumn = viewGroup.width / bubbleSize
                bubbleRow = viewGroup.height / bubbleSize

                listSetup(viewGroup)

                return false
            }
        })


    }

    private fun listSetup(viewGroup: View) {
        val layoutWidth = viewGroup.width
        val layoutHeight = viewGroup.height

        /*if (customizations != null && customizations.DigiTizerBoost) {
            bubbleHeight = resources.getDimensionPixelSize(R.dimen.bubble_image_size1)
            bubbleWidth = resources.getDimensionPixelSize(R.dimen.bubble_image_size1)
        } else {

        }*/

        bubbleHeight = resources.getDimensionPixelSize(R.dimen.bubble_image_size)
        bubbleWidth = resources.getDimensionPixelSize(R.dimen.bubble_image_size)

        bubbleColumn = layoutWidth / bubbleWidth
        bubbleRow = layoutHeight / bubbleHeight

        estimatedHeightOfList = bubbleRow * bubbleHeight
        estimatedWidthOfList = bubbleColumn * bubbleWidth

        val remainingHeightSpace = layoutHeight - estimatedHeightOfList
        val remainingWidthSpace = layoutWidth - estimatedWidthOfList

        total = bubbleRow * bubbleColumn
        if (estimatedHeightOfList < layoutHeight) {
            val extra = remainingHeightSpace / bubbleRow
            bubbleHeight += extra
        }
        if (estimatedWidthOfList < layoutWidth) {
            val extra = remainingWidthSpace / bubbleColumn
            bubbleWidth += extra
        }
        val gridLayoutManager = object : GridLayoutManager(context, bubbleColumn) {
            override fun canScrollVertically(): Boolean = false
        }
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.setHasFixedSize(false)
        recyclerView.itemAnimator = null
        val adapter = TouchTestAdapter(total, bubbleHeight, bubbleWidth, context!!)

        recyclerView.adapter = adapter
        recyclerView.setOnTouchListener { v, e ->
            try {
                val rv = v as RecyclerView
                val childView = rv.findChildViewUnder(e.x, e.y)
                val position = rv.getChildLayoutPosition(childView!!)
                if (childView != null && !adapter.isItemPopped(position)) {

                    if (isTimerStarted) {
                        countDownTimer.cancel()
                        countDownTimer.start()
                    }
                    if (!isTimerStarted) {
                        countDownTimer.start()
                        isTimerStarted = true
                    }
                    adapter.popBubble(position)
                    if (adapter.isAllPopped) {
                        isTimerStarted = false
                        countDownTimer.cancel()
                        test?.status = Test.PASS
                        testListener?.onDone(this, true)
                    }
                }
            } catch (ignore: Exception) {
                ignore.printStackTrace()
            }
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isTimerStarted = false
        countDownTimer.cancel()
    }


    private fun showTimeoutAlert() {

        if (isRemoving) {
            return
        }
        val alertDialog = DialogUtils.createConfirmationAlert(context!!, "No touch detected", "Do you want to skip the test?","No","Yes", object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                if (type == AlertButtonListener.ButtonType.RIGHT) {
                    testListener?.onDone(this@FullScreenBubbleFragment, false)
                }
                isTimerStarted = false
                countDownTimer.cancel()
                dialog.dismiss()
            }
        })

        if (isTimerStarted && !alertDialog.isShowing) {
            alertDialog.show()
        }
    }

}