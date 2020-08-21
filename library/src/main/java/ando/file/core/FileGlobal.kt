package ando.file.core

import ando.file.FileOperator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.fragment.app.Fragment
import ando.file.checkUriFileExit
import java.io.Closeable
import java.io.IOException

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
    val needAddPre: Boolean
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
    cancellationSignal: CancellationSignal? = null
): ParcelFileDescriptor? {
    if (!checkUriFileExit(uri)) return null
    return FileOperator.getContext().contentResolver.openFileDescriptor(
        uri ?: return null,
        mode,
        cancellationSignal
    )
}

fun noNull(s: String?): String = if (s.isNullOrBlank()) "" else s

// closeIO / Activity Control
//-----------------------------------------------------------------------

internal fun closeIO(io: Closeable?) {
    try {
        io?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}


internal fun isActivityLive(activity: Activity?): Boolean {
    return activity != null && !activity.isFinishing && !activity.isDestroyed
}

internal fun startActivity(context: Any, intent: Intent) {
    if (context is Activity) {
        if (isActivityLive(context)) {
            context.startActivity(intent)
        }
    } else if (context is Fragment) {
        val activity = context.activity
        if (isActivityLive(activity)) {
            context.startActivity(intent)
        }
    } else if (context is Context) {
        (context as? Context)?.startActivity(intent)
    }
}

internal fun startActivityForResult(context: Any, intent: Intent, requestCode: Int) {
    if (context is Activity) {
        if (isActivityLive(context)) {
            context.startActivityForResult(intent, requestCode)
        }
    } else if (context is Fragment) {
        val activity = context.activity
        if (isActivityLive(activity)) {
            context.startActivityForResult(intent, requestCode)
        }
    }
}

//dump
//------------------------------------------------------------------------------------------------

fun dumpParcelFileDescriptor(pfd: ParcelFileDescriptor?) {
    if (pfd != null) {
        //读取成功 : 91  1519
        FileLogger.d("读取成功 : ${pfd.fd}  大小:${pfd.statSize}B")
    } else {
        FileLogger.e("读取成功失败!")
    }
}

/**
 * 获取文档元数据
 */
fun dumpMetaData(uri: Uri?) {
    dumpMetaData(uri) { _: String?, _: String? ->
    }
}

fun dumpMetaData(uri: Uri?, block: (displayName: String?, size: String?) -> Unit) {
    // The query, because it only applies to a single document, returns only
    // one row. There's no need to filter, sort, or select fields,
    // because we want all fields for one document.
    val cursor = FileOperator.getContext().contentResolver.query(uri ?: return, null, null, null, null)

    cursor?.use {
        // moveToFirst() returns false if the cursor has 0 rows. Very handy for
        // "if there's anything to look at, look at it" conditionals.
        while (it.moveToNext()) { // moveToFirst die
            // Note it's called "Display Name". This is
            // provider-specific, and might not necessarily be the file name.
            val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))

            val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
            // If the size is unknown, the value stored is null. But because an
            // int can't be null, the behavior is implementation-specific,
            // and unpredictable. So as
            // a rule, check if it's null before assigning to an int. This will
            // happen often: The storage API allows for remote files, whose
            // size might not be locally known.
            val size: String = if (!it.isNull(sizeIndex)) {
                // Technically the column stores an int, but cursor.getString()
                // will do the conversion automatically.
                it.getString(sizeIndex)
            } else {
                "Unknown"
            }

            block.invoke(displayName, size)
            FileLogger.i("文件名称 ：$displayName  Size：$size B")
        }
    }
}


