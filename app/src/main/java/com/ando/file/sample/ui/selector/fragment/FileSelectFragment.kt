package com.ando.file.sample.ui.selector.fragment

import ando.file.core.*
import ando.file.selector.*
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
import androidx.fragment.app.Fragment
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import java.io.File

/**
 * Show Usage In Fragment
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
        FileLogger.e("目录: ${requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}  ___  $documents")

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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError, mTvResult, mIvOrigin, mIvCompressed)
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


    fun Fragment.chooseImage(requestCode: Int, callback: FileSelectCallBack): FileSelector {
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
            .with(this)
            .setRequestCode(requestCode)
            .setTypeMismatchTip("Tipe file tidak didukung")
            .setMinCount(1, "Pilih minimal 1 gambar!")
            .setMaxCount(1, "Pilih minimal 1 gambar!")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_ALL)
            .setMimeTypes("image/*")
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

    fun Fragment.chooseFile(requestCode: Int, callback: FileSelectCallBack): FileSelector {
        val optionsWord = FileSelectOptions().apply {
            fileType = FileType.WORD
            singleFileMaxSize = 5242880
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
            .setMimeTypes(
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

}