package com.ando.file.sample.ui.core

import ando.file.core.*
import ando.file.selector.FileSelectCallBack
import ando.file.selector.FileSelectResult
import ando.file.selector.FileSelector
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R
import com.ando.file.sample.toastLong
import com.ando.file.sample.toastShort
import com.ando.file.sample.utils.ResultUtils
import java.io.File
import java.io.InputStream
import java.util.*

/**
 * # FileUtilsActivity
 *
 * @author javakam
 * @date 2021/1/18  9:49
 */
class FileUtilsActivity : AppCompatActivity() {

    private val mBtSelectFile: Button by lazy { findViewById(R.id.bt_file_select) }

    private val mTvFileUrl1: TextView by lazy { findViewById(R.id.tv_file_utils1) }
    private val mTvFileUrl2: TextView by lazy { findViewById(R.id.tv_file_utils2) }
    private val mTvFileUrl3: TextView by lazy { findViewById(R.id.tv_file_utils3) }
    private val mTvFileUrl4: TextView by lazy { findViewById(R.id.tv_file_utils4) }

    //
    private val mBtFileCreate: Button by lazy { findViewById(R.id.bt_file_utils_create) }
    private val mBtFileOpen: Button by lazy { findViewById(R.id.bt_file_utils_open) }
    private val mBtFileWrite: Button by lazy { findViewById(R.id.bt_file_utils_write) }
    private val mBtFileRead: Button by lazy { findViewById(R.id.bt_file_utils_read) }
    private val mBtFileCopy: Button by lazy { findViewById(R.id.bt_file_utils_copy) }
    private val mBtFileDelete: Button by lazy { findViewById(R.id.bt_file_utils_delete) }
    private val mTvFileContent: TextView by lazy { findViewById(R.id.tv_file_utils_read_content) }
    private val mTvFileInfo: TextView by lazy { findViewById(R.id.tv_file_utils_info) }
    private val mTvFileCopy: TextView by lazy { findViewById(R.id.tv_file_utils_copy_path) }

    private val filePath: String by lazy { getExternalFilesDir(null).absolutePath }
    private val fileName: String = "temp.html"
    private val file: File by lazy { File("$filePath${File.separator}$fileName") }
    private val destFilePath: String by lazy { externalCacheDir.absolutePath }
    private val destFileName: String = "tempCopy.html"
    private val destFile: File by lazy { File("$destFilePath${File.separator}$destFileName") }

    //
    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_utils)
        title = FileUtils.javaClass.simpleName
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
        mBtSelectFile.setOnClickListener {
            mFileSelector = FileSelector.with(this)
                .callback(object : FileSelectCallBack {
                    override fun onSuccess(results: List<FileSelectResult>?) {
                        results?.firstOrNull()?.uri?.apply { refreshExtensionData(this) }
                    }

                    override fun onError(e: Throwable?) {
                        toastLong(e.toString())
                        FileLogger.e(e.toString())
                    }
                })
                .choose()
        }

        //演示操作文件
        refreshFileInfo()
        mBtFileOpen.setOnClickListener {
            if (!isFileExist()) {
                refreshFileInfo()
                return@setOnClickListener
            }
            FileOpener.openFileBySystemChooser(this, FileUri.getUriByFile(file), "选择打开程序")
        }
        mBtFileCreate.setOnClickListener {
            if (file.exists()) {
                toastLong("${fileName}已存在!")
                refreshFileInfo()
                return@setOnClickListener
            }
            FileUtils.createFile(filePath = filePath, fileName = fileName, overwrite = true)
            toastShort("${fileName}创建成功!")
            refreshFileInfo()
        }
        //Write
        mBtFileWrite.setOnClickListener {
            if (!isFileExist()) {
                refreshFileInfo()
                return@setOnClickListener
            }
            val tempText = "Hello World ! \n${Date()}"
            val inputStream: InputStream = tempText.byteInputStream(Charsets.UTF_8)
            FileUtils.write2File(inputStream, filePath, fileName)
            toastShort("${fileName}写入数据成功!")
            refreshFileInfo()
        }
        //Read
        mBtFileRead.setOnClickListener {
            if (!isFileExist()) {
                refreshFileInfo()
                return@setOnClickListener
            }
            val uri: Uri = FileUri.getUriByFile(file) ?: return@setOnClickListener
            val content: String = FileUtils.readFileText(uri) ?: ""
            mTvFileContent.visibility = View.VISIBLE
            mTvFileContent.text = content
        }
        //Copy
        mBtFileCopy.setOnClickListener {
            if (!isFileExist()) {
                refreshFileInfo()
                return@setOnClickListener
            }
            FileLogger.d("destFilePath=$destFilePath destFileName=$destFileName")

            val start = SystemClock.elapsedRealtimeNanos()

            file.copyTo(target = destFile, overwrite = true, bufferSize = DEFAULT_BUFFER_SIZE)
            //val copyResult: Boolean = FileUtils.copyFile(file, destFilePath, destFileName)
            //FileLogger.w("copyResult=$copyResult")

            FileLogger.w("时间差: ${SystemClock.elapsedRealtimeNanos() - start}")

            if (destFile.exists()) {
                mTvFileCopy.visibility = View.VISIBLE
                mTvFileCopy.text = destFile.absolutePath
                toastShort("文件复制成功!")
            }

            refreshFileInfo()
        }
        //Delete
        mBtFileDelete.setOnClickListener {
            //直接删除文件
            //FileUtils.deleteFile(file)
            //FileUtils.deleteFile(destFile)

            //删除指定目录下文件
            FileUtils.deleteFilesNotDir(filePath)
            FileUtils.deleteFilesNotDir(destFilePath)

            if (file.exists()) {
                toastShort("${fileName}删除失败!")
            } else {
                toastShort("${fileName}删除成功!")
            }
            refreshFileInfo()
        }
        //Write Bitmap
        //FileUtils.write2File()
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        val testUrl = "http://image.uc.cn/s/wemedia/s/upload/2020/bwGSqL1efbv804k/1f941436db7fc25304cb91484d0d7c3c.jpg"
        findViewById<TextView>(R.id.tv_file_utils_url).text = "测试Url: $testUrl"

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

    private fun isFileExist(): Boolean =
        if (!file.exists()) {
            toastLong("${fileName}不存在, 请先创建")
            false
        } else true

    private fun refreshFileInfo() {
        if (!file.exists()) mTvFileContent.visibility = View.GONE
        if (!destFile.exists()) mTvFileCopy.visibility = View.GONE
        ResultUtils.setCoreResults(mTvFileInfo, file)
    }

    @SuppressLint("SetTextI18n")
    private fun refreshExtensionData(uri: Uri) {
        findViewById<TextView>(R.id.tv_file_select_uri).apply {
            visibility = View.VISIBLE
            text = "选择结果Uri: $uri"
        }

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