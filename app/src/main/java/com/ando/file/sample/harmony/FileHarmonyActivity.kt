package com.ando.file.sample.harmony

import ando.file.core.FileGlobal
import ando.file.core.FileLogger
import ando.file.selector.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R
import com.ando.file.sample.REQUEST_CHOOSE_FILE
import com.ando.file.sample.toastLong
import com.ando.file.sample.utils.PermissionManager
import com.ando.file.sample.utils.ResultUtils
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.text.Spannable

import android.text.style.ForegroundColorSpan
import kotlin.math.ceil


/**
 * Android BufferedInputStream.read 导致的问题...没解决...
 *
 * https://juejin.cn/post/6983958137464160286
 *
 * @author javakam
 * @date 2021-08-12  11:03
 */
class FileHarmonyActivity : AppCompatActivity() {

    private val mBtSelectSingle: Button by lazy { findViewById(R.id.bt_select_single) }
    private val mTvError: TextView by lazy { findViewById(R.id.tv_error) }
    private val mTvResult: TextView by lazy { findViewById(R.id.tv_result) }

    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_core)
        title = "HarmonyOS"

        mBtSelectSingle.setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) chooseFile()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ResultUtils.resetUI(mTvError, mTvResult)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    private fun chooseFile() {
        mFileSelector = FileSelector
            .with(this)
            .setMultiSelect()
            .setRequestCode(REQUEST_CHOOSE_FILE)
            .setTypeMismatchTip("File type mismatch !")
            .setMinCount(1, "Choose at least one file !!")
            .setMaxCount(1000, "Choose up to one thousand files !")
            .setOverLimitStrategy(FileGlobal.OVER_LIMIT_EXCEPT_OVERFLOW)
            .setSingleFileMaxSize(10737418240, "The size cannot exceed 10G !") //byte (B)
            .setAllFilesMaxSize(53687091200, "The total size cannot exceed 50G !")
            .setMimeTypes("*/*")

            .filter(object : FileSelectCondition {
                override fun accept(fileType: IFileType, uri: Uri?): Boolean {
                    return true
                }
            })
            .callback(object : FileSelectCallBack {
                @SuppressLint("SetTextI18n")
                override fun onSuccess(results: List<FileSelectResult>?) {
                    FileLogger.w("FileSelectCallBack onSuccess ${results?.size}")
                    ResultUtils.resetUI(mTvResult)
                    if (results.isNullOrEmpty()) {
                        toastLong("No file selected")
                        return
                    }
                    ResultUtils.setErrorText(mTvError, null)
                    ResultUtils.setFormattedResults(tvResult = mTvResult, results = results)

                    //模拟上传文件
                    Thread {
                        val r: FileSelectResult = results[0]
                        val newText: CharSequence = mTvResult.text
                        //用于显示上传数据
                        val sb = SpannableStringBuilder(newText)
                        uploadFile(this@FileHarmonyActivity, r.uri ?: return@Thread, r.fileSize) {
                            if (it == null) {
                                Thread.interrupted()
                                return@uploadFile
                            }
                            sb.append(it).appendLine()
                            sb.setSpan(ForegroundColorSpan(Color.RED), mTvResult.text.length, sb.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                            runOnUiThread {
                                mTvResult.text = sb
                            }
                        }

                    }.start()
                }

                override fun onError(e: Throwable?) {
                    FileLogger.e("FileSelectCallBack onError ${e?.message}")
                    ResultUtils.setErrorText(mTvError, e)
                }
            })
            .choose()
    }

    /**
     * 模拟文件上传 (Simulate file upload)
     */
    fun uploadFile(context: Context, uri: Uri, fileTotalSize: Long, callBack: (String?) -> Unit) {
        //1.通过Uri获取文件的InputStream
        val ins: InputStream = context.contentResolver.openInputStream(uri) ?: return
        //2.每次读取8192个字节,速度
        val buffered: BufferedInputStream = ins.buffered(8192)
        //3.临时存储缓冲输出的字节流
        val out = ByteArrayOutputStream()
        //4.用来存储每次读取到的字节数组
        val bytes = ByteArray(1024 * 1024)
        //5.每写入 5M 数据, 开始上传
        val onePartSize = 5 * 1024 * 1024
        //6.计算分片数量
        var totalCount: Int = ceil(fileTotalSize * 1.00 / onePartSize).toInt()
        //不足 onePartSize (5M) 直接传过去
        totalCount = if (totalCount < 1) 1 else totalCount
        callBack.invoke("🌴开始上传, 总共${totalCount}片")
        var partCount = 0

        while (buffered.read(bytes) != -1) { //************ 此处报错 ENOTCONN ************
            out.write(bytes)
            //相等时开始上传, eg: 上传 Buffer out.size()=3145728 partSize=3145728 pCount=1
            if (out.size() >= onePartSize) {
                Thread.sleep(30)

                //上传分片数据
                val log = "第${partCount}片  大小: ${(out.toByteArray()).size / (1024 * 1024)}M"
                callBack.invoke(log)
                Log.e("123", log)
                partCount += 1
                out.reset()
            }
        }

        if (out.size() > 0) {
            //上传不足 onePartSize (5M) 的数据
            val log = "最后一片($partCount)  大小: ${(out.toByteArray()).size / 1024}KB"
            callBack.invoke(log)
            Log.e("123", log)
            out.reset()
        }
        callBack.invoke("🌴上传完成")
        callBack.invoke(null)
    }
}