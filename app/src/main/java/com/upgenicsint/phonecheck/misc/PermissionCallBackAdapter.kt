package com.upgenicsint.phonecheck.misc

import pl.tajchert.nammu.PermissionCallback
import java.lang.ref.WeakReference

/**
 * Created by farhanahmed on 19/11/2016.
 */

class PermissionCallBackAdapter(permissionable: Permissionable) : PermissionCallback {
    private val prefRef =  WeakReference<Permissionable>(permissionable)


    override fun permissionGranted() {
        val ref = prefRef.get()
        if (ref != null) {
            ref.onPermissionGranted()
        }
    }

    override fun permissionRefused() {
        val ref = prefRef.get()
        if (ref != null) {
            ref.onPermissionRefused()
        }
    }
}
