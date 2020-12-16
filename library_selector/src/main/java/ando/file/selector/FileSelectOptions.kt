package ando.file.selector

import ando.file.core.FileType

/**
 * Title: FileSelectOptions
 *
 * @author javakam
 * @date 2020/5/21  10:51
 */
class FileSelectOptions {

    var minCount: Int = 0                              //选择文件最少数量
    var maxCount: Int = Int.MAX_VALUE                  //选择文件最大数量
    var minCountTip: String? = ""
    var maxCountTip: String? = ""

    var fileType: FileType? = null
    var fileTypeMismatchTip: String? = FileSelector.DEFAULT_SINGLE_FILE_TYPE_MISMATCH_THRESHOLD //文件类型不匹配提示
    var singleFileMaxSize: Long = -1                   //单文件大小控制 Byte
    var singleFileMaxSizeTip: String? = FileSelector.DEFAULT_SINGLE_FILE_SIZE_THRESHOLD
    var allFilesMaxSize: Long = -1                     //总文件大小控制 Byte
    var allFilesMaxSizeTip: String? = FileSelector.DEFAULT_SINGLE_FILE_SIZE_THRESHOLD

    var fileCondition: FileSelectCondition? = null


}