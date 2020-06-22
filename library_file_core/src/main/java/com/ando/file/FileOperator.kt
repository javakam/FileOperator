package com.ando.file

import android.app.Application
import android.content.Context
import android.text.TextUtils
import com.ando.file.common.FileLogger
import com.ando.file.common.getExternalCacheDir
import com.ando.file.common.getExternalFilesDir
import com.ando.file.common.getFilesDir
import java.io.File

/**
 * Title: FileOperator
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/5/9  14:16
 */
object FileOperator {

    private lateinit var context: Context
    private lateinit var application: Application
    private var isDebug: Boolean = true

    fun init(application: Application, isDebug: Boolean) {
        this.application = application
        this.context = application.applicationContext
        this.isDebug = isDebug
        FileLogger.init(isDebug)

    }

    fun getContext(): Context {
        return context
    }

    fun getApplication(): Application {
        return application
    }

    fun isDebug(): Boolean {
        return isDebug
    }

    //getDatabasePath
    //--------------------------------------------------------------------------

    /**
     * 获取数据库存储路径/data/data/包名/databases/
     */
    fun getDatabasePath(context: Context, dirName: String): String? {
        val root = context.getDatabasePath(null)
        if (root != null) {
            val path = root.absolutePath + File.separator + dirName + File.separator
            val file = File(path)
            if (!file.exists() && !file.mkdirs()) {
                //throw RuntimeException("can't make dirs in " + file.absolutePath)
            }
            return path
        }
        return null
    }

    //getCacheDir
    //--------------------------------------------------------------------------

    /**
     * 获取数据库存储路径/SDCard/Android/data/包名/cache/
     */
    fun getCacheDir(): String? =
        if (isDebug()) getExternalCacheDir() else getCacheDir()

    /**
     * 获取数据库存储路径/SDCard/Android/data/包名/cache/
     * 设置：对应清除缓存
     */
    fun getCachePath(dirName: String): String? {
        val root = getCacheDir()
        return if (root != null && root.isNotBlank() && !TextUtils.isEmpty(dirName)) {
            val path = root + File.separator + dirName + File.separator
            val file = File(path)
            if (!file.exists() && !file.mkdirs()) {
                //throw  RuntimeException("can't make dirs in " + file.absolutePath);
            }
            path
        } else root
    }

    //getFileDir
    //--------------------------------------------------------------------------

    fun getFileDir(): String? =
        if (isDebug()) getExternalFilesDir() else getFilesDir()

    /**
     * 获取数据库存储路径/SDCard/Android/data/包名/files/
     * 设置：对应清除数据
     */
    fun getFilesPath(dirName: String): String? {
        val root = getFileDir()
        return if (root != null && root.isNotBlank() && !TextUtils.isEmpty(dirName)) {
            val path = root + File.separator + dirName + File.separator
            val file = File(path)
            if (!file.exists() && !file.mkdirs()) {
                //throw  RuntimeException("can't make dirs in " + file.absolutePath);
            }
            path
        } else root
    }


}