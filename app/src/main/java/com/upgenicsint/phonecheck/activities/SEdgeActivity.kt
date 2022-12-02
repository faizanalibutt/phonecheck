package com.upgenicsint.phonecheck.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import com.samsung.android.sdk.SsdkUnsupportedException
import com.samsung.android.sdk.look.Slook
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailSubWindow
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.adapter.TouchTestAdapter
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.models.RecordTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.hardware.EdgeScreenTest
import com.upgenicsint.phonecheck.utils.DialogUtils
import com.upgenicsint.phonecheck.utils.Tools
import java.util.*


class SEdgeActivity : DeviceTestableActivity<EdgeScreenTest>() {
    internal var slook = Slook()
    private var bubbleSize: Int = 0
    private var bubbleColumn: Int = 0
    private var bubbleRow: Int = 0
    private var estimatedWidthOfList: Int = 0
    private var bubbleHeight = 0
    private var bubbleWidth = 0
    private var estimatedHeightOfList: Int = 0
    private var total: Int = 0
    private var isTimerStarted: Boolean = false
    private lateinit var recyclerView:RecyclerView

    internal var countDownTimer: CountDownTimer = object : CountDownTimer(5000, 1000) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            showTimeoutAlert()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_sedge)

        Loader.TIME_VALUE = 0
        SEDGE_SCREEN_TIME = 0
        Loader.RECORD_TIMER_TASK = object : TimerTask() {

            override fun run() {
                Loader.RECORD_HANDLER.post {
                    Loader.TIME_VALUE++
                }
            }
        }
        Loader.RECORD_TIMER_TEST.schedule(Loader.RECORD_TIMER_TASK, 1000, 1000)

        Tools.setBrightness(window,1f)
        onCreateNav()

        test = Loader.instance.getByClassType(EdgeScreenTest::class.java)

        if (test != null) {
            try {
                slook.initialize(this)
                if (slook.isFeatureEnabled(Slook.COCKTAIL_BAR)) {
                    val cocktailView = LayoutInflater.from(context).inflate(R.layout.edge_screen_sub, null)
                    recyclerView = cocktailView.findViewById(R.id.recyclerView)
                    SlookCocktailSubWindow.setSubContentView(this, cocktailView)

                    if (cocktailView != null) {
                        bubbleListSetup(cocktailView)
                    }
                }

            } catch (e: SsdkUnsupportedException) {
                e.printStackTrace()
            }

        }
    }

    override fun checkTest(): Int {
        val test = test ?: return Test.FAILED
        countDownTimer.cancel()
        isTimerStarted = false
        return test.status
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

    @SuppressLint("ClickableViewAccessibility")
    private fun listSetup(viewGroup: View) {
        val layoutWidth = viewGroup.width
        val layoutHeight = viewGroup.height
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
            override fun canScrollVertically() = false
        }
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.setHasFixedSize(false)
        recyclerView.itemAnimator = null
        val adapter = TouchTestAdapter(total, bubbleHeight, bubbleWidth, context)

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
                        finalizeTest()
                    }
                }
            } catch (ignore: Exception) {ignore.printStackTrace()}
            false
        }
    }



    private fun showTimeoutAlert() {
        val alertDialog = DialogUtils.createConfirmationAlert(context, "No touch detected", "Do you want to skip the test?", object : AlertButtonListener {
            override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                if (type == AlertButtonListener.ButtonType.LEFT) {
                    finalizeTest()
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

    override fun finalizeTest() {
        super.finalizeTest()
        closeTimerTest()
    }

    override fun closeTimerTest() {
        super.closeTimerTest()
        try {
            if (Loader.RECORD_TIMER_TASK != null) {
                Loader.RECORD_TIMER_TASK!!.cancel()
                Loader.RECORD_TIMER_TASK = null
                SEDGE_SCREEN_TIME = Loader.TIME_VALUE
                try {
                    val recordPrefs = getSharedPreferences(resources.getString(R.string.record_tests), Context.MODE_PRIVATE)
                    Loader.instance.recordList[recordPrefs.getInt(getString(R.string.record_spenedge), -1)] =
                            RecordTest(context.getString(R.string.report_spenedge_test), SEDGE_SCREEN_TIME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Loader.RECORD_TESTS_TIME.put("SEDGE", "${SEDGE_SCREEN_TIME}s")
                Loader.TIME_VALUE = 0
                //Loader.RECORD_HANDLER.removeCallbacks {}
            }
        } catch (ignored: IllegalArgumentException) {ignored.printStackTrace()}
    }

    companion object {
        var SEDGE_SCREEN_TIME = 0
        val REQ = 7
    }

}
