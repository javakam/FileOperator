package com.ando.file.common

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.ando.file.FileOperator
import java.io.*

private const val PATH_SUFFIX = ".fileProvider"
private const val HIDDEN_PREFIX = "."

/**
 * File (not directories) filter.
 */
var sFileFilter = FileFilter { file: File ->
    val fileName = file.name
    file.isFile && !fileName.startsWith(HIDDEN_PREFIX)
}

/**
 * Folder (directories) filter.
 */
var sDirFilter = FileFilter { file: File ->
    val fileName = file.name
    file.isDirectory && !fileName.startsWith(HIDDEN_PREFIX)
}

// Checks if a volume containing external storage is available for read and write.
fun isExternalStorageWritable(): Boolean = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

// Checks if a volume containing external storage is available to at least read.
fun isExternalStorageReadable(): Boolean =
    Environment.getExternalStorageState() in setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)

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
fun getRootDirectory(): String? = Environment.getRootDirectory().absolutePath

/**
 * 获取 data 目录
 * <pre>path: /data</pre>
 *
 * @return data 目录
 */
fun getDataDirectory(): String? = Environment.getDataDirectory().absolutePath

/**
 * 获取缓存目录
 * <pre>path: data/cache</pre>
 *
 * @return 缓存目录
 */
fun getDownloadCacheDirectory(): String? = Environment.getDownloadCacheDirectory().absolutePath

/**
 * Media File[]
 */
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
fun getExternalFilesDirs(type: String): Array<File> = FileOperator.getContext().getExternalFilesDirs(type)

/**
 * 获取此应用的缓存目录
 * <pre>path: /data/data/package/cache</pre>
 *
 * @return 此应用的缓存目录
 */
fun getCacheDir(): String? = FileOperator.getContext().cacheDir.absolutePath

/**
 * 获取此应用的文件目录
 * <pre>path: /data/data/package/files</pre>
 *
 * @return 此应用的文件目录
 */
fun getFilesDir(): String? = FileOperator.getContext().filesDir.absolutePath

/**
 * 获取此应用的数据库文件目录
 * <pre>path: /data/data/package/databases/name</pre>
 *
 * @param name 数据库文件名
 * @return 数据库文件目录
 */
fun getDatabasePath(name: String?): String? = FileOperator.getContext().getDatabasePath(name).absolutePath

/**
 * 获取此应用的 Obb 目录
 * <pre>path: /storage/emulated/0/Android/obb/package</pre>
 * <pre>一般用来存放游戏数据包</pre>
 *
 * @return 此应用的 Obb 目录
 */
fun getObbDir(): String? = FileOperator.getContext().obbDir.absolutePath

//getExternalFilesDir
//--------------------------------------------------------------------------

/**
 * 获取此应用在外置储存中的缓存目录
 * <pre>path: /storage/emulated/0/Android/data/package/cache</pre>
 *
 * @return 此应用在外置储存中的缓存目录
 */
fun getExternalCacheDir(): String? = FileOperator.getContext().externalCacheDir?.absolutePath

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
fun getExternalFilesDir(): String? = FileOperator.getContext().getExternalFilesDir(null)?.absolutePath

/**
 * 获取此应用在外置储存中的闹钟铃声目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Alarms</pre>
 *
 * @return 此应用在外置储存中的闹钟铃声目录
 */
fun getExternalFilesDirALARMS(): String? = FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_ALARMS)?.absolutePath

/**
 * 获取此应用在外置储存中的相机目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/DCIM</pre>
 *
 * @return 此应用在外置储存中的相机目录
 */
fun getExternalFilesDirDCIM(): String? = FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath

/**
 * 获取此应用在外置储存中的文档目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Documents</pre>
 *
 * @return 此应用在外置储存中的文档目录
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
fun getExternalFilesDirDOCUMENTS(): String? = FileOperator.getContext()
    .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath

/**
 * 获取此应用在外置储存中的下载目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Download</pre>
 *
 * @return 此应用在外置储存中的闹钟目录
 */
fun getExternalFilesDirDOWNLOADS(): String? = FileOperator.getContext()
    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath

/**
 * 获取此应用在外置储存中的视频目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Movies</pre>
 *
 * @return 此应用在外置储存中的视频目录
 */
fun getExternalFilesDirMOVIES(): String? = FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath

/**
 * 获取此应用在外置储存中的音乐目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Music</pre>
 *
 * @return 此应用在外置储存中的音乐目录
 */
fun getExternalFilesDirMUSIC(): String? = FileOperator.getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath

/**
 * 获取此应用在外置储存中的提示音目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Notifications</pre>
 *
 * @return 此应用在外置储存中的提示音目录
 */
fun getExternalFilesDirNOTIFICATIONS(): String? = FileOperator.getContext()
    .getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)?.absolutePath

/**
 * 获取此应用在外置储存中的图片目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Pictures</pre>
 *
 * @return 此应用在外置储存中的图片目录
 */
fun getExternalFilesDirPICTURES(): String? = FileOperator.getContext()
    .getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath

/**
 * 获取此应用在外置储存中的 Podcasts 目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Podcasts</pre>
 *
 * @return 此应用在外置储存中的 Podcasts 目录
 */
fun getExternalFilesDirPODCASTS(): String? = FileOperator.getContext()
    .getExternalFilesDir(Environment.DIRECTORY_PODCASTS)?.absolutePath

/**
 * 获取此应用在外置储存中的铃声目录
 * <pre>path: /storage/emulated/0/Android/data/package/files/Ringtones</pre>
 *
 * @return 此应用在外置储存中的铃声目录
 */
fun getExternalFilesDirRINGTONES(): String? = FileOperator.getContext()
    .getExternalFilesDir(Environment.DIRECTORY_RINGTONES)?.absolutePath

//从 File Path 中获取 Uri
//----------------------------------------------------------------

fun getUriByPath(path: String?): Uri? = if (path.isNullOrBlank()) null else getUriByFile(File(path))

/**
 * Return a content URI for a given file.
 *
 * @param file The file.
 * @return a content URI for a given file
 */
fun getUriByFile(file: File?): Uri? {
    if (file == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val authority = FileOperator.getContext().packageName + PATH_SUFFIX
        FileProvider.getUriForFile(FileOperator.getContext(), authority, file)
    } else {
        Uri.fromFile(file)
    }
}


//获取Uri对应的文件路径 兼容API 26
//----------------------------------------------------------------

/**
 * 根据uri获取文件的绝对路径，解决Android 4.4以上 根据uri获取路径的方法
 *
 * <p>
 *     RequiresPermission(permission.READ_EXTERNAL_STORAGE)
 * @param uri
 * @return
 */
fun getFilePathByUri(uri: Uri?): String? = getFilePathByUri(FileOperator.getContext(), uri)

/**
 * 根据uri获取文件的绝对路径，解决Android 4.4以上 根据uri获取路径的方法
 *
 * <p>
 *      RequiresPermission(permission.READ_EXTERNAL_STORAGE)
 * @param context
 * @param uri
 * @return
 */
fun getFilePathByUri(context: Context?, uri: Uri?): String? {
    if (context == null || uri == null) return null
    val scheme = uri.scheme
    // 以 file:// 开头的
    if (ContentResolver.SCHEME_FILE.equals(scheme, ignoreCase = true)) {//使用第三方应用打开
        uri.path
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //4.4以后
        getPath(context, uri)
    } else { //4.4以下
        getPathKitkat(context, uri)
    }
}

private fun getPathKitkat(context: Context, contentUri: Uri): String? =
    context.contentResolver.query(contentUri, null, null, null, null).use { c ->
        val column = MediaStore.Files.FileColumns.DATA
        return if (null != c && c.moveToFirst()) c.getString(c.getColumnIndex(column)) else null
    }

/**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.<br></br>
 * <br></br>
 * Callers should check whether the path is local before assuming it
 * represents a local file.
 *
 * @param context The context.
 * @param uri     The Uri to query.
 * @see #isLocal
 * @see #getFile
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
private fun getPath(context: Context, uri: Uri?): String? {
    if (uri != null) {
        FileLogger.i(
            "FileUri getPath -> File : ",
            "Authority: " + uri.authority +
                    ", Fragment: " + uri.fragment +
                    ", Port: " + uri.port +
                    ", Query: " + uri.query +
                    ", Scheme: " + uri.scheme +
                    ", Host: " + uri.host +
                    ", Segments: " + uri.pathSegments.toString()
        )
    }
    val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory()
                    .toString() + File.separator + split[1]
            } else if ("home".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + File.separator + "documents" + File.separator + split[1]
            }
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            if (id != null && id.startsWith("raw:")) {
                return id.substring(4)
            }
            // Android Q 待验证
            val contentUriPrefixesToTry = arrayOf(
                "content://downloads/public_downloads",
                "content://downloads/my_downloads",
                "content://downloads/all_downloads"
            )
            for (contentUriPrefix in contentUriPrefixesToTry) {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse(contentUriPrefix),
                    id.toLong()
                )
                try {
                    val path = getDataColumn(context, contentUri, null, null)
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }
                } catch (e: Exception) {
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return resolveAndroidQPath(uri)
            }
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            when (type) {
                "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            return getDataColumn(context, contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(uri?.scheme, ignoreCase = true)) {
        // Return the remote address
        if (isGooglePhotosUri(uri)) {
            return uri?.lastPathSegment
        } else if (isHuaWeiUri(uri)) {
            val uriPath = uri?.path
            //content://com.huawei.hidisk.fileprovider/root/storage/emulated/0/Android/data/com.xxx.xxx/
            if (uriPath != null && uriPath.startsWith("/root")) {
                return uriPath.replace("/root".toRegex(), "")
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) resolveAndroidQPath(uri)
        else getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri?.scheme, ignoreCase = true)) {
        uri?.path
    }
    return uri?.path
}

/**
 * <pre>
 * 两个BUG :
 * 1.Android 10 闪退问题
 * 2.部分机型进入"文件管理器" 执行到  cursor.getColumnIndexOrThrow(column);出现
 * Caused by: java.lang.IllegalArgumentException: column '_data' does not exist. Available columns: []
 * Fixed :
 * FileChooserPathUtils 注释
 * 和
 * https://stackoverflow.com/questions/42508383/illegalargumentexception-column-data-does-not-exist
 * </pre>
 */
private fun getDataColumn(
    context: Context,
    uri: Uri?,
    selection: String?,
    selectionArgs: Array<String>?
): String? {
    val column = MediaStore.Files.FileColumns.DATA
    val projection = arrayOf(column)
    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(uri ?: return null, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(column)
            return cursor.getString(columnIndex)
        } else {
            uri.path
        }
    } finally {
        cursor?.close()
    }
    return uri?.path
}

/**
 * 从 API 26 中解析出路径
 * <pre>
 * 错误的方式 -> 采用复制文件的方式重新写入再获取路径
 * https://stackoverflow.com/questions/42508383/illegalargumentexception-column-data-does-not-exist
 */
@TargetApi(Build.VERSION_CODES.O)
fun resolveAndroidQPath(uri: Uri?): String? = uri?.path

//File Name
//----------------------------------------------------------------

fun getFileNameFromPath(path: String?): String? {
    if (path.isNullOrBlank()) return null
    val cut = path.lastIndexOf('/')
    if (cut != -1) return path.substring(cut + 1)
    return path
}

fun getFileNameFromUri(uri: Uri?): String? {
    if (uri == null) return null
    var filename: String? = null

    val resolver = FileOperator.getContext().contentResolver
    val mimeType = resolver.getType(uri)
    if (mimeType == null) {
        filename = getFileNameFromPath(getFilePathByUri(uri))
    } else {
        val cursor = resolver.query(
            uri, null, null, null, null
        )
        if (cursor != null) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            filename = cursor.getString(nameIndex)
            cursor.close()
        }
    }
    return filename
}

//File Extension
//----------------------------------------------------------------


//The Uri to check
//----------------------------------------------------------------

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is Google Photos.
 */
fun isGooglePhotosUri(uri: Uri?): Boolean {
    return "com.google.android.apps.photos.content" == uri?.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
private fun isExternalStorageDocument(uri: Uri?): Boolean {
    return "com.android.externalstorage.documents" == uri?.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
private fun isDownloadsDocument(uri: Uri?): Boolean {
    return "com.android.providers.downloads.documents" == uri?.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
private fun isMediaDocument(uri: Uri?): Boolean {
    return "com.android.providers.media.documents" == uri?.authority
}

/**
 * content://com.huawei.hidisk.fileprovider/root/storage/emulated/0/Android/data/com.xxx.xxx/
 *
 * @param uri
 * @return
 */
private fun isHuaWeiUri(uri: Uri?): Boolean {
    return "com.huawei.hidisk.fileprovider" == uri?.authority
}

