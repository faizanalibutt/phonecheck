package com.upgenicsint.phonecheck.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.upgenicsint.phonecheck.R

class TimeoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timeout)
    }

    fun closeApplication(view: View) {
        finish()
    }
}
