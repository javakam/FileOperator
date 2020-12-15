package com.ando.file.sample.ui.selector

import ando.file.androidq.FileOperatorQ.getBitmapFromUri
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ando.file.compressor.ImageCompressPredicate
import ando.file.core.*
import ando.file.compressor.OnImageCompressListener
import ando.file.compressor.OnImageRenameListener
import ando.file.compressor.ImageCompressor
import com.ando.file.sample.R
import ando.file.core.FileGlobal.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART
import ando.file.core.FileGlobal.dumpMetaData
import ando.file.core.FileOpener.openFileBySystemChooser
import ando.file.core.FileUri.getFilePathByUri
import ando.file.selector.*
import com.ando.file.sample.getCompressedImageCacheDir
import com.ando.file.sample.utils.PermissionManager
import kotlinx.android.synthetic.main.activity_file_operator.*
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Title: FileSelectFilesMultiActivity
 *
 * Description: 多选文件
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
@SuppressLint("SetTextI18n")
class FileSelectMultiFilesActivity : AppCompatActivity() {

    private val REQUEST_CHOOSE_FILE = 10

    //文件选择
    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_operator)
        PermissionManager.requestStoragePermission(this)
        title = "多选文件"

        mBtOpenMediaFile.visibility = View.VISIBLE

        mBtChooseMultiFiles.visibility = View.VISIBLE
        mBtChooseMultiFiles.setOnClickListener {
            chooseFile()
        }

    }

    private fun chooseFile() {
        /*
       说明:
           FileOptions T 为 String.filePath / Uri / File
           3M 3145728 Byte ; 5M 5242880 Byte; 10M 10485760 ; 20M = 20971520 Byte
           50M 52428800 Byte ; 80M 83886080 ; 100M = 104857600 Byte
        */

        //图片
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            maxCount = 2
            minCountTip = "至少选择一张图片"
            maxCountTip = "最多选择两张图片"
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

        //视频
        val optionsVideo = FileSelectOptions().apply {
            fileType = FileType.VIDEO
            maxCount = 1
            minCountTip = "至少选择一个视频文件"
            maxCountTip = "最多选择一个视频文件"
            singleFileMaxSize = 20971520
            singleFileMaxSizeTip = "单视频最大不超过20M！"
            allFilesMaxSize = 31457280
            allFilesMaxSizeTip = "视频总大小不超过30M！"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (uri != null)
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setMultiSelect()//默认是单选 false
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(5, "最多选五个文件!")

            // 优先使用自定义 FileSelectOptions 中设置的单文件大小限制,如果没有设置则采用该值
            .setSingleFileMaxSize(2097152, "单文件大小不能超过2M！")
            .setAllFilesMaxSize(52428800, "总文件大小不能超过50M！")

            // 超过限制大小两种返回策略: 1.OVER_SIZE_LIMIT_ALL_EXCEPT,超过限制大小全部不返回;2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART,超过限制大小去掉后面相同类型文件
            .setOverSizeLimitStrategy(OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART)
            .setMimeTypes(null)//默认为 null,*/* 即不做文件类型限定;MIME_MEDIA 媒体文件,不同类型系统提供的选择UI不一样 eg:  arrayOf("video/*","audio/*","image/*")
            .applyOptions(optionsImage, optionsVideo)

            // 优先使用 FileSelectOptions 中设置的 FileSelectCondition,没有的情况下才使用通用的
            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
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

                    showSelectResult(results)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("回调 onError ${e?.message}")
                    mTvError.text = mTvError.text.toString().plus(" 错误信息: ${e?.message} \n")
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
                    |👉原文件
                    |$info
                    |""".trimMargin()
            )
        }
        //测试打开音视频文件
        mBtOpenMediaFile.setOnClickListener {
            openFileBySystemChooser(this, results[0].uri)
        }

        results.forEach {
            val uri = it.uri ?: return@forEach
            when (FileType.INSTANCE.typeByUri(uri)) {
                FileType.IMAGE -> {
                    //原图
                    val bitmap = getBitmapFromUri(uri)
                    mIvOrigin.setImageBitmap(bitmap)
                    mIvOrigin.setOnClickListener {
                        openFileBySystemChooser(this, uri)
                    }
                    //压缩(Luban)
                    val photos = mutableListOf<Uri>()
                    photos.add(uri)
                    compressImage(photos) //or Engine.compress(uri,  100L)
                }
                FileType.VIDEO -> {
                    //loadThumbnail(uri, 100, 200)?.let { b -> mIvOrigin.setImageBitmap(b) }
                }
                else -> {
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mTvError.text = ""
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
            .ignoreBy(100)//B
            .setTargetDir(getCompressedImageCacheDir())
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
                    FileLogger.i("compress onSuccess  uri=$uri  path=${uri?.path}  压缩图片缓存目录总大小=${FileSizeUtils.getFolderSize(File(path))}")

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