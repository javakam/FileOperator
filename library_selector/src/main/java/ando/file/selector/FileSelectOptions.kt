package ando.file.selector

import ando.file.core.FileType

/**
 * Title: FileSelectOptions
 *
 * @author javakam
 * @date 2020/5/21  10:51
 */
class FileSelectOptions {

    ///////////////////// 建议在多选不同类型文件时再配置 /////////////////////
    var minCount: Int = 0              //选择文件最少数量, 优先使用 FileSelector.setMinCount 进行判定 , 最小为 0
    var maxCount: Int = Int.MAX_VALUE  //选择文件最大数量, 优先使用 FileSelector.setMaxCount 进行判定 , 最小为 1
    var minCountTip: String? = ""
    var maxCountTip: String? = ""
    ////////////////////////////////////////////////////////////////////

    var fileType: FileType? = null
    var fileTypeMismatchTip: String? = FileSelector.DEFAULT_SINGLE_FILE_TYPE_MISMATCH_THRESHOLD //文件类型不匹配提示
    var singleFileMaxSize: Long = -1                   //单文件大小控制 Byte
    var allFilesMaxSize: Long = -1                     //总文件大小控制 Byte
    var singleFileMaxSizeTip: String? = FileSelector.DEFAULT_SINGLE_FILE_SIZE_THRESHOLD
    var allFilesMaxSizeTip: String? = FileSelector.DEFAULT_SINGLE_FILE_SIZE_THRESHOLD

    var fileCondition: FileSelectCondition? = null


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSelectOptions

        if (minCount != other.minCount) return false
        if (maxCount != other.maxCount) return false
        if (fileType != other.fileType) return false
        if (fileCondition != other.fileCondition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minCount
        result = 31 * result + maxCount
        result = 31 * result + (fileType?.hashCode() ?: 0)
        result = 31 * result + (fileCondition?.hashCode() ?: 0)
        return result
    }

}