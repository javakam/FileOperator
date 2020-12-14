package com.ando.file.sample.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.FileSizeUtils
import ando.file.core.FileLogger
import ando.file.core.FileOpener
import ando.file.core.FileUri
import android.widget.TextView
import androidx.core.net.toUri
import com.ando.file.sample.R
import com.ando.file.sample.clearCompressedImageCacheDir
import com.ando.file.sample.getCompressedImageCacheDir
import com.ando.file.sample.toastShort
import kotlinx.android.synthetic.main.activity_file_clear_cache.*
import java.io.File
import kotlin.text.StringBuilder

/**
 * Title: 清除缓存页面
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/6/10  10:03
 */
@SuppressLint("SetTextI18n")
class FileClearCacheActivity : AppCompatActivity() {

    private lateinit var tvCompressedImageCacheDir: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_clear_cache)
        tvCompressedImageCacheDir = findViewById(R.id.tvCompressedImageCacheDir)

        //清除缓存
        mBtClearCache.setOnClickListener {
            val result = clearCompressedImageCacheDir()
            toastShort(if (result) "清理压缩图片缓存成功!" else "清理压缩图片缓存失败!")
            refresh()
        }

        refresh()
    }

    private fun refresh() {

        fileList()?.forEach {
            FileLogger.i("fileList item: $it")
        }
        databaseList()?.forEach {
            FileLogger.i("databaseList item: $it")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvDataDir.text = "👉Activity.getDataDir :  ${getFileInfo(dataDir)}"
        }

        tvFilesDir.text = "👉Activity.getFilesDir : ${getFileInfo(filesDir)}"

        getSize(noBackupFilesDir)
        getExternalFilesDir(null)?.let { getSize(it) }
        getExternalFilesDirs(null)?.get(0)?.let { getSize(it) }
        getSize(obbDir)
        obbDirs?.get(0)?.let { getSize(it) }
        tvCacheDir.text = "👉Activity.getCacheDir : ${getFileInfo(cacheDir)}"
        getSize(codeCacheDir)
        externalCacheDir?.let { getSize(it) }
        //getExternalCacheDirs
        //getExternalMediaDirs
        //getDir(String name, int mode)

        ///////////////////////
        val compressedImageCacheDir: String = getCompressedImageCacheDir()
        FileUri.getUriByPath(compressedImageCacheDir)?.let { u ->

            /*
            3.4KB
            /data/data/com.ando.file.sample/cache/image

            75.8KB
            /data/data/com.ando.file.sample/cache/image/12msj1phcou6hj27svdm4lco3
             */
            val fileList: List<File>? = File(compressedImageCacheDir).listFiles()?.asList()
            val childFileSb = StringBuilder()
            fileList?.forEachIndexed { i, f ->
                childFileSb.append("\n  No.$i ${f.name} ${FileSizeUtils.formatFileSize(FileSizeUtils.getFileSize(f))}\n ")
            }

            val sizeTotal = FileSizeUtils.calculateFileOrDirSize(compressedImageCacheDir)
            val sizeTotal2 = FileSizeUtils.calculateFileOrDirSize(FileUri.getFilePathByUri(u))

            tvCompressedImageCacheDir.text =
                """🔥压缩图片的缓存目录: 
                | 路径: $compressedImageCacheDir 大小: $sizeTotal
                | 路径: ${FileUri.getFilePathByUri(u)} 大小: $sizeTotal2
                | 大小(OpenableColumns.SIZE): ${FileSizeUtils.getFileSize(u)}
                | 格式化: ${FileSizeUtils.formatFileSize(sizeTotal)}
                | 文件列表: $childFileSb
                | """.trimMargin()
            tvCompressedImageCacheDir.setOnClickListener {
                FileOpener.openFileBySystemChooser(this, u, "file/*")
            }

        }
    }

    /**
     * 读取目录大小
     */
    private fun getSize(file: File): Long {
        return FileSizeUtils.getFolderSize(file)
    }

    private fun getFileInfo(file: File): String {
        return "\n name=${file.name} \n path=${file.path} \n absolutePath=${file.absolutePath} \n 大小=${getSize(file)} \n"
    }

}