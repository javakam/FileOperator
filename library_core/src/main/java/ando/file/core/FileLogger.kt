package ando.file.core

import android.util.Log
import java.net.URLDecoder

/**
 * # FileLogger
 *
 * @author javakam
 * @date 2018-7-9
 */
object FileLogger {

    private const val TAG_DEFAULT = "FileLogger"
    private var TAG = javaClass.simpleName
    private var isDebug = false
    private var enableUrlEncode = false //开启编码: "msf%3A51" ; 关闭解码: "msf:51"

    fun init(isDebug: Boolean, enableUrlEncode: Boolean = false, tag: String = TAG_DEFAULT) {
        FileLogger.enableUrlEncode = enableUrlEncode
        TAG = if (tag.isBlank()) TAG_DEFAULT else tag
        FileLogger.isDebug = isDebug
    }

    private fun String?.codec(): String {
        (if (this.isNullOrBlank()) return "" else this).also { s: String ->
            return if (enableUrlEncode) s
            else URLDecoder.decode(s, "utf-8")
        }
    }

    fun v(msg: String?) {
        if (isDebug) {
            Log.v(TAG, msg.codec())
        }
    }

    fun i(msg: String?) {
        if (isDebug) {
            Log.i(TAG, msg.codec())
        }
    }

    fun d(msg: String?) {
        if (isDebug) {
            Log.d(TAG, msg.codec())
        }
    }

    fun e(msg: String?) {
        if (isDebug) {
            Log.e(TAG, msg.codec())
        }
    }

    fun w(msg: String?) {
        if (isDebug) {
            Log.w(TAG, msg.codec())
        }
    }

    fun v(tag: String?, msg: String?) {
        if (isDebug) {
            Log.v(tag, msg.codec())
        }
    }

    fun i(tag: String?, msg: String?) {
        if (isDebug) {
            Log.i(tag, msg.codec())
        }
    }

    fun d(tag: String?, msg: String?) {
        if (isDebug) {
            Log.d(tag, msg.codec())
        }
    }

    fun e(tag: String?, msg: String?) {
        if (isDebug) {
            Log.e(tag, msg.codec())
        }
    }

    fun w(tag: String?, msg: String?) {
        if (isDebug) {
            Log.w(tag, msg.codec())
        }
    }
}