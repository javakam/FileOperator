package com.ando.file.sample.ui.selector

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ando.file.compressor.ImageCompressPredicate
import ando.file.core.*
import ando.file.compressor.OnImageCompressListener
import ando.file.compressor.OnImageRenameListener
import ando.file.compressor.ImageCompressor
import com.ando.file.sample.R
import ando.file.androidq.getBitmapFromUri
import ando.file.androidq.loadThumbnail
import ando.file.operator.*
import com.ando.file.sample.getPathImageCache
import com.ando.file.sample.utils.PermissionManager
import kotlinx.android.synthetic.main.activity_file_operator.*
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

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

    val REQUEST_CHOOSE_FILE = 10

    //文件选择
    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_operator)
        PermissionManager.verifyStoragePermissions(this)
        title = "多选文件"

        mBtOpenMediaFile.visibility = View.VISIBLE

        mBtChooseMultiFiles.visibility = View.VISIBLE
        mBtChooseMultiFiles.setOnClickListener {
            chooseFile()
        }

    }

    private fun chooseFile() {

        //图片
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            mMinCount = 1
            mMaxCount = 2
            mMinCountTip = "至少选择一张图片"
            mMaxCountTip = "最多选择两张图片"
            mSingleFileMaxSize = 3145728  // 20M = 20971520 B
            mSingleFileMaxSizeTip = "单张图片最大不超过3M！"
            mAllFilesMaxSize = 5242880  //3M 3145728 ; 5M 5242880 ; 10M 10485760 ; 20M = 20971520 B
            mAllFilesMaxSizeTip = "图片总大小不超过5M！"
            mFileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        //视频
        val optionsVideo = FileSelectOptions().apply {
            fileType = FileType.VIDEO
            mMinCount = 1
            mMaxCount = 1
            mMinCountTip = "至少选择一个视频文件"
            mMaxCountTip = "最多选择一个视频文件"
            mSingleFileMaxSize = 20971520  // 20M = 20971520 B
            mSingleFileMaxSizeTip = "单视频最大不超过20M！"
            mAllFilesMaxSize = 31457280  //3M 3145728 ; 5M 5242880 ; 10M 10485760 ; 20M = 20971520 B
            mAllFilesMaxSizeTip = "视频总大小不超过30M！"
            mFileCondition = object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return (uri != null)
                }
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setSelectMode(true)
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(5, "最多选五个文件!")

            // 优先使用自定义 FileSelectOptions 中设置的单文件大小限制,如果没有设置则采用该值
            // 100M = 104857600 KB  ;80M 83886080 ;50M 52428800 ; 20M 20971520  ;5M 5242880 ;
            .setSingleFileMaxSize(2097152, "单文件大小不能超过2M！")
            .setAllFilesMaxSize(52428800, "总文件大小不能超过50M！")

            // 超过限制大小两种返回策略: 1.OVER_SIZE_LIMIT_ALL_DONT,超过限制大小全部不返回;2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART,超过限制大小去掉后面相同类型文件
            .setOverSizeLimitStrategy(OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART)
            .setMimeTypes(null)//默认为 null,*/* 即不做文件类型限定;  MIME_MEDIA 媒体文件, 不同 arrayOf("video/*","audio/*","image/*") 系统提供的选择UI不一样
            .applyOptions(optionsImage, optionsVideo)

            // 优先使用 FileOptions 中设置的 FileSelectCondition , 没有的情况下才使用通用的
            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return  when (fileType) {
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
            //Caused by: java.util.MissingFormatArgumentException: Format specifier '%3A'

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
                    val bitmap = getBitmapFromUri(uri)
                    //原图
                    mIvOrigin.setImageBitmap(bitmap)
                    mIvOrigin.setOnClickListener {
                        openFileBySystemChooser(this, uri)
                    }

                    //压缩(Luban)
                    val photos = mutableListOf<Uri>()
                    photos.add(uri)

                    compressImage(photos)
                    //or
                    //Engine.compress(uri,  100L)
                }
                FileType.VIDEO -> {
                    loadThumbnail(uri, 100, 200)?.let {
                        mIvOrigin.setImageBitmap(it)
                    }
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
     * 压缩图片 Luban算法
     *
     * or
     * 直接压缩 -> Engine.compress(uri,  100L)
     *
     * T 为 String.filePath / Uri / File
     */
    fun <T> compressImage(photos: List<T>) {
        ImageCompressor
            .with(this)
            .load(photos)
            .ignoreBy(100)//B
            .setTargetDir(getPathImageCache())
            .setFocusAlpha(false)
            .enableCache(true)
            .filter(object : ImageCompressPredicate {
                override fun apply(uri: Uri?): Boolean {
                    //getFilePathByUri(uri)
                    FileLogger.i("image predicate $uri  ${getFilePathByUri(uri)}")
                    return if (uri != null) {
                        val path = getFilePathByUri(uri)
                        !(TextUtils.isEmpty(path) || (path?.toLowerCase()
                            ?.endsWith(".gif") == true))
                    } else {
                        false
                    }
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

                    /*
                    uri=content://com.ando.file.sample.fileProvider/ando_file_repo/temp/image/5ikt5v3j7joe8r472odg6b297a
                    path=/ando_file_repo/temp/image/5ikt5v3j7joe8r472odg6b297a
                    文件名称 ：5ikt5v3j7joe8r472odg6b297a  Size：85608 B

                    uri=content://com.ando.file.sample.fileProvider/ando_file_repo/temp/image/17setspjc1rk0h4lo8kft2et22
                    path=/ando_file_repo/temp/image/17setspjc1rk0h4lo8kft2et22
                    文件名称 ：17setspjc1rk0h4lo8kft2et22  Size：85608 B
                     */

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

    private fun shortToast(msg: String?) {
        Toast.makeText(this, msg ?: return, Toast.LENGTH_SHORT).show()
    }
}