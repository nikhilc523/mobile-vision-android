package edu.cs663.falldetect.util

import android.util.Log as AndroidLog

/**
 * Simple logging wrapper for the Fall Detection app.
 * Provides consistent tagging and easy log level control.
 */
object Log {
    private const val TAG = "FallDetect"
    
    fun d(message: String, tag: String = TAG) {
        AndroidLog.d(tag, message)
    }
    
    fun i(message: String, tag: String = TAG) {
        AndroidLog.i(tag, message)
    }
    
    fun w(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (throwable != null) {
            AndroidLog.w(tag, message, throwable)
        } else {
            AndroidLog.w(tag, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (throwable != null) {
            AndroidLog.e(tag, message, throwable)
        } else {
            AndroidLog.e(tag, message)
        }
    }
}

