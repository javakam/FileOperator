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
    private val mBtFileCopyOpen: Button by lazy { findViewById(R.id.bt_file_utils_copy_open) }
    private val mBtFileDelete: Button by lazy { findViewById(R.id.bt_file_utils_delete) }
    private val mTvFileContent: TextView by lazy { findViewById(R.id.tv_file_utils_read_content) }
    private val mTvFileInfo: TextView by lazy { findViewById(R.id.tv_file_utils_info) }
    private val mTvFileCopy: TextView by lazy { findViewById(R.id.tv_file_utils_copy_path) }

    private val filePath: String by lazy { getExternalFilesDir(null)?.absolutePath ?: "" }
    private val fileName: String = "temp.html"
    private val file: File by lazy { File("$filePath${File.separator}$fileName") }
    private val destFilePath: String by lazy { externalCacheDir?.absolutePath ?: "" }
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

        //演示操作文件 (Demo operation file)
        refreshFileInfo()
        mBtFileOpen.setOnClickListener {
            if (!isFileExist()) {
                refreshFileInfo()
                return@setOnClickListener
            }
            FileOpener.openFile(this, FileUri.getUriByFile(file), "Choose to open the program")
        }
        mBtFileCreate.setOnClickListener {
            if (file.exists()) {
                toastShort("$fileName existed !")
                refreshFileInfo()
                return@setOnClickListener
            }
            //测试创建多个文件 (Test to create multiple files)
            for (i in 0..2) {
                FileUtils.createFile(filePath = filePath, fileName = fileName, overwrite = false)
            }
            toastShort("$fileName created successfully !")
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
            toastShort("$fileName data written successfully !")
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

            val start = SystemClock.elapsedRealtimeNanos()

            //kotlin.io.FilesKt__UtilsKt.copyTo
            //file.copyTo(target = destFile, overwrite = true, bufferSize = DEFAULT_BUFFER_SIZE)
            val copyResult: File? = FileUtils.copyFile(file, destFilePath, destFileName)
            FileLogger.w("copyResult= $copyResult")

            FileLogger.w("Time difference: ${SystemClock.elapsedRealtimeNanos() - start}")

            if (destFile.exists()) {
                mTvFileCopy.visibility = View.VISIBLE
                mTvFileCopy.text = destFile.absolutePath
                toastShort("File copied successfully !")
            }

            refreshFileInfo()
        }
        //Open Copied File
        mBtFileCopyOpen.setOnClickListener {
            if (!destFile.exists()) {
                refreshFileInfo()
                toastLong("$destFileName does not exist, need to create and then copy")
                return@setOnClickListener
            }
            FileOpener.openFile(this, FileUri.getUriByFile(destFile), "Choose to open the program")
        }
        //Delete
        mBtFileDelete.setOnClickListener {
            //直接删除文件 (Delete files directly)
            FileUtils.deleteFile(file)
            //FileUtils.deleteFile(destFile)

            //删除指定目录下所有文件 (Delete all files in the specified directory)
            //FileUtils.deleteFilesNotDir(filePath)
            //FileUtils.deleteFilesNotDir(destFilePath)

            if (file.exists()) {
                toastShort("$fileName failed to delete !")
            } else {
                toastShort("$fileName successfully deleted !")
            }
            refreshFileInfo()
        }
        //Write Bitmap
        //FileUtils.write2File()
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        val testUrl = "http://image.uc.cn/s/wemedia/s/upload/2020/bwGSqL1efbv804k/1f941436db7fc25304cb91484d0d7c3c.jpg"
        findViewById<TextView>(R.id.tv_file_utils_url).text = "Test Url: $testUrl"

        //getExtension(String, Char, Boolean)
        mTvFileUrl1.text = """
                Method: getExtension(String, Char, Boolean)
                Suffix: ${FileUtils.getExtension(testUrl)}
            """.trimIndent()

        //getExtension(String)
        mTvFileUrl2.text = """
               Method: getExtension(String)
               Suffix: ${FileUtils.getExtension(testUrl)}
            """.trimIndent()

    }

    private fun isFileExist(): Boolean =
        if (!file.exists()) {
            toastLong("$fileName does not exist, please create first")
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
            text = "${getString(R.string.str_ando_file_select_result)}Uri: $uri"
        }

        //getExtension(Uri)
        mTvFileUrl3.visibility = View.VISIBLE
        mTvFileUrl3.text = """
               Method: getExtension(Uri)
               Uri: $uri
               Suffix: ${FileUtils.getExtension(uri)}
            """.trimIndent()

        //getExtension(FilePath/FileName)
        val filePath = FileUri.getPathByUri(uri) ?: ""
        mTvFileUrl4.visibility = View.VISIBLE
        mTvFileUrl4.text = """
               Method: getExtension(FilePath/FileName)
               Name:${FileUtils.getFileNameFromUri(uri)}
               Path:$filePath
               Uri: $uri
               Suffix: ${FileUtils.getExtension(filePath)}
            """.trimIndent()

    }

}