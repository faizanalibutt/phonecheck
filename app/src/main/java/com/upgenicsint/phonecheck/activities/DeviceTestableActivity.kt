package com.upgenicsint.phonecheck.activities

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View

import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.misc.AlertButtonListener
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import com.upgenicsint.phonecheck.utils.DialogUtils

/**
 * Created by Farhan on 11/1/2016.
 */

abstract class DeviceTestableActivity<T : Test> : BaseActivity() {
    var test: T? = null
    var removePlayback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
    }

    protected open fun getImageForStatus(value: Int) = when (value) {
        Test.PASS -> R.drawable.blue_check
        Test.FAILED -> R.drawable.not_working
        else -> R.drawable.warning
    }

    protected open fun checkTest(): Int
    {
        val test = test ?: return Test.INIT

        if (test.hasSubTest)
        {
            val predicate: (Map.Entry<String, SubTest>) -> Boolean = {

                if (test.resultsFilterMap.containsKey(it.key)) {
                    test.resultsFilterMap[it.key] == true
                } else {
                    true
                }

            }
            val filteredSubTest = test.subTests.filter(predicate)

            val totalPass = filteredSubTest.filter { it.value.value == Test.PASS }.count()
            val totalUnattempted = filteredSubTest.filter { it.value.value == Test.INIT }.count()

            val count = filteredSubTest.size

            if (totalUnattempted == count) {
                test.status = Test.INIT
                //test.isClear = true
            } else if (totalPass == count) {
                test.status = Test.PASS
                //test.isClear = false
            } else {
                test.status = Test.FAILED
                //test.isClear = false
            }
        }

        return test.status
    }
    protected open fun finalizeTest() {

        checkTest()
        setResult(Activity.RESULT_OK)
        finish()
    }

     override fun onNavDoneClick(v: View) {
        super.onNavDoneClick(v)

        if (checkTest() == Test.PASS) {
            finalizeTest()
        } else {
            showDoneAlert(object : AlertButtonListener {
                override fun onClick(dialog: DialogInterface, type: AlertButtonListener.ButtonType) {
                    if (type == AlertButtonListener.ButtonType.RIGHT) {

                        val test = test
                        removePlayback = true
                        if (test!=null)
                        {
                            test.status = Test.FAILED

                            if (test.hasSubTest) {
                                val predicate: (Map.Entry<String, SubTest>) -> Boolean = {

                                    if (test.resultsFilterMap.containsKey(it.key)) {
                                        test.resultsFilterMap[it.key] == true
                                    } else {
                                        true
                                    }

                                }
                                test.subTests.filter(predicate).forEach {
                                    if (it.value.value == Test.INIT)
                                    {
                                        it.value.value = Test.FAILED
                                    }
                                }
                            }
                        }
                        removeMicPlayBackTest()
                        closeTimerTest()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    dialog.dismiss()
                }
            })
        }
    }

    protected open fun closeTimerTest() {}

    protected open fun removeMicPlayBackTest() {}


    override fun onNavBackClick() {
        BaseActivity.isSoftBackPressed = true
        finalizeTest()
    }

    fun showDoneAlert(listener: AlertButtonListener) {
        val alertDialog = DialogUtils.createConfirmationAlert(context, R.string.test_incomplete, R.string.continue_test, getString(R.string.stay), getString(R.string.cont), listener)
        if (!isFinishing)
            alertDialog.show()
    }
}
