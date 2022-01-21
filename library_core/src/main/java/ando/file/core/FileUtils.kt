package ando.file.core

import ando.file.core.FileMimeType.getMimeType
import ando.file.core.FileUri.getPathByUri
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

/**
 * # FileUtils
 *
 * @author javakam
 * @date 2019/11/15 14:37
 */
object FileUtils {

    //Media File Info
    //----------------------------------------------------------------

    /**
     * 获取媒体文件的"拍摄时间" (Get the "shooting time" of the media file)
     *
     * 【注】获取拍摄时间优先级: 图片(ExifInterface) ; 视频,音频(MediaMetadataRetriever) ; 最后如果前两者都没获取到时间, 则使用文件最后修改时间(lastModified)
     *
     * 【Note】Get the shooting time priority: Picture (ExifInterface); Video, Audio (MediaMetadataRetriever); Finally, if the first two do not get the time, use the last modified time of the file (lastModified)
     */
    fun getMediaShotTime(uri: Uri?, block: (Long) -> Unit) {
        if (uri == null) return block.invoke(-1)

        //直接使用 File(mediaFile.path) 获取不到信息 (No information can be obtained directly using File(mediaFile.path))
        //eg: /storage/emulated/0/Movies/VID_20210621_17180117.mp4 true false 1624267109000 ; isFile=false  isDirectory=false  lastModified=0
        val fileReal = File(getPathByUri(uri) ?: return block.invoke(-1))

        //如果 ExifInterface 或 MediaMetadataRetriever 没有获取到时间,使用 lastModified 时间
        //If ExifInterface or MediaMetadataRetriever does not get the time, use the lastModified time
        var fileLastModifiedTime: Long = fileReal.lastModified()
        fileLastModifiedTime = if (fileLastModifiedTime > 0) fileLastModifiedTime else System.currentTimeMillis()

        FileLogger.d(
            "isFile=${fileReal.isFile}  isDirectory=${fileReal.isDirectory}  lastModified=$fileLastModifiedTime"
        )

        //注意:先用 ExifInterface , 后用 MediaMetadataRetriever (Note: Use ExifInterface first, then MediaMetadataRetriever)
        //如果给把图片的 Uri 交给 MediaMetadataRetriever 处理会报错: setDataSource failed: status = 0x80000000
        //If the Uri of the picture is handed over to MediaMetadataRetriever for processing, an error will be reported: setDataSource failed: status = 0x80000000
        try {
            FileOperator.getContext().contentResolver.openInputStream(uri)?.use { i: InputStream ->
                val exifInterface = ExifInterface(i)
                val dateTime: String? = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                val modifiedTime: Long

                // 图片(Image)
                // longitude = 0/1,0/1,0/1
                // latitude=0/1,0/1,0/1
                // device_type=NEX 3 5G
                // dateTime=2021:07:12 14:36:30
                // dateTimeOriginal=2021:07:12 14:36:30
                // dateTimeDigitized=2021:07:12 14:36:30
                if (dateTime.isNullOrBlank()) {//1.视频,音频 (Video, audio)
                    //ExifInterface 获取到的 ExifInterface.TAG_DATETIME 返回 null, 使用 MediaMetadataRetriever 重新获取
                    //ExifInterface.TAG_DATETIME obtained by ExifInterface returns null, use MediaMetadataRetriever to get it again
                    val mmr = MediaMetadataRetriever()
                    modifiedTime = try {
                        mmr.setDataSource(FileOperator.getContext(), uri)
                        //获取媒体的日期(Date the media was acquired): "20210708T070344.000Z"
                        val dateString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                        formatMediaMetadataKeyDate(dateString)?.time ?: fileLastModifiedTime
                    } catch (e: Exception) {
                        FileLogger.e("getMediaShotTime: ${e.message}")
                        fileLastModifiedTime
                    } finally {
                        mmr.release()
                    }
                } else {//2.图片(Image)  ExifInterface.TAG_DATETIME  dateTime=2021:07:12 14:36:30
                    modifiedTime = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dateTime)?.time ?: fileLastModifiedTime
                }
                block.invoke(modifiedTime)
            } ?: block.invoke(fileLastModifiedTime)
        } catch (t: Throwable) {
            FileLogger.e("getMediaShotTime: ${t.message}")
        }
    }

    /**
     * 转换 MediaMetadataRetriever.METADATA_KEY_DATE 特殊的时间格式:
     *
     * Convert MediaMetadataRetriever.METADATA_KEY_DATE to special time format:
     *
     * eg: "20210708T070344.000Z" 👉 Date()
     *
     * > Thanks
     *
     * https://stackoverflow.com/questions/38648437/android-mediametadataretriever-metadata-key-date-gives-only-date-of-video-on-gal/39828238
     *
     * https://blog.csdn.net/qq_31332467/article/details/79166945
     *
     * @param date "20210708T070344.000Z"
     * @return Date Object
     */
    fun formatMediaMetadataKeyDate(date: String?): Date? {
        if (date.isNullOrBlank()) return null

        var inputDate: Date? = null
        try {
            inputDate = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS Z", Locale.getDefault()).parse(date.replace("Z", " UTC")) ?: return null
        } catch (e: Exception) {
            FileLogger.w("error parsing date: $e")
            try {
                inputDate = SimpleDateFormat("yyyy MM dd.SSS Z", Locale.getDefault()).parse(date.replace("Z", " UTC")) ?: return null
            } catch (ex: Exception) {
                FileLogger.e("error parsing date: $ex")
            }
        }
        FileLogger.i("formatMediaMetadataKeyDate: ${inputDate?.time}")
        return inputDate
    }

    /**
     * 打印"音频或视频"的详细信息 (Use MediaMetadataRetriever)
     *
     * Print "audio or video" details
     */
    fun dumpMediaInfoByMediaMetadataRetriever(uri: Uri?) {
        uri?.apply {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(FileOperator.getContext(), uri)
                //获得媒体专辑的标题
                val albumString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                //获取媒体的艺术家信息
                val artistString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                //获取媒体标题信息
                val titleString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                //获取媒体类型
                val mimetypeString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                //获取媒体持续时间
                val durationString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                //获取媒体比特率，位率
                val bitrateString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                //获取媒体的日期
                val dateString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                //如果媒体包含视频，这个键就会检索它的宽度。
                val video_width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                //如果媒体包含视频，这个键就会检索它的高度。
                val video_height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                //元数据键，用于检索歌曲的数量，如音频、视频、文本，在数据源中，如mp4或3gpp文件。
                val NUM_TRACKS = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
                //检索数字字符串的元数据键，该字符串描述了音频数据源的哪个部分来自于
                val DISC_NUMBER = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                //表演者或艺术家的信息。
                val ALBUMARTIST = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                //作者
                val AUTHOR = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                //元数据键检索在原始记录中描述音频数据源的顺序的数字字符串。
                val CD_TRACK_NUMBER = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                //帧速率
                val CAPTURE_FRAMERATE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                } else {
                    ""
                }
                //检索音乐专辑编译状态的元数据键。
                val COMPILATION = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION)
                //元数据键检索关于数据源的composer的信息
                val COMPOSER = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
                //获取数据源的内容类型或类型的元数据键。
                val GENRE = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                //如果这个键存在，那么媒体就包含了音频内容。
                val HAS_AUDIO = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                //如果这个密钥存在，那么媒体就包含了视频内容。。
                val HAS_VIDEO = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                //如果可用，此键将检索位置信息。
                val LOCATION = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                //如果有的话，这个键可以获取视频旋转角度的角度。
                val VIDEO_ROTATION = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                //元数据键，用于检索数据源的写入器(如lyriwriter)的信息。
                val WRITER = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
                //元数据键，用于检索数据源创建或修改时的年份。
                val YEAR = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                //此选项用于getFrameAtTime(long、int)，以检索与最近(在时间)或给定时间最接近的数据源相关联的同步(或键)框架。
                val CLOSEST_SYNC = mmr.extractMetadata(MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                //该选项用于getFrameAtTime(long、int)，用于检索与最近或给定时间最接近的数据源相关的帧(不一定是关键帧)。
                val CLOSEST = mmr.extractMetadata(MediaMetadataRetriever.OPTION_CLOSEST)
                //这个选项用于getFrameAtTime，以检索与在给定时间之前或在给定时间内的数据源相关联的同步(或键)框架。
                val PREVIOUS_SYNC = mmr.extractMetadata(MediaMetadataRetriever.OPTION_PREVIOUS_SYNC)

                FileLogger.i(
                    """
                        ============================== MediaMetadataRetriever Info Begin ==============================
                        METADATA_KEY_ALBUM: $albumString
                        METADATA_KEY_ARTIST: $artistString
                        METADATA_KEY_TITLE: $titleString
                        METADATA_KEY_MIMETYPE: $mimetypeString
                        METADATA_KEY_DURATION: $durationString
                        METADATA_KEY_BITRATE: $bitrateString
                        METADATA_KEY_DATE: $dateString
                        METADATA_KEY_VIDEO_WIDTH: $video_width
                        METADATA_KEY_VIDEO_HEIGHT: $video_height
                        METADATA_KEY_NUM_TRACKS: $NUM_TRACKS
                        METADATA_KEY_DISC_NUMBER: $DISC_NUMBER
                        METADATA_KEY_ALBUMARTIST: $ALBUMARTIST
                        METADATA_KEY_AUTHOR: $AUTHOR
                        METADATA_KEY_CD_TRACK_NUMBER: $CD_TRACK_NUMBER
                        METADATA_KEY_CAPTURE_FRAMERATE: $CAPTURE_FRAMERATE
                        METADATA_KEY_COMPILATION: $COMPILATION
                        METADATA_KEY_COMPOSER: $COMPOSER
                        METADATA_KEY_GENRE: $GENRE
                        METADATA_KEY_HAS_AUDIO: $HAS_AUDIO
                        METADATA_KEY_HAS_VIDEO: $HAS_VIDEO
                        METADATA_KEY_LOCATION: $LOCATION
                        METADATA_KEY_VIDEO_ROTATION: $VIDEO_ROTATION
                        METADATA_KEY_WRITER: $WRITER
                        METADATA_KEY_YEAR: $YEAR
                        OPTION_CLOSEST_SYNC: $CLOSEST_SYNC
                        OPTION_CLOSEST: $CLOSEST
                        OPTION_PREVIOUS_SYNC: $PREVIOUS_SYNC
                        ============================== MediaMetadataRetriever Info END ==============================
                    """.trimIndent()
                )
            } catch (e: Exception) {
                FileLogger.e("dumpMediaInfoByMediaMetadataRetriever: ${e.message}")
            } finally {
                mmr.release()
            }
        }
    }

    /**
     * 打印"图片"的详细信息 (Use ExifInterface)
     *
     * Print the detailed information of "Image"
     */
    fun dumpMediaInfoByExifInterface(uri: Uri?) {
        uri?.use {
            try {
                FileOperator.getContext().contentResolver.openInputStream(uri)?.use { i: InputStream ->
                    //很简单，传入源文件地址就可以
                    val exifInterface = ExifInterface(i)
                    val longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                    val latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                    val length = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
                    val width = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                    val aperture = exifInterface.getAttribute(ExifInterface.TAG_APERTURE_VALUE) //光圈
                    val iso = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED)
                    val balance = exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE) //白平衡
                    val exposure = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) //曝光时间
                    val foch_length = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) //焦距
                    val gps_altitude_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) //海拔高度
                    val device_type = exifInterface.getAttribute(ExifInterface.TAG_MODEL)
                    val dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                    val dateTimeOriginal = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    val dateTimeDigitized = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)

                    //图片
                    // longitude = 0/1,0/1,0/1
                    // latitude=0/1,0/1,0/1
                    // device_type=NEX 3 5G
                    // dateTime=2021:07:12 14:36:30
                    // dateTimeOriginal=2021:07:12 14:36:30
                    // dateTimeDigitized=2021:07:12 14:36:30
                    FileLogger.i(
                        """
                        ============================== ExifInterface Info END ==============================
                        TAG_GPS_LONGITUDE: $longitude
                        TAG_GPS_LATITUDE: $latitude
                        TAG_IMAGE_LENGTH: $length
                        TAG_IMAGE_WIDTH: $width
                        TAG_APERTURE_VALUE: $aperture
                        TAG_ISO_SPEED: $iso
                        TAG_WHITE_BALANCE: $balance
                        TAG_EXPOSURE_TIME: $exposure
                        TAG_FOCAL_LENGTH: $foch_length
                        TAG_GPS_ALTITUDE_REF: $gps_altitude_ref
                        TAG_MODEL: $device_type
                        TAG_DATETIME: $dateTime
                        TAG_DATETIME_ORIGINAL: $dateTimeOriginal
                        TAG_DATETIME_DIGITIZED: $dateTimeDigitized
                        ============================== ExifInterface Info END ==============================
                    """.trimIndent()
                    )
                }
            } catch (t: Throwable) {
                FileLogger.e("dumpMediaInfoByExifInterface: ${t.message}")
            }
        }
    }

    //File Check
    //----------------------------------------------------------------

    /**
     * 检查 Uri 对应的文件是否为 图片
     */
    fun checkImage(uri: Uri?): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFileDescriptor(FileGlobal.openFileDescriptor(uri, FileGlobal.MODE_READ_ONLY)?.fileDescriptor, null, options)
        return options.outWidth != -1
    }

    /**
     * 1. 检查 Uri 是否正确
     * 2. Uri 对应的文件是否存在 (可能是已删除, 也肯是系统 db 存有 Uri 相关记录, 但是文件失效或者损坏)
     *
     * EN
     * 1. Check if Uri is correct
     * 2. Whether the file corresponding to Uri exists (may be deleted, or the system db has Uri related records, but the file is invalid or damaged)
     *
     * https://stackoverflow.com/questions/7645951/how-to-check-if-resource-pointed-by-uri-is-available
     */
    fun checkUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val resolver = FileOperator.getContext().contentResolver

        //1. Check Uri
        var cursor: Cursor? = null
        val isUriExist: Boolean = try {
            cursor = resolver.query(uri, null, null, null, null)
            //cursor null: content Uri was invalid or some other error occurred
            //cursor.moveToFirst() false: Uri was ok but no entry found.
            (cursor != null && cursor.moveToFirst())
        } catch (t: Throwable) {
            FileLogger.e("1.Check Uri Error: ${t.message}")
            false
        } finally {
            try {
                cursor?.close()
            } catch (t: Throwable) {
            }
        }

        //2. Check File Exist
        //如果系统 db 存有 Uri 相关记录, 但是文件失效或者损坏 (If the system db has Uri related records, but the file is invalid or damaged)
        var ins: InputStream? = null
        val isFileExist: Boolean = try {
            ins = resolver.openInputStream(uri)
            // file exists
            true
        } catch (t: Throwable) {
            // File was not found eg: open failed: ENOENT (No such file or directory)
            FileLogger.e("2. Check File Exist Error: ${t.message}")
            false
        } finally {
            try {
                ins?.close()
            } catch (t: Throwable) {
            }
        }
        return isUriExist && isFileExist
    }

    // File Extension
    //----------------------------------------------------------------

    /**
     * ### 通过文件 Uri 获取后缀 eg: txt, png, exe...
     *
     * - 先使用 ContentResolver 去查询, 如果返回""则再尝试使用Uri.toString()去查询
     *
     * - 参考: [storage-samples/ActionOpenDocument](https://github.com/android/storage-samples/blob/main/ActionOpenDocument)
     */
    fun getExtension(uri: Uri?): String {
        return uri?.use {
            var name = FileOperator.getContext().contentResolver.query(this, null, null, null, null)
                ?.use { c: Cursor ->
                    if (c.moveToFirst()) getExtension(c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))) else ""
                } ?: ""

            if (name.isBlank()) name = getExtension(this.toString())
            name
        } ?: ""
    }

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     * <p>
     * url : https://app-xxx-oss/xxx.gif
     *  or
     * fileName : xxx.gif
     *
     * @param fullExtension true ".png" ; false "png"
     * @return fullExtension=false, "gif";
     *         fullExtension=true,  ".gif" substring时不加1
     */
    fun getExtension(pathOrName: String?, split: Char, fullExtension: Boolean = false): String {
        if (pathOrName.isNullOrBlank()) return ""
        val dot = pathOrName.lastIndexOf(split)
        return if (dot != -1) pathOrName.substring(
            if (fullExtension) dot
            else (dot + 1)
        ).lowercase(Locale.getDefault())
        else "" // No extension.
    }

    /**
     * @return [√] "png" ; [×] ".png"
     */
    fun getExtension(pathOrName: String): String = getExtension(pathOrName, '.', false)

    /**
     * @return [√] ".png" ; [×] "png"
     */
    fun getExtensionFull(pathOrName: String): String = getExtension(pathOrName, '.', true)

    /**
     * 修改文件的后缀
     *
     * eg:  changeFileExtension(originName = "/xxx/xxx/test.txt", '.', "jpeg")
     */
    fun changeFileExtension(pathOrName: String, split: Char, newExtension: String): String {
        if (pathOrName.isBlank()) {
            return ""
        }
        val dot = pathOrName.lastIndexOf(split)
        if (dot != -1) {
            val endIndex = dot + 1
            return pathOrName.substring(0, endIndex).lowercase(Locale.getDefault()) + newExtension
        }
        return ""
    }

    //File Name
    //----------------------------------------------------------------

    /**
     * ### 路径分割
     *
     * ```
     * eg: srcPath=/storage/emulated/0/Movies/myVideo.mp4  path=/storage/emulated/0/Movies
     * name=myVideo  suffix=mp4  nameSuffix=myVideo.mp4
     *
     * /xxx/xxx/note.txt ->  path: /xxx/xxx   name: note   suffix: txt
     * ///note.txt       ->  path: ///        name: note   suffix: txt
     * /note.txt         ->  path: ""         name: note   suffix: txt
     * note.txt          ->  path: ""         name: note   suffix: txt
     * ```
     */
    fun splitFilePath(
        srcPath: String?,
        nameSplit: Char = '/',
        suffixSplit: Char = '.',
        block: ((path: String, name: String, suffix: String, nameSuffix: String) -> Unit)? = null,
    ) {
        if (srcPath.isNullOrBlank()) return
        val cut = srcPath.lastIndexOf(nameSplit)
        // /xxx/xxx/note.txt +0: /xxx/xxx +1: /xxx/xxx/
        val path = if (cut == -1) "" else srcPath.substring(0, cut)
        val nameSuffix = if (cut == -1) srcPath else srcPath.substring(cut + 1)

        val dot = nameSuffix.lastIndexOf(suffixSplit)
        if (dot != -1) {
            val suffix = nameSuffix.substring((dot + 1)).lowercase(Locale.getDefault())
            val name = nameSuffix.substring(0, dot)
            FileLogger.d("splitFilePath srcPath=$srcPath path=$path  name=$name  suffix=$suffix nameSuffix=$nameSuffix")
            block?.invoke(path, name, suffix, nameSuffix)
        }
    }

    /**
     * /storage/emulated/0/Movies/myVideo.mp4  ->  /storage/emulated/0/Movies
     */
    fun getFilePathFromFullPath(path: String?, split: Char = '/'): String? {
        if (path.isNullOrBlank()) return null
        val cut = path.lastIndexOf(split)
        // (cut+1): /storage/emulated/0/Movies/
        if (cut != -1) return path.substring(0, cut)
        return path
    }

    /**
     * /storage/emulated/0/Movies/myVideo.mp4  ->  myVideo.mp4
     */
    fun getFileNameFromPath(path: String?, split: Char = '/'): String? {
        if (path.isNullOrBlank()) return null
        val cut = path.lastIndexOf(split)
        if (cut != -1) return path.substring(cut + 1)
        return path
    }

    /**
     * /storage/emulated/0/Movies/myVideo.mp4  ->  myVideo.mp4
     */
    fun getFileNameFromUri(uri: Uri?): String? =
        uri?.use {
            var filename: String? = null
            val resolver = FileOperator.getContext().contentResolver
            val mimeType = resolver.getType(uri)

            if (mimeType == null) {
                filename = getFileNameFromPath(getPathByUri(uri))
            } else {
                resolver.query(uri, null, null, null, null)?.use { c: Cursor ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst()) filename = c.getString(nameIndex)
                }
            }
            if (FileOperator.isDebug()) {
                FileLogger.i("getFileNameFromUri: $mimeType $filename")
            }
            filename
        }

    //File Read
    //----------------------------------------------------------------

    /**
     * 读取文本文件中的内容
     *
     * Read the contents of the text file
     */
    fun readFileText(stream: InputStream?): String? {
        if (stream == null) return null
        val content = StringBuilder()
        try {
            val reader = InputStreamReader(stream)
            val bufferedReader = BufferedReader(reader)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                content.append(line).append("\n")
            }
            bufferedReader.close()
            reader.close()
            stream.close()
        } catch (e: Exception) {
            FileLogger.e(e.message)
        }
        return content.toString()
    }

    fun readFileText(uri: Uri?): String? =
        uri?.use { readFileText(FileOperator.getContext().contentResolver.openInputStream(this)) }

    fun readFileBytes(stream: InputStream?): ByteArray? =
        stream?.use {
            var byteArray: ByteArray? = null
            try {
                val buffer = ByteArrayOutputStream()
                var nRead: Int
                val data = ByteArray(1024)
                while (stream.read(data, 0, data.size).also { nRead = it } != -1) {
                    buffer.write(data, 0, nRead)
                }

                buffer.flush()
                byteArray = buffer.toByteArray()
                buffer.close()
            } catch (e: IOException) {
                FileLogger.e("readFileBytes: ${e.message}")
            }
            return byteArray
        }

    fun readFileBytes(uri: Uri?): ByteArray? =
        uri?.use { readFileBytes(FileOperator.getContext().contentResolver.openInputStream(this)) }

    //File Write
    //----------------------------------------------------------------

    fun createFile(file: File?, overwrite: Boolean = false): File? = createFile(file?.parent, file?.name, overwrite)

    /**
     * 创建文件 (Create a file)
     *
     * eg: filePath is getExternalCacheDir() , fileName is xxx.json
     *
     * System path: /mnt/sdcard/Android/data/ando.guard/cache/xxx.json
     */
    fun createFile(filePath: String?, fileName: String?, overwrite: Boolean = false): File? {
        if (filePath.isNullOrBlank() || fileName.isNullOrBlank()) return null
        if (!createDirectory(filePath)) return null

        var file = File(filePath, fileName)
        if (file.exists()) {
            if (file.isDirectory) file.delete()
            if (!overwrite) {
                splitFilePath(fileName) { _, name, suffix, _ ->
                    var index = 0
                    while (file.exists()) {
                        index++
                        file = File(filePath, "$name($index).$suffix")
                        //FileLogger.w("createFile ${file.path} exist=${file.exists()} ")
                    }
                }
            } else file.delete()
        }
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: IOException) {
            FileLogger.e(e.toString())
        }
        return file
    }

    /**
     * 创建目录 (Create a directory)
     */
    fun createDirectory(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        val file = File(filePath)
        if (file.exists()) {
            if (!file.isDirectory) file.delete() else return true
        }
        return file.mkdirs()
    }

    /**
     * 把 ByteArray 写到 target(File) 中 (Write ByteArray to target(File))
     *
     * eg: /storage/.../xxx.txt
     */
    fun writeBytes2File(bytes: ByteArray, target: File): File {
        val channel: FileChannel = target.outputStream().channel
        channel.write(ByteBuffer.wrap(bytes))
        channel.force(true)//强制刷新
        channel.close()
        FileLogger.i("writeBytesToFile target= ${target.length()}")
        return target
    }

    fun write2File(bitmap: Bitmap, pathAndName: String?, overwrite: Boolean = false) {
        if (pathAndName.isNullOrBlank()) return
        write2File(bitmap, File(pathAndName), overwrite)
    }

    fun write2File(bitmap: Bitmap, filePath: String?, fileName: String?, overwrite: Boolean = false) {
        if (filePath.isNullOrBlank() || fileName.isNullOrBlank()) return
        write2File(bitmap, File(filePath, fileName), overwrite)
    }

    /**
     * Bitmap保存为本地文件 (Save Bitmap as a local file)
     */
    fun write2File(bitmap: Bitmap, file: File?, overwrite: Boolean = false) {
        if (file == null) return
        if (file.exists()) {
            if (file.isDirectory) file.delete()
            if (overwrite) file.delete() else return
        }
        var out: BufferedOutputStream? = null
        try {
            out = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        } catch (e: FileNotFoundException) {
            FileLogger.e(e.message)
        } finally {
            out?.close()
        }
    }

    fun write2File(input: InputStream, pathAndName: String?, overwrite: Boolean = false): File? {
        if (pathAndName.isNullOrBlank()) return null
        return write2File(input, File(pathAndName), overwrite)
    }

    fun write2File(input: InputStream, filePath: String?, fileName: String?, overwrite: Boolean = false): File? {
        if (filePath.isNullOrBlank() || fileName.isNullOrBlank()) return null
        return write2File(input, File(filePath, fileName), overwrite)
    }

    fun write2File(input: InputStream, file: File?, overwrite: Boolean = false): File? {
        if (file == null) return null
        var target: File? = null
        var output: FileOutputStream? = null
        try {
            val dir = file.parentFile
            if (dir == null || !dir.exists()) {
                dir?.mkdirs()
            }

            if (file.exists() && file.isDirectory) file.delete()

            if (!file.exists()) {
                file.createNewFile()
            } else {//Exist
                if (overwrite) file.delete()
                else {
                    if (file.exists()) {
                        target = createFile(file, false)
                    }
                }
            }
            output = if (!overwrite && target != null) FileOutputStream(target)
            else FileOutputStream(file)

            val b = ByteArray(8 * 1024)
            var length: Int
            while (input.read(b).also { length = it } != -1) {
                output.write(b, 0, length)
            }
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                input.close()
                output?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return target ?: file
    }

    //File Copy
    //----------------------------------------------------------------

    /**
     * ### 拷贝文件到指定路径和名称 (Copy the file to the specified path and name)
     *
     * 效率和`kotlin-stdlib-1.4.21.jar`中的`kotlin.io.FilesKt__UtilsKt.copyTo`基本相当
     * ```kotlin
     * fun File.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): File
     * ```
     * Usage:
     * ```kotlin
     * boolean copyResult = FileUtils.copyFile(fileOld, getExternalFilesDir(null).getPath(), "test.txt");
     * File targetFile = new File(getExternalFilesDir(null).getPath() + "/" + "test.txt");
     * ```
     *
     * @param src 源文件 Source File
     * @param destFilePath 目标文件路径(Target file path)
     * @param destFileName 目标文件名称(Target file name)
     * @param overwrite 覆盖目标文件
     */
    fun copyFile(
        src: File,
        destFilePath: String,
        destFileName: String,
        overwrite: Boolean = false,
    ): File? {
        if (!src.exists() || destFilePath.isBlank() || destFileName.isBlank()) return null
        val dest: File?
        if (overwrite) {
            dest = File(destFilePath + File.separator + destFileName)
            if (dest.exists()) dest.delete() // delete file
        } else {
            dest = createFile(destFilePath, destFileName, false)
        }

        try {
            dest?.createNewFile()
        } catch (e: IOException) {
            FileLogger.e(e.toString())
        }
        var srcChannel: FileChannel? = null
        var dstChannel: FileChannel? = null
        try {
            srcChannel = FileInputStream(src).channel
            dstChannel = FileOutputStream(dest).channel
            srcChannel.transferTo(0, srcChannel.size(), dstChannel)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            srcChannel?.close()
            dstChannel?.close()
        }
        return dest
    }

    //File Delete
    //----------------------------------------------------------------

    /**
     * @return 返回0表示删除失败 (EN: Returns 0 to indicate that the deletion failed)
     */
    fun deleteFile(uri: Uri?): Int = getPathByUri(uri)?.run { deleteFileWithoutExcludeNames(File(this), null) } ?: 0

    fun deleteFile(pathAndName: String?): Int =
        if (pathAndName.isNullOrBlank()) 0
        else deleteFileWithoutExcludeNames(File(pathAndName), null)

    /**
     * 删除文件或文件夹
     *
     * Delete files or directories
     *
     * @param file
     * @return Int 删除`文件/文件夹`数量 (Delete the number of `file folders`)
     */
    fun deleteFile(file: File?): Int = deleteFileWithoutExcludeNames(file, null)

    /**
     * 删除文件或文件夹
     *
     * Delete files or directories
     * <p>
     *     建议异步处理
     *
     * @param file  `文件/文件夹`
     * @param excludeFiles 指定名称的一些`文件/文件夹`不做删除 (Some `files/directory` with specified names are not deleted)
     * @return Int 删除`文件/文件夹`数量 (Delete the number of `file folders`)
     */
    fun deleteFileWithoutExcludeNames(file: File?, vararg excludeFiles: String?): Int {
        var count = 0
        if (file == null || !file.exists()) return count
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty() && shouldFileDelete(file, *excludeFiles)) {
                if (file.delete()) count++ //delete directory
            } else {
                var i = 0
                while (children != null && i < children.size) {
                    count += deleteFileWithoutExcludeNames(children[i], null)
                    i++
                }
            }
        }
        if (excludeFiles.isNullOrEmpty()) {
            if (file.delete()) count++
        } else {
            if (shouldFileDelete(file, *excludeFiles)) if (file.delete()) count++
        }
        return count
    }

    private fun shouldFileDelete(file: File, vararg excludeFiles: String?): Boolean {
        var shouldDelete = true
        excludeFiles.forEach {
            shouldDelete = (it?.equals(file.name, true) != true)
            if (shouldDelete) return@forEach
        }
        return shouldDelete
    }

    fun deleteFilesNotDir(uri: Uri?): Boolean =
        getPathByUri(uri)?.run { deleteFilesNotDir(File(this)) } ?: false

    /**
     * 只删除文件，不删除文件夹 (Only delete files, not folders)
     *
     * 如果 `File(dirPath).isDirectory==false`, 那么将不做后续处理
     *
     * If `File(dirPath).isDirectory==false`, then no subsequent processing will be done
     *
     * @param dirPath directory path
     */
    fun deleteFilesNotDir(dirPath: String?): Boolean = if (dirPath.isNullOrBlank()) false else deleteFilesNotDir(File(dirPath))

    /**
     * 只删除文件，不删除文件夹 (Only delete files, not folders)
     *
     * @param dir directory
     */
    fun deleteFilesNotDir(dir: File?): Boolean {
        if (dir == null || !dir.exists() || !dir.isDirectory) return false

        val children = dir.list()
        if (children.isNullOrEmpty()) return true

        val len = children.size
        var child: File?
        for (i in 0 until len) {
            child = File(dir, children[i])
            val success: Boolean = if (child.isDirectory) {
                if (child.list() == null || child.list()?.isEmpty() == true) {
                    continue
                }
                deleteFilesNotDir(child)
            } else {
                child.delete()
            }
            if (!success) return false
            if (i == len - 1) return true
        }
        return false
    }

    //----------------------------------------------------------------

    fun isLocal(url: String?): Boolean = !url.isNullOrBlank() && !url.startsWith("http") && !url.startsWith("https")

    fun isGif(mimeType: String?): Boolean = !mimeType.isNullOrBlank() && mimeType.equals("image/gif", true)

    fun isGif(uri: Uri?): Boolean = if (uri == null) false else isGif(getMimeType(uri))
}