package com.ando.file.sample.ui.selector.fragment

import ando.file.core.FileGlobal
import ando.file.core.FileLogger
import ando.file.core.FileSizeUtils
import ando.file.core.FileUtils
import ando.file.selector.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        fun newInstance(): FileSelectFragment {
            val args = Bundle()
            val fragment = FileSelectFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var mBtSelectSingle: View
    private lateinit var mTvError: TextView
    private lateinit var mTvResult: TextView
    private lateinit var mIvOrigin: ImageView
    private lateinit var mIvCompressed: ImageView

    private var mFileSelector: FileSelector? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.activity_select_single_image, container, false)
        mBtSelectSingle = v.findViewById(R.id.bt_select_single)
        mTvError = v.findViewById(R.id.tv_error)
        mTvResult = v.findViewById(R.id.tv_result)
        mIvOrigin = v.findViewById(R.id.iv_origin)
        mIvCompressed = v.findViewById(R.id.iv_compressed)
        mBtSelectSingle.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()
            }
        }
        return v
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
    private fun chooseFile() {
        val optionsImage = FileSelectOptions().apply {
            fileType = FileType.IMAGE
            fileTypeMismatchTip = "File type mismatch !"
            singleFileMaxSize = 5242880
            singleFileMaxSizeTip = "The largest picture does not exceed 5M !"
            allFilesMaxSize = 10485760
            allFilesMaxSizeTip = "The total picture size does not exceed 10M !"
            fileCondition = object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    //Filter out gif
                    return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
            }
        }

        mFileSelector = FileSelector
            .with(requireContext())
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setTypeMismatchTip("File type mismatch")
            .setMinCount(1, "Choose at least one file!")
            .setMaxCount(10, "Choose up to ten files!")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW)
            .setSingleFileMaxSize(1048576, "The size cannot exceed 1M !")
            .setAllFilesMaxSize(10485760, "The total size cannot exceed 10M !")
            .setMimeTypes("image/*")
            .applyOptions(optionsImage)
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
                        activity?.toastLong("No file selected")
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

}