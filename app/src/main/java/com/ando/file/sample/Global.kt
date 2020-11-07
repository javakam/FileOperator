package com.ando.file.sample

import ando.file.core.FileDirectory.getCacheDir
import android.content.Context
import android.widget.Toast
import java.io.File


const val REQUEST_CODE_SENDER = 0x10

/**
 * 应用缓存目录 :
 *      1.压缩图片后的缓存目录
 *
 * <pre>
 *     path:  /data/data/package/cache
 * </pre>
 */
fun getPathImageCache(): String? {
    val path = "${getCacheDir().absolutePath}/image/"
    val file = File(path)
    return if (file.mkdirs()) {
        path
    } else path
}

fun Context.toastShort(msg: String?) {
    msg?.let {
        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
    }
}

fun Context.toastLong(msg: String?) {
    msg?.let {
        Toast.makeText(this, it, Toast.LENGTH_LONG).show()
    }
}