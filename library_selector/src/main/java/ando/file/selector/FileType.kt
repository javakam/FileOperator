package ando.file.selector

import ando.file.core.FileLogger
import ando.file.core.FileUtils
import android.net.Uri
import java.io.File
import java.util.*

/**
 * # FileType 文件类型
 *
 * 注:
 *
 * - 每一种类型包括的后缀数量有限, 如果下面没有你需要的类型, 可以使用`supplement`动态添加或者直接通过IFileType自定义实现, 详细步骤见Demo
 * - ando.file.core.FileMimeType 中包括大部分的`MimeType`, 可以参照
 *
 * @author javakam
 */
enum class FileType : IFileType {

    INSTANCE,
    UNKNOWN,
    AUDIO(mutableListOf("mp3", "flac", "ogg", "tta", "wav", "wma", "wmv", "m3u", "m4a", "m4b", "m4p", "mid", "mp2", "mpga", "rmvb")),
    VIDEO(mutableListOf("mp4", "m3u8", "avi", "flv", "3gp", "asf", "m4u", "m4v", "mov", "mpe", "mpeg", "mpg", "mpg4")),
    IMAGE(mutableListOf("jpg", "gif", "png", "jpeg", "bmp", "webp")),
    TXT(mutableListOf("txt", "conf", "iml", "ini", "log", "prop", "rc")),
    HTML(mutableListOf("html", "htm", "htmls", "md")),
    PPT(mutableListOf("ppt", "pptx", "pps")),
    EXCEL(mutableListOf("xls", "xlsx")),
    WORD(mutableListOf("doc", "docx")),
    PDF("pdf"),
    CHM("chm"),
    XML("xml"),
    APK("apk"),
    JAR("jar"),
    ZIP("zip");

    private var mimeType: String? = null
    private var mimeArray: MutableList<String>? = null

    constructor(mimeType: String) {
        this.mimeType = mimeType
    }

    constructor(mimeArray: MutableList<String> = mutableListOf()) {
        this.mimeArray = mimeArray
    }

    companion object {
        fun FileType.supplement(vararg mimeArray: String): FileType {
            this.mimeArray?.addAll(mimeArray)
            return this
        }

        fun FileType.remove(vararg mimeType: String): FileType {
            this.mimeArray?.removeAll(mimeType)
            return this
        }

        fun FileType.replace(mimeType: String) {
            this.mimeType = mimeType
        }

        fun FileType.dump() {
            mimeArray?.forEach {
                FileLogger.e("$this : $it")
            }
        }
    }

    override fun getMimeType(): String? {
        return mimeType
    }

    override fun getMimeTypeArray(): MutableList<String>? {
        return mimeArray
    }

    /**
     * url:  https://app-xxx-oss/xxx.gif
     *  or
     * fileName:  xxx.gif
     */
    override fun fromName(fileName: String?): IFileType {
        return fromSuffix(
            FileUtils.getExtension(fileName ?: return UNKNOWN)
        )
    }

    override fun fromName(fileName: String?, split: Char): IFileType {
        return fromSuffix(
            FileUtils.getExtension(fileName ?: return UNKNOWN, split, false)
        )
    }

    override fun fromPath(filePath: String?): IFileType {
        if (filePath.isNullOrBlank()) UNKNOWN
        val file = File(filePath ?: return UNKNOWN)
        return if (!file.exists()) UNKNOWN else fromFile(file)
    }

    override fun fromFile(file: File): IFileType = fromSuffix(FileUtils.getExtension(file.name))

    override fun fromUri(uri: Uri?): IFileType = fromSuffix(FileUtils.getExtension(uri))

    /**
     * 依据文件扩展名的类型确定相应的MimeType
     */
    private fun fromSuffix(suffix: String): IFileType {
        val end = suffix.lowercase(Locale.getDefault())
        values().forEach { t: FileType ->
            if (t.mimeArray.isNullOrEmpty()) {
                if (t.mimeType?.equals(end, true) == true) return t
            } else {
                if (t.mimeArray?.contains(end) == true) return t
            }
        }
        return UNKNOWN
    }

}