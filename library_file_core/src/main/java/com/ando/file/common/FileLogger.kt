package com.ando.file.common

import android.util.Log

/**
 * FileLogger
 *
 * @author javakam
 * @date 2018-7-9
 */
object FileLogger {

    private const val TAG = "FileLogger"
    private var isDebug = false

    fun init(isDebug: Boolean) {
        FileLogger.isDebug = isDebug
    }

    fun v(msg: String?) {
        if (isDebug) {
            Log.v(TAG, msg)
        }
    }
    fun i(msg: String?) {
        if (isDebug) {
            Log.i(TAG, msg)
        }
    }

    fun d(msg: String?) {
        if (isDebug) {
            Log.d(TAG, msg)
        }
    }

    fun e(msg: String?) {
        if (isDebug) {
            Log.e(TAG, msg)
        }
    }

    fun w(msg: String?) {
        if (isDebug) {
            Log.w(TAG, msg)
        }
    }
    fun wtf(msg: String?) {
        if (isDebug) {
            Log.wtf(TAG, msg)
        }
    }

    //
    fun v(tag: String?, msg: String?) {
        if (isDebug) {
            Log.v(tag, msg)
        }
    }

    fun i(tag: String?, msg: String?) {
        if (isDebug) {
            Log.i(tag, msg)
        }
    }

    fun d(tag: String?, msg: String?) {
        if (isDebug) {
            Log.d(tag, msg)
        }
    }

    fun e(tag: String?, msg: String?) {
        if (isDebug) {
            Log.e(tag, msg)
        }
    }

    fun w(tag: String?, msg: String?) {
        if (isDebug) {
            Log.w(tag, msg)
        }
    }

    fun wtf(tag: String?, msg: String?) {
        if (isDebug) {
            Log.wtf(tag, msg)
        }
    }

}