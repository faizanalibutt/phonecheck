package com.upgenicsint.phonecheck.eraser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import java.io.File
import java.util.*

class SDCardPathReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            setResult(1, getStorageDirectories(context)[0], null)
        } catch (ignored: Exception) {
        }

    }

    fun getStorageDirectories(context: Context): Array<String> {
        val storageDirectories: Array<String>
        val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val results = ArrayList<String>()
            val externalDirs = context.getExternalFilesDirs(null)
            for (file in externalDirs) {
                val path = file.path.split("/Android".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Environment.isExternalStorageRemovable(file) || rawSecondaryStoragesStr != null && rawSecondaryStoragesStr.contains(path)) {
                    results.add(path)
                }
            }
            storageDirectories = results.toTypedArray()
        } else {
            val rv = HashSet<String>()

            if (rawSecondaryStoragesStr != null && !TextUtils.isEmpty(rawSecondaryStoragesStr)) {
                val rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                Collections.addAll(rv, *rawSecondaryStorages)
            }
            storageDirectories = rv.toTypedArray()
        }
        return storageDirectories
    }
}
