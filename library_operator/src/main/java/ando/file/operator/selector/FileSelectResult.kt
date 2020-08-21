package ando.file.operator.selector

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import ando.file.core.FileType

/**
 * Title: FileSelectResult
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/5/14  10:32
 */
class FileSelectResult : Parcelable {
    var fileType: FileType? = null

    var uri: Uri? = null

    var filePath: String? = null

    var fileSize: Long = 0L


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

    constructor()

    constructor(source: Parcel) : this(
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {}

    override fun toString(): String {
        return " fileType=$fileType, \n uri=$uri, \n filePath=$filePath, \n fileSize=$fileSize \n "
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FileSelectResult> =
            object : Parcelable.Creator<FileSelectResult> {
                override fun createFromParcel(source: Parcel): FileSelectResult =
                    FileSelectResult(source)

                override fun newArray(size: Int): Array<FileSelectResult?> = arrayOfNulls(size)
            }
    }

}