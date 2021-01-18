package ando.file.core

import ando.file.FileOperator
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import ando.file.core.FileMimeType.getMimeType
import ando.file.core.FileUri.getFilePathByUri
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

/**
 * Title:FileUtils
 *
 * @author javakam
 * @date 2019/11/15 14:37
 */
object FileUtils {

    //File Extension
    //----------------------------------------------------------------

    fun getExtension(uri: Uri?): String = if (uri != null) getExtension(getFilePathByUri(uri) ?: "") else ""

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     * <p>
     * url : https://app-xxx-oss/xxx/1586267702635.gif
     * or
     * fileName : 1586267702635.gif
     *
     * @return 默认返回 gif ; substring 时不加1为 .gif , 即 fullExtension=true
     *
     * The default returns gif; when substring does not add 1 to .gif, that is fullExtension=true
     */
    fun getExtension(pathOrName: String?, split: Char, fullExtension: Boolean): String {
        if (pathOrName.isNullOrBlank()) return ""
        val dot = pathOrName.lastIndexOf(split)
        return if (dot > 0) pathOrName.substring(
            if (fullExtension) dot
            else (dot + 1)).toLowerCase(Locale.getDefault())
        else "" // No extension.
    }

    fun getExtension(pathOrName: String): String = getExtension(pathOrName, '.', false)

    fun getExtensionFull(pathOrName: String): String = getExtension(pathOrName, '.', true)

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
        if (FileOperator.isDebug()) {
            FileLogger.i("getFileNameFromUri: $mimeType")
        }
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

    //File Delete
    //----------------------------------------------------------------

    /**
     * 删除文件或目录
     *
     * Delete files or directories
     *
     * @param file
     * @return 删除`文件/文件夹`数量
     */
    fun deleteFile(file: File?): Int = deleteFilesButDir(file, null)

    /**
     * 删除文件或目录
     *
     * Delete files or directories
     * <p>
     *     建议异步处理
     *
     * @param file  `文件/文件夹`
     * @param excludeFiles 指定名称的一些`文件`不做删除(Some `files` with specified names are not deleted)
     * @return 删除`文件/文件夹`数量
     */
    fun deleteFilesButDir(file: File?, vararg excludeFiles: String?): Int {
        var count = 0
        if (file == null || !file.exists()) return count
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty() && shouldFileDelete(file, *excludeFiles)) {
                if (file.delete()) count++ //delete directory
            } else {
                var i = 0
                while (children != null && i < children.size) {
                    count += deleteFilesButDir(children[i], null)
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
            shouldDelete = (it?.equals(file.name, true) == true)
            if (shouldDelete) return@forEach
        }
        return shouldDelete
    }

    /**
     * 如果 `File(dirPath).isDirectory==false`, 那么将不做后续处理
     *
     * If `File(dirPath).isDirectory==false`, then no subsequent processing will be done
     *
     * @param dirPath directory path
     */
    fun deleteFilesButDir(dirPath: String?): Boolean =
        if (dirPath.isNullOrBlank()) false else deleteFilesButDirs(File(dirPath))

    /**
     * 只删除文件，不删除文件夹
     *
     * Only delete files, not folders
     *
     * @param dir directory
     */
    fun deleteFilesButDirs(dir: File?): Boolean {
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
                deleteFilesButDirs(child)
            } else {
                child.delete()
            }
            if (!success) return false
            if (i == len - 1) return true
        }
        return false
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
        uri?.run {
            readFileText(FileOperator.getContext().contentResolver.openInputStream(this))
        }

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
        uri?.run {
            readFileBytes(FileOperator.getContext().contentResolver.openInputStream(this))
        }

    //File Copy
    //----------------------------------------------------------------

    /**
     * 根据文件路径拷贝文件(Copy files according to file path)
     *
     * eg :
     * boolean copyFile = FileUtils.copyFile(fileOld, "/test_" + i, getExternalFilesDir(null).getPath());
     * File fileNew =new File( getExternalFilesDir(null).getPath() +"/"+ "test_" + i);
     *
     * @param src      源文件
     * @param destFilePath 目标文件路径
     */
    fun copyFile(
        src: File,
        destFileName: String,
        destFilePath: String,
    ): Boolean {
        if (!src.exists() || destFilePath.isBlank()) return false
        val dest = File(destFilePath + destFileName)
        if (dest.exists()) dest.delete() // delete file

        try {
            dest.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var srcChannel: FileChannel? = null
        var dstChannel: FileChannel? = null
        return try {
            srcChannel = FileInputStream(src).channel
            dstChannel = FileOutputStream(dest).channel
            srcChannel.transferTo(0, srcChannel.size(), dstChannel)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            srcChannel?.close()
            dstChannel?.close()
        }
    }

    //File Write
    //----------------------------------------------------------------

    /**
     * 创建文件(Create a file)
     *
     * eg: filePath is getExternalCacheDir() , fileName is xxx.json
     *
     * System path: /mnt/sdcard/Android/data/ando.guard/cache/xxx.json
     */
    fun createFile(filePath: String?, fileName: String?): File? {
        if (filePath.isNullOrBlank() || fileName.isNullOrBlank()) return null
        val file = File(filePath, fileName)
        if (file.exists() && file.isDirectory) file.delete()
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    /**
     * Bitmap 保存为本地文件 (Save Bitmap as a local file)
     *
     * @param pathAndName  格式必须带有后缀 xxx.png (The format must have the suffix xxx.png)
     */
    fun write2File(bitmap: Bitmap, pathAndName: String?) {
        if (pathAndName.isNullOrBlank()) return
        val file = File(pathAndName)
        var out: BufferedOutputStream? = null
        try {
            out = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            out?.close()
        }
    }

    fun write2File(input: InputStream, filePath: String?, fileName: String?) =
        write2File(input, createFile(filePath, fileName)?.absolutePath)

    fun write2File(input: InputStream, pathAndName: String?) {
        if (pathAndName.isNullOrBlank()) return
        var output: FileOutputStream? = null
        try {
            val file = File(pathAndName)
            val dir = file.parentFile
            if (dir == null || !dir.exists()) {
                dir?.mkdirs()
            }
            if (!file.exists()) file.createNewFile()

            output = FileOutputStream(file)
            val b = ByteArray(1024)
            var length: Int
            while (input.read(b).also { length = it } != -1) {
                output.write(b, 0, length)
            }
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            input.close()
            output?.close()
        }
    }

    //File isLocal
    //----------------------------------------------------------------

    /**
     * 检验是否为本地URI
     *
     * @return Whether the URI is a local one.
     */
    fun isLocal(url: String?): Boolean =
        url != null && url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")

    //File Gif
    //----------------------------------------------------------------

    /**
     * gif
     *
     * @param mimeType
     */
    fun isGif(mimeType: String?): Boolean = !mimeType.isNullOrBlank() && mimeType.equals("image/gif", true)

    /**
     * File name/path/url
     */
    fun isGif(uri: Uri?): Boolean = if (uri == null) false else isGif(getMimeType(uri))

}