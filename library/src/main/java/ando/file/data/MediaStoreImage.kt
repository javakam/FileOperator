package ando.file.data

import android.net.Uri
import java.util.*

/**
 * mapping ->
 *  MediaStore.Image.Media._ID,
 *  MediaStore.Image.Media.DISPLAY_NAME,
 */
data class MediaStoreImage(
        override var id: Long, override var uri: Uri?, override var displayName: String?, override var size: Long?,
        val description: String?, override var title: String?, override var mimeType: String?,
        override var dateAdded: Date?
) : BaseMediaColumnsData() {
    constructor(uri: Uri?, displayName: String?, size: Long?) : this(
            0L,
            uri,
            displayName,
            size,
            null,
            null,
            null,
            null
    )
}