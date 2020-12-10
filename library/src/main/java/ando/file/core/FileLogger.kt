package ando.file.core

import android.util.Log

/**
 * FileLogger
 *
 * @author javakam
 * @date 2018-7-9
 */
object FileLogger {

    private const val TAG_DEFAULT = "FileLogger"
    private var TAG = javaClass.simpleName
    private var isDebug = false

    fun init(isDebug: Boolean, tag: String = TAG_DEFAULT) {
        TAG = if (tag.isBlank()) TAG_DEFAULT else tag
        FileLogger.isDebug = isDebug
    }

    fun v(msg: String?) {
        if (isDebug) {
            Log.v(TAG, noNull(msg))
        }
    }

    fun i(msg: String?) {
        if (isDebug) {
            Log.i(TAG, noNull(msg))
        }
    }

    fun d(msg: String?) {
        if (isDebug) {
            Log.d(TAG, noNull(msg))
        }
    }

    fun e(msg: String?) {
        if (isDebug) {
            Log.e(TAG, noNull(msg))
        }
    }

    fun w(msg: String?) {
        if (isDebug) {
            Log.w(TAG, noNull(msg))
        }
    }

    fun wtf(msg: String?) {
        if (isDebug) {
            Log.wtf(TAG, noNull(msg))
        }
    }

    fun v(tag: String?, msg: String?) {
        if (isDebug) {
            Log.v(tag, noNull(msg))
        }
    }

    fun i(tag: String?, msg: String?) {
        if (isDebug) {
            Log.i(tag, noNull(msg))
        }
    }

    fun d(tag: String?, msg: String?) {
        if (isDebug) {
            Log.d(tag, noNull(msg))
        }
    }

    fun e(tag: String?, msg: String?) {
        if (isDebug) {
            Log.e(tag, noNull(msg))
        }
    }

    fun w(tag: String?, msg: String?) {
        if (isDebug) {
            Log.w(tag, noNull(msg))
        }
    }

    fun wtf(tag: String?, msg: String?) {
        if (isDebug) {
            Log.wtf(tag, noNull(msg))
        }
    }

}