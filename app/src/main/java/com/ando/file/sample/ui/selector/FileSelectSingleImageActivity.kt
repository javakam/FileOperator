package com.ando.file.sample.ui.selector

import ando.file.compressor.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.*
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.selector.*
import android.widget.ImageView
import android.widget.TextView
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import java.io.File
import java.util.*

/**
 * Title: FileSelectSingleImageActivity
 *
 * Description: 单选图片
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
class FileSelectSingleImageActivity : AppCompatActivity() {

    private lateinit var mBtSelectSingle: View
    private lateinit var mTvError: TextView
    private lateinit var mTvResult: TextView
    private lateinit var mIvOrigin: ImageView
    private lateinit var mIvCompressed: ImageView

    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_single_image)
        mBtSelectSingle = findViewById(R.id.bt_select_single)
        mTvError = findViewById(R.id.tv_error)
        mTvResult = findViewById(R.id.tv_result)
        mIvOrigin = findViewById(R.id.iv_origin)
        mIvCompressed = findViewById(R.id.iv_compressed)

        title = "单选图片"

        mBtSelectSingle.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError, mTvResult, mIvOrigin, mIvCompressed)
        //选择结果交给 FileSelector 处理, 可通过`requestCode -> REQUEST_CHOOSE_FILE`进行区分
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    /*
    字节码计算器 -> https://calc.itzmx.com/
       3M  = 3145728  Byte
       5M  = 5242880  Byte
       10M = 10485760 Byte
       20M = 20971520 Byte
    */
    private fun chooseFile() {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "文件类型不匹配"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "图片最大不超过5M！"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "总图片大小不超过10M！"//单选条件下无效,只做单个图片大小判断
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setTypeMismatchTip("文件类型不匹配")
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(10, "最多选十个文件!")//单选条件下无效, 只做最少数量判断
            .setOverSizeLimitStrategy(OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART)
            .setSingleFileMaxSize(1048576, "大小不能超过1M！")//单选条件下无效, FileSelectOptions.singleFileMaxSize
            .setAllFilesMaxSize(10485760, "总大小不能超过10M！")//单选条件下无效,只做单个图片大小判断 setSingleFileMaxSize
            .setMimeTypes("image/*")//默认不做文件类型约束,不同类型系统提供的选择UI不一样 eg: arrayOf("video/*","audio/*","image/*")
            .applyOptions(optionsImage)

            //优先使用 FileSelectOptions 中设置的 FileSelectCondition
            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
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
                    ResultUtils.setImageEvent(mIvOrigin, uri)
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

            ResultUtils.setImageEvent(mIvCompressed, uri)
        }
    }

}