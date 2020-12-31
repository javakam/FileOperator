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
        override var id: Long,
    override var uri: Uri?,
    override var displayName: String?,
    val duration: Long?,
    override var size: Long?
) : BaseMediaColumnsData()