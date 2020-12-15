package ando.file.core

import android.net.Uri
import android.webkit.MimeTypeMap
import ando.file.FileOperator.getContext
import ando.file.core.FileUri.getFilePathByUri
import java.util.*

/**
 * 文件 MimeType 工具类
 *
 * 1. getMimeType(str: String?) 先用`android.webkit.MimeTypeMap`获取`mimeType`, 如果为空再去`MIME_TABLES`中找
 *
 * 2. getMimeType(uri: Uri?) 先用`android.content.Context.getContentResolver`获取`mimeType`,
 *   如果结果为空则借助`getFilePathByUri(uri)`将`uri`转换为`path`, 执行`getMimeType(str: String?)`
 */
object FileMimeType {

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

        FileLogger.i("FileMimeType ：extension=$extension  mimeType=$mimeType")
        return mimeType.toLowerCase(Locale.getDefault())
    }

    fun getMimeType(uri: Uri?): String =
        if (uri != null) getContext().contentResolver.getType(uri)?.toLowerCase(Locale.getDefault()) ?: getMimeType(getFilePathByUri(uri))
        else getMimeType(getFilePathByUri(uri))

    /**
     * MimeTypeMap.getSingleton().getMimeTypeFromExtension(...) 的补充
     */
    private fun getMimeTypeSupplement(fileName: String): String {
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
     * 常见的文件 MimeType
     *
     * 1. https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types
     * https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
     *
     * 2. https://www.sitepoint.com/mime-types-complete-list/
     *
     * 3. https://github.com/broofa/mime/blob/master/types
     *
     */
    private val MIME_TABLES =
        arrayOf(
            arrayOf(".3g2", "video/3gpp2"),//or audio/3gpp2（若不含视频）
            arrayOf(".3gp", "video/3gpp"),//or audio/3gpp（若不含视频）
            arrayOf(".7z", "application/x-7z-compressed"),
            arrayOf(".aac", "audio/aac"),
            arrayOf(".abw", "application/x-abiword"),
            arrayOf(".apk", "application/vnd.android.package-archive"),
            arrayOf(".arc", "application/x-freearc"),
            arrayOf(".asf", "video/x-ms-asf"),
            arrayOf(".avi", "video/x-msvideo"),
            arrayOf(".azw", "application/vnd.amazon.ebook"),
            arrayOf(".bin", "application/octet-stream"),
            arrayOf(".bmp", "image/bmp"),
            arrayOf(".bz", "application/x-bzip"),
            arrayOf(".bz2", "application/x-bzip2"),
            arrayOf(".c", "text/plain"),
            arrayOf(".class", "application/octet-stream"),
            arrayOf(".conf", "text/plain"),
            arrayOf(".cpp", "text/x-c"),
            arrayOf(".csh", "application/x-csh"),// text/x-script.csh
            arrayOf(".css", "text/css"),
            arrayOf(".csv", "text/csv"),
            arrayOf(".doc", "application/msword"),
            arrayOf(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            arrayOf(".eot", "application/vnd.ms-fontobject"),
            arrayOf(".epub", "application/epub+zip"),
            arrayOf(".exe", "application/octet-stream"),
            arrayOf(".flac", "audio/*"),//application/x-flac
            arrayOf(".flv", "video/x-flv"),
            arrayOf(".gif", "image/gif"),
            arrayOf(".gradle", "text/plain"),
            arrayOf(".gtar", "application/x-gtar"),
            arrayOf(".gz", "application/x-gzip"),
            arrayOf(".gzip", "application/x-gzip"),
            arrayOf(".h", "text/plain"),
            arrayOf(".hdf", "application/x-hdf"),
            arrayOf(".help", "application/x-helpfile"),
            arrayOf(".htm", "text/html"),
            arrayOf(".html", "text/html"),
            arrayOf(".htmls", "text/html"),
            arrayOf(".ico", "image/vnd.microsoft.icon"),
            arrayOf(".ics", "text/calendar"),
            arrayOf(".iml", "text/plain"),
            arrayOf(".jar", "application/java-archive"),
            arrayOf(".java", "text/x-java-source"), //text/plain
            arrayOf(".jpeg", "image/jpeg"),
            arrayOf(".jpg", "image/jpeg"),
            arrayOf(".js", "text/javascript"),
            arrayOf(".json", "application/json"),
            arrayOf(".jsonld", "application/ld+json"),
            arrayOf(".log", "text/plain"),
            arrayOf(".m3u", "audio/x-mpegurl"),
            arrayOf(".m3u8", "application/vnd.apple.mpegurl"),
            arrayOf(".m4a", "audio/mp4a-latm"),
            arrayOf(".m4b", "audio/mp4a-latm"),
            arrayOf(".m4p", "audio/mp4a-latm"),
            arrayOf(".m4u", "video/vnd.mpegurl"),
            arrayOf(".m4v", "video/x-m4v"),
            arrayOf(".mid", "audio/midi audio/x-midi"),
            arrayOf(".midi", "audio/midi audio/x-midi"),
            arrayOf(".mjs", "text/javascript"),
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
            arrayOf(".mpkg", "application/vnd.apple.installer+xml"),
            arrayOf(".msg", "application/vnd.ms-outlook"),
            arrayOf(".odp", "application/vnd.oasis.opendocument.presentation"),
            arrayOf(".ods", "application/vnd.oasis.opendocument.spreadsheet"),
            arrayOf(".odt", "application/vnd.oasis.opendocument.text"),
            arrayOf(".oga", "audio/ogg"),
            arrayOf(".ogg", "audio/ogg"),
            arrayOf(".ogv", "video/ogg"),
            arrayOf(".ogx", "application/ogg"),
            arrayOf(".otf", "font/otf"),
            arrayOf(".pdf", "application/pdf"),
            arrayOf(".png", "image/png"),
            arrayOf(".pps", "application/vnd.ms-powerpoint"),
            arrayOf(".ppt", "application/vnd.ms-powerpoint"),
            arrayOf(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            arrayOf(".prop", "text/plain"),
            arrayOf(".rar", "application/x-rar-compressed"),
            arrayOf(".rc", "text/plain"),
            arrayOf(".rtf", "application/rtf"),
            arrayOf(".rmvb", "audio/x-pn-realaudio"),
            arrayOf(".rtf", "application/rtf"),
            arrayOf(".sh", "application/x-sh"),
            arrayOf(".svg", "image/svg+xml"),
            arrayOf(".swf", "application/x-shockwave-flash"),
            arrayOf(".tar", "application/x-tar"),
            arrayOf(".tgz", "application/x-compressed"),
            arrayOf(".tif", "image/tiff"),
            arrayOf(".tiff", "image/tiff"),
            arrayOf(".ttf", "font/ttf"),
            arrayOf(".txt", "text/plain"),
            arrayOf(".vsd", "application/vnd.visio"),
            arrayOf(".wav", "audio/wav"),
            arrayOf(".weba", "audio/webm"),
            arrayOf(".webm", "video/webm"),
            arrayOf(".webp", "image/webp"),
            arrayOf(".woff", "font/woff"),
            arrayOf(".woff2", "font/woff2"),
            arrayOf(".wma", "audio/x-ms-wma"),
            arrayOf(".wmv", "audio/x-ms-wmv"),
            arrayOf(".wps", "application/vnd.ms-works"),
            arrayOf(".xhtml", "application/xhtml+xml"),
            arrayOf(".xlc", "application/excel"),
            arrayOf(".xls", "application/vnd.ms-excel"),
            arrayOf(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            arrayOf(".xml", "text/xml"),
            arrayOf(".xul", "application/vnd.mozilla.xul+xml"),
            arrayOf(".z", "application/x-compress"),
            arrayOf(".zip", "application/zip")
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

}