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
import ando.file.core.FileGlobal.dumpMetaData
import ando.file.core.FileMimeType.MIME_MEDIA
import ando.file.core.FileUri.getFilePathByUri
import ando.file.selector.*
import com.ando.file.sample.R
import com.ando.file.sample.getPathImageCache
import com.ando.file.sample.utils.PermissionManager
import kotlinx.android.synthetic.main.activity_file_operator.*
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Title: FileSelectImageSingleActivity
 *
 * Description:
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
@Suppress("UNUSED_PARAMETER")
@SuppressLint("SetTextI18n")
class FileSelectSingleImageActivity : AppCompatActivity() {

    val REQUEST_CHOOSE_FILE = 10

    //文件选择
    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_operator)
        PermissionManager.verifyStoragePermissions(this)

        title = "单选图片"

        mBtChooseSingle.visibility = View.VISIBLE
        mBtChooseSingle.setOnClickListener {
            chooseFile()
        }

    }

    private fun chooseFile() {

        //FileOptions T 为 String.filePath / Uri / File
        val optionsImage = FileSelectOptions()
        optionsImage.fileType = FileType.IMAGE
//        options.mMinCount = 0
//        options.mMaxCount = 10
        optionsImage.mSingleFileMaxSize = 2097152  // 20M = 20971520 B
        optionsImage.mSingleFileMaxSizeTip = "图片最大不超过2M！"
        optionsImage.mAllFilesMaxSize = 5242880  //5M 5242880 ; 20M = 20971520 B
        optionsImage.mAllFilesMaxSizeTip = "总图片大小不超过5M！"
        optionsImage.mFileCondition = object : FileSelectCondition {
            override fun accept(fileType: FileType, uri: Uri?): Boolean {
                return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
            }
        }

        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setSelectMode(false)
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(10, "最多选十个文件!")
            .setSingleFileMaxSize(5242880, "大小不能超过5M！") //5M 5242880 ; 100M = 104857600 KB
            .setAllFilesMaxSize(10485760, "总大小不能超过10M！")//
            .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同 arrayOf("video/*","audio/*","image/*") 系统提供的选择UI不一样
            .applyOptions(optionsImage)

            //优先使用 FileOptions 中设置的 FileSelectCondition
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

                    shortToast("正在压缩图片...")
                    showSelectResult(results)
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("回调 onError ${e?.message}")
                    mTvResultError.text =
                        mTvResultError.text.toString().plus(" 错误信息: ${e?.message} \n")
                }
            })
            .choose()
    }

    private fun showSelectResult(results: List<FileSelectResult>) {

        mTvResult.text = ""
        results.forEach {
            val info = "${it.toString()}格式化 : ${FileSizeUtils.formatFileSize(it.fileSize)}\n"
            FileLogger.w("FileOptions onSuccess  $info")
            //Caused by: java.util.MissingFormatArgumentException: Format specifier '%3A'

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
                    val bitmap = getBitmapFromUri(uri)
                    //原图
                    mIvOrigin.setImageBitmap(bitmap)
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
                    FileLogger.i(
                        "compress onSuccess  uri=$uri  path=${uri?.path}  缓存目录总大小=${
                            FileSizeUtils.getFolderSize(
                                File(path)
                            )
                        }"
                    )

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