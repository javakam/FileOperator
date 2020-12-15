package com.ando.file.sample.ui.selector

import ando.file.androidq.FileOperatorQ.getBitmapFromUri
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_ALL_EXCEPT
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.selector.*
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import java.io.File
import java.util.*

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

    private lateinit var mTvCurrStrategy: TextView
    private lateinit var mTvResult: TextView
    private lateinit var mTvError: TextView
    private lateinit var mRgStrategy: RadioGroup

    private var mFileSelector: FileSelector? = null

    //返回值策略
    private var mOverSizeStrategy: Int = OVER_SIZE_LIMIT_ALL_EXCEPT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_multi_image)
        PermissionManager.requestStoragePermission(this)
        mTvCurrStrategy= findViewById(R.id.tv_curr_strategy)
        mTvResult = findViewById(R.id.tv_result)
        mTvError = findViewById(R.id.tv_error)
        mRgStrategy = findViewById(R.id.rg_strategy)

        title = "多选图片"

        //策略切换
        mRgStrategy.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_strategy1 -> {
                    this.mOverSizeStrategy = OVER_SIZE_LIMIT_ALL_EXCEPT
                    mTvCurrStrategy.text="当前策略: OVER_SIZE_LIMIT_ALL_EXCEPT"
                }
                R.id.rb_strategy2 -> {
                    this.mOverSizeStrategy = OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
                    mTvCurrStrategy.text="当前策略: OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART"
                }
                else -> {
                }
            }
        }
        mTvCurrStrategy.text="当前策略: OVER_SIZE_LIMIT_ALL_EXCEPT"

        findViewById<Button>(R.id.bt_select_multi).setOnClickListener {
            chooseFile()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError, mTvResult)
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
            .setMultiSelect()//默认是单选 false
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(10, "最多选十个文件!")

            //优先使用 FileSelectOptions.singleFileMaxSize , 单位 Byte
            .setSingleFileMaxSize(3145728, "单个大小不能超过3M！")
            .setAllFilesMaxSize(20971520, "总文件大小不能超过20M！")

            //1.OVER_SIZE_LIMIT_ALL_EXCEPT            文件超过限制大小直接返回失败(onError)
            //2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART  文件超过限制大小保留未超限制的文件并返回,去掉后面溢出的部分(onSuccess)
            .setOverSizeLimitStrategy(this.mOverSizeStrategy)
            .setMimeTypes("image/*")//默认不做文件类型约束,不同类型系统提供的选择UI不一样 eg: arrayOf("video/*","audio/*","image/*")
            .applyOptions(optionsImage)

            //优先使用 FileSelectOptions 中设置的 FileSelectCondition
            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> {
                            return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                        }
                        FileType.VIDEO -> false
                        FileType.AUDIO -> false
                        else -> false
                    }
                }
            })
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    ResultUtils.resetUI(mTvResult)
                    if (results.isNullOrEmpty()) {
                        toastShort("没有选取文件")
                        return
                    }

                    showSelectResult(results)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("FileSelectCallBack onError ${e?.message}")
                    ResultUtils.setErrorText(mTvError, e)
                }
            })
            .choose()
    }

    private fun showSelectResult(results: List<FileSelectResult>) {
        ResultUtils.setErrorText(mTvError, null)
        ResultUtils.setFormattedResults(tvResult = mTvResult, results = results)

        val photos = mutableListOf<Uri>()
        results.forEach {
            val uri = it.uri ?: return@forEach
            when (FileType.INSTANCE.typeByUri(uri)) {
                FileType.IMAGE -> {
                    //原图
                    //val bitmap = getBitmapFromUri(uri)
                    //mIvOrigin.setImageBitmap(bitmap)
                    //压缩
                    photos.add(uri)
                }
                else -> {
                }
            }
        }

        //or Engine.compress(uri,  100L)
        compressImage(this, photos) { uri ->
            FileLogger.i(
                "compressImage onSuccess  uri=$uri  path=${uri?.path}  " +
                        "压缩图片缓存目录总大小=${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}"
            )

            ResultUtils.formatCompressedImageInfo(uri) {
                mTvResult.text = mTvResult.text.toString().plus(it)
            }

            //mIvCompressed.setImageBitmap(bitmap)
        }
    }

}