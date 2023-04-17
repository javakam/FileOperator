package ando.file.selector

import ando.file.core.*
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_ALL
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW
import ando.file.core.FileOpener.createChooseIntent
import ando.file.core.FileOperator.getContext
import ando.file.selector.FileType.INSTANCE
import ando.file.selector.FileType.UNKNOWN
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import kotlin.math.max
import kotlin.math.min


/**
 * # FileSelector
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

        fun with(context: Context, launcher: ActivityResultLauncher<Intent>? = null): Builder {
            return Builder(context, launcher)
        }

        fun with(fragment: Fragment, launcher: ActivityResultLauncher<Intent>? = null): Builder {
            return Builder(fragment, launcher)
        }
    }

    private var mStartForResult: ActivityResultLauncher<Intent>? = null
    private var mRequestCode: Int = 0

    private var mExtraMimeTypes: Array<out String>?
    private var mIsMultiSelect: Boolean = false
    private var mMinCount: Int = 0                              //可选文件最小数量(Minimum number of optional files)
    private var mMaxCount: Int = Int.MAX_VALUE                  //可选文件最大数量(Maximum number of optional files)
    private var mMinCountTip: String = TIP_COUNT_MIN
    private var mMaxCountTip: String = TIP_COUNT_MAX
    private var mSingleFileMaxSize: Long = -1                   //单文件大小控制(Single file size) byte (B)
    private var mAllFilesMaxSize: Long = -1                     //总文件大小控制(Total file size control) byte (B)
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
    private val mFileTypeComposite: MutableList<IFileType> by lazy { mutableListOf() }
    private var isOptionsEmpty: Boolean = false

    //onActivityResult
    var requestCode: Int? = -1
    var resultCode: Int? = 0

    init {
        mStartForResult = builder.mStartForResult
        mRequestCode = builder.mRequestCode
        mExtraMimeTypes = builder.mExtraMimeTypes
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

    fun choose(context: Any, mimeType: String?): FileSelector {
        checkParams()
        if (mStartForResult == null) {
            startActivityForResult(context, createChooseIntent(mimeType, mExtraMimeTypes, mIsMultiSelect), mRequestCode)
            return this
        }
        when (context) {
            is ComponentActivity -> {// androidx.activity.ComponentActivity
                mStartForResult?.launch(createChooseIntent(mimeType, mExtraMimeTypes, mIsMultiSelect))
            }
            is Fragment -> {
                mStartForResult?.launch(createChooseIntent(mimeType, mExtraMimeTypes, mIsMultiSelect))
            }
            else -> {
                startActivityForResult(context, createChooseIntent(mimeType, mExtraMimeTypes, mIsMultiSelect), mRequestCode)
            }
        }
        return this
    }

    private fun checkParams() {
        //FileSelectOptions Check
        mFileSelectOptions?.firstOrNull { it.fileType == null || it.fileType == INSTANCE }?.apply {
            throw RuntimeException("$this fileType must not be null or FileType.INSTANCE")
        }
    }

    fun obtainResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        this.requestCode = requestCode
        this.resultCode = resultCode
        this.mFileTypeComposite.clear()

        if (requestCode == -1 || requestCode != mRequestCode) return

        //没有设定 FileSelectOptions 的情况(When FileSelectOptions is not set)
        isOptionsEmpty = mFileSelectOptions.isNullOrEmpty()
        if (mFileSelectOptions == null) mFileSelectOptions = mutableListOf()
        if (isOptionsEmpty) {
            mFileSelectOptions?.add(FileSelectOptions().apply { fileType = UNKNOWN })
            mFileTypeComposite.add(UNKNOWN)
        } else {
            if (mFileTypeComposite.isNotEmpty()) mFileTypeComposite.clear()
            mFileSelectOptions?.forEach {
                it.fileType?.apply { mFileTypeComposite.add(this) }
            }
        }

        //Single choice(Intent.getData); Multiple choice(Intent.getClipData)
        if (mIsMultiSelect) {
            if (intent?.clipData == null) {
                //多选模式下只选择一个文件(Select only one file in multiple selection mode)
                var sbMinCountErrorMsg: StringBuilder? = null
                if ((mFileSelectOptions?.size ?: 0) >= 2) {
                    val strMinCountErrorMsg = "[%s]类型文件至少选择(%s)个，"
                    sbMinCountErrorMsg = StringBuilder()
                    val currentFileType: IFileType? = intent?.data?.let { findFileType(it) }
                    mFileSelectOptions?.apply {
                        for (opt in this) {
                            if (currentFileType != opt.fileType) {
                                if (opt.minCount <= 0) continue
                                sbMinCountErrorMsg?.append(String.format(strMinCountErrorMsg, "${opt.fileType}", "${opt.minCount}"))
                            } else {
                                if (opt.minCount > 1) {
                                    sbMinCountErrorMsg?.append(String.format(strMinCountErrorMsg, "${opt.fileType}", "${opt.minCount}"))
                                }
                            }
                        }
                    }
                    if (sbMinCountErrorMsg.isNotBlank()) {
                        sbMinCountErrorMsg = sbMinCountErrorMsg.deleteCharAt(sbMinCountErrorMsg.length - 1)
                    }
                }

                FileLogger.w("多选模式下只选择一个文件: $mMinCount $sbMinCountErrorMsg")
                if (!sbMinCountErrorMsg.isNullOrBlank() && mOverLimitStrategy == OVER_LIMIT_EXCEPT_ALL) {
                    mFileSelectCallBack?.onError(Throwable(sbMinCountErrorMsg.toString()))
                } else {
                    handleSingleSelectCase(intent)
                }
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

        filterUri(intentData) { o: FileSelectOptions?, t: IFileType, tf: Boolean, s: Long, sf: Boolean ->
            val realOption: FileSelectOptions = o ?: FileSelectOptions().apply {
                fileType = UNKNOWN
                fileTypeMismatchTip = mFileTypeMismatchTip
                minCount = mMinCount
                maxCount = mMaxCount
                minCountTip = mMinCountTip
                maxCountTip = mMaxCountTip
                singleFileMaxSize = mSingleFileMaxSize
                singleFileMaxSizeTip = mSingleFileMaxSizeTip
                allFilesMaxSize = mAllFilesMaxSize
                allFilesMaxSizeTip = mAllFilesMaxSizeTip
                fileCondition = mFileSelectCondition
            }
            if (!(tf || isOptionsEmpty)) {
                mFileSelectCallBack?.onError(
                    Throwable(if (realOption.fileTypeMismatchTip.isNullOrBlank()) mFileTypeMismatchTip else realOption.fileTypeMismatchTip)
                )
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

        //clipData.itemCount minimum is 2
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

        val fileCountMap = ArrayMap<IFileType, Int>()
        val fileSizeMap = ArrayMap<IFileType, Long>()
        //不同文件类型的结果集合(Result collection of different file types)
        val relationMap = ArrayMap<FileSelectOptions, SelectResult>()
        //不同文件类型的结果集合的并集(Union of result sets of different file types)
        val resultList: MutableList<Uri> = mutableListOf()

        var totalSize = 0L
        var isNeedBreak = false
        var isFileTypeIllegal = false  //File Type:  true 文件类型错误(File type error)
        var isFileCountIllegal = false //File Count: true 数量超限(Quantity exceeded)
        var isFileSizeIllegal = false  //File Size:  true 大小超限(Oversize)

        (0 until itemCount).forEach { i ->
            if (isNeedBreak) return this

            val uri = clipData.getItemAt(i)?.uri ?: return@forEach
            filterUri(uri) { o: FileSelectOptions?, t: IFileType, tf: Boolean, s: Long, sf: Boolean ->
                val isCurrentType = (t == o?.fileType)
                FileLogger.w("Multi-> filterUri=${o?.fileType} fileType=$t typeFit=$tf isCurrentType=$isCurrentType size=$s sizeFit=$sf")

                val realOption: FileSelectOptions = o ?: FileSelectOptions().apply {
                    fileType = UNKNOWN
                    fileTypeMismatchTip = mFileTypeMismatchTip
                    minCount = mMinCount
                    maxCount = mMaxCount
                    minCountTip = mMinCountTip
                    maxCountTip = mMaxCountTip
                    singleFileMaxSize = mSingleFileMaxSize
                    singleFileMaxSizeTip = mSingleFileMaxSizeTip
                    allFilesMaxSize = mAllFilesMaxSize
                    allFilesMaxSizeTip = mAllFilesMaxSizeTip
                    fileCondition = mFileSelectCondition
                }

                if (relationMap[realOption] == null) relationMap[realOption] = SelectResult(checkPass = true)
                val selectResult: SelectResult = relationMap[realOption] ?: SelectResult(checkPass = true)

                //FileType Mismatch -> onError
                if (!(tf || isOptionsEmpty)) {
                    mFileSelectCallBack?.onError(
                        Throwable(if (realOption.fileTypeMismatchTip.isNullOrBlank()) mFileTypeMismatchTip else realOption.fileTypeMismatchTip)
                    )

                    if (relationMap.isNotEmpty()) relationMap.clear()
                    if (resultList.isNotEmpty()) resultList.clear()
                    isNeedBreak = true
                    isFileTypeIllegal = true
                    return@filterUri
                }

                //Single type
                val isOnlyOneType: Boolean = ((mFileSelectOptions?.size ?: 0) == 1)

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
                if (!fileCountMap.contains(realType)) {
                    fileCountMap[realType] = 0
                }
                fileCountMap[realType] = fileCountMap[realType]?.plus(1)
                mFileSelectOptions?.forEach { os: FileSelectOptions ->
                    val count: Int = fileCountMap[os.fileType] ?: 0
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
                    FileLogger.i(
                        "Multi-> Count: ${realOption.fileType} currTypeCount=${fileCountMap[realType] ?: 0} isFinally=${itemCount == (i + 1)} " + "realMinCountLimit=${
                            realMinCountLimit(realOption)
                        } realMaxCountLimit=${realMaxCountLimit(realOption)}"
                    )

                    //File Size
                    val mAllMaxSize = realSizeLimitAll(realOption)
                    if (!fileSizeMap.contains(realOption.fileType)) {
                        fileSizeMap[realOption.fileType] = 0L
                    }
                    val currTypeTotalSize: Long = (fileSizeMap[realOption.fileType] ?: 0L).plus(s)
                    fileSizeMap[realOption.fileType] = currTypeTotalSize
                    FileLogger.e("Multi-> currTypeTotalSize=$currTypeTotalSize mAllMaxSize=$mAllMaxSize")

                    if (currTypeTotalSize > mAllMaxSize) {//byte (B)
                        isFileSizeIllegal = true
                        selectResult.checkPass = false
                        return@filterUri
                    }
                }

                //控制总大小(Control Total Size)
                totalSize += s
                FileLogger.i("Multi-> totalSize=$totalSize checkPass=${selectResult.checkPass}")

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
                            relationMap.values.forEach { sr: SelectResult -> resultList.addAll(sr.uriList) }
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
        }//END FOR

        //某些类型没有选(Some types are not selected)
        val isOptionsSizeMatch = (mFileSelectOptions?.size == relationMap.keys.size)
        FileLogger.w(
            "Multi-> isFileTypeIllegal=$isFileTypeIllegal isFileSizeIllegal=$isFileSizeIllegal " + "isFileCountIllegal=$isFileCountIllegal isOptionsSizeMatch=$isOptionsSizeMatch"
        )

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
                    }.keys.toMutableList().map { o: FileSelectOptions? -> o?.fileType }.let { l: List<IFileType?> ->
                        relationMap.values.forEach { ls ->
                            ls.uriList.filter {
                                val fileType = findFileType(it)
                                FileLogger.e("Multi filter data -> $it $fileType ${l.contains(fileType)} ")
                                !l.contains(fileType)
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

        if (!isFileCountIllegal && !isFileTypeIllegal && !relationMap.isEmpty) mFileSelectCallBack?.onSuccess(createResult(resultList))
        return this
    }

    private fun findFileType(uri: Uri): IFileType {
        var fileType: IFileType = UNKNOWN
        mFileTypeComposite.apply {
            if (isEmpty()) return@apply
            forEach {
                if (it.getMimeTypeArray().isNullOrEmpty()) {
                    if (it.getMimeType()?.equals(fileType.getMimeType(), true) == true) fileType = it
                } else {
                    if (it.getMimeTypeArray()?.contains(UNKNOWN.parseSuffix(uri)) == true) fileType = it
                }

                if (fileType == UNKNOWN) fileType = it.fromUri(uri)
                if (fileType != UNKNOWN) return@apply
            }
        }
        FileLogger.d("findFileType=$fileType ; mFileTypeComposite=${mFileTypeComposite.size}")
        return fileType
    }

    private fun filterUri(
        uri: Uri,
        block: (option: FileSelectOptions?, fileType: IFileType, typeFit: Boolean, fileSize: Long, sizeFit: Boolean) -> Unit,
    ) {
        val fileType = findFileType(uri)
        val fileSize = FileSizeUtils.getFileSize(uri)

        val currentOption: List<FileSelectOptions>? = mFileSelectOptions?.filter { it.fileType == fileType }
        val isOptionsNullOrEmpty = isOptionsEmpty || currentOption.isNullOrEmpty()
        FileLogger.i("filterUri: $uri fileType=$fileType option=${currentOption?.size} isOptionsNullOrEmpty=$isOptionsNullOrEmpty")

        if (currentOption.isNullOrEmpty()) {
            if (mFileSelectCondition != null) mFileSelectCondition?.accept(fileType, uri)
            block.invoke(null, fileType, false, fileSize, limitFileSize(fileSize, realSizeLimit(null)))
            return
        }

        if (isOptionsNullOrEmpty) {
            val isAccept = (mFileSelectCondition != null) && (mFileSelectCondition?.accept(fileType, uri) == true)
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

    private fun realMinCountLimit(option: FileSelectOptions?): Int = if (option == null) {
        if (mMinCount <= 0) 1 else mMinCount
    } else {
        if (option.minCount <= 0) 1 else option.minCount
    }

    private fun realMaxCountLimit(option: FileSelectOptions?): Int {
        return max(
            realMinCountLimit(option), if ((option?.maxCount ?: Int.MAX_VALUE) > 0) min(option?.maxCount ?: Int.MAX_VALUE, mRealMaxCount)
            else mRealMaxCount
        )
    }

    private val mRealMaxCount: Int by lazy {
        var shouldCount = 0
        mFileSelectOptions?.forEach { o: FileSelectOptions ->
            shouldCount += o.maxCount
        }
        if (shouldCount == mMaxCount && mMaxCount == 0 || mMaxCount < 0) Int.MAX_VALUE
        else max(shouldCount, mMaxCount)
    }

    private fun realSizeLimit(option: FileSelectOptions?): Long = if (option == null) {
        if (mSingleFileMaxSize < 0) mRealAllFilesMaxSize
        else mSingleFileMaxSize
    } else {
        if (option.singleFileMaxSize < 0) if (option.allFilesMaxSize < 0) realSizeLimit(null) else option.allFilesMaxSize
        else option.singleFileMaxSize
    }

    private fun realSizeLimitAll(option: FileSelectOptions?): Long = when {
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
        FileLogger.e("limitFileSize: $fileSize ; (fileSize <= sizeThreshold): ${fileSize <= sizeThreshold}")
        return fileSize <= sizeThreshold
    }

    private fun createResult(uri: Uri, fileType: IFileType, fileSize: Long): MutableList<FileSelectResult> = mutableListOf<FileSelectResult>().apply {
        add(FileSelectResult().apply {
            this.uri = uri
            this.filePath = FileUri.getPathByUri(uri)
            this.mimeType = FileMimeType.getMimeType(uri)
            this.fileType = fileType
            this.fileSize = fileSize
        })
    }

    private fun createResult(uriList: List<Uri>?): MutableList<FileSelectResult> = mutableListOf<FileSelectResult>().apply {
        uriList?.forEach { u ->
            add(FileSelectResult().apply {
                this.uri = u
                this.filePath = FileUri.getPathByUri(uri)
                this.mimeType = FileMimeType.getMimeType(u)
                this.fileType = findFileType(u)
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

    class Builder internal constructor(private val context: Any, launcher: ActivityResultLauncher<Intent>?) {
        var mStartForResult: ActivityResultLauncher<Intent>? = launcher
        var mRequestCode: Int = 0

        var mExtraMimeTypes: Array<out String>? = null      //eg: Intent.putExtra(Intent.EXTRA_MIME_TYPES, mExtraMimeTypes)
        var mIsMultiSelect: Boolean = false
        var mMinCount: Int = 0                              //可选文件最小数量(Minimum number of optional files)
        var mMaxCount: Int = 0                              //可选文件最大数量(Maximum number of optional files)
        var mMinCountTip: String = TIP_COUNT_MIN
        var mMaxCountTip: String = TIP_COUNT_MAX

        var mSingleFileMaxSize: Long = -1                   //单文件大小控制 byte (B) (Single file size control)
        var mAllFilesMaxSize: Long = -1                     //总文件大小控制 byte (B) (Total file size control)
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

        fun setExtraMimeTypes(vararg extraMimeTypes: String): Builder {
            this.mExtraMimeTypes = if (extraMimeTypes.size == 1) arrayOf(extraMimeTypes[0]) else extraMimeTypes
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

        /**
         * 字节码计算器(Bytecode calculator) 👉 https://calc.itzmx.com/
         *
         * @param sizeThreshold byte (B)
         */
        fun setSingleFileMaxSize(sizeThreshold: Long, sizeThresholdTip: String): Builder {
            this.mSingleFileMaxSize = sizeThreshold
            this.mSingleFileMaxSizeTip = sizeThresholdTip
            return this
        }

        /**
         * @param sizeThreshold byte (B)
         */
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