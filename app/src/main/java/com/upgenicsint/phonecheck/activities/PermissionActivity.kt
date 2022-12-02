package com.upgenicsint.phonecheck.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.upgenicsint.phonecheck.R

class PermissionActivity : AppCompatActivity() {

    fun appInfoButton(v: View) {
        startActivity(newAppDetailsIntent(this, packageName))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
    }

    companion object {

        fun newAppDetailsIntent(context: Context, packageName: String): Intent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.data = Uri.parse("package:" + packageName)
                return intent
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.setClassName("com.android.settings",
                        "com.android.settings.InstalledAppDetails")
                intent.putExtra("pkg", packageName)
                return intent
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setClassName("com.android.settings",
                    "com.android.settings.InstalledAppDetails")
            intent.putExtra("com.android.settings.ApplicationPkgName", packageName)
            return intent
        }
    }
}
