package com.ando.file.sample.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ando.file.core.FileSizeUtils
import ando.file.core.FileLogger
import com.ando.file.sample.R
import com.ando.file.sample.clearCompressedImageCacheDir
import com.ando.file.sample.toastShort
import kotlinx.android.synthetic.main.activity_file_clear_cache.*
import java.io.File

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_clear_cache)

        //清除缓存
        mBtClearCache.setOnClickListener {
            val result = clearCompressedImageCacheDir()
            toastShort(if (result) "清理压缩图片缓存成功!" else "清理压缩图片缓存失败!")

            refresh()
        }

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