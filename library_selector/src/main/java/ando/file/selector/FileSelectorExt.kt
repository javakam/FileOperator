package ando.file.selector

import ando.file.core.FileType
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment

/**
 * Title: FileSelectorExt
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/8/21  10:57
 */
interface FileSelectCallBack {
    fun onSuccess(results: List<FileSelectResult>?)
    fun onError(e: Throwable?)
}

interface FileSelectCondition {
    fun accept(@NonNull fileType: FileType, uri: Uri?): Boolean
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