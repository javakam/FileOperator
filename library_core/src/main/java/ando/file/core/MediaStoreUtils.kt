package ando.file.core

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.exifinterface.media.ExifInterface
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author javakam
 * @date 2021-09-09  11:23
 */
object MediaStoreUtils {

    /**
     * mapping ->
     *  MediaStore.Video.Media._ID,
     *  MediaStore.Video.Media.DISPLAY_NAME,
     *  MediaStore.Video.Media.DURATION,
     *  MediaStore.Video.Media.SIZE
     */
    data class MediaStoreVideo(var id: Long, var uri: Uri?, var displayName: String?, var duration: Long?, var size: Long?)

    /**
     * mapping ->
     *  MediaStore.Image.Media._ID,
     *  MediaStore.Image.Media.DISPLAY_NAME,
     */
    data class MediaStoreImage(
        var id: Long, var uri: Uri?, var displayName: String?, var size: Long?, var description: String?,
        var title: String?, var mimeType: String?, var dateAdded: Date?,
    ) {
        constructor(uri: Uri?, displayName: String?, size: Long?) :
                this(0L, uri, displayName, size, null, null, null, null)
    }

    //MediaStore
    //------------------------------------------------------------------------------------------------

    /**
     * ### Create ContentValues
     * ```
     * values.put(MediaStore.Images.Media.IS_PENDING, isPending)
     * Android Q , MediaStore中添加 MediaStore.Images.Media.IS_PENDING flag，用来表示文件的 isPending 状态，0是可见，其他不可见
     * ```
     * @param displayName 文件名
     * @param description 描述
     * @param mimeType 媒体类型
     * @param title 标题
     * @param relativePath 相对路径 eg: ${Environment.DIRECTORY_PICTURES}/xxx
     * @param isPending 默认0 , 0是可见，其他不可见
     */
    fun createContentValues(
        displayName: String? = null, description: String? = null, mimeType: String? = null, title: String? = null,
        relativePath: String? = null, isPending: Int? = 1,
    ): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.DESCRIPTION, description)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.TITLE, title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, isPending)
            }
        }
    }

    /**
     * ContentResolver的insert方法, 将多媒体文件保存到多媒体的公共集合目录
     *
     * https://developer.huawei.com/consumer/cn/doc/50127
     * ```
     * 可以通过PRIMARY_DIRECTORY和SECONDARY_DIRECTORY字段来设置一级目录和二级目录：
     *（a）一级目录必须是和MIME type的匹配的根目录下的Public目录，一级目录可以不设置，不设置时会放到默认的路径；
     *（b）二级目录可以不设置，不设置时直接保存在一级目录下；
     *（c）应用生成的文档类文件，代码里面默认不设置时，一级是Downloads目录，也可以设置为Documents目录，建议推荐三方应用把文档类的文件一级目录设置为Documents目录；
     *（d）一级目录MIME type，默认目录、允许的目录映射以及对应的读取权限如下表所示： https://user-gold-cdn.xitu.io/2020/6/1/1726dd80a91347cf?w=1372&h=470&f=png&s=308857
     * ```
     * @param uri 多媒体数据库的Uri MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
     * @param context
     * @param mimeType 需要保存文件的mimeType
     * @param displayName 显示的文件名字
     * @param description 文件描述信息
     * @param saveFileName 需要保存的文件名字
     * @param saveSecondaryDir 保存的二级目录
     * @param savePrimaryDir 保存的一级目录  eg : Environment.DIRECTORY_DCIM
     * @return 返回插入数据对应的uri
     */
    fun insertMediaFile(
        uri: Uri?, context: Context, mimeType: String?, displayName: String?, description: String?,
        saveFileName: String?, saveSecondaryDir: String?, savePrimaryDir: String?,
    ): String? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        values.put(MediaStore.Images.Media.DESCRIPTION, description)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, savePrimaryDir + File.separator + saveSecondaryDir)
        }
        //else {
        //    values.put(MediaStore.Images.Media.PRIMARY_DIRECTORY, savePrimaryDir)
        //    values.put(MediaStore.Images.Media.SECONDARY_DIRECTORY, saveSecondaryDir)
        //}
        var url: Uri? = null
        var stringUrl: String? = null /* value to be returned */
        val cr = context.contentResolver
        try {
            if (uri == null || saveFileName.isNullOrBlank()) return null
            url = cr.insert(uri, values) ?: return null
            val buffer = ByteArray(1024)

            val pfd = FileGlobal.openFileDescriptor(uri, FileGlobal.MODE_WRITE_ONLY_ERASING)
            if (pfd != null) {
                val fos = FileOutputStream(pfd.fileDescriptor)
                val ins = context.resources.assets.open(saveFileName)
                while (true) {
                    val numRead = ins.read(buffer)
                    if (numRead == -1) {
                        break
                    }
                    fos.write(buffer, 0, numRead)
                }
                fos.flush()
                try {
                    fos.close()
                } catch (e: IOException) {
                }
                try {
                    pfd.close()
                } catch (e: IOException) {
                }
            }
        } catch (e: Exception) {
            FileLogger.e("Failed to insert media file ${e.message}")
            if (url != null) {
                cr.delete(url, null, null)
                url = null
            }
        }
        if (url != null) {
            stringUrl = url.toString()
        }
        return stringUrl
    }

    /**
     * ```
     * 1.会出现创建多个图片问题
     *
     * 2.MediaStore.Images.Media.INTERNAL_CONTENT_URI
     *
     * java.lang.UnsupportedOperationException: Writing to internal storage is not supported.
     *    at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:172)
     *    at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:140)
     *    at android.content.ContentProviderProxy.insert(ContentProviderNative.java:481)
     *    at android.content.ContentResolver.insert(ContentResolver.java:1844)
     * ```
     */
    fun insertBitmap(bitmap: Bitmap?, values: ContentValues): Uri? {
        val externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val resolver = FileOperator.getContext().contentResolver
        val insertUri = resolver.insert(externalUri, values)
        //标记当前文件是 Pending 状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
            //MediaStore.setIncludePending(insertUri)
        }
        var os: OutputStream? = null
        try {
            if (insertUri != null && bitmap != null) {
                os = resolver.openOutputStream(insertUri)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os?.flush()

                FileLogger.d("创建Bitmap成功 insertBitmap $insertUri")

                //https://developer.android.google.cn/training/data-storage/files/media#native-code
                // Now that we're finished, release the "pending" status, and allow other apps to view the image.
                values.clear()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(insertUri, values, null, null)
                }
            }
        } catch (e: Exception) {
            FileLogger.d("创建失败：${e.message}")
        } finally {
            if (bitmap?.isRecycled == false) bitmap.recycle()
            try {
                os?.close()
            } catch (t: Throwable) {
            }
            return insertUri
        }
    }

    fun insertAudio(displayName: String?) {
        val resolver = FileOperator.getContext().contentResolver
        //https://developer.android.google.cn/training/data-storage/shared/media#kotlin
        // Find all audio files on the primary external storage device.
        // On API <= 28, use VOLUME_EXTERNAL instead.
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }

        // Publish a new song.
        val songDetails =
            createContentValues(displayName, null, null, null, "${Environment.DIRECTORY_MUSIC}/audio", 1)

        // Keeps a handle to the new song's URI in case we need to modify it later.
        val songContentUri = resolver.insert(audioCollection, songDetails)

        songContentUri?.let {
            resolver.openFileDescriptor(songContentUri, "w", null).use {
                // Write data into the pending audio file.
            }
            // Now that we're finished, release the "pending" status, and allow other apps to play the audio track.
            songDetails.clear()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                songDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(songContentUri, songDetails, null, null)
            }
        }
    }

    /**
     * 创建 contentResolver.query 中的两个参数 String selection 和 String[] selectionArgs
     */
    fun buildQuerySelectionStatement(
        @FileGlobal.FileMediaType mediaType: String, displayName: String?, description: String?,
        mimeType: String?, title: String?, relativePath: String?, isFuzzy: Boolean,
    ): FileGlobal.QuerySelectionStatement {
        val symbol = if (isFuzzy) " like " else " = "
        val selection = StringBuilder()
        val selectionArgs: MutableList<String> = mutableListOf()

        var needAddPre = false
        if (!displayName.isNullOrBlank()) {
            val columnDisplayName: String = when (mediaType) {
                FileGlobal.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.DISPLAY_NAME
                FileGlobal.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.DISPLAY_NAME
                else -> MediaStore.Images.Media.DISPLAY_NAME
            }
            selection.append(" $columnDisplayName $symbol ? ")
            selectionArgs.add(displayName)
            needAddPre = true
        }
        if (!description.isNullOrBlank() && mediaType != FileGlobal.MEDIA_TYPE_AUDIO) {// MediaStore.Audio 没有 DESCRIPTION 字段
            val columnDescription: String = when (mediaType) {
                FileGlobal.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.DESCRIPTION
                else -> MediaStore.Images.Media.DESCRIPTION
            }

            selection.append("${if (needAddPre) " and " else " "} $columnDescription $symbol ? ")
            selectionArgs.add(description)
            needAddPre = true
        }
        if (!title.isNullOrBlank()) {
            val columnTitle: String = when (mediaType) {
                FileGlobal.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.TITLE
                FileGlobal.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.TITLE
                else -> MediaStore.Images.Media.TITLE
            }

            selection.append("${if (needAddPre) " and " else " "} $columnTitle $symbol ? ")
            selectionArgs.add(title)
            needAddPre = true
        }
        if (!mimeType.isNullOrBlank()) {
            val columnMimeType: String = when (mediaType) {
                FileGlobal.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.MIME_TYPE
                FileGlobal.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.MIME_TYPE
                else -> MediaStore.Images.Media.MIME_TYPE
            }
            selection.append("${if (needAddPre) " and " else " "} $columnMimeType $symbol ? ")
            selectionArgs.add(mimeType)
            needAddPre = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!relativePath.isNullOrBlank()) {
                val columnRelativePath: String = when (mediaType) {
                    FileGlobal.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.RELATIVE_PATH
                    FileGlobal.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.RELATIVE_PATH
                    else -> MediaStore.Images.Media.RELATIVE_PATH
                }
                selection.append("${if (needAddPre) " and " else " "} $columnRelativePath $symbol ? ")
                selectionArgs.add(relativePath)
                needAddPre = true
            }
        }

        FileLogger.i("查询语句= $selection ")
        return FileGlobal.QuerySelectionStatement(selection, selectionArgs, needAddPre)
    }

    // MediaStore.XXX.Media.EXTERNAL_CONTENT_URI
    fun createMediaCursor(
        uri: Uri, projectionArgs: Array<String>? = arrayOf(MediaStore.Video.Media._ID),
        sortOrder: String? = null, querySelectionStatement: FileGlobal.QuerySelectionStatement? = null,
    ): Cursor? {
        // Need the READ_EXTERNAL_STORAGE permission if accessing video files that your app didn't create.
        when (FileOperator.getContext()
            .checkUriPermission(uri, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION)) {
            PackageManager.PERMISSION_GRANTED -> {
            }
            PackageManager.PERMISSION_DENIED -> {
                FileOperator.getContext().grantUriPermission(FileOperator.getApplication().packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        return FileOperator.getContext().contentResolver.query(
            uri, projectionArgs,
            querySelectionStatement?.selection.toString(),
            querySelectionStatement?.selectionArgs?.toTypedArray(),
            sortOrder
        )
    }

    @RequiresPermission(value = Manifest.permission.READ_EXTERNAL_STORAGE)
    fun queryMediaStoreVideo(
        projectionArgs: Array<String>? = arrayOf(MediaStore.Video.Media._ID),
        sortOrder: String? = null, sourceDuration: Long, sourceUnit: TimeUnit,
    ): MutableList<MediaStoreVideo>? {
        // Need the READ_EXTERNAL_STORAGE permission if accessing video files that your app didn't create.

        // Container for information about each video.
        val videoList = mutableListOf<MediaStoreVideo>()
        val external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val queryStatement = buildQuerySelectionStatement(
            FileGlobal.MEDIA_TYPE_VIDEO, null, null, null, null, null, false
        )
        // Show only videos that are at least 5 minutes in duration.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryStatement.append(
                "${MediaStore.Video.Media.DURATION} >= ? ",
                TimeUnit.MILLISECONDS.convert(sourceDuration, sourceUnit).toString()
            )
        }
        createMediaCursor(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projectionArgs, sortOrder, queryStatement)?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            } else {
                //VERSION.SDK_INT < Q)
                0
            }
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(external, id)
                // Stores column values and the contentUri in a local object that represents the media file.
                videoList += MediaStoreVideo(id, contentUri, name, duration.toLong(), size.toLong())
            }
            return videoList
        }
        return null
    }

    fun queryMediaStoreImages(
        projectionArgs: Array<String>? = arrayOf(MediaStore.Images.Media._ID), sortOrder: String? = null, displayName: String?,
        description: String?, mimeType: String?, title: String?, relativePath: String?, isFuzzy: Boolean,
    ): MutableList<MediaStoreImage>? {
        val queryStatement =
            buildQuerySelectionStatement(FileGlobal.MEDIA_TYPE_IMAGE, displayName, description, mimeType, title, relativePath, isFuzzy)
        return queryMediaStoreImages(projectionArgs, sortOrder, queryStatement)
    }

    fun queryMediaStoreImages(displayName: String): Uri? = queryMediaStoreImages(displayName, false)

    fun queryMediaStoreImages(displayName: String, isFuzzy: Boolean): Uri? {
        val images = queryMediaStoreImages(null, null, displayName, null, null, null, null, isFuzzy)
        if (images.isNullOrEmpty()) {
            return null
        }
        return images[0].uri
    }

    /**
     * 查询全部图片
     */
    fun queryMediaStoreImages(): MutableList<MediaStoreImage>? {
        val queryStatement = buildQuerySelectionStatement(
            FileGlobal.MEDIA_TYPE_IMAGE, null, null, null, null, null, true
        )
        return queryMediaStoreImages(null, null, queryStatement)
    }

    /**
     * 加载媒体文件的集合 👉 ContentResolver.query
     * <pre>
     * 官方指南 👉 内容提供程序基础知识
     * https://developer.android.com/guide/topics/providers/content-provider-basics?hl=zh-cn
     * </pre>
     * 注意事项:
     * 1.多次测试表明 displayName/description/mimeType 可以 作为 and 多条件查询,而其他的字段则会干扰查询结果
     * 2.like 模糊查询,忽略文件名的大小写 ;  =  字段值必须完全一致
     */
    fun queryMediaStoreImages(
        projectionArgs: Array<String>? = arrayOf(MediaStore.Images.Media._ID), sortOrder: String? = null,
        querySelectionStatement: FileGlobal.QuerySelectionStatement?,
    ): MutableList<MediaStoreImage>? {
        val imageList = mutableListOf<MediaStoreImage>()
        val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor: Cursor?
        try {
            cursor = createMediaCursor(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionArgs, sortOrder, querySelectionStatement)
            FileLogger.i("Found ${cursor?.count} images")

            cursor?.use {
                // Cache column indices.
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val descColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DESCRIPTION)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
                val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (it.moveToNext()) { //moveToFirst  moveToNext
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val size = it.getInt(sizeColumn)
                    val desc = it.getString(descColumn)
                    val titleRs = it.getString(titleColumn)
                    val mimeTypeRs = it.getString(mimeColumn)
                    val dateModified = Date(TimeUnit.SECONDS.toMillis(it.getLong(dateModifiedColumn)))

                    val contentUri: Uri = ContentUris.withAppendedId(external, id)
                    imageList += MediaStoreImage(
                        id, contentUri, name, size.toLong(),
                        desc, titleRs, mimeTypeRs, dateModified
                    )
                }
                if (imageList.isNullOrEmpty()) {
                    FileLogger.e("查询失败!")
                }
                imageList.let { l ->
                    l.forEach { img ->
                        FileLogger.d("查询成功，Uri路径  ${img.uri}")
                    }
                }
            }
            return imageList
        } catch (e: Exception) {
            FileLogger.e("查询失败! ${e.message}")
        }
        return null
    }

    //Storage Access Framework (SAF) 👉 https://developer.android.google.cn/training/data-storage/shared/documents-files
    //------------------------------------------------------------------------------------------------

    fun checkUriColumnFlag(uri: Uri, flag: Int): Boolean {
        val cursor = FileOperator.getContext().contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnFlags = cursor.getInt(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS))
            FileLogger.i("Column Flags：$columnFlags  Flag：$flag")
            if (columnFlags >= flag) {
                return true
            }
            cursor.close()
        }
        return false
    }

    /**
     * 选择一个图片文件
     */
    fun selectImage(activity: Activity, requestCode: Int) = selectFile(activity, "image/*", requestCode)

    /**
     * 选择一个文件
     */
    fun selectFile(activity: Activity, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * ### 新建文件 SAF
     *
     * `mimeType 和 fileName 传反了 👉 android.content.ActivityNotFoundException: No Activity found to handle Intent`
     */
    fun createFile(activity: Activity, pickerInitialUri: Uri?, fileName: String, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * ### 打开文件 SAF
     * ```
     * 请注意以下事项：
     *      1.当应用触发 ACTION_OPEN_DOCUMENT Intent 时，该 Intent 会启动选择器，以显示所有匹配的文档提供程序。
     *      2.在 Intent 中添加 CATEGORY_OPENABLE 类别可对结果进行过滤，从而只显示可打开的文档（如图片文件）。
     *      3.intent.setType("image/ *") 语句可做进一步过滤，从而只显示 MIME 数据类型为图像的文档。
     * ```
     */
    fun openFile(activity: Activity, pickerInitialUri: Uri?, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * ### 打开目录 SAF
     *
     * 接收数据 :
     *
     * ```kotlin
     * override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
     *      if (requestCode == your-request-code && resultCode == Activity.RESULT_OK) {
     *          // The result data contains a URI for the document or directory that the user selected.
     *          resultData?.data?.also { uri ->
     *              // Perform operations on the document using its URI.
     *          }
     *      }
     * }
     * ```
     */
    fun openDirectory(activity: Activity, pickerInitialUri: Uri?, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * 移动文件 SAF
     */
    fun moveFile(sourceDocumentUri: Uri, sourceParentDocumentUri: Uri, targetParentDocumentUri: Uri) {
        //Document.COLUMN_FLAGS  DocumentsProvider.moveDocument(String, String, String)
        if (checkUriColumnFlag(sourceDocumentUri, DocumentsContract.Document.FLAG_SUPPORTS_MOVE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    DocumentsContract.moveDocument(FileOperator.getContext().contentResolver,
                        sourceDocumentUri,
                        sourceParentDocumentUri,
                        targetParentDocumentUri)
                } catch (e: FileNotFoundException) {
                    FileLogger.e("${e.message}")
                }
            }
        }
    }

    /**
     * 删除文件 SAF
     */
    fun deleteFile(uri: Uri): Boolean {
        if (checkUriColumnFlag(uri, DocumentsContract.Document.FLAG_SUPPORTS_DELETE)) {
            return DocumentsContract.deleteDocument(FileOperator.getContext().contentResolver, uri)
        }
        return false
    }

    /**
     * ### 重命名文件 SAF
     *
     * ```
     * 注意重名文件
     *
     * 对同一Uri对应的文件重命名不能重复，新旧名相同会报错 java.lang.IllegalStateException: File already exists
     * 因此先判断比对旧Uri对应的文件名是否和 newDisplayName 是否相同
     * ```
     */
    fun renameFile(uri: Uri, newDisplayName: String?, block: (isSuccess: Boolean, msg: String) -> Unit) {
        if (!checkUriColumnFlag(uri, DocumentsContract.Document.FLAG_SUPPORTS_RENAME)) {
            block.invoke(false, "重命名失败")
            return
        }

        val cursor = FileOperator.getContext().contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {//新旧名不能相同
                val displayName =
                    cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                if (!displayName.equals(if (newDisplayName.isNullOrBlank()) "" else newDisplayName, true)) {
                    DocumentsContract.renameDocument(FileOperator.getContext().contentResolver, uri, newDisplayName ?: "")
                }
                //查看目录中是否已存在 newDisplayName 的文件 -> 涉及到获取当前目录临时权限,太麻烦了,交给外部做吧 getDocumentTree
                // try {
                //     val root: DocumentFile? = getDocumentTree(activity ,uri,)
                //     val findFile = root?.findFile(newDisplayName ?: "")
                // } catch (e: SecurityException) {
                // }
                block.invoke(true, "重命名成功")
                return
            }
        } catch (e: Exception) {
            FileLogger.e(e.message)
            block.invoke(false, "已存在该名称的文件")
            return
        } finally {
            try {
                cursor?.close()
            } catch (e: IOException) {
            }
        }
        block.invoke(false, "重命名失败")
    }

    /**
     * ### 照片的位置信息
     */
    @RequiresPermission(value = Manifest.permission.ACCESS_MEDIA_LOCATION)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMediaLocation(uri: Uri, block: (latLong: FloatArray) -> Unit) {
        val photoUri = MediaStore.setRequireOriginal(uri)
        FileOperator.getContext().contentResolver.openInputStream(photoUri)?.use { stream ->
            ExifInterface(stream).run {
                val latLong: FloatArray = floatArrayOf(0F, 0F)
                // If lat/long is null, fall back to the coordinates (0, 0).
                // val latLongResult = getLatLong(latLong)
                block.invoke(latLong)
            }
        }
    }

    /**
     * ### 通过Uri获取Bitmap,耗时操作不应该在主线程
     *
     * https://developer.android.google.cn/training/data-storage/shared/documents-files#bitmap
     *
     * Note: You should complete this operation on a background thread, not the UI thread.
     */
    @Throws(IOException::class, IllegalStateException::class)
    fun getBitmapFromUri(uri: Uri?): Bitmap? =
        FileGlobal.openFileDescriptor(uri, FileGlobal.MODE_READ_ONLY)?.fileDescriptor?.let {
            BitmapFactory.decodeFileDescriptor(it)
        }

    /**
     * ### 读取文档信息
     *
     * https://developer.android.google.cn/training/data-storage/shared/documents-files#input_stream
     */
    fun readTextFromUri(uri: Uri): String {
        val sb = StringBuilder()
        FileOperator.getContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
            }
        }
        return sb.toString()
    }

    fun readTextFromUri(uri: Uri, block: (result: String?) -> Unit) {
        FileOperator.getContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val sb = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
                block.invoke(sb.toString())
            }
        }
    }

    /**
     * 编辑文档
     */
    fun writeTextToUri(uri: Uri, text: String?) {
        if (text.isNullOrBlank() || !checkUriColumnFlag(uri, DocumentsContract.Document.FLAG_SUPPORTS_WRITE)) return
        try {
            FileGlobal.openFileDescriptor(uri, FileGlobal.MODE_WRITE_ONLY_ERASING)?.use {
                FileOutputStream(it.fileDescriptor).use { fos -> fos.write(text.toByteArray()) }
            }
        } catch (e: Throwable) {
            FileLogger.e("writeTextToUri Failed : ${e.message}")
        }
    }

    /**
     * 加载媒体 单个媒体文件的缩略图 👉 ContentResolver.loadThumbnail
     * <p>
     * ContentResolver.loadThumbnail,传入size，返回指定大小的缩略图
     */
    fun loadThumbnail(uri: Uri?, width: Int, height: Int): Bitmap? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return FileOperator.getContext().contentResolver.loadThumbnail(uri ?: return null, Size(width, height), null)
            }
        } catch (e: FileNotFoundException) {
            FileLogger.e("loadThumbnail Failed : ${e.message}")
        }
        return null
    }

    /**
     * 权限 Manifest.permission.READ_EXTERNAL_STORAGE
     * <pre>
     *     1.只有在删除非当前APP的应用 图片时候才会触发 RecoverableSecurityException
     *     2.重复删除同一uri对应的文件,会出现  java.lang.SecurityException: com.xxx.sample has no access to content://media/external/images/media/353235
     *     3.如果删除的是整个目录中的文件(eg:MediaStore.Images.Media.EXTERNAL_CONTENT_URI),系统会在数据库Table中记录当前应用创建文件时的信息,
     *       此时用户执行操作"系统设置->应用信息->存储->删除数据"会把应用的数据全部删除,Table信息也会被删除. 这样会导致使用 ContentResolver.delete(uri) 做删除时
     *       不能删除之前创建的文件,因此建议采用 SAF 方式做清空目录操作
     */
    //@RequiresPermission(allOf = [Manifest.permission.READ_EXTERNAL_STORAGE])
    fun deleteUri(activity: Activity, uri: Uri?, where: String?, selectionArgs: Array<String>?, requestCode: Int): Boolean {
        var delete = 0
        try {
            //删除失败 -> 重复删除同一 Uri 对应的文件!
            if (!FileUtils.checkUri(uri)) return false

            delete = FileOperator.getContext().contentResolver.delete(uri ?: return false, where, selectionArgs)
            FileLogger.d("删除结果 $uri $delete")
        } catch (e1: SecurityException) {
            /*
            更新其他应用的媒体文件
            如果应用使用分区存储，它通常无法更新其他应用存放到媒体存储中的媒体文件。不过，仍然可以通过捕获平台抛出的 RecoverableSecurityException 来征得用户同意以修改文件。
            */
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // In your code, handle IntentSender.SendIntentException.
                    val recoverableSecurityException = e1 as? RecoverableSecurityException ?: throw e1
                    val requestAccessIntentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                    activity.startIntentSenderForResult(
                        requestAccessIntentSender, requestCode, null, 0, 0, 0, null
                    )
                } else {
                    FileLogger.e("低于Q版本 ${e1.message} ")
                }
            } catch (e2: IntentSender.SendIntentException) {
                FileLogger.e("delete Fail e2 $uri  ${e2.message} ")
            }
        }
        return delete != -1
    }

    fun deleteUri(activity: Activity, uri: Uri?, requestCode: Int): Boolean = deleteUri(activity, uri, null, null, requestCode)

    fun deleteUriDirectory(activity: Activity, requestCode: Int, @FileGlobal.FileMediaType mediaType: String): Boolean {
        val uri = when (mediaType) {
            FileGlobal.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            FileGlobal.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return deleteUri(activity, uri, null, null, requestCode)
    }

    fun deleteUriMediaStoreImage(activity: Activity, mediaImage: MediaStoreImage, requestCode: Int): Boolean =
        deleteUri(activity, mediaImage.uri, "${MediaStore.Images.Media._ID} = ?", arrayOf(mediaImage.id.toString()), requestCode)


    /**
     * 获取虚拟文件的输入流,需要传入想要的 mimeType
     * <p>
     * https://developer.android.google.cn/training/data-storage/shared/documents-files#open-virtual-file
     */
    @Throws(IOException::class)
    fun getInputStreamForVirtualFile(uri: Uri, mimeTypeFilter: String): InputStream? {
        val resolver = FileOperator.getContext().contentResolver
        val openableMimeTypes: Array<String>? = resolver.getStreamTypes(uri, mimeTypeFilter)
        return if (openableMimeTypes?.isNotEmpty() == true) {
            resolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)?.createInputStream()
        } else {
            FileLogger.e("文件文找到!")  //throw FileNotFoundException()
            null
        }
    }

    /**
     * 判断是否为虚拟文件
     * <p>
     *     https://developer.android.google.cn/training/data-storage/shared/documents-files#open-virtual-file
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun isVirtualFile(uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(FileOperator.getContext(), uri)) return false
        val cursor: Cursor? = FileOperator.getContext().contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null, null, null
        )
        val flags: Int = cursor?.use { if (cursor.moveToFirst()) cursor.getInt(0) else 0 } ?: 0
        return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
    }

    ///////////////////////////////

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(value = Manifest.permission.READ_EXTERNAL_STORAGE)
    fun testQueryMediaVideoByUri() {
        val projectionArgs = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE)

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        val videoList = queryMediaStoreVideo(projectionArgs, sortOrder, 5L, TimeUnit.MINUTES)
        videoList?.let { video ->
            video.forEach {
                FileLogger.i("视频列表: $it")
            }
        }
    }

}