package ando.file.selector

import ando.file.core.FileUtils
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import java.io.File
import java.util.*

/**
 * # FileSelectorExt
 *
 * @author javakam
 * @date 2020/8/21 10:57
 */

/**
 * 用于自定义文件类型 (Used for custom file types)
 */
interface IFileType {
    fun fromFileName(fileName: String?): IFileType = FileType.UNKNOWN
    fun fromFileName(fileName: String?, split: Char): IFileType = FileType.UNKNOWN
    fun fromFilePath(filePath: String?): IFileType = FileType.UNKNOWN
    fun fromFile(file: File): IFileType = FileType.UNKNOWN
    fun fromFileUri(uri: Uri?): IFileType = FileType.UNKNOWN
    fun parseSuffix(uri: Uri?): String = FileUtils.getExtension(uri).toLowerCase(Locale.getDefault())
}

interface FileSelectCallBack {
    fun onSuccess(results: List<FileSelectResult>?)
    fun onError(e: Throwable?)
}

interface FileSelectCondition {
    fun accept(@NonNull fileType: IFileType, uri: Uri?): Boolean
}

internal fun startActivityForResult(context: Any?, intent: Intent, requestCode: Int) {
    if (context == null) return
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            context.startActivityForResult(intent, requestCode)
        }
    } else if (context is Fragment) {
        val act: Activity? = context.activity
        if (act?.isFinishing == false && !act.isDestroyed) {
            context.startActivityForResult(intent, requestCode)
        }
    }
}