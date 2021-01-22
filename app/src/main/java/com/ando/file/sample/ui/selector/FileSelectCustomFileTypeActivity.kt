package com.ando.file.sample.ui.selector

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_ALL
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW
import ando.file.selector.*
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import com.ando.file.sample.utils.ResultUtils.asVerticalList

/**
 * FileSelectCustomFileTypeActivity
 *
 * Description: 自定义文件类型
 *
 * @author javakam
 * @date 2021-01-22
 */
@SuppressLint("SetTextI18n")
class FileSelectCustomFileTypeActivity : AppCompatActivity() {

    private val mShowText: String = "选择多个不同类型文件"
    private lateinit var mTvCurrStrategy: TextView
    private lateinit var mRgStrategy: RadioGroup
    private lateinit var mBtSelect: Button
    private lateinit var mTvError: TextView
    private lateinit var mRvResults: RecyclerView

    private var mOverLimitStrategy: Int = OVER_LIMIT_EXCEPT_ALL

    //展示结果(Show results)
    private var mResultShowList: MutableList<ResultUtils.ResultShowBean>? = null
    private val mAdapter: FileSelectResultAdapter by lazy { FileSelectResultAdapter() }

    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_multi_files)
        mTvCurrStrategy = findViewById(R.id.tv_curr_strategy)
        mTvError = findViewById(R.id.tv_error)
        mRgStrategy = findViewById(R.id.rg_strategy)
        mBtSelect = findViewById(R.id.bt_select_multi)
        mRvResults = findViewById(R.id.rv_images)
        mRvResults.asVerticalList()
        mRvResults.adapter = mAdapter
        title = "多选文件"

        //策略切换
        mRgStrategy.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_strategy1 -> {
                    this.mOverLimitStrategy = OVER_LIMIT_EXCEPT_ALL
                    mTvCurrStrategy.text = "当前策略: OVER_LIMIT_EXCEPT_ALL"
                }
                R.id.rb_strategy2 -> {
                    this.mOverLimitStrategy = OVER_LIMIT_EXCEPT_OVERFLOW
                    mTvCurrStrategy.text = "当前策略: OVER_LIMIT_EXCEPT_OVERFLOW"
                }
                else -> {
                }
            }
        }
        mTvCurrStrategy.text = "当前策略: ${
            if (this.mOverLimitStrategy == OVER_LIMIT_EXCEPT_ALL) "OVER_LIMIT_EXCEPT_ALL"
            else "OVER_LIMIT_EXCEPT_OVERFLOW"
        }"

        mBtSelect.text = "$mShowText (0)"
        mBtSelect.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    //自定义 IFileType
    object FileTypePhp : IFileType {
        override fun fromUri(uri: Uri?): IFileType {
            return if (parseSuffix(uri).equals("json", true)) FileTypePhp else FileType.UNKNOWN
        }
    }

    //or
    enum class FileTypeJson : IFileType {
        JSON;

        override fun fromUri(uri: Uri?): IFileType {
            return resolveFileMatch(uri, "json", JSON)
        }
    }

    /*
    字节码计算器 -> https://calc.itzmx.com/
       3M  = 3145728  Byte
       5M  = 5242880  Byte
       10M = 10485760 Byte
       20M = 20971520 Byte
    */
    private fun chooseFile() {
        //图片
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            minCount = 1
            maxCount = 2
            minCountTip = "至少选择一张图片"
            maxCountTip = "最多选择两张图片"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "单张图片最大不超过5M！"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "图片总大小不超过10M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        //音频
        val optionsAudio = FileSelectOptions().apply {
            fileType = FileType.AUDIO
            minCount = 2
            maxCount = 3
            minCountTip = "至少选择两个音频文件"
            maxCountTip = "最多选择三个音频文件"
            singleFileMaxSize = 20971520
            singleFileMaxSizeTip = "单音频最大不超过20M！"
            allFilesMaxSize = 31457280
            allFilesMaxSizeTip = "音频总大小不超过30M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (uri != null)
                }
            }
        }

        //文本文件 txt
        val optionsTxt = FileSelectOptions().apply {
            fileType = FileType.TXT
            minCount = 1
            maxCount = 2
            minCountTip = "至少选择一个文本文件"
            maxCountTip = "最多选择两个文本文件"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "单文本文件最大不超过5M！"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "文本文件总大小不超过10M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (uri != null)
                }
            }
        }

        //FileType.TXT.supplement("xxx")
        //FileType.IMAGE.supplement("json", "txt")
        //FileType.IMAGE.dump()

        val optionsJsonFile = FileSelectOptions().apply {
            fileType = FileTypeJson.JSON
            minCount = 1
            maxCount = 2
            minCountTip = "至少选择一个JSON文件"
            maxCountTip = "最多选择两个JSON文件"
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()//默认是单选false
            .setMinCount(1, "设定类型文件至少选择一个!")
            .setMaxCount(4, "最多选四个文件!")
            .setSingleFileMaxSize(2097152, "单文件大小不能超过2M！")
            .setAllFilesMaxSize(52428800, "总文件大小不能超过50M！")
            .setOverLimitStrategy(this.mOverLimitStrategy)
            .setMimeTypes("audio/*", "image/*", "text/*", "application/json")
            .applyOptions(optionsImage, optionsAudio, optionsTxt, optionsJsonFile)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                        FileType.AUDIO -> true
                        FileType.TXT -> true
                        FileTypeJson.JSON -> true
                        else -> false
                    }
                }
            })
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    FileLogger.w("FileSelectCallBack onSuccess ${results?.size}")
                    mAdapter.setData(null)
                    if (results.isNullOrEmpty()) {
                        toastLong("没有选取文件")
                        return
                    }
                    showSelectResult(results)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("FileSelectCallBack onError ${e?.message}")
                    ResultUtils.setErrorText(mTvError, e)

                    mAdapter.setData(null)
                    mBtSelect.text = "$mShowText (0)"
                }
            })
            .choose()
    }

    private fun showSelectResult(results: List<FileSelectResult>) {
        ResultUtils.setErrorText(mTvError, null)
        mBtSelect.text = "$mShowText (${results.size})"
        ResultUtils.formatResults(results, true) { l ->
            mResultShowList = l.map { p ->
                ResultUtils.ResultShowBean(originUri = p.first, originResult = p.second)
            }.toMutableList()
        }
        mAdapter.setData(mResultShowList)
    }

}