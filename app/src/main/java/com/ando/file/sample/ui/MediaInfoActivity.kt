package com.ando.file.sample.ui

import ando.file.core.FileLogger
import ando.file.core.FileUtils
import ando.file.selector.FileSelectCallBack
import ando.file.selector.FileSelectResult
import ando.file.selector.FileSelector
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.ando.file.sample.*
import com.ando.file.sample.utils.PermissionManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * 获取媒体文件信息(图片,音频,视频)\n(获取准确的拍摄时间)
 *
 * Get media file information (picture, audio, video)\n(Get accurate shooting time)
 */
@SuppressLint("SetTextI18n")
class MediaInfoActivity : AppCompatActivity() {

    private val tvShotTime: TextView by lazy { findViewById(R.id.tvShotTime) }

    private var mFileSelector: FileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_info)

        title = "Media Info"
        //Select File
        findViewById<Button>(R.id.btSelectMediaFile).setOnClickListener {
            PermissionManager.requestStoragePermission(this) {
                if (it) {
                    mFileSelector = FileSelector.with(this)
                        //.setMultiSelect()
                        .callback(object : FileSelectCallBack {
                            override fun onSuccess(results: List<FileSelectResult>?) {
                                results?.firstOrNull()?.uri?.apply {
                                    handleResult(this)
                                }
                            }

                            override fun onError(e: Throwable?) {
                                toastLong(e.toString())
                                FileLogger.e(e.toString())
                            }
                        })
                        .choose()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mFileSelector?.obtainResult(requestCode, resultCode, data)
    }

    @SuppressLint("SimpleDateFormat")
    private fun handleResult(uri: Uri) {
        //Check Uri Right
        if (!FileUtils.checkUri(uri)) {
            tvShotTime.setTextColor(Color.RED)
            tvShotTime.text = "Uri 无效!"
            return
        }

        //打印媒体信息(print media info)
        FileUtils.dumpMediaInfoByMediaMetadataRetriever(uri)
        FileUtils.dumpMediaInfoByExifInterface(uri)

        //获取媒体文件拍摄时间
        FileUtils.getMediaShotTime(uri = uri) { shotTime: Long ->
            runOnUiThread {
                tvShotTime.text = ""
                if (shotTime == -1L) {//获取失败使用 System.currentTimeMillis()
                    tvShotTime.setTextColor(Color.RED)
                    tvShotTime.text = "拍摄时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(System.currentTimeMillis()))}"
                } else {
                    tvShotTime.setTextColor(Color.BLACK)
                    tvShotTime.text = "拍摄时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(shotTime))} \n shotTime=$shotTime"
                }
            }
        }
    }

}