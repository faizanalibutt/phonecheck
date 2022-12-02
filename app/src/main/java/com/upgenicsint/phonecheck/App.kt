package com.upgenicsint.phonecheck

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDexApplication

import com.crashlytics.android.Crashlytics

import io.fabric.sdk.android.Fabric
import com.upgenicsint.phonecheck.Locale.LocaleHelper



/**
 * Created by Farhan on 11/4/2016.
 */

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        //MultiDex.install(this);
        if (!BuildConfig.DEBUG)
            Fabric.with(this, Crashlytics())
    }

}
