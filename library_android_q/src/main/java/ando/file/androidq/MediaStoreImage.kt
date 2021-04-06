package ando.file.androidq

import android.net.Uri
import java.util.*

/**
 * mapping ->
 *  MediaStore.Image.Media._ID,
 *  MediaStore.Image.Media.DISPLAY_NAME,
 */
data class MediaStoreImage(
    var id: Long,
    var uri: Uri?,
    var displayName: String?,
    var size: Long?,
    var description: String?,
    var title: String?,
    var mimeType: String?,
    var dateAdded: Date?,
) {
    @Suppress("UNUSED")
    constructor(uri: Uri?, displayName: String?, size: Long?) : this(0L, uri, displayName, size,
        null, null, null, null)

}