package ando.file.selector

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.collection.ArrayMap
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_ALL_DONT
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.core.FileOpener.createChooseIntent
import ando.file.core.FileType.INSTANCE

/**
 * Title: FileSelector
 * <pre>
 *
 * </pre>
 * @author javakam
 * @date 2020/5/21  9:32
 */
class FileSelector private constructor(builder: Builder) {

    companion object {
        val DEFAULT_SINGLE_FILE_SIZE_THRESHOLD = "超过限定文件大小"
        val DEFAULT_ALL_FILE_SIZE_THRESHOLD = "超过限定文件总大小"

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
    private var mMinCountTip: String = ""
    private var mMaxCountTip: String = ""
    private var mSingleFileMaxSize: Long = -1                   //单文件大小控制 B
    private var mAllFilesMaxSize: Long = -1                     //总文件大小控制 B
    private var mOverSizeLimitStrategy = OVER_SIZE_LIMIT_ALL_DONT

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

        //单选 Intent.getData ; 多选  Intent.getClipData
        return if (mIsMultiSelect) {
            // Android 系统文件判断策略 : Intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            // 开启多选条件下只选择一个文件时,需要走单选逻辑... Σ( ° △ °|||)︴
            if (intent?.clipData == null) handleSingleSelectCase(intent) else handleMultiSelectCase(intent)
        } else {
            handleSingleSelectCase(intent)
        }
    }

    private fun handleSingleSelectCase(intent: Intent?): FileSelector {
        this.mIsMultiSelect = false
        intent?.data?.let {
            processIntentUri(it) { o, t, s, r ->

                if (r) mFileSelectCallBack?.onSuccess(createResult(it, t, s))
                else {
                    if (o?.fileType == t) {
                        mFileSelectCallBack?.onError(Throwable(if (o.mSingleFileMaxSizeTip != null) o.mSingleFileMaxSizeTip else o.mAllFilesMaxSizeTip))
                    } else {
                        mFileSelectCallBack?.onError(Throwable(mSingleFileMaxSizeTip))
                    }
                }
            }
        }
        return this
    }

    private fun handleMultiSelectCase(intent: Intent?): FileSelector {
        this.mIsMultiSelect = true
        val clipData = intent?.clipData ?: return this

        //clipData.itemCount at least 2
        if (clipData.itemCount <= 0) return this
        if (clipData.itemCount > mMaxCount) {
            mFileSelectCallBack?.onError(Throwable(mMaxCountTip))
            return this
        }

        val uriListAll: MutableList<Uri> = mutableListOf()
        val uriList = ArrayMap<FileSelectOptions, MutableList<Uri>>()

        var totalSize = 0L
        var isNeedBreak = false
        var isFileSizeIllegal = false
        val canJoinTypeMap = ArrayMap<FileSelectOptions, Boolean>()

        (0 until clipData.itemCount).forEach { i ->
            if (isNeedBreak) return this

            val u = clipData.getItemAt(i)?.uri ?: return@forEach
            processIntentUri(u) { o, t, s, r ->
                val isCurrentType = (t == o?.fileType)
                FileLogger.w("processIntentUri: ${o?.fileType}  isCurrentType=$isCurrentType  isSuccess=$r")

                if (!r) {
                    isFileSizeIllegal = true
                    if (isCurrentType && mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_DONT) {
                        mFileSelectCallBack?.onError(Throwable(o?.mSingleFileMaxSizeTip ?: mSingleFileMaxSizeTip))
                        isNeedBreak = true
                    }
                    return@processIntentUri
                }
                if (!canJoinTypeMap.contains(o)) {
                    canJoinTypeMap[o] = true
                }

                //Control Custom Option Size
                if (isCurrentType && o != null) {
                    FileLogger.i("handleMultiSelectCase  Custom Option  mMaxCount : ${(uriList[o]?.size ?: 0 >= o.mMaxCount)} ")
                    if (uriList[o]?.size ?: 0 >= o.mMaxCount) {
                        if (mOverSizeLimitStrategy == OVER_SIZE_LIMIT_ALL_DONT) mFileSelectCallBack?.onError(Throwable(o.mMaxCountTip))
                        canJoinTypeMap[o] = false
                        isFileSizeIllegal = true
                        isNeedBreak = true
                        return@processIntentUri
                    }

                    val mAllMaxSize = realLimitSizeAllThreshold(o)
                    val totalSizeByFileType = calculateCurrentOptionSize(o.fileType?.name) + s
                    mFileSizeMap[o.fileType?.name] = totalSizeByFileType

                    FileLogger.i("handleMultiSelectCase totalSizeByFileType  : $totalSizeByFileType  mAllMaxSize=$mAllMaxSize")
                    if (totalSizeByFileType > mAllMaxSize) {//byte (B)
                        isFileSizeIllegal = true
                        canJoinTypeMap[o] = false
                        return@processIntentUri
                    }
                }

                //Control Total Size
                totalSize += s
                FileLogger.i("handleMultiSelectCase totalSize  : $totalSize  canJoin=${canJoinTypeMap[o]} ")

                if (totalSize > mAllFilesMaxSize) {//byte (B)
                    isFileSizeIllegal = true
                    when (mOverSizeLimitStrategy) {
                        OVER_SIZE_LIMIT_ALL_DONT -> {
                            mFileSelectCallBack?.onError(Throwable(mAllFilesMaxSizeTip))
                            isNeedBreak = true
                        }
                        OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART -> {
                            uriListAll.clear()
                            uriList.values.forEach { uriListAll.addAll(it) }

                            mFileSelectCallBack?.onSuccess(createResult(uriListAll))
                            isNeedBreak = true
                        }
                    }
                    return@processIntentUri
                }

                if (true == canJoinTypeMap[o]) {
                    if (uriList[o].isNullOrEmpty()) uriList[o] = mutableListOf<Uri>()
                    uriList[o]?.add(u)
                    uriListAll.add(u)
                }
            }
        }

        FileLogger.i("handleMultiSelectCase isIllegal  : $isFileSizeIllegal ")

        //filter data
        if (isFileSizeIllegal) {
            when (mOverSizeLimitStrategy) {
                OVER_SIZE_LIMIT_ALL_DONT -> {
                    canJoinTypeMap.filter { (k, v) ->
                        if (v == false && uriList[k]?.size ?: 0 < k.mMaxCount)
                            mFileSelectCallBack?.onError(Throwable(k.mAllFilesMaxSizeTip ?: mAllFilesMaxSizeTip))

                        uriList.clear()
                        uriListAll.clear()
                        v == false
                    }.keys
                        .toList()
                        .map { o -> o?.fileType }
                        .let { l ->
                            uriList.values.forEach { ls ->
                                ls.filter {
                                    // FileLogger.e("lll $it  ${INSTANCE.typeByUri(it)}  ${l.contains(INSTANCE.typeByUri(it))} ")
                                    !l.contains(INSTANCE.typeByUri(it))
                                }.toMutableList().let {
                                    uriListAll.addAll(it)
                                }
                            }
                        }
                }
                OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART -> {
                    FileLogger.e("uriListAll ${uriListAll.size}")

                    uriListAll.clear()
                    canJoinTypeMap.keys.forEach { op ->
                        uriList.forEach {
                            //if ( it.value.size <it.key.mMaxCount){
                            it.value.filter { uri -> INSTANCE.typeByUri(uri) == op.fileType }.let { ls ->
                                uriListAll.addAll(ls.take(it.key.mMaxCount))
                            }
                        }
                    }
                    FileLogger.e("uriListAll mFileSelectCallBack ${uriListAll.size}")

                    mFileSelectCallBack?.onSuccess(createResult(uriListAll))
                    return this
                }
            }
        }

        if (!uriList.isNullOrEmpty()) mFileSelectCallBack?.onSuccess(createResult(uriListAll))
        return this
    }


    private fun processIntentUri(uri: Uri, block: (option: FileSelectOptions?, fileType: FileType, fileSize: Long, sizeFit: Boolean) -> Unit) {
        val fileType = INSTANCE.typeByUri(uri)
        val fileSize = FileSizeUtils.getFileSize(uri)

        val currentOption: List<FileSelectOptions>? = mFileOptions?.filter { it.fileType == fileType }
        val isFileOptionsNullOrEmpty = mFileOptions.isNullOrEmpty() || currentOption.isNullOrEmpty()

        FileLogger.i("Select File ：chooseFilePath: $uri   fileType: $fileType currentOption:${currentOption?.size}  isFileOptionsNullOrEmpty=$isFileOptionsNullOrEmpty")

        if (isFileOptionsNullOrEmpty) {
            //没有设置 FileOptions 时,使用通用的配置
            val isAccept = (mFileSelectCondition != null) && mFileSelectCondition?.accept(fileType, uri) ?: false
            if (!isAccept) return
            block.invoke(
                null,
                fileType,
                fileSize,
                limitFileSize(fileSize, realLimitSizeThreshold(null))
            )
        } else {
            currentOption?.forEach {
                //获取 CallBack -> 优先使用 FileOptions 中设置的 FileSelectCallBack
                //控制类型 -> 自定义规则 -> 优先使用 FileOptions 中设置的 FileSelectCondition
                val isAccept = mFileSelectCondition?.accept(fileType, uri) ?: true && it.mFileCondition?.accept(fileType, uri) ?: true
                if (!isAccept) {
                    block.invoke(
                        it,
                        fileType,
                        fileSize,
                        limitFileSize(fileSize, realLimitSizeThreshold(null))
                    )
                    return@forEach
                }
                val success = limitFileSize(fileSize, realLimitSizeThreshold(it))
                block.invoke(it, fileType, fileSize, success)
                if (!success) return@forEach
            }
        }
    }

    private fun calculateCurrentOptionSize(fileType: String?): Long {
        if (!mFileSizeMap.contains(fileType)) {
            mFileSizeMap[fileType] = 0L
        }
        return mFileSizeMap[fileType] ?: 0L
    }

    private fun realLimitSizeThreshold(option: FileSelectOptions?): Long =
        if (option == null) if (mSingleFileMaxSize < 0) if (mAllFilesMaxSize < 0) Long.MAX_VALUE else mAllFilesMaxSize else mSingleFileMaxSize
        else if (option.mSingleFileMaxSize < 0) if (option.mAllFilesMaxSize < 0) realLimitSizeThreshold(null) else option.mAllFilesMaxSize else option.mSingleFileMaxSize

    private fun realLimitSizeAllThreshold(option: FileSelectOptions?): Long =
        if (option == null) Long.MAX_VALUE
        else if (option.mAllFilesMaxSize < 0) if (mAllFilesMaxSize < 0) Long.MAX_VALUE else mAllFilesMaxSize else option.mAllFilesMaxSize

    private fun limitFileSize(fileSize: Long, sizeThreshold: Long): Boolean {
        FileLogger.i("limitFileSize  : $fileSize")
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
        var mMinCountTip: String = ""
        var mMaxCountTip: String = ""
        var mSingleFileMaxSize: Long = -1                   //单文件大小控制 B
        var mAllFilesMaxSize: Long = -1                     //总文件大小控制 B
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

        fun setSelectMode(isMultiSelect: Boolean): Builder {
            this.mIsMultiSelect = isMultiSelect
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