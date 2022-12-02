package com.upgenicsint.phonecheck.fragments

import android.app.Activity
import android.support.v4.app.Fragment

/**
 * Created by Farhan on 10/20/2016.
 */

interface TestListener {
    fun onDone(fragment: Fragment, isPass: Boolean)
    fun onDone1(activity: Activity, isPass: Boolean)
}