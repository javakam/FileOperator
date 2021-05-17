package com.ando.file.sample.utils

import ando.file.androidq.FileOperatorQ
import ando.file.core.*
import ando.file.core.FileGlobal.dumpMetaData
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
import com.ando.file.sample.getStr
import com.ando.file.sample.showAlert
import java.io.File

/**
 * # ResultUtils
 *
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
                if (it) FileOpener.openFile(v.context, uri, FileMimeType.getMimeType(uri))
            }
        }
    }

    fun setErrorText(tvError: TextView, e: Throwable?) {
        if (e == null) {
            tvError.visibility = View.GONE
            return
        }
        tvError.visibility = View.VISIBLE
        tvError.text = tvError.text.toString().plus("${getStr(R.string.str_ando_file_error_info)} ${e.message}")
    }

    fun setImageEvent(imageView: ImageView, uri: Uri?) {
        val bitmap: Bitmap? = FileOperatorQ.getBitmapFromUri(uri)
        val context = imageView.context
        imageView.setImageBitmap(
            if (bitmap == null || bitmap.isRecycled)
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_place_holder) else bitmap
        )
        imageView.setOnClickListener {
            FileOpener.openFile(context, uri, "image/*")
        }
    }

    fun setCoreResults(tvResult: TextView, file: File?) {
        tvResult.text = ""
        if (file == null || !file.exists()) {
            tvResult.visibility = View.GONE
            return
        } else {
            tvResult.visibility = View.VISIBLE
        }
        val uri: Uri = FileUri.getUriByFile(file) ?: return
        val size: Long = FileSizeUtils.calculateFileOrDirSize(file.path)

        val info = "${getStr(R.string.str_ando_file_format_size)}: ${FileSizeUtils.formatFileSize(size)}\n" +
                " ${getStr(R.string.str_ando_file_format_size2)}: ${FileSizeUtils.formatFileSize(size, 3)}\n" +
                " ${getStr(R.string.str_ando_file_format_size3)}: ${
                    FileSizeUtils.formatSizeByTypeWithUnit(size, 1, FileSizeUtils.FileSizeType.SIZE_TYPE_KB)
                }"

        dumpMetaData(uri = uri) { name: String?, _: String? ->
            val text = """
                    | ------------------
                    | 🍎${getStr(R.string.str_ando_file_name)}: $name
                    | ${getStr(R.string.str_ando_file_path)}: ${file.path}
                    | ${getStr(R.string.str_ando_file_suffix)}: ${FileUtils.getExtension(file.name)}
                    | MimeType: ${FileMimeType.getMimeType(uri)}
                    | $info
                    | ${getStr(R.string.str_ando_file_exist)}: ${file.exists()}
                    | ------------------${"\n"}""".trimMargin()
            tvResult.text = tvResult.text.toString().plus(text)
        }
    }

    /**
     * mimeType, FileSize
     */
    fun setCoreResults(tvResult: TextView, results: List<FileSelectResult>?) {
        tvResult.text = ""
        if (results.isNullOrEmpty()) return
        results.forEachIndexed { _, fsr: FileSelectResult ->
            val info = "$fsr" +
                    " 🍑 Uri  转换为 Path=${FileUri.getPathByUri(fsr.uri)}\n" +
                    //" 🍈 Uri  转换为 Path(Uri.parse)=${tvResult.context.contentResolver.}\n" +
                    " 🍉 Path 转换为 Uri(FileProvider)=${FileUri.getUriByPath(fsr.filePath)}\n" +
                    " 🍍 Path 转换为 Uri(Uri.fromFile)=${Uri.fromFile(File(fsr.filePath ?: ""))}\n" +
                    " ${getStr(R.string.str_ando_file_format_size)}: ${FileSizeUtils.formatFileSize(fsr.fileSize)}\n" +
                    " ${getStr(R.string.str_ando_file_format_size2)}: ${FileSizeUtils.formatFileSize(fsr.fileSize, 3)}\n" +
                    " ${getStr(R.string.str_ando_file_format_size3)}: ${
                        FileSizeUtils.formatSizeByTypeWithUnit(fsr.fileSize, 1, FileSizeUtils.FileSizeType.SIZE_TYPE_KB)
                    }"

            dumpMetaData(uri = fsr.uri) { name: String?, _: String? ->
                val text = """
                    | ------------------
                    | 🍎${getStr(R.string.str_ando_file_name)}: $name
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
            val info = "${fsr}${getStr(R.string.str_ando_file_format_size)}: ${FileSizeUtils.formatFileSize(fsr.fileSize)}\n" +
                    " ${getStr(R.string.str_ando_file_format_size2)}: ${FileSizeUtils.formatFileSize(fsr.fileSize, 3)}\n" +
                    " ${getStr(R.string.str_ando_file_format_size)}: ${
                        FileSizeUtils.formatSizeByTypeWithUnit(fsr.fileSize,
                            1,
                            FileSizeUtils.FileSizeType.SIZE_TYPE_KB)
                    }"
            dumpMetaData(uri = fsr.uri) { name: String?, _: String? ->
                infoList.add(
                    (fsr.uri ?: return@dumpMetaData) to if (isMulti) {
                        """
                    | 🍎${getStr(R.string.str_ando_file_before_compression)} ($i)
                    | ${getStr(R.string.str_ando_file_name)}: $name
                    | $info
                    """.trimMargin()
                    } else {
                        """ ${getStr(R.string.str_ando_file_select_result)}:
                    | ---------
                    | 🍎${getStr(R.string.str_ando_file_before_compression)}
                    | ${getStr(R.string.str_ando_file_name)}: $name
                    | $info
                    | ---------${"\n\n"}""".trimMargin()
                    }
                )
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
            block.invoke(
                if (isMulti) {
                    """
                | 🍎${getStr(R.string.str_ando_file_after_compression)}
                | ${getStr(R.string.str_ando_file_name)}: $name
                | Uri: $uri 
                | ${getStr(R.string.str_ando_file_path)}: ${uri?.path} 
                | ${getStr(R.string.str_ando_file_size)}: $size
                | ${getStr(R.string.str_ando_file_format_size4)}: ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}
                | ${getStr(R.string.str_ando_file_compress_dir_size)}: ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}
                """.trimMargin()
                } else {
                    """${"\n\n"} ---------
                | 🍎${getStr(R.string.str_ando_file_after_compression)}
                | ${getStr(R.string.str_ando_file_name)}: $name
                | Uri: $uri 
                | ${getStr(R.string.str_ando_file_path)}: ${uri?.path} 
                | ${getStr(R.string.str_ando_file_size)}: $size
                | ${getStr(R.string.str_ando_file_format_size4)}: ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}
                | ${getStr(R.string.str_ando_file_compress_dir_size)}: ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}
                | ---------${"\n"}""".trimMargin()
                }
            )
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