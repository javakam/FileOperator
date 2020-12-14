package com.ando.file.sample.ui.selector

import ando.file.compressor.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.*
import ando.file.core.FileMimeType.MIME_MEDIA
import ando.file.core.FileUri.getFilePathByUri
import ando.file.selector.*
import android.widget.ImageView
import android.widget.TextView
import com.ando.file.sample.R
import com.ando.file.sample.getCompressedImageCacheDir
import com.ando.file.sample.toastShort
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Title: FileSelectSingleImageActivity
 *
 * Description: 单选图片
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
@Suppress("UNUSED_PARAMETER")
class FileSelectSingleImageActivity : AppCompatActivity() {

    private lateinit var mBtSelectSingle: View
    private lateinit var mTvError: TextView
    private lateinit var mTvResult: TextView
    private lateinit var mIvOrigin: ImageView
    private lateinit var mIvCompressed: ImageView

    //文件选择
    private val REQUEST_CHOOSE_FILE = 10
    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_select_single)
        mBtSelectSingle = findViewById(R.id.bt_select_single)
        mTvError = findViewById(R.id.tv_error)
        mTvResult = findViewById(R.id.tv_result)
        mIvOrigin = findViewById(R.id.iv_origin)
        mIvCompressed = findViewById(R.id.iv_compressed)

        title = "单选图片"

        mBtSelectSingle.visibility = View.VISIBLE
        mBtSelectSingle.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError, mTvResult, mIvOrigin, mIvCompressed)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    private fun chooseFile() {
        /*
        说明:
            FileOptions T 为 String.filePath / Uri / File
            3M 3145728 Byte ; 5M 5242880 Byte; 10M 10485760 ; 20M = 20971520 Byte
         */
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "文件类型不匹配"
            singleFileMaxSize = 3145728
            singleFileMaxSizeTip = "图片最大不超过3M！"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "总图片大小不超过10M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setTypeMismatchTip("文件类型不匹配") //会覆盖 FileSelectOptions 中的 fileTypeMismatchTip
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(10, "最多选十个文件!")
            .setSingleFileMaxSize(5242880, "大小不能超过5M！") //5M 5242880 ; 100M = 104857600 Byte
            .setAllFilesMaxSize(10485760, "总大小不能超过10M！")//
            .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同类型系统提供的选择UI不一样 eg:  arrayOf("video/*","audio/*","image/*")
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
        compressImage(photos)//or Engine.compress(uri,  100L)
    }


    /**
     * 压缩图片 1.Luban算法; 2.直接压缩 -> Engine.compress(uri,  100L)
     *
     * T 为 String.filePath / Uri / File
     */
    private fun <T> compressImage(photos: List<T>) {
        ImageCompressor
            .with(this)
            .load(photos)
            .ignoreBy(100)//单位 Byte
            .setTargetDir(getCompressedImageCacheDir())
            .setFocusAlpha(false)
            .enableCache(true)
            .filter(object : ImageCompressPredicate {
                override fun apply(uri: Uri?): Boolean {
                    //FileLogger.i("image predicate $uri  ${getFilePathByUri(uri)}")
                    return if (uri != null) !FileUtils.getExtension(uri).endsWith("gif") else false
                }
            })
            .setRenameListener(object : OnImageRenameListener {
                override fun rename(uri: Uri?): String? {
                    try {
                        val filePath = getFilePathByUri(uri)
                        val md = MessageDigest.getInstance("MD5")
                        md.update(filePath?.toByteArray() ?: return "")
                        return BigInteger(1, md.digest()).toString(32)
                    } catch (e: NoSuchAlgorithmException) {
                        e.printStackTrace()
                    }
                    return ""
                }
            })
            .setImageCompressListener(object : OnImageCompressListener {
                override fun onStart() {}
                override fun onSuccess(uri: Uri?) {
                    FileLogger.i(
                        "compressImage onSuccess  uri=$uri  path=${uri?.path}  " +
                                "压缩图片缓存目录总大小=${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}"
                    )

                    ResultUtils.formatCompressedImageInfo(uri) {
                        mTvResult.text = mTvResult.text.toString().plus(it)
                    }

                    ResultUtils.setImageEvent(mIvCompressed, uri)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("compressImage onError ${e?.message}")
                }
            }).launch()
    }

}