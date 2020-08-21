package ando.file.core

import android.net.Uri
import android.webkit.MimeTypeMap
import ando.file.FileOperator.getContext
import java.util.*

/**
 * Title: MimeType
 *
 * @author javakam
 * @date 2020/6/3  14:42
 */

/**
 * 根据 File Name/Path/Url 获取相应的 MimeType
 *
 * @return mineType  "application/x-flac" , "video/3gpp" ...
 */
fun getMimeType(str: String?): String {
    val type = "*/*"
    if (str.isNullOrBlank()) return type

    val extension = MimeTypeMap.getFileExtensionFromUrl(str)
    val mimeType = if (extension.isNullOrBlank()) getMimeTypeSupplement(str)
    else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: type

    FileLogger.i("系统 MimeType ：$mimeType  extension=$extension")
    return mimeType.toLowerCase(Locale.getDefault())
}

fun getMimeType(uri: Uri?): String =
    if (uri != null) getContext().contentResolver.getType(uri)?.toLowerCase(Locale.getDefault()) ?: getMimeType(getFilePathByUri(uri))
    else getMimeType(getFilePathByUri(uri))

/**
 * MimeTypeMap.getSingleton().getMimeTypeFromExtension(...) 的补充
 */
fun getMimeTypeSupplement(fileName: String): String {
    var type = "*/*"
    val dotIndex = fileName.lastIndexOf(".")
    if (dotIndex < 0) return type
    val end = fileName.substring(dotIndex).toLowerCase(Locale.getDefault())
    if (end.isBlank()) return type
    for (mimeTypes in MIME_TABLES) {
        if (end.equals(mimeTypes[0], true)) {
            type = mimeTypes[1]
            break
        }
    }
    return type.toLowerCase(Locale.getDefault())
}

/**
 * https://github.com/broofa/mime/blob/master/types
 */
private val MIME_TABLES =
    arrayOf(
        arrayOf(".flac", "audio/*"),//application/x-flac
        arrayOf(".flv", "video/x-flv"),
        arrayOf(".m3u8", "application/vnd.apple.mpegurl"),
        arrayOf(".3gp", "video/3gpp"),
        arrayOf(".apk", "application/vnd.android.package-archive"),
        arrayOf(".asf", "video/x-ms-asf"),
        arrayOf(".avi", "video/x-msvideo"),
        arrayOf(".bin", "application/octet-stream"),
        arrayOf(".bmp", "image/bmp"),
        arrayOf(".c", "text/plain"),
        arrayOf(".class", "application/octet-stream"),
        arrayOf(".conf", "text/plain"),
        arrayOf(".cpp", "text/plain"),
        arrayOf(".doc", "application/msword"),
        arrayOf(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        arrayOf(".xls", "application/vnd.ms-excel"),
        arrayOf(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        arrayOf(".exe", "application/octet-stream"),
        arrayOf(".gif", "image/gif"),
        arrayOf(".gtar", "application/x-gtar"),
        arrayOf(".gz", "application/x-gzip"),
        arrayOf(".h", "text/plain"),
        arrayOf(".htm", "text/html"),
        arrayOf(".html", "text/html"),
        arrayOf(".jar", "application/java-archive"),
        arrayOf(".java", "text/plain"),
        arrayOf(".jpeg", "image/jpeg"),
        arrayOf(".jpg", "image/jpeg"),
        arrayOf(".js", "application/x-javascript"),
        arrayOf(".log", "text/plain"),
        arrayOf(".m3u", "audio/x-mpegurl"),
        arrayOf(".m3u8", "application/vnd.apple.mpegurl"),
        arrayOf(".m4a", "audio/mp4a-latm"),
        arrayOf(".m4b", "audio/mp4a-latm"),
        arrayOf(".m4p", "audio/mp4a-latm"),
        arrayOf(".m4u", "video/vnd.mpegurl"),
        arrayOf(".m4v", "video/x-m4v"),
        arrayOf(".mov", "video/quicktime"),
        arrayOf(".mp2", "audio/x-mpeg"),
        arrayOf(".mp3", "audio/x-mpeg"),
        arrayOf(".mp4", "video/mp4"),
        arrayOf(".mpc", "application/vnd.mpohun.certificate"),
        arrayOf(".mpe", "video/mpeg"),
        arrayOf(".mpeg", "video/mpeg"),
        arrayOf(".mpg", "video/mpeg"),
        arrayOf(".mpg4", "video/mp4"),
        arrayOf(".mpga", "audio/mpeg"),
        arrayOf(".msg", "application/vnd.ms-outlook"),
        arrayOf(".ogg", "audio/ogg"),
        arrayOf(".pdf", "application/pdf"),
        arrayOf(".png", "image/png"),
        arrayOf(".webp", "image/webp"),
        arrayOf(".pps", "application/vnd.ms-powerpoint"),
        arrayOf(".ppt", "application/vnd.ms-powerpoint"),
        arrayOf(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        arrayOf(".prop", "text/plain"),
        arrayOf(".rc", "text/plain"),
        arrayOf(".rmvb", "audio/x-pn-realaudio"),
        arrayOf(".rtf", "application/rtf"),
        arrayOf(".sh", "text/plain"),
        arrayOf(".tar", "application/x-tar"),
        arrayOf(".tgz", "application/x-compressed"),
        arrayOf(".txt", "text/plain"),
        arrayOf(".wav", "audio/x-wav"),
        arrayOf(".wma", "audio/x-ms-wma"),
        arrayOf(".wmv", "audio/x-ms-wmv"),
        arrayOf(".wps", "application/vnd.ms-works"),
        arrayOf(".xml", "text/plain"),
        arrayOf(".z", "application/x-compress"),
        arrayOf(".zip", "application/x-zip-compressed")
    )

/**
 * 音频,视频,图片
 */
val MIME_MEDIA = arrayOf(
    "video/*",
    "audio/*",
    "image/*"
)

val MIME_DOCUMENT = arrayOf(
    "application/msword",  //.doc
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  //.docx
    "application/vnd.ms-excel",  //.xls
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",  //.xlsx
    "application/pdf",  //.pdf
    "application/vnd.ms-works" //.wps
)

val MIME_PPT = arrayOf(
    "application/vnd.ms-powerpoint",  //.pps
    "application/vnd.ms-powerpoint",  //.ppt
    "application/vnd.openxmlformats-officedocument.presentationml.presentation" //.pptx
)