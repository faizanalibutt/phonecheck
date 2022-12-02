package com.upgenicsint.phonecheck.test.hardware

import android.content.Context
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.Locale.LanguageSupport
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.TouchTestActivity
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException

/**
 * Created by Farhan on 10/20/2016.
 */

class LCDTest(context: Context) : Test(context) {

    override val jsonKey: String
        get() = Test.ScreenTestKey

    override val title: String
        get() = "LCD"

    override val detail: String
        get() = context.getString(R.string.lcd_desc)

    override val iconResource: Int
        get() = R.drawable.touch

    override val hasSubTest get() = true

    override val activityRequestCode: Int
        get() = TouchTestActivity.REQ

    init {
        if (!Loader.instance.isTestListLoaded){
            subTests.put(Test.glassConditionTestKey, SubTest(context.getString(R.string.glass_condition1)))
            subTests.put(Test.LCDTestKey, SubTest(context.getString(R.string.lcd_display)))
//            subTests.put(Test.BacklightTestKey, SubTest(context.getString(R.string.lcd_brightness)))
        }
        if(Loader.instance.isTestListLoaded && Loader.instance.filterContains("Glass Cracked")){
            subTests.put(Test.glassConditionTestKey, SubTest(context.getString(R.string.glass_condition1)))
        }
        if(Loader.instance.isTestListLoaded && Loader.instance.filterContains("LCD")){
            subTests.put(Test.LCDTestKey, SubTest(context.getString(R.string.lcd_display)))
        }
        if(Loader.instance.isTestListLoaded && Loader.instance.filterContains("Brightness")){
            subTests.put(Test.BacklightTestKey, SubTest(context.getString(R.string.lcd_brightness)))
        }
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        super.perform(context, autoPerformMode)
        startIntent(context, TouchTestActivity::class.java, TouchTestActivity.REQ)
        isRunning = false
        return status
    }


    @Throws(JSONException::class)
    override fun onSaveState(storeOperation: StoreOperation) {
        storeOperation.add(jsonKey, status).save()
        super.onSaveState(storeOperation)
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        return super.onRestoreState(getOperation)
    }

    override fun requireUserInteraction() = false

    override fun requireActivity() = true
}
