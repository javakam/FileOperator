package ando.file.core

import ando.file.FileOperator
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.text.TextUtils
import ando.file.core.FileLogger.e
import ando.file.core.FileLogger.w
import ando.file.core.FileMimeType.getMimeType
import ando.file.core.FileUri.getFilePathByUri
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

/**
 * Title:FileUtils
 *
 * Description:
 *
 * @author javakam
 * @date 2019/11/15 14:37
 */
object FileUtils {

    //File Extension
    //----------------------------------------------------------------

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     * <p>
     * url : https://app-xxx-oss.oss-cn-beijing.aliyuncs.com/serverData/discuss/2020-04-07/1586267702635.gif
     * or
     * fileName : 1586267702635.gif
     *
     * @return 默认返回 gif ; substring 时不加1为 .gif , 即 fullExtension=true
     */
    fun getExtension(fileName: String?, split: Char, fullExtension: Boolean): String {
        if (fileName.isNullOrBlank()) return ""
        val dot = fileName.lastIndexOf(split)
        return if (dot > 0) fileName.substring(if (fullExtension) dot else (dot + 1))
            .toLowerCase(Locale.getDefault()) else "" // No extension.
    }

    fun getExtension(fileName: String): String = getExtension(fileName, '.', false)

    fun getExtension(uri: Uri?): String =
        if (uri != null) getExtension(getFilePathByUri(uri) ?: "") else ""

    fun getExtension(file: File?): String = if (file != null) getExtension(file.name) else ""

    fun getExtensionFull(fileName: String): String = getExtension(fileName, '.', true)

    fun getExtensionFull(file: File?): String =
        if (file != null) getExtensionFull(file.name) else ""

    fun getExtensionFromUri(uri: Uri?): String {
        FileOperator.getContext().contentResolver.query(uri ?: return "", null, null, null, null)
            .use { cursor ->
                if (cursor != null) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    FileLogger.i("getExtensionFromUri Display Name：$nameIndex")

                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    // If the file's name contains extension , we cut it down for latter use (copy a new file).
                    return getExtension(fileName)
                }
            }
        return ""
    }

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

    //File Delete
    //----------------------------------------------------------------

    /**
     * 删除文件或目录
     *
     * @param file
     * @return 0 目录不存在 ; 返回删除的 文件/文件夹 数量
     */
    fun deleteFile(file: File?): Int = deleteFilesButDir(file, null)

    /**
     * 删除文件或目录
     * <p>
     *     建议异步处理
     *
     * @param file  目录
     * @param excludeDirs  跳过指定名称的一些`目录/文件`
     */
    fun deleteFilesButDir(file: File?, vararg excludeDirs: String?): Int {
        var count = 0
        if (file == null || !file.exists()) return count
        if (file.isDirectory) {
            val children = file.listFiles()
            var i = 0
            while (children != null && i < children.size) {
                count += deleteFile(children[i])
                i++
            }
        }
        if (!excludeDirs.isNullOrEmpty()) {
            excludeDirs.forEach {
                if (it?.equals(file.name, true) == false) if (file.delete()) count++
            }
        }
        return count
    }

    fun deleteFileDir(dirPath: String?): Boolean =
        if (dirPath.isNullOrBlank()) false else deleteFileDir(File(dirPath))

    /**
     * 只删除文件，不删除文件夹
     *
     * @param dir 目录
     */
    fun deleteFileDir(dir: File?): Boolean {
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
                deleteFileDir(child)
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
     */
    fun readFileText(path: String?): String {
        if (TextUtils.isEmpty(path)) {
            return ""
        }
        val content = StringBuilder() //文件内容字符串
        val file = File(path ?: return "")
        if (file.isDirectory) {
            w("The File doesn't not exist.")
        } else {
            try {
                val ins: InputStream = FileInputStream(file)
                val reader = InputStreamReader(ins)
                val bufferedReader = BufferedReader(reader)
                var line: String?
                //分行读取
                while (bufferedReader.readLine().also { line = it } != null) {
                    content.append(line).append("\n")
                }
                ins.close()
                reader.close()
                bufferedReader.close()
            } catch (e: FileNotFoundException) {
                e("The File doesn't not exist.")
            } catch (e: IOException) {
                e(e.message)
            }
        }
        return content.toString()
    }

    fun readFileBytes(filePath: String?): ByteArray? {
        if (filePath.isNullOrBlank()) return null
        var fis: FileInputStream? = null
        var bytesArray: ByteArray? = null
        try {
            val file = File(filePath)
            bytesArray = ByteArray(file.length().toInt())
            //read file into bytes[]
            fis = FileInputStream(file)
            fis.read(bytesArray)
        } catch (e: IOException) {
            e("readFileBytes -> ${e.message}")
        } finally {
            fis?.close()
        }
        return bytesArray
    }

    //File Copy
    //----------------------------------------------------------------

    /**
     * 根据文件路径拷贝文件 java.nio
     *
     * eg :
     * boolean copyFile = FileUtils.copyFile(fileOld, "/test_" + i, getExternalFilesDir(null).getPath());
     * File fileNew =new File( getExternalFilesDir(null).getPath() +"/"+ "test_" + i);
     *
     * @param src      源文件
     * @param destPath 目标文件路径
     * @return boolean 成功true、失败false
     */
    fun copyFile(
        src: File?,
        destFileName: String,
        destPath: String?,
    ): Boolean {
        if (src == null || !src.exists() || destPath.isNullOrBlank()) return false
        val dest = File(destPath + destFileName)
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
     * Bitmap 保存为本地文件
     * @param fileName  格式必须带有后缀 xxx.png
     */
    fun write2File(bitmap: Bitmap, fileName: String?) {
        if (fileName.isNullOrBlank()) return
        val file = File(fileName)
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

    fun write2File(input: InputStream?, filePath: String?) {
        if (filePath.isNullOrBlank()) return
        var output: FileOutputStream? = null
        try {
            val file = File(filePath)
            val dir = file.parentFile
            if (dir == null || !dir.exists()) {
                dir?.mkdirs()
            }
            if (!file.exists()) file.createNewFile()

            output = FileOutputStream(file)
            val b = ByteArray(1024)
            var length: Int
            while (input?.read(b).also { length = it ?: 0 } != -1) {
                output.write(b, 0, length)
            }
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            input?.close()
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
    fun isGif(mimeType: String?): Boolean =
        !mimeType.isNullOrBlank() && mimeType.equals("image/gif", true)

    /**
     * File name/path/url
     */
    fun isGif(uri: Uri?): Boolean = if (uri == null) false else isGif(getMimeType(uri))

}