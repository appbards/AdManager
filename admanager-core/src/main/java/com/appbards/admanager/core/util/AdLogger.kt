package com.appbards.admanager.core.util

import android.util.Log

object AdLogger {
    private const val TAG = "AdManager"
    private var isEnabled = false

    fun enable(enabled: Boolean) {
        isEnabled = enabled
    }

    fun d(message: String) {
        if (isEnabled) Log.d(TAG, message)
    }

    fun i(message: String) {
        if (isEnabled) Log.i(TAG, message)
    }

    fun w(message: String) {
        if (isEnabled) Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }
}