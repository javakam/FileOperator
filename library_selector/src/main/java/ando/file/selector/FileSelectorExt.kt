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
 * 用于自定义文件类型 (Used for custom file types)
 */
interface IFileType {
    fun getMimeType(): String? = null
    fun getMimeTypeArray(): MutableList<String>? = mutableListOf()
    fun fromName(fileName: String?): IFileType = FileType.UNKNOWN
    fun fromName(fileName: String?, split: Char): IFileType = FileType.UNKNOWN
    fun fromPath(filePath: String?): IFileType = FileType.UNKNOWN
    fun fromFile(file: File): IFileType = FileType.UNKNOWN
    fun fromUri(uri: Uri?): IFileType = FileType.UNKNOWN
    fun parseSuffix(uri: Uri?): String = FileUtils.getExtension(uri).lowercase(Locale.getDefault())

    /**
     * 自定义`IFileType`实现类, 需要把返回的`Uri`对应上, 否则会判定为 FileType.UNKNOWN
     *
     * @param uri File Uri
     * @param fileSuffix Custom File Suffix
     * @param fileType Custom IFileType Implementation class, Capital of fileSuffix
     */
    fun resolveFileMatch(uri: Uri?, fileSuffix: String, fileType: IFileType): IFileType =
        if (parseSuffix(uri).equals(fileSuffix, true)) fileType else FileType.UNKNOWN
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
