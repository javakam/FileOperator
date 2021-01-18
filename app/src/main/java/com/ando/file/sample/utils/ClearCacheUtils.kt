package com.ando.file.sample.utils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import ando.file.core.FileSizeUtils.formatFileSize
import ando.file.core.FileSizeUtils.getFolderSize
import ando.file.core.FileUtils.deleteFilesNotDir
import java.io.File

/**
 * ClearCacheUtils
 *
 * Description:清除缓存工具类
 *
 * @author javakam
 * @date 2018/11/2  15:03
 */
object ClearCacheUtils {

    private const val TAG = "ClearCacheUtils"

    /**
     * 提示清理缓存弹窗
     */
    fun showClearDialog(
        context: Context?,
        onPositiveClickListener: DialogInterface.OnClickListener?,
    ): Dialog {
        return AlertDialog.Builder(context!!)
            .setCancelable(true)
            .setMessage("确定清除缓存吗？")
            .setPositiveButton("确定", onPositiveClickListener)
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 获取缓存大小
     */
    //Samsung两种方式获取到的路径相同
    //getCacheDir         --> /storage/emulated/0/Android/data/com.xxx.angel/cache/info
    //getExternalCacheDir --> /storage/emulated/0/Android/data/com.xxx.angel/cache/info
    fun getTotalCacheSize(dir: File): String {

        //Context.getExternalFilesDir() --> SDCard/Android/data/你的应用的包名/files/ 目录，一般放一些长时间保存的数据
        //Context.getExternalCacheDir() --> SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
        val cacheSize = getFolderSize(dir)
        //L.w("缓存大小 : " + cacheSize);
        return formatFileSize(cacheSize)
    }

    /**
     * 清除缓存
     */
    fun clearAllCache(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val file = File(path)
        if (!file.exists()) file.mkdirs()
        return deleteFilesNotDir(file)
    }

    /**
     * 清除缓存
     */
    fun clearAllCache(dir: File?): Boolean {
        return deleteFilesNotDir(dir)
    }

}