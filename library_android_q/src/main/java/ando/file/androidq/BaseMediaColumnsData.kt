package ando.file.androidq

import android.net.Uri
import java.util.*

/**
 * BaseMediaColumnsData
 * <p>
 * Description: android.provider.MediaStore.MediaColumns 映射数据类 , 包含常用的字段
 * </p>
 * @author javakam
 * @date 2020/5/28  14:31
 */
open class BaseMediaColumnsData {

    open var id: Long = -1L
    open var uri: Uri? = null
    open var displayName: String? = null
    open var size: Long? = 0L
    open var title: String? = null
    open var mimeType: String? = null
    open var relativePath: String? = null
    open var isPending: Int? = 0
    open var volumeName: String? = null
    open var dateAdded: Date? = null
    open var dateModified: Long? = 0L
    open var dateTaken: Long? = 0L

}