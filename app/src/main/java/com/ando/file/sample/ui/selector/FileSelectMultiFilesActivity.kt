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
 * # FileSelectMultiFilesActivity
 *
 * Description: 多选多类型文件 (Multiple selection of multiple types of files)
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
@SuppressLint("SetTextI18n")
class FileSelectMultiFilesActivity : AppCompatActivity() {

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
        title = "多选文件(Multiple selection files)"

        //策略切换 (Strategy switching)
        val prefix=getString(R.string.str_ando_file_current_strategy)
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
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
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
            minCount = 1
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

        //音频 Audio
        val optionsAudio = FileSelectOptions().apply {
            fileType = FileType.AUDIO
            minCount = 2
            maxCount = 3
            minCountTip = "Select at least two audio files"
            maxCountTip = "Select up to three audio files"
            singleFileMaxSize = 20971520
            singleFileMaxSizeTip = "Maximum single audio does not exceed 20M !"
            allFilesMaxSize = 31457280
            allFilesMaxSizeTip = "The total audio size does not exceed 30 M !"
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
            singleFileMaxSizeTip = "Single text file does not exceed 5M !"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "The total size of the text file does not exceed 10M !"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (uri != null)
                }
            }
        }

        /*
          注:如果某个FileSelectOptions没通过限定条件, 则该FileSelectOptions不会返回
          eg: 采用上面的限制条件下,图片、音频、文本文件各选一个, 因为音频最小数量设定为`2`不满足设定条件则去除所有音频选择结果
            , 所以返回结果中只有图片和文本文件(限于OVER_LIMIT_EXCEPT_OVERFLOW)
         */
        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()//默认是单选false

            /*
            实际最少数量限制为 setMinCount 和 (optionsImage.minCount + optionsAudio.minCount +...) 中的最小值
            实际最大数量限制为 setMaxCount 和 (optionsImage.maxCount + optionsAudio.maxCount +...) 中的最大值, 所以此处的最大值限制是无效的
             */
            .setMinCount(1, "设定类型文件至少选择一个!")
            .setMaxCount(4, "最多选四个文件!")

            /*
            实际单文件大小限制为 setSingleFileMaxSize 和 (optionsImage.singleFileMaxSize + optionsAudio.singleFileMaxSize +...) 中的最小值
            实际总大小限制为 setAllFilesMaxSize 和 (optionsImage.allFilesMaxSize + optionsAudio.allFilesMaxSize +...) 中的最大值
             */
            // 优先使用 `自定义FileSelectOptions` 中设置的单文件大小限制, 如果没有设置则采用该值
            .setSingleFileMaxSize(2097152, "单文件大小不能超过2M！")
            .setAllFilesMaxSize(52428800, "总文件大小不能超过50M！")

            //1. 文件超过限制数量或大小
            //2. 单一类型: 保留未超限制的文件并返回, 去掉后面溢出的部分; 多种类型: 保留正确的文件, 去掉错误类型的所有文件
            .setOverLimitStrategy(this.mOverLimitStrategy)
            //eg: ando.file.core.FileMimeType
            .setMimeTypes("audio/*", "image/*", "text/*")//默认不做文件类型约束为"*/*", 不同类型系统提供的选择UI不一样 eg: "video/*","audio/*","image/*"
            //如果setMimeTypes和applyOptions没对应上会出现`文件类型不匹配问题`
            .applyOptions(optionsImage, optionsAudio, optionsTxt)

            //优先使用 FileSelectOptions 中设置的 FileSelectCondition
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                        FileType.AUDIO -> true
                        FileType.TXT -> true
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