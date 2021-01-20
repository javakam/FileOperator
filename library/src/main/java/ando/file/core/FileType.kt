package ando.file.core

import android.net.Uri
import java.io.File
import java.util.*

/**
 * 常用的文件类型
 *
 * {@link ando.file.core.FileMimeType}
 *
 * @author javakam
 */
enum class FileType : FileGlobal.IFileType {

    INSTANCE, UNKNOWN,
    AUDIO, VIDEO, IMAGE,
    PPT, EXCEL, WORD, WPS, PDF, CHM,
    TXT, HTML,
    APK, ZIP;

    /**
     * url : https://app-xxx-oss/xxx/1586267702635.gif
     * or
     * fileName : 1586267702635.gif
     */
    fun typeByFileName(fileName: String?): FileType {
        return typeByFileSuffix(
            FileUtils.getExtension(fileName ?: return UNKNOWN)
        )
    }

    fun typeByFileName(fileName: String?, split: Char): FileType {
        return typeByFileSuffix(
            FileUtils.getExtension(fileName ?: return UNKNOWN, split, false)
        )
    }

    fun typeByUri(uri: Uri?): FileType = typeByFileSuffix(FileUtils.getExtension(uri))

    fun typeByFilePath(filePath: String?): FileType {
        if (filePath.isNullOrBlank()) UNKNOWN
        val file = File(filePath ?: return UNKNOWN)
        return if (!file.exists()) UNKNOWN else typeByFile(file)
    }

    fun typeByFile(file: File): FileType = typeByFileSuffix(FileUtils.getExtension(file.name))

    /**
     * 依据扩展名的类型决定 MimeType
     */
    fun typeByFileSuffix(end: String): FileType =
        when (end.toLowerCase(Locale.getDefault())) {
            "jpg", "gif", "png", "jpeg", "bmp", "webp" -> IMAGE
            "3gp", "flv", "mp4", "m3u8", "avi", "asf", "m4u", "m4v", "mov", "mpe", "mpeg", "mpg", "mpg4" -> VIDEO
            "mp2", "mp3", "m3u", "m4a", "m4b", "m4p", "mpga", "flac", "rmvb", "mid", "ogg", "wav", "wma", "wmv", "tta" -> AUDIO
            "apk" -> APK
            "ppt", "pptx", "pps" -> PPT
            "xls", "xlsx" -> EXCEL
            "doc", "docx" -> WORD
            "wps" -> WPS
            "pdf" -> PDF
            "chm" -> CHM
            "txt", "xml", "rc", "sh", "c", "conf", "cpp", "prop", "java", "kt" -> TXT
            "html", "htm", "htmls", "md" -> HTML
            "zip" -> ZIP
            else -> UNKNOWN
        }
}