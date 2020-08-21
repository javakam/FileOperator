package ando.file.core

import ando.file.FileOperator
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.annotation.IntDef
import androidx.annotation.StringDef

/**
 * 文件的访问模式 mode :
 * 可能是“r”表示只读访问，
 * “w”表示只写访问(擦除文件中当前的任何数据)，“wa”表示只写访问，以追加到任何现有数据，
 * “rw”表示对任何现有数据的读写访问，“rwt”表示对任何现有文件的读写访问。
 * <pre>
 * Access mode for the file.  May be "r" for read-only access,
 * "w" for write-only access (erasing whatever data is currently in
 * the file), "wa" for write-only access to append to any existing data,
 * "rw" for read and write access on any existing data, and "rwt" for read
 * and write access that truncates any existing file.
 * </pre>
 *
 * android.os.ParcelFileDescriptor#openInternal 👇
 * https://www.man7.org/linux/man-pages/man2/open.2.html
 */
const val MODE_READ_ONLY = "r"
const val MODE_WRITE_ONLY_ERASING = "w"
const val MODE_WRITE_ONLY_APPEND = "wa"
const val MODE_READ_WRITE_DATA = "rw"
const val MODE_READ_WRITE_FILE = "rwt"

@Retention(AnnotationRetention.SOURCE)
@StringDef(value = [MODE_READ_ONLY, MODE_WRITE_ONLY_ERASING, MODE_WRITE_ONLY_APPEND, MODE_READ_WRITE_DATA, MODE_READ_WRITE_FILE])
annotation class FileOpenMode {}

const val MEDIA_TYPE_IMAGE = "image"
const val MEDIA_TYPE_AUDIO = "audio"
const val MEDIA_TYPE_VIDEO = "video"

@Retention(AnnotationRetention.SOURCE)
@StringDef(value = [MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO])
annotation class FileMediaType {}

const val OVER_SIZE_LIMIT_ALL_DONT = 1                //超过限制大小全部不返回
const val OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART = 2    //超过限制大小去掉后面相同类型文件

@Retention(AnnotationRetention.SOURCE)
@IntDef(value = [OVER_SIZE_LIMIT_ALL_DONT, OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART])
annotation class FileOverSizeStrategy {}

/**
 * eg:
 *      val queryStatement = buildQuerySelectionStatement(MEDIA_TYPE_VIDEO, null, null, null, null, null, false)
 *      queryStatement.append(
 *          "${MediaStore.Video.Media.DURATION} >= ? ",
 *          noNull(TimeUnit.MILLISECONDS.convert(sourceDuration,sourceUnit).toString())
 *      )
 */
data class QuerySelectionStatement(
    val selection: StringBuilder,
    val selectionArgs: MutableList<String>,
    val needAddPre: Boolean,
) {
    fun append(selectionNew: String, selectionArgsNew: String) {
        selection.append("${if (needAddPre) " and " else " "} $selectionNew ")
        selectionArgs.add(selectionArgsNew)
    }
}

/**
 * 加载媒体 单个媒体文件 👉 ContentResolver.openFileDescriptor
 * <p>
 * 根据文件描述符选择对应的打开方式。"r"表示读，"w"表示写
 */
fun openFileDescriptor(
    uri: Uri?,
    @FileOpenMode mode: String = MODE_READ_ONLY,
    cancellationSignal: CancellationSignal? = null,
): ParcelFileDescriptor? {
    if (!checkUriFileExit(uri)) return null
    return FileOperator.getContext().contentResolver.openFileDescriptor(uri ?: return null, mode, cancellationSignal)
}

fun checkUriFileExit(uri: Uri?): Boolean {
    val cursor = FileOperator.getContext().contentResolver.query(uri ?: return false, null, null, null, null)
    if (cursor == null || !cursor.moveToFirst()) {
        FileLogger.e("删除失败 -> 1.没有找到 Uri 对应的文件 ; 2.目录为空 ")
        closeIO(cursor)
        return false
    }
    return true
}

//dump
//------------------------------------------------------------------------------------------------

fun dumpParcelFileDescriptor(pfd: ParcelFileDescriptor?) =
    if (pfd != null) {
        //读取成功 : 91  1519
        FileLogger.d("读取成功 : ${pfd.fd}  大小:${pfd.statSize}B")
    } else {
        FileLogger.e("读取成功失败!")
    }

/**
 * 获取文档元数据
 */
fun dumpMetaData(uri: Uri?) = dumpMetaData(uri) { _: String?, _: String? -> }

fun dumpMetaData(uri: Uri?, block: (displayName: String?, size: String?) -> Unit) {
    val cursor =
        FileOperator.getContext().contentResolver.query(uri ?: return, null, null, null, null)

    cursor?.use {
        while (it.moveToNext()) { // moveToFirst die
            val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))

            val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
            val size: String = if (!it.isNull(sizeIndex)) {
                it.getString(sizeIndex)
            } else "Unknown"
            block.invoke(displayName, size)
            FileLogger.i("文件名称 ：$displayName  Size：$size B")
        }
    }
}