package com.example.teledrive.util
import android.util.Log

object TeleDriveLogger {
    fun i(msg: String) { Log.i("TeleDrive", msg) }
    fun e(msg: String, t: Throwable? = null) { Log.e("TeleDrive", msg, t) }
    fun d(msg: String) { Log.d("TeleDrive", msg) }
}
