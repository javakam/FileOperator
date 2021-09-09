package ando.file.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import androidx.fragment.app.Fragment

/**
 * 文件的访问模式(File access mode) mode :
 *
 * 1. “r”表示只读访问，
 *
 * 2. “w”表示只写访问(擦除文件中当前的任何数据)，“wa”表示只写访问，以追加到任何现有数据，
 *
 * 3. “rw”表示对任何现有数据的读写访问，“rwt”表示对任何现有文件的读写访问。
 *
 *
 * > Access mode for the file.  May be "r" for read-only access,
 * "w" for write-only access (erasing whatever data is currently in
 * the file), "wa" for write-only access to append to any existing data,
 * "rw" for read and write access on any existing data, and "rwt" for read
 * and write access that truncates any existing file.
 *
 * See android.os.ParcelFileDescriptor#openInternal
 *  [https://www.man7.org/linux/man-pages/man2/open.2.html](https://www.man7.org/linux/man-pages/man2/open.2.html)
 */
internal const val AUTHORITY = ".andoFileProvider"

internal fun startActivity(context: Any, intent: Intent) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            context.startActivity(intent)
        }
    } else if (context is Fragment) {
        val activity = context.activity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            context.startActivity(intent)
        }
    } else if (context is Context) {
        context.startActivity(intent)
    }
}

//Permission
//----------------------------------------------------------------

/**
 * @return 传入的Uri是否已具备访问权限 (Whether the incoming Uri has access permission)
 */
fun giveUriPermission(uri: Uri?): Boolean {
    return uri?.run {
        when (FileOperator.getContext().checkUriPermission(
            this, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            PackageManager.PERMISSION_DENIED -> {
                FileOperator.getContext().grantUriPermission(
                    FileOperator.getApplication().packageName, this, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                false
            }
            else -> false
        }
    } ?: false
}

fun revokeUriPermission(uri: Uri?) {
    FileOperator.getContext().revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

inline fun <R> Uri.use(block: Uri.() -> R): R {
    var isAlreadyHavePermission = false
    try {
        isAlreadyHavePermission = giveUriPermission(this)
        return block()
    } catch (t: Throwable) {
        FileLogger.e("giveUriPermission Error ${t.message}")
    } finally {
        if (!isAlreadyHavePermission) {
            try {
                revokeUriPermission(this)
            } catch (t: Throwable) {
                FileLogger.e("revokeUriPermission Error ${t.message}")
            }
        }
    }
    return block()
}

object FileGlobal {

    const val MODE_READ_ONLY = "r"
    const val MODE_WRITE_ONLY_ERASING = "w"
    const val MODE_WRITE_ONLY_APPEND = "wa"
    const val MODE_READ_WRITE_DATA = "rw"
    const val MODE_READ_WRITE_FILE = "rwt"

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(value = [MODE_READ_ONLY, MODE_WRITE_ONLY_ERASING, MODE_WRITE_ONLY_APPEND, MODE_READ_WRITE_DATA, MODE_READ_WRITE_FILE])
    annotation class FileOpenMode


    const val MEDIA_TYPE_IMAGE = "image"
    const val MEDIA_TYPE_AUDIO = "audio"
    const val MEDIA_TYPE_VIDEO = "video"

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(value = [MEDIA_TYPE_IMAGE, MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO])
    annotation class FileMediaType


    /**
     * 1. 文件超过`数量或大小`限制直接返回失败
     * 2. 回调 onError
     *
     * - The file exceeds the `number or size` limit and returns directly to failure
     * - Callback onError
     */
    const val OVER_LIMIT_EXCEPT_ALL: Int = 1

    /**
     * 1. 文件超过数量限制或大小限制
     * 2. 单一类型: 保留未超限制的文件并返回, 去掉后面溢出的部分; 多种类型: 保留正确的文件, 去掉错误类型的所有文件
     * 3. 回调 onSuccess
     *
     * - The file exceeds the number limit or the size limit
     * - 1. Single type: keep the file that is not over the limit and return, remove the overflow part;
     *      2. Multiple types: keep the correct file, remove all files of the wrong type
     * - Call back onSuccess
     */
    const val OVER_LIMIT_EXCEPT_OVERFLOW: Int = 2

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [OVER_LIMIT_EXCEPT_ALL, OVER_LIMIT_EXCEPT_OVERFLOW])
    annotation class FileOverLimitStrategy


    /**
     * eg:
     * ```kotlin
     *      val queryStatement = buildQuerySelectionStatement(MEDIA_TYPE_VIDEO,
     *          null, null, null, null, null, false)
     *
     *      queryStatement.append(
     *          "${MediaStore.Video.Media.DURATION} >= ? ",
     *          noNull(TimeUnit.MILLISECONDS.convert(sourceDuration,sourceUnit).toString())
     *      )
     * ```
     */
    data class QuerySelectionStatement(val selection: StringBuilder, val selectionArgs: MutableList<String>, val needAddPre: Boolean) {
        fun append(selectionNew: String, selectionArgsNew: String) {
            selection.append("${if (needAddPre) " and " else " "} $selectionNew ")
            selectionArgs.add(selectionArgsNew)
        }
    }

    /**
     * ### 加载媒体 单个媒体文件 👉 ContentResolver.openFileDescriptor
     *
     * Load media single media file
     *
     * 根据文件描述符选择对应的打开方式。"r"表示读，"w"表示写
     *
     * Select the corresponding opening method according to the file descriptor. "r" means read, "w" means write
     */
    fun openFileDescriptor(
        uri: Uri?, @FileOpenMode mode: String = MODE_READ_ONLY, cancellationSignal: CancellationSignal? = null,
    ): ParcelFileDescriptor? {
        if (!FileUtils.checkUri(uri)) return null
        return FileOperator.getContext().contentResolver.openFileDescriptor(uri ?: return null, mode, cancellationSignal)
    }

    //dump
    //---------------------------------------------------------------------------------

    fun dumpParcelFileDescriptor(pfd: ParcelFileDescriptor?) =
        if (pfd != null) {
            //读取成功 : 87  大小:2498324B
            FileLogger.d("Read successfully: getStatSize=${pfd.statSize}B")
        } else {
            FileLogger.e("Reading failed!")
        }

    /**
     * 获取文档元数据(Get document metadata)
     */
    fun dumpMetaData(uri: Uri?, block: ((displayName: String?, size: String?) -> Unit)? = null) {
        val cursor =
            FileOperator.getContext().contentResolver.query(uri ?: return, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) { // moveToFirst die
                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                val size: String = if (!it.isNull(sizeIndex)) {
                    it.getString(sizeIndex)
                } else "Unknown"
                block?.invoke(displayName, size)
                FileLogger.i("Name ：$displayName  Size：$size B")
            }
        }
    }

}