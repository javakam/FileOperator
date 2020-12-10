package com.ando.file.sample.ui.selector

import ando.file.androidq.FileOperatorQ.getBitmapFromUri
import ando.file.androidq.FileOperatorQ.loadThumbnail
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.*
import ando.file.compressor.ImageCompressPredicate
import ando.file.compressor.OnImageCompressListener
import ando.file.compressor.OnImageRenameListener
import ando.file.compressor.ImageCompressor
import com.ando.file.sample.R
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_ALL_DONT
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.core.FileGlobal.dumpMetaData
import ando.file.core.FileMimeType.MIME_MEDIA
import ando.file.core.FileUri.getFilePathByUri
import ando.file.selector.*
import com.ando.file.sample.getPathImageCache
import com.ando.file.sample.toastShort
import com.ando.file.sample.utils.PermissionManager
import kotlinx.android.synthetic.main.activity_file_operator.*
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
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

    private val REQUEST_CHOOSE_FILE = 10

    //文件选择
    private var mFileSelector: FileSelector? = null

    //返回值策略
    private var mOverSizeStrategy: Int = OVER_SIZE_LIMIT_ALL_DONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_operator)
        PermissionManager.verifyStoragePermissions(this)

        title = "多选图片"

        mBtChooseMulti.visibility = View.VISIBLE
        mBtChooseMulti.setOnClickListener {
            this.mOverSizeStrategy = OVER_SIZE_LIMIT_ALL_DONT
            chooseFile()
        }

        mBtChooseMulti2.visibility = View.VISIBLE
        mBtChooseMulti2.setOnClickListener {
            this.mOverSizeStrategy = OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
            chooseFile()
        }
    }

    private fun chooseFile() {
        /*
        说明:
            FileOptions T 为 String.filePath / Uri / File
            3M 3145728 Byte ; 5M 5242880 Byte; 10M 10485760 ; 20M = 20971520 Byte
         */
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            //maxCount = 2
            singleFileMaxSize = 3145728
            singleFileMaxSizeTip = "单张图片最大不超过3M！"
            allFilesMaxSize = 5242880
            allFilesMaxSizeTip = "图片总大小不超过5M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setSelectMode(true)
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(10, "最多选十个文件!")
            //优先以自定义的 optionsImage.mSingleFileMaxSize , 单位 Byte
            .setSingleFileMaxSize(2097152, "单个大小不能超过2M！")
            .setAllFilesMaxSize(20971520, "总文件大小不能超过20M！")

            //1.OVER_SIZE_LIMIT_ALL_DONT  超过限制大小全部不返回  ; 2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART  超过限制大小去掉后面相同类型文件
            .setOverSizeLimitStrategy(this.mOverSizeStrategy)
            .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同类型系统提供的选择UI不一样 eg:  arrayOf("video/*","audio/*","image/*")
            .applyOptions(optionsImage)

            //优先使用 FileSelectOptions 中设置的 FileSelectCondition
            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> {
                            return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                        }
                        FileType.VIDEO -> true
                        FileType.AUDIO -> true
                        else -> true
                    }
                }
            })
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    FileLogger.w("回调 onSuccess ${results?.size}")
                    mTvResult.text = ""
                    if (results.isNullOrEmpty()) return

                    toastShort("正在压缩图片...")
                    showSelectResult(results)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("回调 onError ${e?.message}")
                    mTvResultError.text = mTvResultError.text.toString().plus(" 错误信息: ${e?.message} \n")
                }
            })
            .choose()
    }

    private fun showSelectResult(results: List<FileSelectResult>) {
        mTvResult.text = ""
        results.forEach {
            val info = "${it.toString()}格式化 : ${FileSizeUtils.formatFileSize(it.fileSize)}\n"
            FileLogger.w("FileOptions onSuccess  \n $info")

            mTvResult.text = mTvResult.text.toString().plus(
                """选择结果 : ${FileType.INSTANCE.typeByUri(it.uri)} 
                    |---------
                    |👉压缩前
                    |$info
                    |""".trimMargin()
            )
        }

        results.forEach {
            val uri = it.uri ?: return@forEach
            when (FileType.INSTANCE.typeByUri(uri)) {
                FileType.IMAGE -> {
                    //原图
                    val bitmap = getBitmapFromUri(uri)
                    mIvOrigin.setImageBitmap(bitmap)
                    //压缩(Luban)
                    val photos = mutableListOf<Uri>()
                    photos.add(uri)
                    compressImage(photos) //or Engine.compress(uri,  100L)
                }
                FileType.VIDEO -> {
                    loadThumbnail(uri, 100, 200)?.let { b -> mIvOrigin.setImageBitmap(b) }
                }
                else -> {
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mTvResultError.text = ""
        mTvResult.text = ""
        mIvOrigin.setImageBitmap(null)
        mIvCompressed.setImageBitmap(null)

        mFileSelector?.obtainResult(requestCode, resultCode, data)
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
            .ignoreBy(100)//Byte
            .setTargetDir(getPathImageCache())
            .setFocusAlpha(false)
            .enableCache(true)
            .filter(object : ImageCompressPredicate {
                override fun apply(uri: Uri?): Boolean {
                    FileLogger.i("image predicate $uri  ${getFilePathByUri(uri)}")
                    return if (uri != null) {
                        val path = getFilePathByUri(uri)
                        !(TextUtils.isEmpty(path) || (path?.toLowerCase(Locale.getDefault())?.endsWith(".gif") == true))
                    } else false
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
                    val path = "$cacheDir/image/"
                    FileLogger.i("compress onSuccess  uri=$uri  path=${uri?.path}  缓存目录总大小=${FileSizeUtils.getFolderSize(File(path))}")

                    val bitmap = getBitmapFromUri(uri)
                    dumpMetaData(uri) { displayName: String?, size: String? ->
                        runOnUiThread {
                            mTvResult.text = mTvResult.text.toString().plus(
                                "\n ---------\n👉压缩后 \n Uri : $uri \n 路径: ${uri?.path} \n 文件名称 ：$displayName \n 大小：$size B \n" +
                                        "格式化 : ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}\n ---------"
                            )
                        }
                    }
                    mIvCompressed.setImageBitmap(bitmap)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("compress onError ${e?.message}")
                }
            }).launch()
    }

}