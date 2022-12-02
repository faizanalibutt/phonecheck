package com.upgenicsint.phonecheck.utils

import android.os.Build
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.upgenicsint.phonecheck.Loader
import com.upgenicsint.phonecheck.activities.MainActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by farhanahmed on 12/11/2016.
 */

object FirebaseUtil {
    @JvmField val SENSOR = "Sensor"
    @JvmField val TELEPHONE = "Telephony"
    @JvmField val CAMERA = "Camera"
    @JvmField val AUDIO = "Audio"
    @JvmField val EXCEPTION = "Exception"
    @JvmField val LOADED_TEST = "Test Load"
    @JvmField val NOT_LOADED_TEST = "Test Not Load"
    @JvmField val BUTTON = "Button"
    @JvmField val RESULT = "Result"
    @JvmField val FINGERPRINT = "Fingerprint"

    @JvmStatic fun addNew(name: String): DatabaseReference {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        var ref = firebaseDatabase.reference

        ref = ref.child(removeControlCharacters(MainActivity.BUILD_VERSION))
        ref = ref.child(removeControlCharacters(Build.VERSION.SDK_INT.toString()))
        ref = ref.child(removeControlCharacters(Build.BRAND) + "_" + removeControlCharacters(Build.MODEL) + "_" + removeControlCharacters(Build.SERIAL))
        try {
            ref = ref.child(removeControlCharacters(Loader.imei))
        } catch (e: Throwable) {
            ref = ref.child("UNKNOWN IMEI")
        }

        ref = ref.child(removeControlCharacters(name))

        return ref
    }


    fun removeControlCharacters(s: String): String = s.replace(".", "_")

    @Throws(JSONException::class)
    fun jsonToMap(json: JSONObject): Map<String, Any> {
        var retMap: Map<String, Any> = HashMap()

        if (json !== JSONObject.NULL) {
            retMap = toMap(json)
        }
        return retMap
    }

    @Throws(JSONException::class)
    fun toMap(element: JSONObject): Map<String, Any> {
        val map = HashMap<String, Any>()

        val keysItr = element.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            var value = element.get(key)

            if (value is JSONArray) {
                value = toList(value)
            } else if (value is JSONObject) {
                value = toMap(value)
            }
            map.put(key, value)
        }
        return map
    }

    @Throws(JSONException::class)
    fun toList(array: JSONArray): List<Any> {
        val list = ArrayList<Any>()
        for (i in 0 until array.length()) {
            var value = array.get(i)
            if (value is JSONArray) {
                value = toList(value)
            } else if (value is JSONObject) {
                value = toMap(value)
            }
            list.add(value)
        }
        return list
    }
}
