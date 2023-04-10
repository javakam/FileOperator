package com.ando.file.sample.ui.selector

import ando.file.core.*
import ando.file.core.FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW
import ando.file.selector.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import com.ando.file.sample.utils.noShake
import java.io.File

/**
 * # FileSelectSingleImageActivity
 *
 * Description: 单选图片 (Single selection picture)
 *
 * @author javakam
 * @date 2020/5/19  16:04
 */
class FileSelectSingleImageActivity : AppCompatActivity() {

    private lateinit var mBtSelectSingle: View
    private lateinit var mBtDelete: View
    private lateinit var mTvError: TextView
    private lateinit var mTvResult: TextView
    private lateinit var mIvOrigin: ImageView
    private lateinit var mIvCompressed: ImageView

    private var mFileSelector: FileSelector? = null
    private var currUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_single_image)
        mBtSelectSingle = findViewById(R.id.bt_select_single)
        mBtDelete = findViewById(R.id.bt_delete)
        mTvError = findViewById(R.id.tv_error)
        mTvResult = findViewById(R.id.tv_result)
        mIvOrigin = findViewById(R.id.iv_origin)
        mIvCompressed = findViewById(R.id.iv_compressed)

        title = "单选图片(Single selection picture)"

        mBtSelectSingle.noShake {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()

                //测试分享 (Test sharing)
                /*if (it){
                    mFileSelector = FileSelector.with(this)
                        .setExtraMimeTypes("application/json")
                        .callback(object : FileSelectCallBack {
                            override fun onError(e: Throwable?) {
                                Log.e("123", e?.toString())
                            }
                            override fun onSuccess(results: List<FileSelectResult>?) {
                                results?.firstOrNull()?.apply {
                                    Log.e("123", "${this.uri} ${FileMimeType.getMimeType(uri)}")
                                    showSelectResult(results)
                                    mTvResult.setOnClickListener {
                                        uri?.apply {
                                            FileOpener.openShare(this@FileSelectSingleImageActivity,this)
                                        }
                                    }
                                }
                            }
                        })
                        .choose()
                }*/

            }
        }

        //todo 2022年1月21日 17:43:34  批量删除
        //删除该文件
        //用于测试 👉 https://github.com/javakam/FileOperator/issues/70
        mBtDelete.visibility = View.VISIBLE
        mBtDelete.noShake {
            FileLogger.w("删除..... $currUri")

            currUri?.apply {
                //val result: Int = FileUtils.deleteFile(this)
                //FileLogger.w("删除结果..... $result")

                val result2: Boolean = MediaStoreUtils.deleteFile(this)
                if (result2) {
                    toastLong("删除成功!")
                    recreate()
                } else {
                    toastLong("删除失败!")
                }
            }
        }
    }

    /*
    注: 使用 ActivityResultLauncher 启动页面, 会先后回调 onActivityResult 和 ActivityResultCallback.onActivityResult,
        建议在 ActivityResultCallback.onActivityResult 中处理结果
     */

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //同下
    }

    //v3.0.0 开始使用 ActivityResultLauncher 跳转页面
    private val mStartForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            FileLogger.w("Back ok -> ActivityResultCallback")
            handleResult(REQUEST_CHOOSE_FILE, result.resultCode, result.data)
        }

    override fun onDestroy() {
        mStartForResult.unregister()
        super.onDestroy()
    }

    private fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ResultUtils.resetUI(mTvError, mTvResult, mIvOrigin, mIvCompressed)
        //选择结果交给 FileSelector 处理, 可通过`requestCode -> REQUEST_CHOOSE_FILE`进行区分
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    private fun chooseFile() {
        //测试Android11文件管理页面样式 2022年1月21日 17:36:05
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
//            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
//            //1
//            //setTypeAndNormalize("*/*")
//            //putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
//
//            //2 效果同1完全一致!!!
//            setTypeAndNormalize("image/*")
//            addCategory(Intent.CATEGORY_OPENABLE)
//        }
//        startActivity(intent)
//        return

        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "File type mismatch !"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "The largest picture does not exceed 5M !"
            allFilesMaxSize = 104857600
            //单选条件下无效,只做单个图片大小判断
            //EN:Invalid under single selection conditions, only single image size judgment
            allFilesMaxSizeTip = "The total picture size does not exceed 100M !"

            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    //Filter out gif
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(this, launcher = mStartForResult)
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setTypeMismatchTip("File type mismatch !")
            .setMinCount(1, "Choose at least one file !")
            .setMaxCount(10, "Choose up to ten files !")//单选条件下无效, 只做最少数量判断 Invalid under single selection condition, only judge the minimum number
            .setOverLimitStrategy(OVER_LIMIT_EXCEPT_OVERFLOW)
            .setSingleFileMaxSize(10485760, "The size cannot exceed 10M !")//单选条件下无效, 使用 FileSelectOptions.singleFileMaxSize
            .setAllFilesMaxSize(104857600, "Total size cannot exceed 100M !")//单选条件下无效, 只做单个图片大小判断 setSingleFileMaxSize
            .setExtraMimeTypes("image/*")
            .applyOptions(optionsImage)

            //优先使用 FileSelectOptions 中设置的 FileSelectCondition
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
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
                        toastLong("No file selected")
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
        currUri = null
        ResultUtils.setErrorText(mTvError, null)
        ResultUtils.setFormattedResults(tvResult = mTvResult, results = results)

        currUri = results[0].uri
        //Original image
        ResultUtils.setImageEvent(mIvOrigin, currUri)
        //Compress
        val photos = listOf(currUri)
        FileLogger.e("uri=$currUri")

        //or val bitmap:Bitmap=ImageCompressEngine.compressPure(uri)
        compressImage(this, photos) { _, u ->
            FileLogger.i(
                "compressImage onSuccess uri=$u " +
                        "${getString(R.string.str_ando_file_compress_dir_size)} = ${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}"
            )

            ResultUtils.formatCompressedImageInfo(u, false) {
                mTvResult.text = mTvResult.text.toString().plus(it)
            }

            ResultUtils.setImageEvent(mIvCompressed, u)
        }
    }

}