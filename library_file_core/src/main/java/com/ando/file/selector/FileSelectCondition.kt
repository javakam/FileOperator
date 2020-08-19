package com.ando.file.selector

import android.net.Uri
import androidx.annotation.NonNull
import com.ando.file.common.FileType

/**
 * Title: FileSelectPredicate
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/5/21  11:19
 */
interface FileSelectCondition {

    fun accept(@NonNull fileType: FileType, uri: Uri?): Boolean

}