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

internal fun isActivityLive(activity: Activity?): Boolean {
    return activity != null && !activity.isFinishing && !activity.isDestroyed
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