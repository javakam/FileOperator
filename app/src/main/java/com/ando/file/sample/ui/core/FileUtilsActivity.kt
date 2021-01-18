package com.ando.file.sample.ui.core

import ando.file.core.FileUri
import ando.file.core.FileUtils
import ando.file.selector.FileSelectCallBack
import ando.file.selector.FileSelectResult
import ando.file.selector.FileSelector
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R
import java.io.File
import java.net.URI

/**
 * Title: FileUtilsActivity
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2021/1/18  9:49
 */
class FileUtilsActivity : AppCompatActivity() {

    private val mBtSelectSingleFile: Button by lazy { findViewById(R.id.bt_file_select_single) }
    private val mTvFileUri: TextView by lazy { findViewById(R.id.tv_file_select_uri) }

    private val mTvFileUrl: TextView by lazy { findViewById(R.id.tv_file_utils_url) }
    private val mTvFileUrl1: TextView by lazy { findViewById(R.id.tv_file_utils1) }
    private val mTvFileUrl2: TextView by lazy { findViewById(R.id.tv_file_utils2) }
    private val mTvFileUrl3: TextView by lazy { findViewById(R.id.tv_file_utils3) }
    private val mTvFileUrl4: TextView by lazy { findViewById(R.id.tv_file_utils4) }

    //
    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_utils)
        initListener()
        initData()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    @SuppressLint("SetTextI18n")
    private fun initListener() {
        mBtSelectSingleFile.setOnClickListener {
            mFileSelector = FileSelector.with(this)
                .callback(object : FileSelectCallBack {
                    override fun onSuccess(results: List<FileSelectResult>?) {
                        results?.firstOrNull()?.uri?.apply { refreshData(this) }
                    }

                    override fun onError(e: Throwable?) {
                        mTvFileUri.visibility = View.VISIBLE
                        mTvFileUri.setTextColor(Color.RED)
                        mTvFileUri.text = e.toString()
                    }
                })
                .choose()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        val testUrl = "http://image.uc.cn/s/wemedia/s/upload/2020/bwGSqL1efbv804k/1f941436db7fc25304cb91484d0d7c3c.jpg"
        mTvFileUrl.text = "测试Url: $testUrl"

        //getExtension(String, Char, Boolean)
        mTvFileUrl1.text = """
                方法: getExtension(String, Char, Boolean)
                后缀: ${FileUtils.getExtension(testUrl)}
            """.trimIndent()

        //getExtension(String)
        mTvFileUrl2.text = """
               方法: getExtension(String)
               后缀: ${FileUtils.getExtension(testUrl)}
            """.trimIndent()

    }

    @SuppressLint("SetTextI18n")
    private fun refreshData(uri: Uri) {
        mTvFileUri.visibility = View.VISIBLE
        mTvFileUri.setTextColor(Color.BLACK)
        mTvFileUri.text = "选择结果Uri: $uri"

        //getExtension(Uri)
        mTvFileUrl3.visibility = View.VISIBLE
        mTvFileUrl3.text = """
               方法: getExtension(Uri)
               Uri: $uri
               后缀: ${FileUtils.getExtension(uri)}
            """.trimIndent()

        //getExtension(FilePath/FileName)
        val filePath = FileUri.getFilePathByUri(uri) ?: ""
        mTvFileUrl4.visibility = View.VISIBLE
        mTvFileUrl4.text = """
               方法: getExtension(FilePath/FileName)
               Name:${FileUtils.getFileNameFromUri(uri)}
               Path:$filePath
               Uri: $uri
               后缀: ${FileUtils.getExtension(filePath)}
            """.trimIndent()

    }

}