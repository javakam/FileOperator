package ando.file.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java.io.Closeable
import java.io.IOException

/**
 * Title: InternalExt
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/8/21  17:04
 */

internal fun noNull(s: String?): String = if (s.isNullOrBlank()) "" else s

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