package ando.file.androidq

import android.net.Uri

/**
 * mapping ->
 *  MediaStore.Video.Media._ID,
 *  MediaStore.Video.Media.DISPLAY_NAME,
 *  MediaStore.Video.Media.DURATION,
 *  MediaStore.Video.Media.SIZE
 */
data class MediaStoreVideo(
    var id: Long,
    var uri: Uri?,
    var displayName: String?,
    var duration: Long?,
    var size: Long?,
)