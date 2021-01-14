package ando.file.downloader.file

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import ando.file.common.FileType

/**
 * FileBean
 *
 * Description:
 *
 * @author javakam
 * @date 2020/1/21  14:36
 */
class FileBean : Parcelable {

    var id: String? = null
    var fileType: FileType? = null
    var name: String? = null
    var uri: Uri? = null
    var path: String? = null
    var mimeType: String? = null
    var size: String? = null
    var sizeBytes: Long = 0
    var date: String? = null
    var filterTypes: Array<String>? = null
    var isSelected = false
    var isInvalid = false
    var extra: Bundle? = null

    constructor(isInvalid: Boolean) {
        this.isInvalid = isInvalid
    }

    constructor()

    constructor(source: Parcel) : this(
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {}

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FileBean> = object : Parcelable.Creator<FileBean> {
            override fun createFromParcel(source: Parcel): FileBean =
                FileBean(source)
            override fun newArray(size: Int): Array<FileBean?> = arrayOfNulls(size)
        }
    }
}