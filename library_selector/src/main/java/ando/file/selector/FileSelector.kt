package ando.file.selector

import ando.file.FileOperator.getContext
import ando.file.FileOperator.isDebug
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.collection.ArrayMap
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_ALL
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW
import ando.file.core.FileOpener.createChooseIntent
import ando.file.core.FileType.INSTANCE
import ando.file.core.FileType.UNKNOWN
import android.content.ClipData
import kotlin.math.max
import kotlin.math.min

/**
 * FileSelector
 *
 * @author javakam
 * @date 2020/5/21  9:32
 */
class FileSelector private constructor(builder: Builder) {

    companion object {
        val TIP_SINGLE_FILE_TYPE_MISMATCH: String by lazy { getContext().getString(R.string.ando_str_single_file_type_mismatch) }
        val TIP_SINGLE_FILE_SIZE: String by lazy { getContext().getString(R.string.ando_str_single_file_size) }
        val TIP_ALL_FILE_SIZE: String by lazy { getContext().getString(R.string.ando_str_all_file_size) }
        val TIP_COUNT_MIN: String by lazy { getContext().getString(R.string.ando_str_count_min) }
        val TIP_COUNT_MAX: String by lazy { getContext().getString(R.string.ando_str_count_max) }

        fun with(context: Context): Builder {
            return Builder(context)
        }
    }

    private var mContext: Context? = null
    private var mRequestCode: Int = 0

    private var mMimeTypes: Array<String>?
    private var mIsMultiSelect: Boolean = false
    private var mMinCount: Int = 0                              //可选文件最小数量(Minimum number of optional files)
    private var mMaxCount: Int = Int.MAX_VALUE                  //可选文件最大数量(Maximum number of optional files)
    private var mMinCountTip: String = TIP_COUNT_MIN
    private var mMaxCountTip: String = TIP_COUNT_MAX
    private var mSingleFileMaxSize: Long = -1                   //单文件大小控制(Single file size) Byte
    private var mAllFilesMaxSize: Long = -1                     //总文件大小控制(Total file size control) Byte
    private var mOverLimitStrategy = OVER_LIMIT_EXCEPT_ALL

    private var mFileTypeMismatchTip: String = TIP_SINGLE_FILE_TYPE_MISMATCH
    private var mSingleFileMaxSizeTip: String = TIP_SINGLE_FILE_SIZE
    private var mAllFilesMaxSizeTip: String = TIP_SINGLE_FILE_SIZE

    private var mFileSelectCondition: FileSelectCondition? = null
    private var mFileSelectCallBack: FileSelectCallBack? = null

    /**
     * 不限定类型时会被视为不作任何类型限定 -> FileSelectOptions().apply { fileType = UNKNOWN }
     *
     * When the type is not limited, it will be regarded as not being type limited
     */
    private var mFileSelectOptions: MutableList<FileSelectOptions>? = null

    private val mFileCountMap = ArrayMap<FileType, Int>()
    private val mFileSizeMap = ArrayMap<FileType, Long>()

    private var isOptionsEmpty: Boolean = false
    private val optionUnknown: FileSelectOptions by lazy { FileSelectOptions().apply { fileType = UNKNOWN } }

    //onActivityResult
    var requestCode: Int? = -1
    var resultCode: Int? = 0

    init {
        mRequestCode = builder.mRequestCode
        mMimeTypes = builder.mMimeTypes
        mIsMultiSelect = builder.mIsMultiSelect
        mMinCount = builder.mMinCount
        mMaxCount = builder.mMaxCount
        mMinCountTip = builder.mMinCountTip
        mMaxCountTip = builder.mMaxCountTip
        mSingleFileMaxSize = builder.mSingleFileMaxSize
        mFileTypeMismatchTip = builder.mFileTypeMismatchTip
        mSingleFileMaxSizeTip = builder.mSingleFileMaxSizeTip
        mAllFilesMaxSize = builder.mAllFilesMaxSize
        mAllFilesMaxSizeTip = builder.mAllFilesMaxSizeTip
        mOverLimitStrategy = builder.mOverLimitStrategy
        mFileSelectCondition = builder.mFileSelectCondition
        mFileSelectCallBack = builder.mFileSelectCallBack
        mFileSelectOptions = builder.mFileSelectOptions
    }

    fun choose(context: Context, mimeType: String?): FileSelector {
        this.mContext = context
        startActivityForResult(context, createChooseIntent(mimeType, mMimeTypes, mIsMultiSelect), mRequestCode)
        return this
    }

    fun obtainResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        this.requestCode = requestCode
        this.resultCode = resultCode

        if (requestCode == -1 || requestCode != mRequestCode) return

        //没有设定 FileSelectOptions 的情况(When FileSelectOptions is not set)
        isOptionsEmpty = mFileSelectOptions.isNullOrEmpty()
        if (mFileSelectOptions == null) mFileSelectOptions = mutableListOf()
        if (isOptionsEmpty) mFileSelectOptions?.add(optionUnknown)

        //单选(Single choice) Intent.getData; 多选(Multiple choice) Intent.getClipData
        if (mIsMultiSelect) {
            if (intent?.clipData == null) {
                //单一类型和多种类型(Single type and multiple types)
                if ((mFileSelectOptions?.size ?: 0 >= 2) && (mOverLimitStrategy == OVER_LIMIT_EXCEPT_ALL))
                    mFileSelectCallBack?.onError(Throwable(mMinCountTip))
                else handleSingleSelectCase(intent)
            } else handleMultiSelectCase(intent)
        } else handleSingleSelectCase(intent)
    }

    private fun handleSingleSelectCase(intent: Intent?) {
        this.mIsMultiSelect = false
        val intentData: Uri? = intent?.data
        if (intentData == null) {
            if (mMinCount > 0) {
                mFileSelectCallBack?.onError(Throwable(mMinCountTip))
            } else {
                mFileSelectCallBack?.onSuccess(emptyList())
            }
            return
        }

        filterUri(intentData) { o: FileSelectOptions?, t: FileType, tf: Boolean, s: Long, sf: Boolean ->
            val realOption: FileSelectOptions = o ?: optionUnknown
            if (!(tf || isOptionsEmpty)) {
                mFileSelectCallBack?.onError(Throwable(
                    if (realOption.fileTypeMismatchTip?.isNotBlank() == true) realOption.fileTypeMismatchTip else mFileTypeMismatchTip
                ))
                return@filterUri
            }
            if (!sf) {
                if (realOption.fileType == t) {
                    mFileSelectCallBack?.onError(Throwable(if (realOption.singleFileMaxSizeTip != null) realOption.singleFileMaxSizeTip else realOption.allFilesMaxSizeTip))
                } else {
                    mFileSelectCallBack?.onError(Throwable(mSingleFileMaxSizeTip))
                }
            } else {
                mFileSelectCallBack?.onSuccess(createResult(intentData, t, s))
            }
        }
    }

    private fun handleMultiSelectCase(intent: Intent?): FileSelector {
        this.mIsMultiSelect = true
        val clipData: ClipData = intent?.clipData ?: return this

        //clipData.itemCount 最小值为2(The minimum value of clipData.itemCount is 2)
        val itemCount = clipData.itemCount
        val isStrictStrategy = (mOverLimitStrategy == OVER_LIMIT_EXCEPT_ALL)
        if (isStrictStrategy && itemCount < realMinCountLimit(null)) {
            mFileSelectCallBack?.onError(Throwable(mMinCountTip))
            return this
        }
        if (isStrictStrategy && itemCount > realMaxCountLimit(null)) {
            mFileSelectCallBack?.onError(Throwable(mMaxCountTip))
            return this
        }

        //不同文件类型的结果集合(Result collection of different file types)
        val relationMap = ArrayMap<FileSelectOptions, SelectResult>()
        //不同文件类型的结果集合的并集(Union of result sets of different file types)
        val resultList: MutableList<Uri> = mutableListOf()

        var totalSize = 0L
        var isNeedBreak = false
        var isFileTypeIllegal = false
        var isFileCountIllegal = false  //文件数量(File Count): true 数量超限(Quantity exceeded)
        var isFileSizeIllegal = false   //文件大小(File Size): true 大小超限(Oversize)

        (0 until itemCount).forEach { i ->
            if (isNeedBreak) return this

            val uri = clipData.getItemAt(i)?.uri ?: return@forEach
            filterUri(uri) { o: FileSelectOptions?, t: FileType, tf: Boolean, s: Long, sf: Boolean ->
                val isCurrentType = (t == o?.fileType)
                if (isDebug()) {
                    FileLogger.w("Multi-> filterUri: ${o?.fileType} t=$t tf=$tf isCurrentType=$isCurrentType sf=$sf")
                }

                val realOption: FileSelectOptions = o ?: optionUnknown

                if (relationMap[realOption] == null) relationMap[realOption] = SelectResult(checkPass = true)
                val selectResult: SelectResult = relationMap[realOption] ?: SelectResult(checkPass = true)

                //文件类型不匹配(FileType Mismatch) -> onError
                if (!(tf || isOptionsEmpty)) {
                    mFileSelectCallBack?.onError(Throwable(
                        if (realOption.fileTypeMismatchTip?.isNotBlank() == true) realOption.fileTypeMismatchTip else mFileTypeMismatchTip
                    ))

                    if (relationMap.isNotEmpty()) relationMap.clear()
                    if (resultList.isNotEmpty()) resultList.clear()
                    isNeedBreak = true
                    isFileTypeIllegal = true
                    return@filterUri
                }

                //单一类型(Single type)
                val isOnlyOneType: Boolean = (mFileSelectOptions?.size ?: 0 == 1)

                //FileSize
                if (!sf) {
                    isFileSizeIllegal = true

                    if (isStrictStrategy) {
                        mFileSelectCallBack?.onError(Throwable(realOption.singleFileMaxSizeTip ?: mSingleFileMaxSizeTip))
                        isNeedBreak = true
                        return@filterUri
                    } else {
                        if (!isOnlyOneType) relationMap[realOption]?.checkPass = false
                        else return@filterUri
                    }
                }

                //File Count
                val realType = realOption.fileType
                if (!mFileCountMap.contains(realType)) {
                    mFileCountMap[realType] = 0
                }
                mFileCountMap[realType] = mFileCountMap[realType]?.plus(1)
                mFileSelectOptions?.forEach { os: FileSelectOptions ->
                    val count: Int = mFileCountMap[os.fileType] ?: 0
                    //min
                    //最后再判断最少数量(Finally determine the minimum number)
                    if (itemCount == (i + 1)) {
                        if (count < realMinCountLimit(os)) {
                            isFileCountIllegal = true
                            isNeedBreak = true
                            if (isStrictStrategy) {
                                mFileSelectCallBack?.onError(Throwable(realMinCountTip(os)))
                                return@filterUri
                            } else {
                                //如果某个FileSelectOptions没通过限定条件, 则该FileSelectOptions不会返回
                                //If a FileSelectOptions does not pass the qualification, the FileSelectOptions will not return
                                if (!isOnlyOneType) relationMap[os]?.checkPass = false
                                else return@filterUri
                            }
                        }
                    }
                    //max
                    if (count > realMaxCountLimit(os)) {
                        isFileCountIllegal = true
                        isNeedBreak = true
                        if (isStrictStrategy) {
                            mFileSelectCallBack?.onError(Throwable(realMaxCountTip(os)))
                            return@filterUri
                        } else {
                            if (!isOnlyOneType) relationMap[os]?.checkPass = false
                            else return@filterUri
                        }
                    }
                }

                //控制自定义选项大小(Control Custom Option size)
                if (isCurrentType || isOptionsEmpty) {
                    if (isDebug()) {
                        FileLogger.i("Multi-> Count: ${realOption.fileType} currTypeCount=${mFileCountMap[realType] ?: 0} isFinally=${itemCount == (i + 1)} " +
                                "realMinCountLimit=${realMinCountLimit(realOption)} realMaxCountLimit=${realMaxCountLimit(realOption)}")
                    }

                    //File Size
                    val mAllMaxSize = realSizeLimitAll(realOption)
                    if (!mFileSizeMap.contains(realOption.fileType)) {
                        mFileSizeMap[realOption.fileType] = 0L
                    }
                    val currTypeTotalSize: Long = mFileSizeMap[realOption.fileType] ?: 0L + s
                    mFileSizeMap[realOption.fileType] = currTypeTotalSize
                    FileLogger.i("Multi-> currTypeTotalSize=$currTypeTotalSize  mAllMaxSize=$mAllMaxSize")

                    if (currTypeTotalSize > mAllMaxSize) {//byte (B)
                        isFileSizeIllegal = true
                        selectResult.checkPass = false
                        return@filterUri
                    }
                }

                //控制总大小(Control Total Size)
                totalSize += s
                if (isDebug()) {
                    FileLogger.i("Multi-> totalSize: $totalSize checkPass=${selectResult.checkPass} ")
                }

                //not mRealAllFilesMaxSize
                if (totalSize > mAllFilesMaxSize) {//byte (B)
                    isFileSizeIllegal = true
                    isNeedBreak = true

                    when (mOverLimitStrategy) {
                        OVER_LIMIT_EXCEPT_ALL -> {
                            mFileSelectCallBack?.onError(Throwable(mAllFilesMaxSizeTip))
                        }
                        OVER_LIMIT_EXCEPT_OVERFLOW -> {
                            if (resultList.isNotEmpty()) resultList.clear()
                            relationMap.values.forEach { sr -> resultList.addAll(sr.uriList) }
                            mFileSelectCallBack?.onSuccess(createResult(resultList))
                        }
                    }
                    return@filterUri
                }

                //添加到结果列表(add to result list)
                if (selectResult.checkPass) {
                    selectResult.uriList.add(uri)
                    resultList.add(uri)
                }
            }
        }

        //某些类型没有选(Some types are not selected)
        val isOptionsSizeMatch = (mFileSelectOptions?.size == relationMap.keys.size)
        FileLogger.w("Multi-> isFileTypeIllegal=$isFileTypeIllegal isFileSizeIllegal=$isFileSizeIllegal " +
                "isFileCountIllegal=$isFileCountIllegal isOptionsSizeMatch=$isOptionsSizeMatch")

        //filter data
        if (isFileSizeIllegal || isFileCountIllegal || !isOptionsSizeMatch) {
            when (mOverLimitStrategy) {
                OVER_LIMIT_EXCEPT_ALL -> {
                    if (!isOptionsSizeMatch && !isNeedBreak) {
                        mFileSelectCallBack?.onError(Throwable(realMinCountTip(null)))
                        return this
                    }

                    relationMap.filter { (_: FileSelectOptions, v: SelectResult) ->
                        if (v.uriList.isNotEmpty()) v.uriList.clear()
                        if (resultList.isNotEmpty()) resultList.clear()
                        !v.checkPass
                    }.keys
                        .toMutableList()
                        .map { o: FileSelectOptions? -> o?.fileType }
                        .let { l: List<FileType?> ->
                            relationMap.values.forEach { ls ->
                                ls.uriList.filter {
                                    if (isDebug()) {
                                        FileLogger.e("Multi filter data -> $it ${INSTANCE.typeByUri(it)} ${l.contains(INSTANCE.typeByUri(it))} ")
                                    }
                                    !l.contains(INSTANCE.typeByUri(it))
                                }.apply {
                                    resultList.addAll(this)
                                }
                            }
                        }
                }
                OVER_LIMIT_EXCEPT_OVERFLOW -> {
                    if (resultList.isNotEmpty()) resultList.clear()

                    relationMap.filter { it.key != null && it.value.checkPass }.keys.forEach { op: FileSelectOptions? ->
                        relationMap.forEach { m: Map.Entry<FileSelectOptions?, SelectResult?> ->
                            if (m.key?.fileType == op?.fileType) {
                                m.value.let { s: SelectResult? ->
                                    s?.apply { resultList.addAll(uriList) }
                                }
                            }
                        }
                    }

                    FileLogger.e("Multi filter data -> uriListAll=${resultList.size}")
                    mFileSelectCallBack?.onSuccess(createResult(resultList))
                    return this
                }
            }
        }

        if (!isFileCountIllegal && !isFileTypeIllegal && !relationMap.isNullOrEmpty()) mFileSelectCallBack?.onSuccess(createResult(resultList))
        return this
    }

    private fun filterUri(
        uri: Uri,
        block: (option: FileSelectOptions?, fileType: FileType, typeFit: Boolean, fileSize: Long, sizeFit: Boolean) -> Unit,
    ) {
        val fileType = INSTANCE.typeByUri(uri)
        val fileSize = FileSizeUtils.getFileSize(uri)

        val currentOption: List<FileSelectOptions>? = mFileSelectOptions?.filter { it.fileType == fileType }
        val isOptionsNullOrEmpty = isOptionsEmpty || currentOption.isNullOrEmpty()
        if (isDebug()) {
            FileLogger.i("filterUri: $uri fileType=$fileType currentOption=${currentOption?.size} isOptionsNullOrEmpty=$isOptionsNullOrEmpty")
        }

        if (currentOption.isNullOrEmpty()) {
            block.invoke(null, fileType, false, fileSize, limitFileSize(fileSize, realSizeLimit(null)))
            return
        }

        if (isOptionsNullOrEmpty) {
            //没有设置 FileSelectOptions 时,使用通用的配置
            //When FileSelectOptions is not set, the general configuration is used
            val isAccept = (mFileSelectCondition == null) || (mFileSelectCondition?.accept(fileType, uri) == true)
            if (!isAccept) return
            block.invoke(null, fileType, true, fileSize, limitFileSize(fileSize, realSizeLimit(null)))
        } else {
            currentOption.forEach { o: FileSelectOptions ->
                //获取 CallBack -> 优先使用 FileSelectOptions 中设置的 FileSelectCallBack
                //Get CallBack -> Prefer to use FileSelectCallBack set in FileSelectOptions

                //控制类型 -> 自定义规则 -> 优先使用 FileSelectOptions 中设置的 FileSelectCondition
                //Control type -> Custom rule -> Preferentially use FileSelectCondition set in FileSelectOptions
                val isAccept = mFileSelectCondition?.accept(fileType, uri) ?: true && o.fileCondition?.accept(fileType, uri) ?: true
                if (!isAccept) {
                    block.invoke(o, fileType, true, fileSize, limitFileSize(fileSize, realSizeLimit(o)))
                    return@forEach
                }
                val success = limitFileSize(fileSize, realSizeLimit(o))
                block.invoke(o, fileType, true, fileSize, success)
                if (!success) return@forEach
            }
        }
    }

    private fun realMinCountTip(option: FileSelectOptions?): String = option?.minCountTip ?: mMinCountTip

    private fun realMaxCountTip(option: FileSelectOptions?): String = option?.maxCountTip ?: mMaxCountTip

    private fun realMinCountLimit(option: FileSelectOptions?): Int =
        if (option == null) {
            if (mMinCount <= 0) 1 else mMinCount
        } else {
            if (option.minCount <= 0) 1 else option.minCount
        }

    private fun realMaxCountLimit(option: FileSelectOptions?): Int {
        return max(realMinCountLimit(option),
            if (option?.maxCount ?: Int.MAX_VALUE > 0)
                min(option?.maxCount ?: Int.MAX_VALUE, mRealMaxCount)
            else mRealMaxCount)
    }

    private val mRealMaxCount: Int by lazy {
        var shouldCount = 0
        mFileSelectOptions?.forEach { o: FileSelectOptions ->
            shouldCount += o.maxCount
        }
        if (shouldCount == mMaxCount && mMaxCount == 0 || mMaxCount < 0) Int.MAX_VALUE
        else max(shouldCount, mMaxCount)
    }

    private fun realSizeLimit(option: FileSelectOptions?): Long =
        if (option == null) {
            if (mSingleFileMaxSize < 0) mRealAllFilesMaxSize
            else mSingleFileMaxSize
        } else {
            if (option.singleFileMaxSize < 0) if (option.allFilesMaxSize < 0) realSizeLimit(null) else option.allFilesMaxSize
            else option.singleFileMaxSize
        }

    private fun realSizeLimitAll(option: FileSelectOptions?): Long =
        when {
            option == null -> Long.MAX_VALUE
            option.allFilesMaxSize < 0 -> mRealAllFilesMaxSize
            else -> option.allFilesMaxSize
        }

    private val mRealAllFilesMaxSize: Long by lazy {
        var shouldSize = 0L
        mFileSelectOptions?.forEach { o: FileSelectOptions ->
            shouldSize += o.allFilesMaxSize
        }
        if (shouldSize == mAllFilesMaxSize && mAllFilesMaxSize == 0L || mAllFilesMaxSize < 0L) Long.MAX_VALUE
        else max(shouldSize, mAllFilesMaxSize)
    }

    private fun limitFileSize(fileSize: Long, sizeThreshold: Long): Boolean {
        if (isDebug()) {
            FileLogger.i("limitFileSize  : $fileSize ${fileSize <= sizeThreshold}")
        }
        return fileSize <= sizeThreshold
    }

    private fun createResult(
        uri: Uri,
        fileType: FileType,
        fileSize: Long,
    ): MutableList<FileSelectResult> =
        mutableListOf<FileSelectResult>().apply {
            add(FileSelectResult().apply {
                this.uri = uri
                this.filePath = uri.path
                this.mimeType = FileMimeType.getMimeType(uri)
                this.fileType = fileType
                this.fileSize = fileSize
            })
        }

    private fun createResult(uriList: List<Uri>?): MutableList<FileSelectResult> =
        mutableListOf<FileSelectResult>().apply {
            uriList?.forEach { u ->
                add(FileSelectResult().apply {
                    this.uri = u
                    this.filePath = u.path
                    this.mimeType = FileMimeType.getMimeType(u)
                    this.fileType = INSTANCE.typeByUri(u)
                    this.fileSize = FileSizeUtils.getFileSize(u)
                })
            }
        }

    internal data class SelectResult(
        /**
         * FileSelectOptions 对应的结果列表(FileSelectOptions corresponding result list)
         */
        var uriList: MutableList<Uri> = mutableListOf(),
        /**
         * 自定义FileSelectOptions是否通过(Custom FileSelectOptions Pass)
         */
        var checkPass: Boolean = false,
    )

    class Builder internal constructor(private val context: Context) {
        var mRequestCode: Int = 0

        var mMimeTypes: Array<String>? = null
        var mIsMultiSelect: Boolean = false
        var mMinCount: Int = 0                              //可选文件最小数量(Minimum number of optional files)
        var mMaxCount: Int = 0                              //可选文件最大数量(Maximum number of optional files)
        var mMinCountTip: String = TIP_COUNT_MIN
        var mMaxCountTip: String = TIP_COUNT_MAX

        var mSingleFileMaxSize: Long = -1                   //单文件大小控制 B (Single file size control)
        var mAllFilesMaxSize: Long = -1                     //总文件大小控制 B (Total file size control)
        var mFileTypeMismatchTip: String = TIP_SINGLE_FILE_TYPE_MISMATCH
        var mSingleFileMaxSizeTip: String = TIP_SINGLE_FILE_SIZE
        var mAllFilesMaxSizeTip: String = TIP_ALL_FILE_SIZE
        var mOverLimitStrategy = OVER_LIMIT_EXCEPT_ALL

        var mFileSelectCondition: FileSelectCondition? = null
        var mFileSelectCallBack: FileSelectCallBack? = null
        var mFileSelectOptions: MutableList<FileSelectOptions>? = null

        private fun build(): FileSelector {
            return FileSelector(this)
        }

        fun setRequestCode(requestCode: Int): Builder {
            this.mRequestCode = requestCode
            return this
        }

        fun setMimeTypes(mimeTypes: Array<String>?): Builder {
            this.mMimeTypes = mimeTypes
            return this
        }

        fun setMimeTypes(mimeTypes: String): Builder {
            this.mMimeTypes = arrayOf(mimeTypes)
            return this
        }

        fun setMultiSelect(): Builder {
            this.mIsMultiSelect = true
            return this
        }

        fun setMinCount(minCount: Int, msg: String): Builder {
            this.mMinCount = minCount
            this.mMinCountTip = msg
            return this
        }

        fun setMaxCount(maxCount: Int, msg: String): Builder {
            this.mMaxCount = maxCount
            this.mMaxCountTip = msg
            return this
        }

        fun setTypeMismatchTip(typeMismatchTip: String): Builder {
            this.mFileTypeMismatchTip = typeMismatchTip
            return this
        }

        fun setSingleFileMaxSize(sizeThreshold: Long, sizeThresholdTip: String): Builder {
            this.mSingleFileMaxSize = sizeThreshold
            this.mSingleFileMaxSizeTip = sizeThresholdTip
            return this
        }

        fun setAllFilesMaxSize(sizeThreshold: Long, sizeThresholdTip: String): Builder {
            this.mAllFilesMaxSize = sizeThreshold
            this.mAllFilesMaxSizeTip = sizeThresholdTip
            return this
        }

        fun setOverLimitStrategy(@FileGlobal.FileOverLimitStrategy overLimitStrategy: Int): Builder {
            this.mOverLimitStrategy = overLimitStrategy
            return this
        }

        fun filter(conditions: FileSelectCondition): Builder {
            this.mFileSelectCondition = conditions
            return this
        }

        fun callback(callBack: FileSelectCallBack): Builder {
            this.mFileSelectCallBack = callBack
            return this
        }

        fun applyOptions(vararg options: FileSelectOptions): Builder {
            this.mFileSelectOptions = options.toMutableList()
            return this
        }

        fun create(): Builder {
            return this
        }

        fun choose(): FileSelector {
            return choose(null)
        }

        fun choose(mimeType: String?): FileSelector {
            return build().choose(context, mimeType)
        }

    }

}