package com.ando.file.sample.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.FileSizeUtils
import ando.file.core.FileLogger
import ando.file.core.FileUri
import android.widget.Button
import android.widget.TextView
import com.ando.file.sample.*
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

    private lateinit var tvDataDir: TextView
    private lateinit var tvFilesDir: TextView
    private lateinit var tvCacheDir: TextView
    private lateinit var tvCompressedImgCacheDir: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_clear_cache)
        tvDataDir = findViewById(R.id.tvDataDir)
        tvFilesDir = findViewById(R.id.tvFilesDir)
        tvCacheDir = findViewById(R.id.tvCacheDir)
        tvCompressedImgCacheDir = findViewById(R.id.tvCompressedImageCacheDir)

        //清除缓存
        findViewById<Button>(R.id.mBtClearCache).setOnClickListener {
            val result = clearCompressedImageCacheDir()
            toastLong(if (result) "清理压缩图片缓存成功!" else "清理压缩图片缓存失败!")
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

        /////////////////////// ando.file.core.FileUri.getPath -> MediaStore (and general)
        val compressedImageCacheDir: String = getCompressedImageCacheDir()
        FileUri.getUriByPath(compressedImageCacheDir)?.let { uri ->

            /*
            3.4KB
            /data/data/com.ando.file.sample/cache/image

            75.8KB
            /data/data/com.ando.file.sample/cache/image/12msj1phcou6hj27svdm4lco3
             */
            val fileList: List<File>? = File(compressedImageCacheDir).listFiles()?.asList()
            val childFileSb = StringBuilder()
            fileList?.forEachIndexed { i, f ->
                childFileSb.append("\n $i -> ${f.name} 大小: ${FileSizeUtils.formatFileSize(FileSizeUtils.getFileSize(f))}")
            }

            val sizeTotal = FileSizeUtils.calculateFileOrDirSize(compressedImageCacheDir)
            val sizeTotal2 = FileSizeUtils.calculateFileOrDirSize(FileUri.getFilePathByUri(uri))

            tvCompressedImgCacheDir.text =
                """🍎压缩图片的缓存目录: 
                | ❎路径: ${FileUri.getFilePathByUri(uri)} 大小: $sizeTotal2
                | ❎大小(OpenableColumns.SIZE): ${FileSizeUtils.getFileSize(uri)}
                | ---
                | ✅路径: $compressedImageCacheDir 大小: $sizeTotal
                | 格式化: ${FileSizeUtils.formatFileSize(sizeTotal)}
                | 🍎缓存图片列表(${fileList?.size}): $childFileSb
                | """.trimMargin()
            tvCompressedImgCacheDir.setOnClickListener {
                //FileOpener.openFileBySystemChooser(this, u, "file/*")
            }
        }
        ///////////////////////

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