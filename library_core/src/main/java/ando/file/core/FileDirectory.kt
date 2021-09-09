package ando.file.core

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import java.io.*

object FileDirectory {

    private const val HIDDEN_PREFIX = "."

    /**
     * File (not directories) filter.
     */
    var sFileFilter = FileFilter { file: File ->
        file.isFile && !file.name.startsWith(HIDDEN_PREFIX)
    }

    /**
     * Folder (directories) filter.
     */
    var sDirFilter = FileFilter { file: File ->
        file.isDirectory && !file.name.startsWith(HIDDEN_PREFIX)
    }

    // Checks if a volume containing external storage is available for read and write.
    fun isExternalStorageWritable(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    // Checks if a volume containing external storage is available to at least read.
    fun isExternalStorageReadable(): Boolean =
        Environment.getExternalStorageState() in setOf(
            Environment.MEDIA_MOUNTED,
            Environment.MEDIA_MOUNTED_READ_ONLY
        )

    /**
     * 获取外部存储空间视图模式
     * AndroidManifest.xml 中设置 requestLegacyExternalStorage 可修改外部存储空间视图模式，true为 Legacy View，false为 Filtered View。
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun isExternalStorageLegacy(): Boolean = Environment.isExternalStorageLegacy()

    /**
     * 获取 Android 系统根目录
     * <pre>path: /system</pre>
     *
     * @return 系统根目录
     */
    fun getRootDirectory(): File = Environment.getRootDirectory()

    /**
     * 获取 data 目录
     * <pre>path: /data</pre>
     *
     * @return data 目录
     */
    fun getDataDirectory(): File = Environment.getDataDirectory()

    /**
     * 获取缓存目录
     * <pre>path: data/cache</pre>
     *
     * @return 缓存目录
     */
    fun getDownloadCacheDirectory(): File = Environment.getDownloadCacheDirectory()

    /**
     * Media File[]
     */
    @Suppress("DEPRECATION")
    fun getExternalMediaDirs(): Array<File> = FileOperator.getContext().externalMediaDirs

    /**
     * Obb File[]
     */
    fun getObbDirs(): Array<File> = FileOperator.getContext().obbDirs

    /**
     * Cache File[]
     */
    fun getExternalCacheDirs(): Array<File> = FileOperator.getContext().externalCacheDirs

    /**
     * Data File[]
     * <pre>
     *     getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)[0]
     *     等效于
     *     getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
     * </pre>
     */
    fun getExternalFilesDirs(type: String): Array<File> =
        FileOperator.getContext().getExternalFilesDirs(type)

    /**
     * 获取此应用的缓存目录
     * <pre>path: /data/data/package/cache</pre>
     *
     * @return 此应用的缓存目录
     */
    fun getCacheDir(): File = FileOperator.getContext().cacheDir

    /**
     * 获取此应用的文件目录
     * <pre>path: /data/data/package/files</pre>
     *
     * @return 此应用的文件目录
     */
    fun getFilesDir(): File = FileOperator.getContext().filesDir

    /**
     * 获取此应用的数据库文件目录
     * <pre>path: /data/data/package/databases/name</pre>
     *
     * @param name 数据库文件名
     * @return 数据库文件目录
     */
    fun getDatabasePath(name: String?): File = FileOperator.getContext().getDatabasePath(name)

    /**
     * 获取此应用的 Obb 目录
     * <pre>path: /storage/emulated/0/Android/obb/package</pre>
     * <pre>一般用来存放游戏数据包</pre>
     *
     * @return 此应用的 Obb 目录
     */
    fun getObbDir(): File = FileOperator.getContext().obbDir


    //getExternalFilesDir
    //--------------------------------------------------------------------------

    /**
     * 获取此应用在外置储存中的缓存目录
     * <pre>path: /storage/emulated/0/Android/data/package/cache</pre>
     *
     * @return 此应用在外置储存中的缓存目录
     */
    fun getExternalCacheDir(): File? = FileOperator.getContext().externalCacheDir

    /**
     * 获取此应用在外置储存中的文件目录
     * <pre>path: /storage/emulated/0/Android/data/package/files</pre>
     *
     *  <pre>
     *      /storage/emulated/0/Android/data/package/files/Documents/
     *
     *      getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)[0]
     *      等效于
     *      getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
     *  </pre>
     * @return 此应用在外置储存中的文件目录
     */
    fun getExternalFilesDir(): File? = FileOperator.getContext().getExternalFilesDir(null)

    /**
     * 获取此应用在外置储存中的闹钟铃声目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Alarms</pre>
     *
     * @return 此应用在外置储存中的闹钟铃声目录
     */
    fun getExternalFilesDirALARMS(): File? =
        FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_ALARMS)

    /**
     * 获取此应用在外置储存中的相机目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/DCIM</pre>
     *
     * @return 此应用在外置储存中的相机目录
     */
    fun getExternalFilesDirDCIM(): File? =
        FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)

    /**
     * 获取此应用在外置储存中的文档目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Documents</pre>
     *
     * @return 此应用在外置储存中的文档目录
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun getExternalFilesDirDOCUMENTS(): File? = FileOperator.getContext()
        .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    /**
     * 获取此应用在外置储存中的下载目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Download</pre>
     *
     * @return 此应用在外置储存中的闹钟目录
     */
    fun getExternalFilesDirDOWNLOADS(): File? = FileOperator.getContext()
        .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    /**
     * 获取此应用在外置储存中的视频目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Movies</pre>
     *
     * @return 此应用在外置储存中的视频目录
     */
    fun getExternalFilesDirMOVIES(): File? =
        FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)

    /**
     * 获取此应用在外置储存中的音乐目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Music</pre>
     *
     * @return 此应用在外置储存中的音乐目录
     */
    fun getExternalFilesDirMUSIC(): File? =
        FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)

    /**
     * 获取此应用在外置储存中的提示音目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Notifications</pre>
     *
     * @return 此应用在外置储存中的提示音目录
     */
    fun getExternalFilesDirNOTIFICATIONS(): File? = FileOperator.getContext()
        .getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)

    /**
     * 获取此应用在外置储存中的图片目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Pictures</pre>
     *
     * @return 此应用在外置储存中的图片目录
     */
    fun getExternalFilesDirPICTURES(): File? = FileOperator.getContext()
        .getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    /**
     * 获取此应用在外置储存中的 Podcasts 目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Podcasts</pre>
     *
     * @return 此应用在外置储存中的 Podcasts 目录
     */
    fun getExternalFilesDirPODCASTS(): File? = FileOperator.getContext()
        .getExternalFilesDir(Environment.DIRECTORY_PODCASTS)

    /**
     * 获取此应用在外置储存中的铃声目录
     * <pre>path: /storage/emulated/0/Android/data/package/files/Ringtones</pre>
     *
     * @return 此应用在外置储存中的铃声目录
     */
    fun getExternalFilesDirRINGTONES(): File? = FileOperator.getContext()
        .getExternalFilesDir(Environment.DIRECTORY_RINGTONES)

}