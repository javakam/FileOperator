package com.ando.file.sample.ui.core

import ando.file.core.*
import ando.file.selector.*
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.ando.file.sample.*
import com.ando.file.sample.R
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils

/**
 * 核心库(Core)
 */
class FileCoreActivity : AppCompatActivity() {

    private lateinit var mBtSelectSingle: Button
    private lateinit var mTvError: TextView
    private lateinit var mTvResult: TextView

    private var mFileSelector: FileSelector? = null

    companion object {
        const val mime = "MimeType"
        const val size = "FileSize"
        const val uriAndPath = "FileUri/FilePath"
        fun openMimeType(activity: AppCompatActivity) = open(activity, mime)
        fun openFileSize(activity: AppCompatActivity) = open(activity, size)
        fun openFileUriAndPath(activity: AppCompatActivity) = open(activity, uriAndPath)
        private fun open(activity: AppCompatActivity, action: String) {
            Intent(activity, FileCoreActivity::class.java).apply {
                putExtra("action", action)
                activity.startActivity(this)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_core_mime_type)
        mBtSelectSingle = findViewById(R.id.bt_select_single)
        mTvError = findViewById(R.id.tv_error)
        mTvResult = findViewById(R.id.tv_result)

        val action = intent.getStringExtra("action")
        title = action

        mBtSelectSingle.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) {
                    when (action) {
                        mime, size, uriAndPath -> chooseFile(action)
                        else -> {
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError, mTvResult)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    private fun chooseFile(action: String) {
        mFileSelector = FileSelector
            .with(this)
            .setMultiSelect()
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setTypeMismatchTip("文件类型不匹配")
            .setMinCount(1, "至少选一个文件!")
            .setMaxCount(10, "最多选十个文件!")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW)
            .setSingleFileMaxSize(5242880, "大小不能超过5M！")
            .setAllFilesMaxSize(20971520, "总大小不能超过20M！")
            .setMimeTypes("*/*")

            .filter(object : FileSelectCondition {
                override fun accept(fileType: FileType, uri: Uri?): Boolean {
                    return true
                }
            })
            .callback(object : FileSelectCallBack {
                override fun onSuccess(results: List<FileSelectResult>?) {
                    FileLogger.w("FileSelectCallBack onSuccess ${results?.size}")
                    ResultUtils.resetUI(mTvResult)
                    if (results.isNullOrEmpty()) {
                        toastLong("没有选取文件")
                        return
                    }
                    ResultUtils.setErrorText(mTvError, null)
                    when (action) {
                        mime -> ResultUtils.setCoreResults(tvResult = mTvResult, results = results)
                        size -> ResultUtils.setCoreResults(tvResult = mTvResult, results = results)
                        else -> ResultUtils.setFormattedResults(tvResult = mTvResult, results = results)
                    }
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("FileSelectCallBack onError ${e?.message}")
                    ResultUtils.setErrorText(mTvError, e)
                }
            })
            .choose()
    }

}