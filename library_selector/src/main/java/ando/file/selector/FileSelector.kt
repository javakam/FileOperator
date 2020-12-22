package ando.file.selector

import ando.file.FileOperator.getContext
import ando.file.FileOperator.isDebug
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.collection.ArrayMap
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_ALL_EXCEPT
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.core.FileOpener.createChooseIntent
import ando.file.core.FileType.INSTANCE
import android.content.ClipData
import kotlin.math.max
import kotlin.math.min

/**
 * Title: FileSelector
 *
 * @author javakam
 * @date 2020/5/21  9:32
 */
class FileSelector private constructor(builder: Builder) {

    companion object {
        val TIP_SINGLE_FILE_TYPE_MISMATCH by lazy { getContext().getString(R.string.ando_str_single_file_type_mismatch) }
        val TIP_SINGLE_FILE_SIZE by lazy { getContext().getString(R.string.ando_str_single_file_size) }
        val TIP_ALL_FILE_SIZE by lazy { getContext().getString(R.string.ando_str_all_file_size) }
        val TIP_COUNT_MIN by lazy { getContext().getString(R.string.ando_str_count_min) }
        val TIP_COUNT_MAX by lazy { getContext().getString(R.string.ando_str_count_max) }

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
    private var mOverSizeLimitStrategy = OVER_SIZE_LIMIT_ALL_EXCEPT

    private var mFileTypeMismatchTip: String = TIP_SINGLE_FILE_TYPE_MISMATCH
    private var mSingleFileMaxSizeTip: String = TIP_SINGLE_FILE_SIZE
    private var mAllFilesMaxSizeTip: String = TIP_SINGLE_FILE_SIZE

    private var mFileSelectCondition: FileSelectCondition? = null
    private var mFileSelectCallBack: FileSelectCallBack? = null
    private var mFileOptions: List<FileSelectOptions>? = null

    private val mFileCountMap = ArrayMap<FileType, Int>()
    private val mFileSizeMap = ArrayMap<String, Long>()

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
        mOverSizeLimitStrategy = builder.mOverSizeLimitStrategy
        mFileSelectCondition = builder.mFileSelectCondition
        mFileSelectCallBack = builder.mFileSelectCallBack
        mFileOptions = builder.mFileOptions
    }

    fun choose(context: Context, mimeType: String?): FileSelector {
        this.mContext = context
        startActivityForResult(context, createChooseIntent(mimeType, mMimeTypes, mIsMultiSelect), mRequestCode)
        return this
    }

    //onActivityResult
    var requestCode: Int? = -1
    var resultCode: Int? = 0
    var intent: Intent? = null

    fun obtainResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        this.requestCode = requestCode
        this.resultCode = resultCode
        this.intent = intent

        if (requestCode == -1 || requestCode != mRequestCode) return

        //单选(Single choice) Intent.getData; 多选(Multiple choice) Intent.getClipData
        if (mIsMultiSelect) {
            if (intent?.clipData == null)
            //单一类型和多种类型(Single type and multiple types)
                if ((mFileOptions?.size ?: 0 >= 2) && (mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT)) {
                    mFileSelectCallBack?.onError(Throwable(mMinCountTip))
                } else handleSingleSelectCase(intent)
            else handleMultiSelectCase(intent)
        } else handleSingleSelectCase(intent)
    }

    private fun handleSingleSelectCase(intent: Intent?) {
        this.mIsMultiSelect = false
        val intentData: Uri? = intent?.data
        if (intentData == null) {
            mFileSelectCallBack?.onError(Throwable(mMinCountTip))
            return
        }

        filterUri(intentData) { o: FileSelectOptions?, t: FileType, tf: Boolean, s: Long, sf: Boolean ->
            if (!tf) {
                mFileSelectCallBack?.onError(Throwable(
                    if (o?.fileTypeMismatchTip?.isNotBlank() == true) o.fileTypeMismatchTip else mFileTypeMismatchTip
                ))
                return@filterUri
            }
            if (sf) mFileSelectCallBack?.onSuccess(createResult(intentData, t, s))
            else {
                if (o?.fileType == t) {
                    mFileSelectCallBack?.onError(Throwable(if (o.singleFileMaxSizeTip != null) o.singleFileMaxSizeTip else o.allFilesMaxSizeTip))
                } else {
                    mFileSelectCallBack?.onError(Throwable(mSingleFileMaxSizeTip))
                }
            }
        }
    }

    private fun handleMultiSelectCase(intent: Intent?): FileSelector {
        this.mIsMultiSelect = true
        val clipData: ClipData = intent?.clipData ?: return this

        //clipData.itemCount 最小值为2(The minimum value of clipData.itemCount is 2)
        val itemCount = clipData.itemCount
        val isStrictStrategy = (mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT)
        if (isStrictStrategy && itemCount < realMinCountLimit(null)) {
            mFileSelectCallBack?.onError(Throwable(mMinCountTip))
            return this
        }
        if (isStrictStrategy && itemCount > realMaxCountLimit(null)) {
            mFileSelectCallBack?.onError(Throwable(mMaxCountTip))
            return this
        }

        //不同文件类型的结果集合(Result collection of different file types)
        val uriList = ArrayMap<FileSelectOptions, SelectResult>()
        //不同文件类型的结果集合的并集(Union of result sets of different file types)
        val uriListAll: MutableList<Uri> = mutableListOf()

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
                FileLogger.w("Multi-> filterUri: ${o?.fileType} isCurrentType=$isCurrentType sf=$sf")

                if (uriList[o] == null) uriList[o] = SelectResult(checkPass = true)
                val selectResult: SelectResult = uriList[o] ?: SelectResult(checkPass = true)

                //FileType Mismatch -> onError
                if (!tf) {
                    mFileSelectCallBack?.onError(Throwable(
                        if (o?.fileTypeMismatchTip?.isNotBlank() == true) o.fileTypeMismatchTip else mFileTypeMismatchTip
                    ))

                    if (uriList.isNotEmpty()) uriList.clear()
                    if (uriListAll.isNotEmpty()) uriListAll.clear()
                    isNeedBreak = true
                    isFileTypeIllegal = true
                    return@filterUri
                }

                //FileSize
                if (!sf) {
                    isFileSizeIllegal = true
                    if (isCurrentType && mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT) {
                        mFileSelectCallBack?.onError(Throwable(o?.singleFileMaxSizeTip ?: mSingleFileMaxSizeTip))
                        isNeedBreak = true
                    }
                    return@filterUri
                }

                //File Count
                if (!mFileCountMap.contains(t)) {
                    mFileCountMap[t] = 0
                }
                mFileCountMap[t] = mFileCountMap[t]?.plus(1)
                mFileOptions?.forEach { os: FileSelectOptions ->
                    val count: Int = mFileCountMap[os.fileType] ?: 0
                    //min
                    //itemCount == (i + 1) 最后再判断最少数量(finally determine the minimum number)
                    if (itemCount == (i + 1)) {
                        if (count < realMinCountLimit(os)) {
                            isFileCountIllegal = true
                            isNeedBreak = true
                            if (isStrictStrategy) {
                                mFileSelectCallBack?.onError(Throwable(realMinCountTip(os)))
                                isNeedBreak = true
                            }
                            return@filterUri
                        }
                    }
                    //max
                    if (count > realMaxCountLimit(os)) {
                        isFileCountIllegal = true
                        isNeedBreak = true
                        if (isStrictStrategy) {
                            mFileSelectCallBack?.onError(Throwable(realMaxCountTip(os)))
                            isNeedBreak = true
                        }
                        return@filterUri
                    }
                }

                //控制自定义选项大小(Control Custom Option size)
                if (isCurrentType) {
                    if (isDebug()) {
                        FileLogger.i("Multi-> Count: ${o?.fileType} currTypeCount=${mFileCountMap[t] ?: 0} isFinally=${itemCount == (i + 1)} " +
                                "realMinCountLimit=${realMinCountLimit(o)} realMaxCountLimit=${realMaxCountLimit(o)}")
                    }

                    //File Size
                    val mAllMaxSize = realSizeLimitAll(o)
                    if (!mFileSizeMap.contains(o?.fileType?.name)) {
                        mFileSizeMap[o?.fileType?.name] = 0L
                    }
                    val currTypeTotalSize: Long = mFileSizeMap[o?.fileType?.name] ?: 0L + s
                    mFileSizeMap[o?.fileType?.name] = currTypeTotalSize
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

                if (totalSize > mAllFilesMaxSize) {//byte (B)
                    isFileSizeIllegal = true
                    isNeedBreak = true

                    when (mOverSizeLimitStrategy) {
                        OVER_SIZE_LIMIT_ALL_EXCEPT -> {
                            mFileSelectCallBack?.onError(Throwable(mAllFilesMaxSizeTip))
                        }
                        OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART -> {
                            if (uriListAll.isNotEmpty()) uriListAll.clear()
                            uriList.values.forEach { sr -> uriListAll.addAll(sr.uriList) }
                            mFileSelectCallBack?.onSuccess(createResult(uriListAll))
                        }
                    }
                    return@filterUri
                }

                //添加到结果列表(add to result list)
                if (selectResult.checkPass) {
                    selectResult.uriList.add(uri)
                    uriListAll.add(uri)
                }
            }
        }

        //某些类型没有选(Some types are not selected)
        val isOptionsSizeMatch = (mFileOptions?.size == uriList.keys.size)
        FileLogger.w("Multi-> isFileTypeIllegal=$isFileTypeIllegal isFileSizeIllegal=$isFileSizeIllegal " +
                "isFileCountIllegal=$isFileCountIllegal isOptionsSizeMatch=$isOptionsSizeMatch")

        //filter data
        if (isFileSizeIllegal || isFileCountIllegal || !isOptionsSizeMatch) {
            when (mOverSizeLimitStrategy) {
                OVER_SIZE_LIMIT_ALL_EXCEPT -> {
                    if (!isOptionsSizeMatch && !isNeedBreak) {
                        mFileSelectCallBack?.onError(Throwable(realMinCountTip(null)))
                        return this
                    }

                    uriList.filter { (_: FileSelectOptions, v: SelectResult) ->
//                        val count = uriList[k]?.size ?: 0
//                        if (count < realMinCountLimit(k)) {
//                            mFileSelectCallBack?.onError(Throwable(realMinCountTip(k)))
//                        }
//                        if (count > realMaxCountLimit(k)) {
//                            mFileSelectCallBack?.onError(Throwable(realMaxCountTip(k)))
//                        }

                        if (v.uriList.isNotEmpty()) v.uriList.clear()
                        if (uriListAll.isNotEmpty()) uriListAll.clear()
                        !v.checkPass
                    }.keys
                        .toMutableList()
                        .map { o: FileSelectOptions? -> o?.fileType }
                        .let { l: List<FileType?> ->
                            uriList.values.forEach { ls ->
                                ls.uriList.filter {
                                    if (isDebug()) {
                                        FileLogger.e("Multi filter data -> $it ${INSTANCE.typeByUri(it)}  ${l.contains(INSTANCE.typeByUri(it))} ")
                                    }
                                    !l.contains(INSTANCE.typeByUri(it))
                                }.apply {
                                    uriListAll.addAll(this)
                                }
                            }
                        }
                }
                OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART -> {
                    if (uriListAll.isNotEmpty()) uriListAll.clear()

                    //todo 2020年12月22日11:28:57
                    //.filter { it.value.checkPass }
                    uriList.keys.forEach { op: FileSelectOptions ->
                        uriList.forEach { m: Map.Entry<FileSelectOptions, SelectResult> ->
                            if (m.key.fileType == op.fileType) {
                                m.value.let { s: SelectResult ->
                                    uriListAll.addAll(s.uriList)
                                }
                            }
                        }
                    }

                    FileLogger.e("Multi filter data -> uriListAll=${uriListAll.size}")
                    mFileSelectCallBack?.onSuccess(createResult(uriListAll))
                    return this
                }
            }
        }

        if (!isFileCountIllegal && !isFileTypeIllegal && !uriList.isNullOrEmpty()) mFileSelectCallBack?.onSuccess(createResult(uriListAll))
        return this
    }

    private fun filterUri(
        uri: Uri,
        block: (option: FileSelectOptions?, fileType: FileType, typeFit: Boolean, fileSize: Long, sizeFit: Boolean) -> Unit,
    ) {
        val fileType = INSTANCE.typeByUri(uri)
        val fileSize = FileSizeUtils.getFileSize(uri)

        val currentOption: List<FileSelectOptions>? = mFileOptions?.filter { it.fileType == fileType }
        val isOptionsNullOrEmpty = mFileOptions.isNullOrEmpty() || currentOption.isNullOrEmpty()
        if (isDebug()) {
            FileLogger.i("filterUri: $uri fileType=$fileType currentOption=${currentOption?.size} isFileOptionsNullOrEmpty=$isOptionsNullOrEmpty")
        }

        if (currentOption.isNullOrEmpty()) {
            block.invoke(null, fileType, false, fileSize, false)
            return
        }

        if (isOptionsNullOrEmpty) {
            //没有设置 FileSelectOptions 时,使用通用的配置 (When FileOptions is not set, the general configuration is used)
            val isAccept = (mFileSelectCondition != null) && mFileSelectCondition?.accept(fileType, uri) ?: false
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
                    block.invoke(o, fileType, true, fileSize, limitFileSize(fileSize, realSizeLimit(null)))
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
        val realGlobalMax = if (mMaxCount <= 0) Int.MAX_VALUE else mMaxCount
        return max(realMinCountLimit(option),
            if (option?.maxCount ?: Int.MAX_VALUE > 0)
                min(option?.maxCount ?: Int.MAX_VALUE, realGlobalMax)
            else realGlobalMax)
    }

    private fun realSizeLimit(option: FileSelectOptions?): Long =
        if (option == null) {
            if (mSingleFileMaxSize < 0) if (mAllFilesMaxSize < 0) Long.MAX_VALUE else mAllFilesMaxSize
            else mSingleFileMaxSize
        } else {
            if (option.singleFileMaxSize < 0) if (option.allFilesMaxSize < 0) realSizeLimit(null) else option.allFilesMaxSize
            else option.singleFileMaxSize
        }

    private fun realSizeLimitAll(option: FileSelectOptions?): Long =
        if (option == null) Long.MAX_VALUE
        else
            if (option.allFilesMaxSize < 0) if (mAllFilesMaxSize < 0) Long.MAX_VALUE else mAllFilesMaxSize
            else option.allFilesMaxSize

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
        var mMaxCount: Int = Int.MAX_VALUE                  //可选文件最大数量(Maximum number of optional files)
        var mMinCountTip: String = TIP_COUNT_MIN
        var mMaxCountTip: String = TIP_COUNT_MAX
        var mSingleFileMaxSize: Long = -1                   //单文件大小控制 B (Single file size control)
        var mAllFilesMaxSize: Long = -1                     //总文件大小控制 B (Total file size control)
        var mFileTypeMismatchTip: String = TIP_SINGLE_FILE_TYPE_MISMATCH
        var mSingleFileMaxSizeTip: String = TIP_SINGLE_FILE_SIZE
        var mAllFilesMaxSizeTip: String = TIP_ALL_FILE_SIZE
        var mOverSizeLimitStrategy = OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART

        var mFileSelectCondition: FileSelectCondition? = null
        var mFileSelectCallBack: FileSelectCallBack? = null
        var mFileOptions: List<FileSelectOptions>? = null

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

        fun setOverSizeLimitStrategy(@FileGlobal.FileOverSizeStrategy overSizeLimitStrategy: Int): Builder {
            this.mOverSizeLimitStrategy = overSizeLimitStrategy
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
            this.mFileOptions = options.asList()
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