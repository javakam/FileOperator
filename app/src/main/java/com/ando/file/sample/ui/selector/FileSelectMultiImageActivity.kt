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
import com.ando.file.sample.utils.ResultUtils.ResultShowBean
import com.ando.file.sample.utils.ResultUtils.asVerticalList
import java.io.File

/**
 * # FileSelectMultiImageActivity
 *
 * Description: 多选图片 (Multiple selection pictures)
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
@SuppressLint("SetTextI18n")
class FileSelectMultiImageActivity : AppCompatActivity() {

    private val mShowText: String by lazy { getString(R.string.str_ando_file_select_multiple_and_compress) }
    private lateinit var mTvCurrStrategy: TextView
    private lateinit var mRgStrategy: RadioGroup
    private lateinit var mBtSelect: Button
    private lateinit var mTvError: TextView
    private lateinit var mRvResults: RecyclerView

    private var mOverLimitStrategy: Int = OVER_LIMIT_EXCEPT_ALL

    //展示结果(Show results)
    private var mResultShowList: MutableList<ResultShowBean>? = null
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

        title = "多选图片(Multiple selection pictures)"

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

        mBtSelect.text="$mShowText (0)"
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

    private fun chooseFile() {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "File type mismatch !"
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

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()
            .setMinCount(1, "Choose at least one picture!")
            .setMaxCount(10, "Choose up to ten pictures!")
            .setSingleFileMaxSize(3145728, "The size of a single picture cannot exceed 3M !")
            .setAllFilesMaxSize(20971520, "The total size of the picture does not exceed 20M !")
            .setOverLimitStrategy(this.mOverLimitStrategy)
            .setMimeTypes("image/*")
            .applyOptions(optionsImage)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE) && (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
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
                ResultShowBean(originUri = p.first, originResult = p.second)
            }.toMutableList()
        }

        //List<FileSelectResult> -> List<Uri>
        val photos: List<Uri> = results
            .filter { (it.uri != null) && (FileType.INSTANCE.fromUri(it.uri) == FileType.IMAGE) }
            .map {
                it.uri!!
            }

        var count = 0
        compressImage(this, photos) { index, u ->
            FileLogger.i("compressImage onSuccess index=$index uri=$u " +
                    "Total size of compressed image cache directory = ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}"
            )

            ResultUtils.formatCompressedImageInfo(u, true) {
                if (index != -1) {
                    mResultShowList?.get(index)?.compressedResult = it
                    mResultShowList?.get(index)?.compressedUri = u
                    count++
                }
            }

            //建议加个加载中的弹窗 (It is recommended to add a loading dialog)
            if (count == mResultShowList?.size ?: 0) {
                mAdapter.setData(mResultShowList)
            }
        }
    }

}