package com.example.teledrive.util
import android.util.Log

object TeleDriveLogger {
    fun i(tag: String, msg: String) { Log.i("TeleDrive:$tag", msg) }
    fun e(tag: String, msg: String, t: Throwable? = null) { Log.e("TeleDrive:$tag", msg, t) }
    fun d(tag: String, msg: String) { Log.d("TeleDrive:$tag", msg) }
}
