package com.ando.file.sample.ui.selector

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_ALL_EXCEPT
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.selector.*
import android.view.View
import android.view.ViewGroup
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
class FileSelectMultiImageActivity : AppCompatActivity() {

    private lateinit var mTvCurrStrategy: TextView
    private lateinit var mRgStrategy: RadioGroup
    private lateinit var mBtSelect: Button
    private lateinit var mTvError: TextView
    private lateinit var mRvResults: RecyclerView

    private var mFileSelector: FileSelector? = null

    private var mOverSizeStrategy: Int = OVER_SIZE_LIMIT_ALL_EXCEPT

    //展示结果(Show results)
    private var mResultShowList: MutableList<ResultShowBean>? = null
    private val mAdapter: FileSelectResultAdapter by lazy { FileSelectResultAdapter() }

    @SuppressLint("SetTextI18n")
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
                    this.mOverSizeStrategy = OVER_SIZE_LIMIT_ALL_EXCEPT
                    mTvCurrStrategy.text = "当前策略: OVER_SIZE_LIMIT_ALL_EXCEPT"
                }
                R.id.rb_strategy2 -> {
                    this.mOverSizeStrategy = OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
                    mTvCurrStrategy.text = "当前策略: OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART"
                }
                else -> {
                }
            }
        }
        mTvCurrStrategy.text = "当前策略: ${
            if (this.mOverSizeStrategy == OVER_SIZE_LIMIT_ALL_EXCEPT) "OVER_SIZE_LIMIT_ALL_EXCEPT"
            else "OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART"
        }"

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
            .setMultiSelect()//默认是单选false, 当applyOptions>=2时,会按照多选处理
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(2, "最多选两个文件!")

            //优先使用 FileSelectOptions.singleFileMaxSize , 单位 Byte
            .setSingleFileMaxSize(3145728, "单个大小不能超过3M！")
            .setAllFilesMaxSize(20971520, "总文件大小不能超过20M！")

            //1.OVER_SIZE_LIMIT_ALL_EXCEPT            文件超过数量限制和大小限制直接返回失败(onError)
            //2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART  文件超过数量限制和大小限制保留未超限制的文件并返回,去掉后面溢出的部分(onSuccess)
            .setOverSizeLimitStrategy(this.mOverSizeStrategy)
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
                    mBtSelect.text = "选择多张图片并压缩 (0)"
                }
            })
            .choose()
    }

    @SuppressLint("SetTextI18n")
    private fun showSelectResult(results: List<FileSelectResult>) {
        ResultUtils.setErrorText(mTvError, null)
        mBtSelect.text = "选择多张图片并压缩 (${results.size})"
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
        //or Engine.compress(uri,  100L)
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

    inner class FileSelectResultAdapter : RecyclerView.Adapter<FileSelectResultAdapter.SelectResultHolder>() {

        private var mData: MutableList<ResultShowBean>? = null

        fun setData(data: MutableList<ResultShowBean>?) {
            if (this.mData?.isNotEmpty() == true) this.mData?.clear()
            this.mData = data
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectResultHolder {
            return SelectResultHolder(layoutInflater.inflate(R.layout.item_select_image_result, parent, false))
        }

        override fun onBindViewHolder(holder: SelectResultHolder, position: Int) {
            mData?.get(position)?.let { b ->
                holder.tvResult.text = b.originResult
                holder.tvCompressedResult.text = b.compressedResult

                //Event
                ResultUtils.setItemEvent(holder.tvResult, b.originUri, "确定打开原图片?")
                ResultUtils.setItemEvent(holder.tvCompressedResult, b.compressedUri, "确定打开压缩后的图片?")
            }
        }

        override fun getItemCount(): Int = mData?.size ?: 0

        inner class SelectResultHolder(v: View) : RecyclerView.ViewHolder(v) {
            var tvResult: TextView = v.findViewById(R.id.tv_result)
            var tvCompressedResult: TextView = v.findViewById(R.id.tv_result_compressed)
        }
    }

}