package ando.file.selector

import android.net.Uri
import android.os.Parcelable
import ando.file.core.FileType
import kotlinx.android.parcel.Parcelize

/**
 * Title: FileSelectResult
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/5/14  10:32
 */
@Parcelize
data class FileSelectResult(
    var fileType: FileType?,
    var uri: Uri?,
    var filePath: String?,
    var fileSize: Long = 0L
) : Parcelable {

    constructor() : this(null, null, null, 0L)

    override fun toString(): String {
        return " fileType=$fileType, \n uri=$uri, \n filePath=$filePath, \n fileSize=$fileSize \n "
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSelectResult

        if (fileType != other.fileType) return false
        if (uri != other.uri) return false
        if (filePath != other.filePath) return false
        if (fileSize != other.fileSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileType?.hashCode() ?: 0
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + fileSize.hashCode()
        return result
    }

}