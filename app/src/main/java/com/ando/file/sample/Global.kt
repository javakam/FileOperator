package com.ando.file.sample

import ando.file.compressor.ImageCompressPredicate
import ando.file.compressor.ImageCompressor
import ando.file.compressor.OnImageCompressListener
import ando.file.compressor.OnImageRenameListener
import ando.file.core.*
import ando.file.core.FileDirectory.getCacheDir
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.ando.file.sample.utils.ClearCacheUtils
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * startActivityForResult -> requestCode
 */
const val REQUEST_CHOOSE_FILE = 10

var GLOBAL_DIALOG: AlertDialog? = null

/**
 * 应用缓存目录(App cache directory) :
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

fun getStr(@StringRes res: Int) = FileOperator.getContext().getString(res)

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

fun showAlert(context: Context, title: String, msg: String, block: (isPositive: Boolean) -> Unit) {
    GLOBAL_DIALOG?.dismiss()
    GLOBAL_DIALOG = AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(msg)
        .setPositiveButton("Open") { _, _ ->
            block.invoke(true)
        }
        .setNegativeButton("Cancel") { _, _ ->
            block.invoke(false)
        }
        .create()

    GLOBAL_DIALOG?.show()
}

/**
 * 压缩图片 1.Luban算法; 2.直接压缩 -> val bitmap:Bitmap=ImageCompressEngine.compressPure(uri)
 *
 * T : String.filePath / Uri / File
 */
fun <T> compressImage(context: Context, photos: List<T>, success: (index: Int, uri: Uri?) -> Unit) {
    ImageCompressor
        .with(context)
        .load(photos)
        .ignoreBy(50)//单位 Byte
        .setTargetDir(getCompressedImageCacheDir())
        .setFocusAlpha(false)
        .enableCache(true)
        .filter(object : ImageCompressPredicate {
            override fun apply(uri: Uri?): Boolean {
                //FileLogger.i("compressImage predicate $uri  ${FileUri.getFilePathByUri(uri)}")
                return if (uri != null) !FileUtils.getExtension(uri).endsWith("gif") else false
            }
        })
        .setRenameListener(object : OnImageRenameListener {
            override fun rename(uri: Uri?): String? {
                try {
                    val fileName = FileUtils.getFileNameFromUri(uri)
                    val md = MessageDigest.getInstance("MD5")
                    md.update(fileName?.toByteArray() ?: return "")
                    return BigInteger(1, md.digest()).toString(32)
                } catch (e: NoSuchAlgorithmException) {
                    FileLogger.e("rename onError ${e.message}")
                }
                return ""
            }
        })
        .setImageCompressListener(object : OnImageCompressListener {
            override fun onStart() {}
            override fun onSuccess(index: Int, uri: Uri?) {
                success.invoke(index, uri)
            }

            override fun onError(e: Throwable?) {
                FileLogger.e("compressImage onError ${e?.message}")
            }
        }).launch()
}