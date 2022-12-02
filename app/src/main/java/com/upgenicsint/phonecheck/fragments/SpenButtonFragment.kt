package com.upgenicsint.phonecheck.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import com.upgenicsint.phonecheck.Loader

import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.BaseActivity
import com.upgenicsint.phonecheck.activities.ButtonsTestActivity
import com.upgenicsint.phonecheck.activities.DeviceTestableActivity
import com.upgenicsint.phonecheck.adapter.ButtonsAdapter
import com.upgenicsint.phonecheck.misc.HomeWatcher
import com.upgenicsint.phonecheck.misc.SpenHomeWatcher
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.test.chip.AutofocusTest
import com.upgenicsint.phonecheck.test.hardware.ButtonTest
import com.upgenicsint.phonecheck.utils.FirebaseUtil
import kotlinx.android.synthetic.main.activity_button_test.*
import kotlinx.android.synthetic.main.fragment_spen_button.*
import java.util.HashMap

class SpenButtonFragment : TestFragment() {

    internal var deviceTestableActivity: DeviceTestableActivity<*>? = null
    internal var mHomeWatcher: SpenHomeWatcher? = null
    private val listOfKeyPressed = HashMap<String, String>()
    private var ignoreHomeFirstEvent = false
    private var ignoreActivityFocus: Boolean = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
//        mHomeWatcher?.startWatch()
        mHomeWatcher = SpenHomeWatcher(activity!!.applicationContext)
        mHomeWatcher?.startWatch()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = LayoutInflater.from(activity!!.applicationContext).inflate(R.layout.fragment_spen_button, container, false)
        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView_fragment!!.layoutManager = LinearLayoutManager(activity!!.applicationContext, LinearLayoutManager.VERTICAL, false)
        var test = Loader.Companion.instance.getByClassType(ButtonTest::class.java)
        val adapter = ButtonsAdapter(activity!!.applicationContext, test!!.hardwareTestArrayList2)
        recyclerView_fragment!!.adapter = adapter

        mHomeWatcher?.onHomePressedListener = object : SpenHomeWatcher.OnHomePressedListener{
            override fun onHomePressed() {

            }

            override fun onEventOccurred(eventString: String) {

            }

            override fun onRecentPressed() {
                if (ViewConfiguration.get(context).hasPermanentMenuKey() && (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_APP_SWITCH) || !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_MENU))) {
                    FirebaseUtil.addNew(FirebaseUtil.BUTTON).child("Menu Event").child("Check 1").setValue("Called")

                    return
                }
                performTest(KeyEvent.KEYCODE_MENU)
            }

        }
    }

//    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//
//        listOfKeyPressed.put("Key " + event.keyCode, if (event.action == KeyEvent.ACTION_DOWN) "ACTION_DOWN" else "" + event.action)
//
//
//        return if (event.action == KeyEvent.ACTION_DOWN) {
//
//            performTest(event.keyCode)
//        } else super.dispatchKeyEvent(event)
//
//    }

    fun performTest(keyCode: Int): Boolean {
        val test = Loader.Companion.instance.getByClassType(ButtonTest::class.java)
//        val test = test ?: return false
        val hardwareTest = test!!.getKeyForKeyCode(keyCode)

        if (hardwareTest != null && hardwareTest.subTest.value != Test.PASS) {
            hardwareTest.subTest.value = Test.PASS
            val position = test.hardwareTestArrayList.indexOf(hardwareTest)
            recyclerView_fragment.adapter!!.notifyItemChanged(position)

            test.status = Test.PASS
//            if (checkTest() == Test.PASS) {
//                if (BaseActivity.autoPerform) {
//                    finalizeTest()
//                }
//            }
            return true
        }
        return false
    }
}
