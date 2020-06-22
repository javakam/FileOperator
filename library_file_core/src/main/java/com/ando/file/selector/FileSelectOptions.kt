package com.ando.file.selector

import com.ando.file.common.FileType

/**
 * Title: FileSelectOptions
 *
 * @author javakam
 * @date 2020/5/21  10:51
 */
 class FileSelectOptions{

    var mMinCount: Int = 0                              //可选文件最小数量
    var mMaxCount: Int = Int.MAX_VALUE                  //可选文件最大数量
    var mMinCountTip: String? = ""
    var mMaxCountTip: String? = ""

    var fileType: FileType? = null
    var mSingleFileMaxSize: Long = -1      //单文件大小控制 KB
    var mSingleFileMaxSizeTip: String? = FileSelector.DEFAULT_SINGLE_FILE_SIZE_THRESHOLD
    var mAllFilesMaxSize: Long = -1       //总文件大小控制 KB
    var mAllFilesMaxSizeTip: String? = FileSelector.DEFAULT_SINGLE_FILE_SIZE_THRESHOLD

    var mFileCondition: FileSelectCondition? = null


    internal var mIsFileSizePassed = false
    internal var mIsMinCountPassed = false
    internal var mIsMaxCountPassed = false

}