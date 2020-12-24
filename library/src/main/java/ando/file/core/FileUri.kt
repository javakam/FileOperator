/**
 * Copyright (C)  javakam, FileOperator Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ando.file.core

import ando.file.FileOperator
import android.annotation.SuppressLint
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
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


/**
 * Title: FileUri
 * <p>
 * Description: Uri 工具类
 * </p>
 * @author javakam
 * @date 2020/8/24  11:24
 */
object FileUri {

    //从 File Path 中获取 Uri
    //----------------------------------------------------------------

    fun getUriByPath(path: String?): Uri? = if (path.isNullOrBlank()) null else getUriByFile(File(path))

    /**
     * Return a content URI for a given file.
     *
     * @param file The file.
     * @return a content URI for a given file
     */
    fun getUriByFile(file: File?): Uri? =
        file?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val authority = FileOperator.getContext().packageName + AUTHORITY
                FileProvider.getUriForFile(FileOperator.getContext(), authority, file)
            } else Uri.fromFile(file)
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
     * 需要权限: RequiresPermission(permission.READ_EXTERNAL_STORAGE)
     *
     * @param context
     * @param uri
     * @return
     */
    fun getFilePathByUri(context: Context?, uri: Uri?): String? {
        if (context == null || uri == null) return null
        val scheme = uri.scheme
        // 以 file:// 开头的使用第三方应用打开
        if (ContentResolver.SCHEME_FILE.equals(scheme, ignoreCase = true)) return uri.path
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) getPath(context, uri) else getPathKitkat(context, uri)
    }

    /**
     * 4.4版本
     */
    private fun getPathKitkat(context: Context, contentUri: Uri): String? =
        context.contentResolver.query(contentUri, null, null, null, null).use { c ->
            @Suppress("DEPRECATION")
            val column = MediaStore.Files.FileColumns.DATA
            return if (null != c && c.moveToFirst()) c.getString(c.getColumnIndex(column)) else null
        }

    /**
     * 从Uri获取文件路径。这将获取Storage Access Framework文档的路径，
     * 以及MediaStore和其他基于文件的ContentProviders的_data字段。<br>
     * <br>
     * 调用者应在假定该路径代表本地文件之前检查该路径是否为本地
     *
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * - 错误的方式: 采用复制文件的方式重新写入再获取路径, 很多项目里都是用的这种方式很坑...
     *
     *      https://stackoverflow.com/questions/42508383/illegalargumentexception-column-data-does-not-exist
     *
     * - Changed From: https://github.com/coltoscosmin/FileUtils/blob/42050f5791331bec2a888dc1d368aef128e98a3e/FileUtils.java#L113
     *
     * @param context Context
     * @param uri     要查询的Uri(The Uri to query)
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun getPath(context: Context, uri: Uri?): String? {
        if (uri != null) {
            FileLogger.i(
                "FileUri getPath -> Authority: " + uri.authority +
                        ", Fragment: " + uri.fragment +
                        ", Port: " + uri.port +
                        ", Query: " + uri.query +
                        ", Scheme: " + uri.scheme +
                        ", Host: " + uri.host +
                        ", Segments: " + uri.pathSegments.toString()
            )
        } else return null

        @SuppressLint("ObsoleteSdkInt")
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
            if (isLocalStorageDocument(uri)) {
                // The path is the id
                return DocumentsContract.getDocumentId(uri);
            }
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    @Suppress("DEPRECATION")
                    return Environment.getExternalStorageDirectory().toString() + File.separator + split[1]
                } else if ("home".equals(type, ignoreCase = true)) {
                    @Suppress("DEPRECATION")
                    return Environment.getExternalStorageDirectory().toString() + File.separator + "documents" + File.separator + split[1]
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4)
                }
                val contentUriPrefixesToTry = arrayOf(
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads",
                    "content://downloads/all_downloads"
                )
                for (contentUriPrefix in contentUriPrefixesToTry) {
                    val contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), id.toLong())
                    try {
                        val path = getDataColumn(context, contentUri, null, null)
                        if (!path.isNullOrBlank()) return path
                    } catch (e: Exception) {
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return uri.path
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val contentUri: Uri? = when (split[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
            //GoogleDriveProvider
            else if (isGoogleDriveUri(uri)) {
                return getGoogleDriveFilePath(uri, context)
            }
        }
        // MediaStore (and general)
        else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            // Google drive legacy provider
            else if (isGoogleDriveUri(uri)) {
                return getGoogleDriveFilePath(uri, context)
            }
            // Huawei
            else if (isHuaWeiUri(uri)) {
                val uriPath = uri.path ?: uri.toString()
                //content://com.huawei.hidisk.fileprovider/root/storage/emulated/0/Android/data/com.xxx.xxx/
                if (uriPath.startsWith("/root")) {
                    return uriPath.replace("/root".toRegex(), "")
                }
            }
            //For AndroidQ: getDataColumn is same as uri.path
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) uri.path
            else getDataColumn(context, uri, null, null)
        }
        // File
        else if ("file".equals(uri.scheme, true)) {
            return uri.path
        }
        return uri.toString()
    }

    /**
     * BUG :
     *      1.Android 10 闪退问题
     *      2.部分机型进入"文件管理器" 执行到  cursor.getColumnIndexOrThrow(column);出现
     *          Caused by: java.lang.IllegalArgumentException: column '_data' does not exist. Available columns: []
     * Fixed :
     *      FileChooserPathUtils 注释
     *      &
     *      https://stackoverflow.com/questions/42508383/illegalargumentexception-column-data-does-not-exist
     */
    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        @Suppress("DEPRECATION")
        val column = MediaStore.Files.FileColumns.DATA
        val projection = arrayOf(column)
        context.contentResolver.query(uri ?: return null, projection, selection, selectionArgs, null)?.use {
            try {
                if (it.moveToFirst()) {
                    //if (isDebug()) DatabaseUtils.dumpCursor(it)
                    val columnIndex = it.getColumnIndex(column)
                    return it.getString(columnIndex)
                } else uri.path
            } catch (e: Throwable) {
                FileLogger.e("getDataColumn -> ${e.message}")
            }
        }
        return uri.path
    }

    //The Uri to check
    //----------------------------------------------------------------

    private fun getGoogleDriveFilePath(uri: Uri, context: Context): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { c: Cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             *     * move to the first row in the Cursor, get the data,
             *     * and display it.
             * */
            val nameIndex: Int = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex: Int = c.getColumnIndex(OpenableColumns.SIZE)
            c.moveToFirst()
            val name: String = c.getString(nameIndex)
            val size = c.getLong(sizeIndex).toString()
            val file = File(context.cacheDir, name)

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                outputStream = FileOutputStream(file)
                var read = 0
                val maxBufferSize = 1 * 1024 * 1024
                val bytesAvailable: Int = inputStream?.available() ?: 0
                val bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
                val buffers = ByteArray(bufferSize)
                while (inputStream?.read(buffers)?.also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
                outputStream?.close()
                c.close()
            }
            return file.path
        }
        return uri.toString()
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri?): Boolean {
        return "com.google.android.apps.photos.content".equals(uri?.authority, true)
    }

    fun isGoogleDriveUri(uri: Uri?): Boolean {
        return "com.google.android.apps.docs.storage.legacy" == uri?.authority || "com.google.android.apps.docs.storage" == uri?.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is local.
     */
    fun isLocalStorageDocument(uri: Uri?): Boolean {
        return AUTHORITY.equals(uri?.authority, true)
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents".equals(uri?.authority, true)
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri?): Boolean {
        return "com.android.providers.downloads.documents".equals(uri?.authority, true)
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri?): Boolean {
        return "com.android.providers.media.documents".equals(uri?.authority, true)
    }

    /**
     * content://com.huawei.hidisk.fileprovider/root/storage/emulated/0/Android/data/com.xxx.xxx/
     *
     * @param uri
     * @return
     */
    private fun isHuaWeiUri(uri: Uri?): Boolean {
        return "com.huawei.hidisk.fileprovider".equals(uri?.authority, true)
    }

}