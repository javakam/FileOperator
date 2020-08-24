package ando.file

import android.app.Application
import android.content.Context
import android.text.TextUtils
import ando.file.core.FileLogger
import ando.file.core.FileDirectory.getExternalCacheDir
import ando.file.core.FileDirectory.getExternalFilesDir
import ando.file.core.FileDirectory.getFilesDir
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
    private var isDebug: Boolean = false

    fun init(application: Application, isDebug: Boolean) {
        FileOperator.application = application
        context = application.applicationContext
        FileOperator.isDebug = isDebug
        FileLogger.init(isDebug)
    }

    fun getContext(): Context = context

    fun getApplication(): Application = application

    fun isDebug(): Boolean = isDebug

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
    fun getCacheDir(): File? =
        if (isDebug()) getExternalCacheDir() else context.cacheDir

    fun getCachePath(dirName: String): String? = getCachePath(getCacheDir(), dirName)

    /**
     * 获取数据库存储路径/SDCard/Android/data/包名/cache/
     * 设置：对应清除缓存
     */
    fun getCachePath(cacheDir: File?, dirName: String): String? {
        val root = cacheDir?.absolutePath
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

    fun getFileDir(): File? =
        if (isDebug()) getExternalFilesDir() else getFilesDir()

    fun getFilesPath(dirName: String): String? = getFilesPath(getFileDir(), dirName)

    /**
     * 获取数据库存储路径/SDCard/Android/data/包名/files/
     * 设置：对应清除数据
     */
    fun getFilesPath(filesDir: File?, dirName: String): String? {
        val root = filesDir?.absolutePath
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