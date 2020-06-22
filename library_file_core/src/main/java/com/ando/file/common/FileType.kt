package com.ando.file.common

import android.net.Uri
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.File
import java.util.*

/**
 * 常用的文件类型 -> FileMimeType.kt
 */
enum class FileType {
    /**
     *
     */
    INSTANCE, AUDIO,
    VIDEO, IMAGE,
    APK, PPT,
    EXCEL, WORD, WPS,
    PDF, CHM,
    TXT, HTML,
    ZIP,
    UNKNOWN, VALID;

    /**
     * url : https://app-xxx-oss.oss-cn-beijing.aliyuncs.com/xxx/xxxx/2020-04-07/1586267702635.gif
     * or
     * fileName : 1586267702635.gif
     */
    fun typeByFileName(fileName: String?): FileType {
        return typeByFileSuffix(
            FileUtils.getExtension(
                fileName ?: return UNKNOWN
            )
        )
    }

    fun typeByFileName(fileName: String?, split: Char): FileType {
        return typeByFileSuffix(
            FileUtils.getExtension(
                fileName ?: return UNKNOWN, split, false
            )
        )
    }

    fun typeByUri(uri: Uri?): FileType = typeByFileSuffix(
        FileUtils.getExtension(
            uri
        )
    )

    fun typeByFilePath(filePath: String?): FileType {
        if (TextUtils.isEmpty(filePath)) UNKNOWN
        val file = File(filePath ?: return UNKNOWN)
        return if (!file.exists()) UNKNOWN else typeByFile(file)
    }

    fun typeByFile(file: File): FileType = typeByFileSuffix(
        FileUtils.getExtension(
            file
        )
    )

    /**
     * 依据扩展名的类型决定 MimeType
     */
    fun typeByFileSuffix(end: String): FileType =
        when (end.toLowerCase(Locale.getDefault())) {
            "jpg", "gif", "png", "jpeg", "bmp", "webp" -> IMAGE
            "3gp", "flv", "mp4", "m3u8", "avi", "asf", "m4u", "m4v", "mov", "mpe", "mpeg", "mpg", "mpg4" -> VIDEO
            "mp2", "mp3", "m3u", "m4a", "m4b", "m4p", "mpga", "flac", "rmvb", "mid", "xmf", "ogg", "wav", "wma", "wmv", "ape", "wavpack", "tak", "tta" -> AUDIO
            "apk" -> APK
            "ppt", "pptx", "pps" -> PPT
            "xls", "xlsx" -> EXCEL
            "doc", "docx" -> WORD
            "wps" -> WPS
            "pdf" -> PDF
            "chm" -> CHM
            "txt", "xml", "rc", "sh", "c", "conf", "cpp", "prop", "java", "kt" -> TXT
            "html", "htm", "md" -> HTML
            "zip" -> ZIP
            else -> UNKNOWN
        }

}