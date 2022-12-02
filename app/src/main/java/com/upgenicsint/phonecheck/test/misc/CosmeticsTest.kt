package com.upgenicsint.phonecheck.test.misc

import android.content.Context
import android.content.pm.PackageManager
import com.farhanahmed.cabinet.operations.GetOperation
import com.farhanahmed.cabinet.operations.StoreOperation
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.R
import com.upgenicsint.phonecheck.activities.CosmeticsTestActivity
import com.upgenicsint.phonecheck.containsIgnoreCase
import com.upgenicsint.phonecheck.models.Cosmetics
import com.upgenicsint.phonecheck.models.CosmeticsKeys
import com.upgenicsint.phonecheck.test.SubTest
import com.upgenicsint.phonecheck.test.Test
import org.json.JSONException
import java.util.*

class CosmeticsTest (context: Context) : Test(context) {

    private var cosmeticsList: List<CosmeticsKeys>? = Objects.requireNonNull<Cosmetics>(Loader.instance.loadCosmetics()).getCosmetics()

    override val jsonKey: String
        get() = Test.cosmeticsTestKey

    override val title: String
        get() = "Cosmetics Test"

    override val detail: String
        get() = "Please answer the few questions asked to complete this test"

    override val iconResource: Int
        get() = R.drawable.touch

    override val activityRequestCode: Int
        get() = CosmeticsTestActivity.REQ

    override val hasSubTest: Boolean
        get() = true

    override fun requireActivity(): Boolean {
        return true
    }

    override fun requireUserInteraction(): Boolean {
        return true
    }

    init {
        if (cosmeticsList != null) {
            for (e in 0 until cosmeticsList!!.size) {
                if (cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("Android") || cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("All")) {
                    subTests.put(cosmeticsList!!.get(e).shortkey, SubTest(cosmeticsList!!.get(e).shortkey))
                }
            }
        }
    }

    @Throws(JSONException::class)
    override fun onSaveStateCosmetics(storeOperation: StoreOperation) {
        storeOperation.add(jsonKey, status)
        super.onSaveStateCosmetics(storeOperation)
        for (e in 0 until cosmeticsList!!.size) {
            if (cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("Android") || cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("All")) {
                Loader.RESULTCOSMETICS.put(cosmeticsList!!.get(e).shortkey, toJsonStatus())
            }
        }
    }

    @Throws(JSONException::class)
    override fun onRestoreState(getOperation: GetOperation): Boolean {
        status = getOperation.getInt(jsonKey, status)
        for (e in 0 until cosmeticsList!!.size) {
            if (cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("Android") || cosmeticsList!!.get(e).getPlatform().containsIgnoreCase("All")) {
                Loader.RESULTCOSMETICS.put(cosmeticsList!!.get(e).shortkey, toJsonStatus())
            }
        }
        return super.onRestoreState(getOperation)
    }

    override fun perform(context: Context, autoPerformMode: Boolean): Int {
        startIntent(context, CosmeticsTestActivity::class.java)
        return super.perform(context, autoPerformMode)
    }
}
