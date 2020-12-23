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