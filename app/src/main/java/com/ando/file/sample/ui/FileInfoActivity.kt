package com.ando.file.sample.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.FileSizeUtils
import ando.file.core.FileLogger
import ando.file.core.FileUri
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import com.ando.file.sample.*
import java.io.File
import kotlin.text.StringBuilder

/**
 * # Android目录信息和清除缓存
 *
 * Android directory information and clear cache
 *
 * @author javakam
 * @date 2020/6/10  10:03
 */
@SuppressLint("SetTextI18n")
class FileInfoActivity : AppCompatActivity() {

    private lateinit var tvDataDir: TextView
    private lateinit var tvFilesDir: TextView
    private lateinit var tvCacheDir: TextView
    private lateinit var tvCompressedImgCacheDir: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_info)
        tvDataDir = findViewById(R.id.tvDataDir)
        tvFilesDir = findViewById(R.id.tvFilesDir)
        tvCacheDir = findViewById(R.id.tvCacheDir)
        tvCompressedImgCacheDir = findViewById(R.id.tvCompressedImageCacheDir)

        title = "File Directory"
        //clear cache
        findViewById<Button>(R.id.mBtClearCache).setOnClickListener {
            val result = clearCompressedImageCacheDir()
            toastLong(if (result) "Successfully cleaned compressed image cache !" else "Failed to clean compressed image cache !")
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

        //系统会直接创建相应的目录 The system will directly create the corresponding directory
        getExternalFilesDirs(Environment.DIRECTORY_ALARMS)
        getExternalFilesDirs(Environment.DIRECTORY_DCIM)
        getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)
        getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)
        getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
        getExternalFilesDirs(Environment.DIRECTORY_MUSIC)
        getExternalFilesDirs(Environment.DIRECTORY_NOTIFICATIONS)
        getExternalFilesDirs(Environment.DIRECTORY_PICTURES)
        getExternalFilesDirs(Environment.DIRECTORY_PODCASTS)
        getExternalFilesDirs(Environment.DIRECTORY_RINGTONES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getExternalFilesDirs(Environment.DIRECTORY_AUDIOBOOKS)
        }
        //etc

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
                """🍎Cache directory for compressed pictures: 
                | ❎Path: ${FileUri.getFilePathByUri(uri)} Size: $sizeTotal2
                | ❎Size(OpenableColumns.SIZE): ${FileSizeUtils.getFileSize(uri)}
                | ---
                | Path: $compressedImageCacheDir Size: $sizeTotal
                | Format: ${FileSizeUtils.formatFileSize(sizeTotal)}
                | 🍎Cached picture list (${fileList?.size}): $childFileSb
                | """.trimMargin()
            tvCompressedImgCacheDir.setOnClickListener {
                //FileOpener.openFile(this, u, "file/*")
            }
        }
        ///////////////////////

    }

    /**
     * 读取目录大小 (Read directory size)
     */
    private fun getSize(file: File): Long {
        return FileSizeUtils.getFolderSize(file)
    }

    private fun getFileInfo(file: File): String {
        return "\n name=${file.name} \n path=${file.path} \n absolutePath=${file.absolutePath} \n 大小=${getSize(file)} \n"
    }

}