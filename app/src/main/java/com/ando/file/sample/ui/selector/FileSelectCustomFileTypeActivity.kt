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
 * # FileSelectCustomFileTypeActivity
 *
 * Description: 自定义文件类型 (Custom file type)
 *
 * @author javakam
 * @date 2021-01-22
 */
@SuppressLint("SetTextI18n")
class FileSelectCustomFileTypeActivity : AppCompatActivity() {

    private val mShowText: String by lazy { getString(R.string.str_ando_file_select_multiple_diff) }
    private lateinit var mTvCurrStrategy: TextView
    private lateinit var mRgStrategy: RadioGroup
    private lateinit var mBtSelect: Button
    private lateinit var mTvError: TextView
    private lateinit var mRvResults: RecyclerView

    private var mOverLimitStrategy: Int = OVER_LIMIT_EXCEPT_ALL

    //展示结果 (Show results)
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
        title = "Custom File Type"

        //策略切换 (Strategy switching)
        val prefix = getString(R.string.str_ando_file_current_strategy)
        mRgStrategy.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_strategy1 -> {
                    this.mOverLimitStrategy = OVER_LIMIT_EXCEPT_ALL
                    mTvCurrStrategy.text = "$prefix OVER_LIMIT_EXCEPT_ALL"
                }
                R.id.rb_strategy2 -> {
                    this.mOverLimitStrategy = OVER_LIMIT_EXCEPT_OVERFLOW
                    mTvCurrStrategy.text = "$prefix OVER_LIMIT_EXCEPT_OVERFLOW"
                }
                else -> {
                }
            }
        }
        mTvCurrStrategy.text = "$prefix ${
            if (this.mOverLimitStrategy == OVER_LIMIT_EXCEPT_ALL) "OVER_LIMIT_EXCEPT_ALL"
            else "OVER_LIMIT_EXCEPT_OVERFLOW"
        }"

        mBtSelect.text = "$mShowText (0)"
        mBtSelect.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()
//                if (it) chooseFile2()
            }
        }
    }

    private fun chooseFile2() {
        val optionsTest = FileSelectOptions().apply {
            /*
            FileType
            TEST(mutableListOf("mp3", "txt", "json")),
             */

//            fileType = FileType.TEST
//            fileCondition = object : FileSelectCondition {
//                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
//                    FileLogger.w("FileSelectCondition optionsTest fileType=$fileType ; uri=$uri")
//                    return true
//                }
//            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()//默认是单选false
            .setSingleFileMaxSize(209715200, "The size of a single file cannot exceed 200M !")
            .setOverLimitStrategy(OVER_LIMIT_EXCEPT_OVERFLOW)
            .setExtraMimeTypes("audio/*", "text/plain", "application/*") //, "application/*"
            //.applyOptions(optionsImage, optionsAudio, optionsTxt, optionsJsonFile)
            .applyOptions(optionsTest)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    FileLogger.w("FileSelectCondition FileSelector fileType=$fileType ; uri=$uri")
                    return true
                }
            })
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    FileLogger.w("FileSelectCallBack onSuccess ${results?.size}")
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("FileSelectCallBack onError ${e?.message}")
                }
            })
            .choose()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    //Custom IFileType
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
    Bytecode calculator -> https://calc.itzmx.com/
       3M  = 3145728  Byte
       5M  = 5242880  Byte
       10M = 10485760 Byte
       20M = 20971520 Byte
    */
    private fun chooseFile() {
        //图片 Image
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            minCount = 0
            maxCount = 2
            minCountTip = "Select at least one picture"
            maxCountTip = "Select up to two pictures"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "A single picture does not exceed 5M !"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "The total size of the picture does not exceed 10M !"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        //音频 audio
        val optionsAudio = FileSelectOptions().apply {
            fileType = FileType.AUDIO
            minCount = 2
            maxCount = 3
            minCountTip = "Select at least two audio files"
            maxCountTip = "Select up to three audio files"
            singleFileMaxSize = 20971520
            singleFileMaxSizeTip = "Maximum single audio does not exceed 20M !"
            allFilesMaxSize = 31457280
            allFilesMaxSizeTip = "The total audio size does not exceed 30M !"
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
            minCountTip = "Select at least one text file"
            maxCountTip = "Select up to two text files"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "The maximum size of a single text file is 5 M!"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "The total size of the text file does not exceed 10 M!"
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
            minCount = 0
            maxCount = 2
            minCountTip = "Choose at least one JSON file"
            maxCountTip = "Choose up to two JSON files"
        }

        /* 2023年4月14日 11:03:26
        说明：通过applyOptions(optionsImage, optionsAudio, optionsTxt, optionsJsonFile)指定了四种类型可以选择，
        其中的每一种类型包含多种MimeType，例如：TXT(mutableListOf("txt", "conf", "iml", "ini", "log", "prop", "rc"))

        当在选择文件时候，分单选和多选两种情况：
        1 单选：选择指定类型的任意文件都可以。即OVER_LIMIT_EXCEPT_ALL和OVER_LIMIT_EXCEPT_OVERFLOW都行。
        2 多选(setMultiSelect())：建议使用OVER_LIMIT_EXCEPT_OVERFLOW。
        如果使用`OVER_LIMIT_EXCEPT_ALL`，每一种指定类型的文件都至少选取setMinCount(int)个，
        比如只选择了一个xxx.txt文件是会报错的，因为其它类型的文件也设置了最小数量限制但却没有被选择，进而被判定为选取失败抛出最小限定的异常。
        因此，多文件选择建议使用OVER_LIMIT_EXCEPT_OVERFLOW策略，因为这种策略只会对超出最大限定数量的多余文件进行剔除并正常返回数据。
         */
        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()//默认是单选false
//            .setMinCount(1, "Select at least one set type file!")
            .setMaxCount(4, "Choose up to four files!")
            .setSingleFileMaxSize(209715200, "The size of a single file cannot exceed 200M !")
            .setAllFilesMaxSize(524288000, "The total file size cannot exceed 500M !")
            .setOverLimitStrategy(this.mOverLimitStrategy)
            .setExtraMimeTypes("audio/*", "image/*", "text/*", "application/json")
            .applyOptions(optionsImage, optionsAudio, optionsTxt, optionsJsonFile)
            //.applyOptions(optionsImage, optionsTxt)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                        FileType.AUDIO -> true
                        FileType.TXT -> !FileType.INSTANCE.parseSuffix(uri).equals("md", true)
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
                        toastLong("No file selected")
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