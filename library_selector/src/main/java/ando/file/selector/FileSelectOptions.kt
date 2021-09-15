package ando.file.selector

import ando.file.selector.FileSelector.Companion.TIP_COUNT_MAX
import ando.file.selector.FileSelector.Companion.TIP_COUNT_MIN

/**
 * FileSelectOptions
 *
 * @author javakam
 * @date 2020/5/21  10:51
 */
class FileSelectOptions {

    ///////////////////// 多选不同类型文件时配置 /////////////////////
    // Configure when multiple files of different types are selected)
    /**
     * 选择文件最少数量, 优先使用 FileSelector.setMinCount 进行判定
     *
     * Select the minimum number of files, and use FileSelector.setMinCount first for judgment
     */
    var minCount: Int = 0

    /**
     * 选择文件最大数量, 优先使用 FileSelector.setMaxCount 进行判定
     *
     * Select the maximum number of files, first use FileSelector.setMaxCount for judgment
     */
    var maxCount: Int = 0
    var minCountTip: String? = TIP_COUNT_MIN
    var maxCountTip: String? = TIP_COUNT_MAX
    //////////////////////////////////////////////////////////////

    var fileType: IFileType? = null

    /**
     * 文件类型不匹配提示(File type mismatch prompt)
     */
    var fileTypeMismatchTip: String? = FileSelector.TIP_SINGLE_FILE_TYPE_MISMATCH

    /**
     * 单文件大小控制(Single file size control) Byte
     */
    var singleFileMaxSize: Long = -1

    /**
     * 总文件大小控制(Total file size control) Byte
     */
    var allFilesMaxSize: Long = -1
    var singleFileMaxSizeTip: String? = FileSelector.TIP_SINGLE_FILE_SIZE
    var allFilesMaxSizeTip: String? = FileSelector.TIP_SINGLE_FILE_SIZE

    var fileCondition: FileSelectCondition? = null


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSelectOptions

        if (minCount != other.minCount) return false
        if (maxCount != other.maxCount) return false
        if (singleFileMaxSize != other.singleFileMaxSize) return false
        if (allFilesMaxSize != other.allFilesMaxSize) return false
        if (fileType != other.fileType) return false
        if (fileCondition != other.fileCondition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minCount
        result = 31 * result + maxCount
        result = 31 * result + singleFileMaxSize.toInt()
        result = 31 * result + allFilesMaxSize.toInt()
        result = 31 * result + (fileType?.hashCode() ?: 0)
        result = 31 * result + (fileCondition?.hashCode() ?: 0)
        return result
    }

}