package com.ando.file.sample.utils

import ando.file.androidq.FileOperatorQ
import ando.file.core.FileGlobal.dumpMetaData
import ando.file.core.FileOpener
import ando.file.core.FileSizeUtils
import ando.file.selector.FileSelectResult
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ando.file.sample.getCompressedImageCacheDir
import java.io.File

/**
 * Title: ResultUtils
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/12/10  11:06
 */
object ResultUtils {

    private fun redBoldText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return "<span style='color:red;font-weight:bold'>$text</span>"
    }

    fun setErrorText(tvError: TextView, e: Throwable?) {
        if (e == null) {
            tvError.visibility = View.GONE
            return
        }
        tvError.visibility = View.VISIBLE
        tvError.text = tvError.text.toString().plus(" 错误信息: ${e.message} \n")
    }

    fun setImageEvent(imageView: ImageView, uri: Uri?) {
        val bitmap: Bitmap? = FileOperatorQ.getBitmapFromUri(uri)
        imageView.setImageBitmap(bitmap)
        imageView.setOnClickListener {
            FileOpener.openFileBySystemChooser(imageView.context, uri, "image/*")
        }
    }

    fun setFormattedResults(tvResult: TextView, results: List<FileSelectResult>?) {
        tvResult.text = ""
        formatResults(results = results).forEach {
            tvResult.text = tvResult.text.toString().plus(it)
        }
    }

    fun formatResults(results: List<FileSelectResult>?): List<String> {
        if (results.isNullOrEmpty()) return emptyList()
        val infoList = mutableListOf<String>()
        results.forEach {
            val info = "${it}格式化大小: ${FileSizeUtils.formatFileSize(it.fileSize)}\n" +
                    " 格式化大小(保留三位小数): ${FileSizeUtils.formatFileSize(it.fileSize, 3)}\n" +
                    " 格式化大小(自定义单位, 保留一位小数): ${FileSizeUtils.formatSizeByTypeWithUnit(it.fileSize, 1, FileSizeUtils.FileSizeType.SIZE_TYPE_KB)}"
            dumpMetaData(uri = it.uri) { name: String?, _: String? ->
                infoList.add(""" 选择结果:
                    | ---------
                    | 🍎压缩前
                    | 文件名: $name
                    | $info
                    | ---------${"\n\n"}""".trimMargin())
            }
        }
        return infoList
    }

    fun formatCompressedImageInfo(uri: Uri?, block: (info: String) -> Unit) {
        dumpMetaData(uri) { name: String?, size: String? ->
            block.invoke("""${"\n\n"} ---------
                | 🍎压缩后
                | 文件名: $name
                | Uri: $uri 
                | 路径: ${uri?.path} 
                | 大小: $size
                | 格式化(默认单位, 保留两位小数): ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}
                | 压缩图片缓存目录总大小: ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}
                | ---------${"\n"}""".trimMargin())
        }
    }

    fun resetUI(vararg views: View?) {
        views.forEach { v ->
            if (v is TextView) {
                v.text = ""
            } else if (v is ImageView) {
                v.setImageBitmap(null)
            }
        }
    }

}