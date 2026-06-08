package com.example.teledrive.util

import android.util.Log

object TeleDriveLogger {
    private const val TAG = "TeleDrive"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }
}
