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
 * Title: FileSelectImageMultiActivity
 *
 * Description: 多选图片
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
@SuppressLint("SetTextI18n")
class FileSelectMultiImageActivity : AppCompatActivity() {

    private val mShowText: String = "选择多张图片并压缩"
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

        title = "多选图片"

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
            fileTypeMismatchTip = "文件类型不匹配"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "单张图片最大不超过5M！"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "图片总大小不超过10M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()//默认是单选false
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(2, "最多选两个文件!")

            //优先使用 FileSelectOptions.singleFileMaxSize , 单位 Byte
            .setSingleFileMaxSize(3145728, "单个大小不能超过3M！")
            .setAllFilesMaxSize(20971520, "总文件大小不能超过20M！")

            //1. 文件超过数量限制或大小限制
            //2. 单一类型: 保留未超限制的文件并返回, 去掉后面溢出的部分; 多种类型: 保留正确的文件, 去掉错误类型的所有文件
            .setOverLimitStrategy(this.mOverLimitStrategy)
            .setMimeTypes("image/*")//默认不做文件类型约束,不同类型系统提供的选择UI不一样 eg: arrayOf("video/*","audio/*","image/*")
            .applyOptions(optionsImage)

            //优先使用 FileSelectOptions 中设置的 FileSelectCondition
            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE) && (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
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
                ResultShowBean(originUri = p.first, originResult = p.second)
            }.toMutableList()
        }

        //List<FileSelectResult> -> List<Uri>
        val photos: List<Uri> = results
            .filter { (it.uri != null) && (FileType.INSTANCE.typeByUri(it.uri) == FileType.IMAGE) }
            .map {
                it.uri!!
            }

        var count = 0
        compressImage(this, photos) { index, u ->
            FileLogger.i("compressImage onSuccess index=$index uri=$u " +
                    "压缩图片缓存目录总大小=${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}"
            )

            ResultUtils.formatCompressedImageInfo(u, true) {
                if (index != -1) {
                    mResultShowList?.get(index)?.compressedResult = it
                    mResultShowList?.get(index)?.compressedUri = u
                    count++
                }
            }

            //建议加个加载中的弹窗
            if (count == mResultShowList?.size ?: 0) {
                mAdapter.setData(mResultShowList)
            }
        }
    }

}