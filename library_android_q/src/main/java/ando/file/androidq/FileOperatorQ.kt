package ando.file.androidq

import android.Manifest.permission.ACCESS_MEDIA_LOCATION
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import ando.file.FileOperator.getContext
import ando.file.core.*
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Title: FileOperatorQ
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/5/22  16:16
 */

//todo 2020年5月28日 17:14:02 测试该方法
private fun getAppSpecificAlbumStorageDir(context: Context, albumName: String): File? {
    // Get the pictures directory that's inside the app-specific directory on  external storage.
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), albumName)
    if (!file.exists() && !file.mkdirs()) {
        FileLogger.e("Directory not created")
    }
    FileLogger.i("Directory created")
    return file
}

//App-Specific getExternalFilesDirs
//------------------------------------------------------------------------------------------------

/**
 * 1.异步执行
 * 2.重复创建同名文件旧的会被覆盖 , 需要防抖处理
 *
 * @param type The type of files directory to return. May be {@code null}
 * for the root of the files directory or one of the following
 * constants for a subdirectory:
 *      {@link android.os.Environment#DIRECTORY_MUSIC},
 *      {@link android.os.Environment#DIRECTORY_PODCASTS},
 *      {@link android.os.Environment#DIRECTORY_RINGTONES},
 *      {@link android.os.Environment#DIRECTORY_ALARMS},
 *      {@link android.os.Environment#DIRECTORY_NOTIFICATIONS},
 *      {@link android.os.Environment#DIRECTORY_PICTURES}, or
 *      {@link android.os.Environment#DIRECTORY_MOVIES}.
 */
fun createFileInAppSpecific(type: String, displayName: String?, text: String?, block: (file: File?) -> Unit) {
    // val fileDir = getContext().getExternalFilesDirs(type)
    // 或者
    val fileDir = getContext().getExternalFilesDir(type)
    if (fileDir != null && fileDir.exists()) {
        try {
            val newFile = File(fileDir.absolutePath,
                if (displayName == null || displayName.isBlank()) SystemClock.currentThreadTimeMillis().toString() else displayName)

            FileOutputStream(newFile).use {
                it.write((if (text == null || text.isBlank()) "" else text).toByteArray(Charsets.UTF_8))
                it.flush()

                FileLogger.d("创建成功")
                block.invoke(newFile)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            FileLogger.d("创建失败")
        }
    }
}

//MediaStore
//------------------------------------------------------------------------------------------------

/**
 * ContentValues
 * <pre>
 * values.put(MediaStore.Images.Media.IS_PENDING, isPending)
 * Android Q , MediaStore中添加 MediaStore.Images.Media.IS_PENDING flag，用来表示文件的 isPending 状态，0是可见，其他不可见
 * </pre>
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
 * ContentResolver的insert方法 , 将多媒体文件保存到多媒体的公共集合目录
 * <p>
 * https://developer.huawei.com/consumer/cn/doc/50127
 * <pre>
 *     可以通过PRIMARY_DIRECTORY和SECONDARY_DIRECTORY字段来设置一级目录和二级目录：
（a）一级目录必须是和MIME type的匹配的根目录下的Public目录，一级目录可以不设置，不设置时会放到默认的路径；
（b）二级目录可以不设置，不设置时直接保存在一级目录下；
（c）应用生成的文档类文件，代码里面默认不设置时，一级是Downloads目录，也可以设置为Documents目录，建议推荐三方应用把文档类的文件一级目录设置为Documents目录；
（d）一级目录MIME type，默认目录、允许的目录映射以及对应的读取权限如下表所示： https://user-gold-cdn.xitu.io/2020/6/1/1726dd80a91347cf?w=1372&h=470&f=png&s=308857
 *
 * @param uri：多媒体数据库的Uri MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
 * @param context
 * @param mimeType：需要保存文件的mimeType
 * @param displayName：显示的文件名字
 * @param description：文件描述信息
 * @param saveFileName：需要保存的文件名字
 * @param saveSecondaryDir：保存的二级目录
 * @param savePrimaryDir：保存的一级目录  eg : Environment.DIRECTORY_DCIM
 * @return 返回插入数据对应的uri
 */
fun insertMediaFile(
    uri: Uri?,
    context: Context,
    mimeType: String?,
    displayName: String?,
    description: String?,
    saveFileName: String?,
    saveSecondaryDir: String?,
    savePrimaryDir: String?,
): String? {
    val values = ContentValues()
    values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
    values.put(MediaStore.Images.Media.DESCRIPTION, description)
    values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.put(MediaStore.Images.Media.RELATIVE_PATH, savePrimaryDir + File.separator + saveSecondaryDir)
    }
//    else {
//        values.put(MediaStore.Images.Media.PRIMARY_DIRECTORY, savePrimaryDir)
//        values.put(MediaStore.Images.Media.SECONDARY_DIRECTORY, saveSecondaryDir)
//    }
    var url: Uri? = null
    var stringUrl: String? = null /* value to be returned */
    val cr = context.contentResolver
    try {
        if (uri == null || saveFileName.isNullOrBlank()) return null
        url = cr.insert(uri, values) ?: return null
        val buffer = ByteArray(1024)

        val pfd = openFileDescriptor(uri, MODE_WRITE_ONLY_ERASING)
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
            closeIO(fos)
            closeIO(pfd)
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
 * <pre>
 *   1.会出现创建多个图片问题
 *
 *   2.MediaStore.Images.Media.INTERNAL_CONTENT_URI
 *
 *   java.lang.UnsupportedOperationException: Writing to internal storage is not supported.
 *      at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:172)
 *      at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:140)
 *      at android.content.ContentProviderProxy.insert(ContentProviderNative.java:481)
 *      at android.content.ContentResolver.insert(ContentResolver.java:1844)
 * </pre>
 */
fun insertBitmap(bitmap: Bitmap?, values: ContentValues): Uri? {
    val externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val resolver = getContext().contentResolver
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
            // Now that we're finished, release the "pending" status, and allow other apps
            // to view the image.
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
        closeIO(os)
        return insertUri
    }
}

//todo 2020年5月28日 17:14:02 测试该方法
private fun insertAudio(displayName: String?) {
    val resolver = getContext().contentResolver

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
        createContentValues(displayName, null, null, null, "${Environment.DIRECTORY_MUSIC}/sl", 1)

    // Keeps a handle to the new song's URI in case we need to modify it later.
    val songContentUri = resolver.insert(audioCollection, songDetails)

    songContentUri?.let {
        resolver.openFileDescriptor(songContentUri, "w", null).use {
            // Write data into the pending audio file.
        }
        // Now that we're finished, release the "pending" status, and allow other apps
        // to play the audio track.
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
    @FileMediaType mediaType: String,
    displayName: String?,
    description: String?,
    mimeType: String?,
    title: String?,
    relativePath: String?,
    isFuzzy: Boolean,
): QuerySelectionStatement {
    val symbol = if (isFuzzy) " like " else " = "
    val selection = StringBuilder()
    val selectionArgs: MutableList<String> = mutableListOf<String>()

    var needAddPre = false
    if (isNotBlank(displayName)) {
        val columnDisplayName: String? = when (mediaType) {
            MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.DISPLAY_NAME
            MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.DISPLAY_NAME
            else -> MediaStore.Images.Media.DISPLAY_NAME
        }
        selection.append(" $columnDisplayName $symbol ? ")
        selectionArgs.add(noNull(displayName))
        needAddPre = true
    }
    if (isNotBlank(description) && mediaType != MEDIA_TYPE_AUDIO) {// MediaStore.Audio 没有 DESCRIPTION 字段
        val columnDescription: String? = when (mediaType) {
            MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.DESCRIPTION
            else -> MediaStore.Images.Media.DESCRIPTION
        }

        selection.append("${if (needAddPre) " and " else " "} $columnDescription $symbol ? ")
        selectionArgs.add(noNull(description))
        needAddPre = true
    }
    if (isNotBlank(title)) {
        val columnTitle: String? = when (mediaType) {
            MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.TITLE
            MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.TITLE
            else -> MediaStore.Images.Media.TITLE
        }

        selection.append("${if (needAddPre) " and " else " "} $columnTitle $symbol ? ")
        selectionArgs.add(noNull(title))
        needAddPre = true
    }
    if (isNotBlank(mimeType)) {
        val columnMimeType: String? = when (mediaType) {
            MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.MIME_TYPE
            MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.MIME_TYPE
            else -> MediaStore.Images.Media.MIME_TYPE
        }
        selection.append("${if (needAddPre) " and " else " "} $columnMimeType $symbol ? ")
        selectionArgs.add(noNull(mimeType))
        needAddPre = true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (isNotBlank(relativePath)) {
            val columnRelativePath: String? = when (mediaType) {
                MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.RELATIVE_PATH
                MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.RELATIVE_PATH
                else -> MediaStore.Images.Media.RELATIVE_PATH
            }
            selection.append("${if (needAddPre) " and " else " "} $columnRelativePath $symbol ? ")
            selectionArgs.add(noNull(relativePath))
            needAddPre = true
        }
    }

    FileLogger.i("查询语句= $selection ")
    return QuerySelectionStatement(selection, selectionArgs, needAddPre)
}


// MediaStore.XXX.Media.EXTERNAL_CONTENT_URI
fun getMediaCursor(
    uri: Uri,
    projectionArgs: Array<String>? = arrayOf(MediaStore.Video.Media._ID),
    sortOrder: String? = null,
    querySelectionStatement: QuerySelectionStatement?,
): Cursor? {
    // Need the READ_EXTERNAL_STORAGE permission if accessing video files that your app didn't create.
    return getContext().contentResolver.query(
        uri,
        projectionArgs,
        querySelectionStatement?.selection.toString(),
        querySelectionStatement?.selectionArgs?.toTypedArray(),
        sortOrder
    )
}

@RequiresPermission(value = READ_EXTERNAL_STORAGE)
fun testQueryMediaVideoByUri() {
    val projectionArgs =
        arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE)
    // Display videos in alphabetical order based on their display name.
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
    val videoList = queryMediaStoreVideo(projectionArgs, sortOrder, 5L, TimeUnit.MINUTES)
    videoList?.let { video ->
        video.forEach {
            FileLogger.i("视频列表: $it")
        }
    }
}

@RequiresPermission(value = READ_EXTERNAL_STORAGE)
fun queryMediaStoreVideo(
    projectionArgs: Array<String>? = arrayOf(MediaStore.Video.Media._ID),
    sortOrder: String? = null,
    sourceDuration: Long,
    sourceUnit: TimeUnit,
): MutableList<MediaStoreVideo>? {
    // Need the READ_EXTERNAL_STORAGE permission if accessing video files that your app didn't create.

    // Container for information about each video.
    val videoList = mutableListOf<MediaStoreVideo>()
    val external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    val queryStatement = buildQuerySelectionStatement(
        MEDIA_TYPE_VIDEO, null, null, null, null, null, false
    )
    // Show only videos that are at least 5 minutes in duration.
    queryStatement.append(
        "${MediaStore.Video.Media.DURATION} >= ? ", noNull(
            TimeUnit.MILLISECONDS.convert(
                sourceDuration,
                sourceUnit
            ).toString()
        )
    )
    getMediaCursor(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projectionArgs, sortOrder, queryStatement)?.use { cursor ->
        // Cache column indices.
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val duration = cursor.getInt(durationColumn)
            val size = cursor.getInt(sizeColumn)

            val contentUri: Uri = ContentUris.withAppendedId(external, id)
            // Stores column values and the contentUri in a local object
            // that represents the media file.
            videoList += MediaStoreVideo(id, contentUri, name, duration.toLong(), size.toLong())
        }
        return videoList
    }
    return null
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
    projectionArgs: Array<String>? = arrayOf(MediaStore.Images.Media._ID),
    sortOrder: String? = null,
    querySelectionStatement: QuerySelectionStatement?,
): MutableList<MediaStoreImage>? {
    val imageList = mutableListOf<MediaStoreImage>()
    val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val cursor: Cursor?
    try {
        cursor = getMediaCursor(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionArgs, sortOrder, querySelectionStatement)
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

fun queryMediaStoreImages(
    projectionArgs: Array<String>? = arrayOf(MediaStore.Images.Media._ID), sortOrder: String? = null, displayName: String?,
    description: String?, mimeType: String?, title: String?, relativePath: String?, isFuzzy: Boolean,
): MutableList<MediaStoreImage>? {
    val queryStatement = buildQuerySelectionStatement(MEDIA_TYPE_IMAGE, displayName, description, mimeType, title, relativePath, isFuzzy)
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
    val queryStatement = buildQuerySelectionStatement(MEDIA_TYPE_IMAGE,
        null, null, null, null, null, true)
    return queryMediaStoreImages(null, null, queryStatement)
}

//Storage Access Framework (SAF) 👉 https://developer.android.google.cn/training/data-storage/shared/documents-files
//------------------------------------------------------------------------------------------------

/**
 * 读取文件
 */
const val REQUEST_CODE_SAF_SELECT_SINGLE_IMAGE: Int = 0x01

/**
 * 创建文件
 */
const val REQUEST_CODE_SAF_CREATE_FILE: Int = 0x02

/**
 * 编辑文档
 */
const val REQUEST_CODE_SAF_EDIT_FILE: Int = 0x03

/**
 * 选择目录
 */
const val REQUEST_CODE_SAF_CHOOSE_DOCUMENT_DIR: Int = 0x04

/**
 * 选择一个图片文件
 */
fun selectSingleImage(activity: Activity) = selectSingleFile(activity, "image/*", REQUEST_CODE_SAF_SELECT_SINGLE_IMAGE)

/**
 * 选择一个文件
 */
fun selectSingleFile(activity: Activity, mimeType: String, requestCode: Int) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
    }
    activity.startActivityForResult(intent, requestCode)
}

/**
 * 新建一个文件
 * <pre>
 *   mimeType 和 fileName 传反了引发的血案 👇
 *   android.content.ActivityNotFoundException: No Activity found to handle Intent
 *   { act=android.intent.action.CREATE_DOCUMENT cat=[android.intent.category.DEFAULT,android.intent.category.OPENABLE] typ=sl.txt (has extras) }
 *      at android.app.Instrumentation.checkStartActivityResult(Instrumentation.java:2113)
 *      at android.app.Instrumentation.execStartActivity(Instrumentation.java:1739)
 * </pre>
 */
fun createFileSAF(
    activity: Activity,
    pickerInitialUri: Uri?,
    fileName: String,
    mimeType: String,
    requestCode: Int = REQUEST_CODE_SAF_CREATE_FILE,
) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        putExtra(Intent.EXTRA_TITLE, fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
    }
    activity.startActivityForResult(intent, if (requestCode < 1) REQUEST_CODE_SAF_CREATE_FILE else requestCode)
}

/**
 * Fires an intent to spin up the "file chooser" UI and select an image.
 * <p>
 * 请注意以下事项：
 *      1.当应用触发 ACTION_OPEN_DOCUMENT Intent 时，该 Intent 会启动选择器，以显示所有匹配的文档提供程序。
 *      2.在 Intent 中添加 CATEGORY_OPENABLE 类别可对结果进行过滤，从而只显示可打开的文档（如图片文件）。
 *      3.intent.setType("image/ *") 语句可做进一步过滤，从而只显示 MIME 数据类型为图像的文档。
 */
//todo 2020年5月28日 17:14:02 测试该方法
private fun performFileSearch(activity: Activity, mimeType: String, requestCode: Int) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
    }
    activity.startActivityForResult(intent, requestCode)
}

/**
 * 打开文件
 * <p>
 * 请注意以下事项：
 *      1.当应用触发 ACTION_OPEN_DOCUMENT Intent 时，该 Intent 会启动选择器，以显示所有匹配的文档提供程序。
 *      2.在 Intent 中添加 CATEGORY_OPENABLE 类别可对结果进行过滤，从而只显示可打开的文档（如图片文件）。
 *      3.intent.setType("image/ *") 语句可做进一步过滤，从而只显示 MIME 数据类型为图像的文档。
 */
//todo 2020年5月28日 17:14:02 测试该方法
private fun openFileSAF(activity: Activity, pickerInitialUri: Uri?, mimeType: String, requestCode: Int) {
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
 * <pre>
 *     接收数据 :
 * override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
 *      if (requestCode == your-request-code && resultCode == Activity.RESULT_OK) {
 *          // The result data contains a URI for the document or directory that the user selected.
 *          resultData?.data?.also { uri ->
 *          // Perform operations on the document using its URI.
 *          }
 *      }
 * }
 * </pre>
 */
//todo 2020年5月28日 17:14:02 测试该方法
private fun openDirectorySAF(activity: Activity, pickerInitialUri: Uri?, requestCode: Int) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
    }
    activity.startActivityForResult(intent, requestCode)
}

//todo 2020年5月28日 17:14:02 测试该方法
private fun moveFileSAF(
    sourceDocumentUri: Uri,
    sourceParentDocumentUri: Uri,
    targetParentDocumentUri: Uri,
) {
    //Document.COLUMN_FLAGS  DocumentsProvider.moveDocument(String, String, String)
    if (checkUriFlagSAF(sourceDocumentUri, DocumentsContract.Document.FLAG_SUPPORTS_MOVE)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                DocumentsContract.moveDocument(getContext().contentResolver, sourceDocumentUri, sourceParentDocumentUri, targetParentDocumentUri)
            } catch (e: FileNotFoundException) {
                FileLogger.e("${e.message}")
            }
        }
    }
}

/**
 * 删除文档
 */
fun deleteFileSAF(uri: Uri): Boolean {
    if (checkUriFlagSAF(uri, DocumentsContract.Document.FLAG_SUPPORTS_DELETE)) {
        return DocumentsContract.deleteDocument(getContext().contentResolver, uri)
    }
    return false
}

fun checkUriFlagSAF(uri: Uri, flag: Int): Boolean {
    val cursor = getContext().contentResolver.query(uri, null, null, null, null)
    if (cursor != null && cursor.moveToFirst()) {
        val columnFlags =
            cursor.getInt(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS))
        FileLogger.i("Column Flags：$columnFlags  Flag：$flag")
        if (columnFlags >= flag) {
            return true
        }
        cursor.close()
    }
    return false
}

/**
 * 获取虚拟文件的输入流,需要传入想要的 mimeType
 * <p>
 * https://developer.android.google.cn/training/data-storage/shared/documents-files#open-virtual-file
 */
@Throws(IOException::class)
private fun getInputStreamForVirtualFile(uri: Uri, mimeTypeFilter: String): InputStream? {
    val resolver = getContext().contentResolver
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
    if (!DocumentsContract.isDocumentUri(getContext(), uri)) return false
    val cursor: Cursor? = getContext().contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
        null, null, null)
    val flags: Int = cursor?.use { if (cursor.moveToFirst()) cursor.getInt(0) else 0 } ?: 0
    return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
}

/**
 * SAF重命名文件
 * <pre>
 *     注意: 同一目录下,绝对不能存在相同名称的文件
 *
 *     对同一Uri对应的文件重命名不能重复，新旧名相同会报错 java.lang.IllegalStateException: File already exists
 *     因此先判断比对旧Uri对应的文件名是否和 newDisplayName 是否相同
 * </pre>
 */
fun renameFileSAF(
    uri: Uri,
    newDisplayName: String?,
    block: (isSuccess: Boolean, msg: String) -> Unit,
) {
    if (checkUriFlagSAF(uri, DocumentsContract.Document.FLAG_SUPPORTS_RENAME)) {
        val cursor = getContext().contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {//新旧名不能相同
                val displayName =
                    cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                if (!displayName.equals(if (newDisplayName.isNullOrBlank()) "" else newDisplayName, true)) {
                    DocumentsContract.renameDocument(getContext().contentResolver, uri, newDisplayName ?: "")
                }
                //查看目录中是否已存在 newDisplayName 的文件 -> 涉及到获取当前目录临时权限,太麻烦了,交给外部做吧 getDocumentTree
//                try {
//                    val root: DocumentFile? = getDocumentTree(activity ,uri,)
//                    val findFile = root?.findFile(newDisplayName ?: "")
//                } catch (e: SecurityException) {
//                }
                block.invoke(true, "重命名成功")
                return
            }
        } catch (e: Exception) {
            FileLogger.e(e.message)
            block.invoke(false, "已存在该名称的文件")
            return
        } finally {
            closeIO(cursor)
        }
    }
    block.invoke(false, "重命名失败")
}

/**
 * 获取目录的访问权限, 并访问文件列表
 */
fun getDocumentTreeSAF(activity: Activity, uri: Uri?, requestCode: Int): DocumentFile? {
    var root: DocumentFile? = null
    if (uri != null) {
        try {
            val takeFlags: Int = activity.intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            // Check for the freshest data.
            activity.contentResolver.takePersistableUriPermission(uri, takeFlags)

            // todo  activity.contentResolver.persistedUriPermissions
            FileLogger.d("已经获得永久访问权限")
            root = DocumentFile.fromTreeUri(activity, uri)
            return root
        } catch (e: SecurityException) {
            FileLogger.d("uri 权限失效，调用目录获取")
            activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), requestCode)
        }
    } else {
        FileLogger.d("没有永久访问权限，调用目录获取")
        activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), requestCode)
    }
    return root
}

fun getDocumentTreeSAF(activity: Activity, requestCode: Int): DocumentFile? {
    val sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
    val uriString = sp.getString("uri", "")
    val treeUri = Uri.parse(uriString)
    return getDocumentTreeSAF(activity, treeUri, requestCode)
}

/**
 * 永久保留权限
 */
fun saveDocTreePersistablePermissionSAF(activity: Activity, uri: Uri) {
    val sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
    sp.edit {
        this.putString("uri", uri.toString())
        this.apply()
    }
    val takeFlags: Int = activity.intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
}

/**
 * 照片中的位置信息
 */
@RequiresPermission(value = ACCESS_MEDIA_LOCATION)
@RequiresApi(Build.VERSION_CODES.Q)
fun getMediaLocation(uri: Uri, block: (latLong: FloatArray) -> Unit) {
    val photoUri = MediaStore.setRequireOriginal(uri)
    getContext().contentResolver.openInputStream(photoUri)?.use { stream ->
        ExifInterface(stream).run {
            val latLong: FloatArray = floatArrayOf(0F, 0F)
            // If lat/long is null, fall back to the coordinates (0, 0).
            // val latLongResult = getLatLong(latLong)
            block.invoke(latLong)
        }
    }
}

//ContentResolver
//------------------------------------------------------------------------------------------------

/**
 * 通过Uri获取Bitmap,耗时操作不应该在主线程
 * <p>
 * https://developer.android.google.cn/training/data-storage/shared/documents-files#bitmap
 *
 * Note: You should complete this operation on a background thread, not the UI thread.
 */
@Throws(IOException::class, IllegalStateException::class)
fun getBitmapFromUri(uri: Uri?): Bitmap? =
    openFileDescriptor(uri, MODE_READ_ONLY)?.fileDescriptor?.let {
        BitmapFactory.decodeFileDescriptor(it)
    }

/**
 * 读取文档信息
 * <p>
 * https://developer.android.google.cn/training/data-storage/shared/documents-files#input_stream
 */
fun readTextFromUri(uri: Uri): String {
    val sb = StringBuilder()
    getContext().contentResolver.openInputStream(uri)?.use { inputStream ->
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
    getContext().contentResolver.openInputStream(uri)?.use { inputStream ->
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
    if (text.isNullOrBlank() || !checkUriFlagSAF(uri, DocumentsContract.Document.FLAG_SUPPORTS_WRITE)) return
    try {
        openFileDescriptor(uri, MODE_WRITE_ONLY_ERASING)?.use {
            FileOutputStream(it.fileDescriptor).use { fos -> fos.write(text.toByteArray()) }
        }
    } catch (e: FileNotFoundException) {
        FileLogger.e("writeTextToUri Failed : ${e.message}")
    } catch (e: IOException) {
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
            return getContext().contentResolver.loadThumbnail(uri ?: return null, Size(width, height), null)
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
        if (!checkUriFileExit(uri)) return false

        delete = getContext().contentResolver.delete(uri ?: return false, where, selectionArgs)
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
                activity.startIntentSenderForResult(requestAccessIntentSender, requestCode,
                    null, 0, 0, 0, null)
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

fun deleteUriDirectory(
    activity: Activity,
    requestCode: Int,
    @FileMediaType mediaType: String,
): Boolean {
    val uri = when (mediaType) {
        MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    return deleteUri(activity, uri, null, null, requestCode)
}

fun deleteUriMediaStoreImage(activity: Activity, mediaImage: MediaStoreImage, requestCode: Int): Boolean =
    deleteUri(activity, mediaImage.uri, "${MediaStore.Images.Media._ID} = ?", arrayOf(mediaImage.id.toString()), requestCode)

// String Empty checks
//-----------------------------------------------------------------------

private fun noNull(any: Any?): String =
    when (any) {
        is String -> (any as? String ?: "")
        is Int -> (any as? Int ?: "").toString()
        else -> any.toString()
    }

private fun isNotBlank(cs: CharSequence?): Boolean = (!(cs.isNullOrBlank()))

//Dump
//------------------------------------------------------------------------------------------------

/**
 * 获取文档元数据
 */
fun dumpDocumentFileTree(root: DocumentFile?) {
    root?.listFiles()?.forEach loop@{ it ->
        //FileLogger.d( "目录下文件名称：${it.name}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dumpMetaData(it.uri)
        }
    }
}

private fun closeIO(io: Closeable?) {
    try {
        io?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}