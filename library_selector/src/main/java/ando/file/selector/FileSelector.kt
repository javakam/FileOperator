package ando.file.selector

import ando.file.FileOperator
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.collection.ArrayMap
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_ALL_EXCEPT
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.core.FileOpener.createChooseIntent
import ando.file.core.FileType.INSTANCE

/**
 * Title: FileSelector
 *
 * @author javakam
 * @date 2020/5/21  9:32
 */
class FileSelector private constructor(builder: Builder) {

    companion object {
        const val DEFAULT_SINGLE_FILE_TYPE_MISMATCH_THRESHOLD = "文件类型不匹配"
        const val DEFAULT_SINGLE_FILE_SIZE_THRESHOLD = "超过限定文件大小"
        const val DEFAULT_ALL_FILE_SIZE_THRESHOLD = "超过限定文件总大小"
        const val DEFAULT_COUNT_ZERO_THRESHOLD = "至少选择一个文件"
        const val DEFAULT_COUNT_MAX_THRESHOLD = "超过限定文件数量"

        fun with(context: Context): Builder {
            return Builder(context)
        }
    }

    private var mContext: Context? = null
    private var mRequestCode: Int = 0

    private var mMimeTypes: Array<String>?
    private var mIsMultiSelect: Boolean = false
    private var mMinCount: Int = 0                              //可选文件最小数量
    private var mMaxCount: Int = Int.MAX_VALUE                  //可选文件最大数量
    private var mMinCountTip: String = DEFAULT_COUNT_ZERO_THRESHOLD
    private var mMaxCountTip: String = DEFAULT_COUNT_MAX_THRESHOLD
    private var mSingleFileMaxSize: Long = -1                   //单文件大小控制 Byte
    private var mAllFilesMaxSize: Long = -1                     //总文件大小控制 Byte
    private var mOverSizeLimitStrategy = OVER_SIZE_LIMIT_ALL_EXCEPT

    private var mFileTypeMismatchTip: String = DEFAULT_SINGLE_FILE_TYPE_MISMATCH_THRESHOLD
    private var mSingleFileMaxSizeTip: String = DEFAULT_SINGLE_FILE_SIZE_THRESHOLD
    private var mAllFilesMaxSizeTip: String = DEFAULT_SINGLE_FILE_SIZE_THRESHOLD

    private var mFileSelectCondition: FileSelectCondition? = null
    private var mFileSelectCallBack: FileSelectCallBack? = null
    private var mFileOptions: List<FileSelectOptions>? = null

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

    fun obtainResult(requestCode: Int, resultCode: Int, intent: Intent?): FileSelector {
        this.requestCode = requestCode
        this.resultCode = resultCode
        this.intent = intent

        if (requestCode == -1 || requestCode != mRequestCode) return this

        //单选 Intent.getData ; 多选 Intent.getClipData
        return if (mIsMultiSelect) {
            // Android 系统文件判断策略 : Intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            // 开启多选条件下只选择一个文件时, 系统走的是单选逻辑... Σ( ° △ °|||)︴
            if (intent?.clipData == null) handleSingleSelectCase(intent) else handleMultiSelectCase(intent)
        } else {
            handleSingleSelectCase(intent)
        }
    }

    private fun handleSingleSelectCase(intent: Intent?): FileSelector {
        this.mIsMultiSelect = false
        val intentData: Uri? = intent?.data
        if (intentData == null) {
            mFileSelectCallBack?.onError(Throwable(mMinCountTip))
            return this
        }

        filterUri(intentData) { o, t, tf, s, sf ->
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
        return this
    }

    private fun handleMultiSelectCase(intent: Intent?): FileSelector {
        this.mIsMultiSelect = true
        val clipData = intent?.clipData ?: return this

        //clipData.itemCount at least 2
        val itemCount = clipData.itemCount
        if (itemCount <= 1) return this
        if (itemCount < mMinCount) {
            mFileSelectCallBack?.onError(Throwable(mMinCountTip))
            return this
        }
        if (itemCount > mMaxCount) {
            mFileSelectCallBack?.onError(Throwable(mMaxCountTip))
            return this
        }

        //不同文件类型的结果集合(Result collection of different file types)
        val uriList = ArrayMap<FileSelectOptions, MutableList<Uri>>()
        //不同文件类型的结果集合的并集(Union of result sets of different file types)
        val uriListAll: MutableList<Uri> = mutableListOf()

        var totalSize = 0L
        var isNeedReturn = false
        var isFileTypeIllegal = false
        var isFileCountIllegal = false  //文件数量(File Count): true 数量超限(Quantity exceeded)
        var isFileSizeIllegal = false   //文件大小(File Size): true 大小超限(Oversize)
        val typeRelatedMap = ArrayMap<FileSelectOptions, Boolean>()

        (0 until itemCount).forEach { i ->
            if (isNeedReturn) return this

            val uri = clipData.getItemAt(i)?.uri ?: return@forEach
            filterUri(uri) { o, t, tf, s, sf ->
                val isCurrentType = (t == o?.fileType)
                FileLogger.w("Multi-> filterUri: ${o?.fileType} isCurrentType=$isCurrentType isSuccess=$sf")

                //FileType Mismatch -> onError
                if (!tf) {
                    mFileSelectCallBack?.onError(Throwable(
                        if (o?.fileTypeMismatchTip?.isNotBlank() == true) o.fileTypeMismatchTip else mFileTypeMismatchTip
                    ))

                    if (uriList.isNotEmpty()) uriList.clear()
                    if (uriListAll.isNotEmpty()) uriListAll.clear()
                    isNeedReturn = true
                    isFileTypeIllegal = true
                    return@filterUri
                }

                //FileSize
                if (!sf) {
                    isFileSizeIllegal = true
                    if (isCurrentType && mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT) {
                        mFileSelectCallBack?.onError(Throwable(o?.singleFileMaxSizeTip ?: mSingleFileMaxSizeTip))
                        isNeedReturn = true
                    }
                    return@filterUri
                }

                if (!typeRelatedMap.contains(o)) {
                    typeRelatedMap[o] = true
                }

                //控制自定义选项数量和大小(Control Custom Option count and size)
                if (isCurrentType && o != null) {
                    val count = uriList[o]?.size ?: 0

                    //maxCount最小值为1(maxCount minimum is 1)
                    val realMax: Int = if (o.maxCount <= 0) 1 else o.maxCount

                    FileLogger.i("Multi-> Custom Option ${o.fileType} count=$count min:${count < o.minCount} max:$realMax")
                    if (count != 0 && count < o.minCount) {
                        if (mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT) mFileSelectCallBack?.onError(Throwable(o.minCountTip))
                        typeRelatedMap[o] = false
                        isFileCountIllegal = true
                        isNeedReturn = true
                        return@filterUri
                    }

                    if (count > 0 && count > realMax) {
                        if (mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT) mFileSelectCallBack?.onError(Throwable(o.maxCountTip))
                        typeRelatedMap[o] = false
                        isFileCountIllegal = true
                        isNeedReturn = true
                        return@filterUri
                    }

                    val mAllMaxSize = realSizeLimitAll(o)
                    val totalSizeByFileType = calculateCurrOptionSize(o.fileType?.name) + s
                    mFileSizeMap[o.fileType?.name] = totalSizeByFileType

                    FileLogger.i("Multi-> totalSizeByFileType  : $totalSizeByFileType  mAllMaxSize=$mAllMaxSize")
                    if (totalSizeByFileType > mAllMaxSize) {//byte (B)
                        isFileSizeIllegal = true
                        typeRelatedMap[o] = false
                        return@filterUri
                    }
                }

                //Control Total Size
                totalSize += s
                FileLogger.i("Multi-> totalSize  : $totalSize  canJoin=${typeRelatedMap[o]} ")

                if (totalSize > mAllFilesMaxSize) {//byte (B)
                    isFileSizeIllegal = true
                    when (mOverSizeLimitStrategy) {
                        OVER_SIZE_LIMIT_ALL_EXCEPT -> {
                            mFileSelectCallBack?.onError(Throwable(mAllFilesMaxSizeTip))
                            isNeedReturn = true
                        }
                        OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART -> {
                            if (uriListAll.isNotEmpty()) uriListAll.clear()
                            uriList.values.forEach { l -> uriListAll.addAll(l) }
                            mFileSelectCallBack?.onSuccess(createResult(uriListAll))
                            isNeedReturn = true
                        }
                    }
                    return@filterUri
                }

                //add to result list
                if (typeRelatedMap[o] == true) {
                    if (uriList[o].isNullOrEmpty()) uriList[o] = mutableListOf()
                    uriList[o]?.add(uri)
                    uriListAll.add(uri)
                }
            }
        }

        FileLogger.w("Multi-> isFileTypeIllegal=$isFileTypeIllegal isFileSizeIllegal=$isFileSizeIllegal isFileCountIllegal=$isFileCountIllegal")

        //filter data
        if (isFileSizeIllegal || isFileCountIllegal) {
            when (mOverSizeLimitStrategy) {
                OVER_SIZE_LIMIT_ALL_EXCEPT -> {
                    typeRelatedMap.filter { (k: FileSelectOptions, v: Boolean) ->
                        val count = uriList[k]?.size ?: 0
//                        if (count != 0 && count < k.minCount) {
//                            mFileSelectCallBack?.onError(Throwable(k.minCountTip ?: k.minCountTip?:mMinCountTip))
//                        }
//                        val realMax: Int = if (k.maxCount <= 0) 1 else k.maxCount
//                        if (count > 0 && count > realMax) {
//                        }

                        if (v == false && count < k.maxCount) {
                            mFileSelectCallBack?.onError(Throwable(k.allFilesMaxSizeTip ?: mAllFilesMaxSizeTip))
                        }

                        if (uriList.isNotEmpty()) uriList.clear()
                        if (uriListAll.isNotEmpty()) uriListAll.clear()
                        v == false
                    }.keys
                        .toMutableList()
                        .map { o: FileSelectOptions? -> o?.fileType }
                        .let { l: List<FileType?> ->
                            uriList.values.forEach { ls ->
                                ls.filter {
                                    FileLogger.e("Multi-> $it ${INSTANCE.typeByUri(it)}  ${l.contains(INSTANCE.typeByUri(it))} ")
                                    !l.contains(INSTANCE.typeByUri(it))
                                }.apply {
                                    uriListAll.addAll(this)
                                }
                            }
                        }
                }
                OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART -> {
                    FileLogger.e("Multi-> uriListAll ${uriListAll.size}")

                    if (uriListAll.isNotEmpty()) uriListAll.clear()
                    typeRelatedMap.keys.forEach { op ->
                        uriList.forEach {
                            it.value.filter { uri -> INSTANCE.typeByUri(uri) == op.fileType }.let { l ->
                                uriListAll.addAll(l.take(it.key.maxCount))
                            }
                        }
                    }

                    FileLogger.e("Multi-> uriListAll mFileSelectCallBack ${uriListAll.size}")
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
        val isFileOptionsNullOrEmpty = mFileOptions.isNullOrEmpty() || currentOption.isNullOrEmpty()
        if (FileOperator.isDebug()) {
            FileLogger.i("filterUri: $uri fileType=$fileType currentOption=${currentOption?.size} isFileOptionsNullOrEmpty=$isFileOptionsNullOrEmpty")
        }

        if (currentOption.isNullOrEmpty()) {
            block.invoke(null, fileType, false, fileSize, false)
            return
        }

        if (isFileOptionsNullOrEmpty) {
            //没有设置 FileOptions 时,使用通用的配置
            val isAccept = (mFileSelectCondition != null) && mFileSelectCondition?.accept(fileType, uri) ?: false
            if (!isAccept) return
            block.invoke(null, fileType, true, fileSize, limitFileSize(fileSize, realSizeLimit(null)))
        } else {
            currentOption.forEach { o: FileSelectOptions ->
                //获取 CallBack -> 优先使用 FileSelectOptions 中设置的 FileSelectCallBack
                //控制类型 -> 自定义规则 -> 优先使用 FileSelectOptions 中设置的 FileSelectCondition
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

    //todo 2020年12月17日17:18:34 数量限制
    private fun realMinCountLimit(option: FileSelectOptions?): Int =
        if (option == null) {
            if (mMinCount < 0) 1 else mMinCount
        } else {
            if (option.minCount < 0) 1 else option.minCount
        }

    private fun calculateCurrOptionSize(fileType: String?): Long {
        if (!mFileSizeMap.contains(fileType)) {
            mFileSizeMap[fileType] = 0L
        }
        return mFileSizeMap[fileType] ?: 0L
    }

    private fun realSizeLimit(option: FileSelectOptions?): Long =
        if (option == null) {
            if (mSingleFileMaxSize < 0)
                if (mAllFilesMaxSize < 0) Long.MAX_VALUE else mAllFilesMaxSize
            else mSingleFileMaxSize
        } else {
            if (option.singleFileMaxSize < 0)
                if (option.allFilesMaxSize < 0) realSizeLimit(null) else option.allFilesMaxSize
            else option.singleFileMaxSize
        }

    private fun realSizeLimitAll(option: FileSelectOptions?): Long =
        if (option == null) Long.MAX_VALUE
        else
            if (option.allFilesMaxSize < 0)
                if (mAllFilesMaxSize < 0) Long.MAX_VALUE else mAllFilesMaxSize
            else option.allFilesMaxSize

    private fun limitFileSize(fileSize: Long, sizeThreshold: Long): Boolean {
        if (FileOperator.isDebug()) {
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

    class Builder internal constructor(private val context: Context) {
        var mRequestCode: Int = 0

        var mMimeTypes: Array<String>? = null
        var mIsMultiSelect: Boolean = false
        var mMinCount: Int = 0                              //可选文件最小数量
        var mMaxCount: Int = Int.MAX_VALUE                  //可选文件最大数量
        var mMinCountTip: String = DEFAULT_COUNT_ZERO_THRESHOLD
        var mMaxCountTip: String = DEFAULT_COUNT_MAX_THRESHOLD
        var mSingleFileMaxSize: Long = -1                   //单文件大小控制 B
        var mAllFilesMaxSize: Long = -1                     //总文件大小控制 B
        var mFileTypeMismatchTip: String = DEFAULT_SINGLE_FILE_TYPE_MISMATCH_THRESHOLD
        var mSingleFileMaxSizeTip: String = DEFAULT_SINGLE_FILE_SIZE_THRESHOLD
        var mAllFilesMaxSizeTip: String = DEFAULT_ALL_FILE_SIZE_THRESHOLD
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