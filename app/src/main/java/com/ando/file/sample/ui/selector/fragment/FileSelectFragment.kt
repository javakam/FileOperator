package com.ando.file.sample.ui.selector.fragment

import ando.file.core.*
import ando.file.selector.*
import ando.file.selector.FileType.Companion.supplement
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import java.io.File

/**
 * Usage In Fragment
 *
 * @author javakam
 * @date 2021-1-25 09:48:10
 */
class FileSelectFragment : Fragment() {

    companion object {
        private const val REQUEST_CHOOSE_IMAGE = 10
        private const val REQUEST_CHOOSE_FILE = 11

        fun newInstance(): FileSelectFragment {
            val args = Bundle()
            val fragment = FileSelectFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var mBtChoosePicture: Button
    private lateinit var mBtChooseFile: Button
    private lateinit var mTvError: TextView
    private lateinit var mTvResult: TextView
    private lateinit var mIvOrigin: ImageView
    private lateinit var mIvCompressed: ImageView

    private var mFileSelector: FileSelector? = null
    private var mFileSelectorRequest = REQUEST_CHOOSE_IMAGE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FileLogger.e("getExternalStorageDirectory: ${Environment.getExternalStorageDirectory()}")

        //Public Documents Directory
        val documents: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        FileLogger.e("Directory : ${requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}  ___  $documents")

        FileLogger.d(
            "getExternalStorageState: ${Environment.getExternalStorageState(documents)} \n " +
                    "isExternalStorageEmulated: ${Environment.isExternalStorageEmulated(documents)} \n " +
                    "isExternalStorageRemovable: ${Environment.isExternalStorageRemovable(documents)}"
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            FileLogger.d("isExternalStorageLegacy: ${Environment.isExternalStorageLegacy(documents)}")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            FileLogger.d("isExternalStorageManager: ${Environment.isExternalStorageManager(documents)}")
        }

        documents.listFiles()?.forEach {
            FileLogger.w("${it.name}  ${it.absolutePath}")
        }

        val v = inflater.inflate(R.layout.activity_select_single_image, container, false)
        mBtChoosePicture = v.findViewById(R.id.bt_select_single)
        mBtChooseFile = v.findViewById(R.id.bt_select_file)
        mTvError = v.findViewById(R.id.tv_error)
        mTvResult = v.findViewById(R.id.tv_result)
        mIvOrigin = v.findViewById(R.id.iv_origin)
        mIvCompressed = v.findViewById(R.id.iv_compressed)

        //Choose a picture
        mBtChoosePicture.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                //if (it) chooseFile()
                if (it) {
                    mFileSelectorRequest = REQUEST_CHOOSE_IMAGE
                    mFileSelector = chooseImage(REQUEST_CHOOSE_IMAGE, mCallBack)
                }
            }
        }

        //Choose a file (Not picture)
        mBtChooseFile.visibility = View.VISIBLE
        mBtChooseFile.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) {
                    mFileSelectorRequest = REQUEST_CHOOSE_FILE
                    mFileSelector = chooseFile(REQUEST_CHOOSE_FILE, mCallBack)

                    //Fixed At 2021年11月29日 星期一
                    //#62 https://github.com/javakam/FileOperator/issues/62
                    //testIssue62(REQUEST_CHOOSE_FILE)

                }
            }
        }

        return v
    }

    private val mCallBack = object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            ResultUtils.resetUI(mTvResult)
            if (results.isNullOrEmpty()) {
                activity?.toastLong("No file selected")
                return
            }
            showSelectResult(results)

            when (mFileSelectorRequest) {
                REQUEST_CHOOSE_IMAGE -> activity?.toastShort("Select picture successfully !")
                REQUEST_CHOOSE_FILE -> activity?.toastShort("Select file successfully !")
            }
        }

        override fun onError(e: Throwable?) {
            FileLogger.e("FileSelectCallBack onError ${e?.message}")
            ResultUtils.setErrorText(mTvError, e)
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
            handleResult(com.ando.file.sample.REQUEST_CHOOSE_FILE, result.resultCode, result.data)
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

    /*
    Bytecode calculator -> https://calc.itzmx.com/
       3M  = 3145728  Byte
       5M  = 5242880  Byte
       10M = 10485760 Byte
       20M = 20971520 Byte
    */

    private fun showSelectResult(results: List<FileSelectResult>) {
        ResultUtils.setErrorText(mTvError, null)
        ResultUtils.setFormattedResults(tvResult = mTvResult, results = results)

        val uri = results[0].uri
        //Original image
        ResultUtils.setImageEvent(mIvOrigin, uri)
        //compress
        val photos = listOf(uri)

        //or val bitmap:Bitmap=ImageCompressEngine.compressPure(uri)
        compressImage(requireActivity(), photos) { _, u ->
            FileLogger.i(
                "compressImage onSuccess uri=$u " +
                        "The total size of the compressed image cache directory = " +
                        "${FileSizeUtils.getFolderSize(File(getCompressedImageCacheDir()))}"
            )
            ResultUtils.formatCompressedImageInfo(u, false) {
                mTvResult.text = mTvResult.text.toString().plus(it)
            }
            ResultUtils.setImageEvent(mIvCompressed, u)
        }
    }


    private fun Fragment.chooseImage(requestCode: Int, callback: FileSelectCallBack): FileSelector {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "Tipe file tidak didukung"
            minCount = 1
            maxCount = 1
            minCountTip = "Pilih minimal 1 gambar!"
            maxCountTip = "Pilih minimal 1 gambar!"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }
        return FileSelector
            .with(this, launcher = mStartForResult)
            .setRequestCode(requestCode)
            .setTypeMismatchTip("Tipe file tidak didukung")
            .setMinCount(1, "Pilih minimal 1 gambar!")
            .setMaxCount(1, "Pilih minimal 1 gambar!")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_ALL)
            .setExtraMimeTypes("image/*")
            .applyOptions(optionsImage)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return when (fileType) {
                        FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                        else -> false
                    }
                }
            })
            .callback(callback)
            .choose()
    }

    private fun Fragment.chooseFile(requestCode: Int, callback: FileSelectCallBack): FileSelector {
        val optionsWord = FileSelectOptions().apply {
            fileType = FileType.WORD
            singleFileMaxSize = 5242880 //5M
        }
        val optionsExcel = FileSelectOptions().apply {
            fileType = FileType.EXCEL
            singleFileMaxSize = 5242880
        }
        val optionsPDF = FileSelectOptions().apply {
            fileType = FileType.PDF
            singleFileMaxSize = 5242880
        }
        val optionsPPT = FileSelectOptions().apply {
            fileType = FileType.PPT
            singleFileMaxSize = 5242880
        }
        val optionsTXT = FileSelectOptions().apply {
            fileType = FileType.TXT
            singleFileMaxSize = 5242880
        }
        val optionsZIP = FileSelectOptions().apply {
            fileType = FileType.ZIP
            singleFileMaxSize = 5242880
        }
        return FileSelector
            .with(this)
            .setRequestCode(requestCode)
            .setTypeMismatchTip("Tipe file tidak didukung")
            .setMinCount(1, "Pilih minimal 1 file!")
            .setMaxCount(1, "Pilih minimal 1 file!")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_ALL)
            .setExtraMimeTypes(
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/zip",
                "application/pdf"
            )
            .applyOptions(optionsWord, optionsExcel, optionsPDF, optionsPPT, optionsTXT, optionsZIP)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return if (uri != null && !uri.path.isNullOrBlank()) {
                        when (fileType) {
                            FileType.WORD, FileType.EXCEL, FileType.PDF, FileType.PPT, FileType.TXT, FileType.ZIP -> true
                            else -> false
                        }
                    } else false
                }
            })
            .callback(callback)
            .choose()
    }

    //#62 https://github.com/javakam/FileOperator/issues/62
    fun testIssue62(requestCode: Int) {
        val optionsType = FileSelectOptions().apply {
            fileType = FileType.TXT.supplement("jar", "apk", "doc")
            singleFileMaxSize = 5242880 //5M
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return (fileType == this@apply.fileType && uri != null && !uri.path.isNullOrBlank())
                }
            }
        }

        val extraMimeTypes = arrayOf("text/*", "application/*")
        mFileSelector = FileSelector
            .with(this)
            .setRequestCode(requestCode)
            .setSingleFileMaxSize(5242880, "上传文件大小不能超过5M!")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_ALL)
            .setTypeMismatchTip("暂不支持上传此格式!")
            .setExtraMimeTypes(*extraMimeTypes)
            .applyOptions(optionsType)
            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    FileLogger.w("${optionsType.fileType}")
                    FileType.TXT.getMimeTypeArray()?.forEach {
                        FileLogger.w(it)
                    }
                    return (fileType == optionsType.fileType && uri != null && !uri.path.isNullOrBlank())

                }
            }).callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    FileLogger.e("成功: ${results?.size}")
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("失败: ${e?.message}")
                }
            })
            .choose()

    }

}