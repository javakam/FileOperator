package com.ando.file.sample

import ando.file.core.FileDirectory.getCacheDir
import android.content.Context
import android.widget.Toast
import com.ando.file.sample.utils.ClearCacheUtils
import java.io.File

/**
 * 应用缓存目录 :
 *  1. 压缩图片后的缓存目录
 *
 * 注: 手机文件管理器看不到, 需要用AS自带的Device File Explorer查看
 *
 * path:  /data/data/com.ando.file.sample/cache/image/
 */
fun getCompressedImageCacheDir(): String {
    val path = "${getCacheDir().absolutePath}/image/"
    val file = File(path)
    return if (file.mkdirs()) path else path
}

fun clearCompressedImageCacheDir(): Boolean {
    return ClearCacheUtils.clearAllCache(getCompressedImageCacheDir())
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