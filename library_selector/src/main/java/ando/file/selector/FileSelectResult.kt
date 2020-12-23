/**
 * Copyright (C)  javakam, FileOperator Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ando.file.selector

import android.net.Uri
import android.os.Parcelable
import ando.file.core.FileType
import kotlinx.parcelize.Parcelize

/**
 * ### 选择结果
 *
 * @author javakam
 * @date 2020/5/14  10:32
 */
@Parcelize
data class FileSelectResult(
    var fileType: FileType?,
    var mimeType: String?,
    var uri: Uri?,
    var filePath: String?,
    var fileSize: Long = 0L,
) : Parcelable {

    constructor() : this(null, null, null, null, 0L)

    override fun toString(): String {
        return "文件类型(FileType)= $fileType \n MimeType= $mimeType \n Uri= $uri \n 路径(Path)= $filePath \n 大小(Byte)= $fileSize \n "
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSelectResult

        if (fileType != other.fileType) return false
        if (mimeType != other.mimeType) return false
        if (uri != other.uri) return false
        if (filePath != other.filePath) return false
        if (fileSize != other.fileSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileType?.hashCode() ?: 0
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + fileSize.hashCode()
        return result
    }

}