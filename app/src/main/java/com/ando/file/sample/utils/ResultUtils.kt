/**
 * Copyright (C)  javakam, FileOperator Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ando.file.sample.utils

import ando.file.androidq.FileOperatorQ
import ando.file.core.FileGlobal.dumpMetaData
import ando.file.core.FileMimeType
import ando.file.core.FileOpener
import ando.file.core.FileSizeUtils
import ando.file.selector.FileSelectResult
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ando.file.sample.R
import com.ando.file.sample.getCompressedImageCacheDir
import com.ando.file.sample.showAlert
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

    data class ResultShowBean(
        var originResult: String = "",
        var compressedResult: String = "",
        var originUri: Uri? = null,
        var compressedUri: Uri? = null,
    )

    fun RecyclerView.asVerticalList() {
        setHasFixedSize(true)
        itemAnimator = null
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State,
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.set(0, 5, 0, 5)
            }
        })
    }

    fun setItemEvent(v: View?, uri: Uri?, title: String) {
        v?.setOnClickListener {
            showAlert(v.context, title, uri?.toString() ?: "") {
                if (it) FileOpener.openFileBySystemChooser(v.context, uri, FileMimeType.getMimeType(uri))
            }
        }
    }

    fun setErrorText(tvError: TextView, e: Throwable?) {
        if (e == null) {
            tvError.visibility = View.GONE
            return
        }
        tvError.visibility = View.VISIBLE
        tvError.text = tvError.text.toString().plus("错误信息: ${e.message}")
    }

    fun setImageEvent(imageView: ImageView, uri: Uri?) {
        val bitmap: Bitmap? = FileOperatorQ.getBitmapFromUri(uri)
        val context = imageView.context
        imageView.setImageBitmap(if (bitmap == null || bitmap.isRecycled)
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_place_holder) else bitmap)
        imageView.setOnClickListener {
            FileOpener.openFileBySystemChooser(context, uri, "image/*")
        }
    }

    /**
     * mimeType, FileSize
     */
    fun setCoreResults(tvResult: TextView, results: List<FileSelectResult>?) {
        tvResult.text = ""
        if (results.isNullOrEmpty()) return
        results.forEachIndexed { _, fsr ->
            val info = "${fsr}格式化大小: ${FileSizeUtils.formatFileSize(fsr.fileSize)}\n" +
                    " 格式化大小(不带单位, 保留三位小数): ${FileSizeUtils.formatFileSize(fsr.fileSize, 3)}\n" +
                    " 格式化大小(自定义单位, 保留一位小数): ${FileSizeUtils.formatSizeByTypeWithUnit(fsr.fileSize, 1, FileSizeUtils.FileSizeType.SIZE_TYPE_KB)}"

            dumpMetaData(uri = fsr.uri) { name: String?, _: String? ->
                val text = """
                    | ------------------
                    | 🍎文件名: $name
                    | $info
                    | ------------------${"\n\n\n"}""".trimMargin()
                tvResult.text = tvResult.text.toString().plus(text)
            }
        }
    }

    fun setFormattedResults(tvResult: TextView, results: List<FileSelectResult>?) {
        tvResult.text = ""
        formatResults(results = results, isMulti = false) { l ->
            l.forEach {
                tvResult.text = tvResult.text.toString().plus(it.second)
            }
        }
    }

    fun formatResults(results: List<FileSelectResult>?, isMulti: Boolean, block: (resultsForShow: List<Pair<Uri, String>>) -> Unit) {
        if (results.isNullOrEmpty()) return
        val infoList = mutableListOf<Pair<Uri, String>>()
        results.forEachIndexed { i, fsr ->
            val info = "${fsr}格式化大小: ${FileSizeUtils.formatFileSize(fsr.fileSize)}\n" +
                    " 格式化大小(不带单位, 保留三位小数): ${FileSizeUtils.formatFileSize(fsr.fileSize, 3)}\n" +
                    " 格式化大小(自定义单位, 保留一位小数): ${FileSizeUtils.formatSizeByTypeWithUnit(fsr.fileSize, 1, FileSizeUtils.FileSizeType.SIZE_TYPE_KB)}"
            dumpMetaData(uri = fsr.uri) { name: String?, _: String? ->
                infoList.add((fsr.uri ?: return@dumpMetaData) to if (isMulti) {
                    """
                    | 🍎压缩前 ($i)
                    | 文件名: $name
                    | $info
                    """.trimMargin()
                } else {
                    """ 选择结果:
                    | ---------
                    | 🍎压缩前
                    | 文件名: $name
                    | $info
                    | ---------${"\n\n"}""".trimMargin()
                })
            }
        }
        block.invoke(infoList)
    }

    fun formatCompressedImageInfo(uri: Uri?, isMulti: Boolean, block: (info: String) -> Unit) {
        dumpMetaData(uri) { name: String?, size: String? ->
            if (name.isNullOrBlank() && uri == null) {
                block.invoke("")
                return@dumpMetaData
            }
            block.invoke(if (isMulti) {
                """
                | 🍎压缩后
                | 文件名: $name
                | Uri: $uri 
                | 路径: ${uri?.path} 
                | 大小: $size
                | 格式化(默认单位, 保留两位小数): ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}
                | 压缩图片缓存目录总大小: ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}
                """.trimMargin()
            } else {
                """${"\n\n"} ---------
                | 🍎压缩后
                | 文件名: $name
                | Uri: $uri 
                | 路径: ${uri?.path} 
                | 大小: $size
                | 格式化(默认单位, 保留两位小数): ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}
                | 压缩图片缓存目录总大小: ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}
                | ---------${"\n"}""".trimMargin()
            })
        }
    }

    fun resetUI(vararg views: View?) {
        views.forEach { v ->
            if (v is TextView) {
                v.text = ""
            } else if (v is ImageView) {
                v.setImageBitmap(BitmapFactory.decodeResource(v.resources, R.mipmap.ic_place_holder))
            }
        }
    }

}