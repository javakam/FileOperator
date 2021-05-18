package ando.file.core

import ando.file.core.FileMimeType.getMimeType
import ando.file.core.FileUri.getPathByUri
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

/**
 * # FileUtils
 *
 * @author javakam
 * @date 2019/11/15 14:37
 */
object FileUtils {

    //File Extension
    //----------------------------------------------------------------

    fun getExtension(uri: Uri?): String {
        var name = if (uri == null) return "" else ""
        FileOperator.getContext().contentResolver.query(uri, null, null, null, null)
            ?.use { c: Cursor ->
                if (c.moveToFirst()) name = getExtension(c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME)))
            }
        return name
    }

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     * <p>
     * url : https://app-xxx-oss/xxx.gif
     *  or
     * fileName : xxx.gif
     *
     * @return 默认返回gif, fullExtension=false ;
     *         substring时不加1为 .gif, fullExtension=true
     *
     */
    fun getExtension(pathOrName: String?, split: Char, fullExtension: Boolean = false): String {
        if (pathOrName.isNullOrBlank()) return ""
        val dot = pathOrName.lastIndexOf(split)
        return if (dot != -1) pathOrName.substring(
            if (fullExtension) dot
            else (dot + 1)
        ).toLowerCase(Locale.getDefault())
        else "" // No extension.
    }

    fun getExtension(pathOrName: String): String = getExtension(pathOrName, '.', false)

    fun getExtensionFull(pathOrName: String): String = getExtension(pathOrName, '.', true)

    //File Name
    //----------------------------------------------------------------

    /**
     * /xxx/xxx/note.txt -> path: /xxx/xxx   name: note   suffix: txt
     * ///note.txt       -> path: ///        name: note   suffix: txt
     * /note.txt         -> path: ""         name: note   suffix: txt
     * note.txt          -> path: ""         name: note   suffix: txt
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
            val suffix = nameSuffix.substring((dot + 1)).toLowerCase(Locale.getDefault())
            val name = nameSuffix.substring(0, dot)
            FileLogger.d("srcPath=$srcPath path=$path  name=$name  suffix=$suffix nameSuffix=$nameSuffix")
            block?.invoke(path, name, suffix, nameSuffix)
        }
    }

    fun getFileNameFromPath(path: String?, split: Char = '/'): String? {
        if (path.isNullOrBlank()) return null
        val cut = path.lastIndexOf(split)
        if (cut != -1) return path.substring(cut + 1)
        return path
    }

    fun getFileNameFromUri(uri: Uri?): String? {
        if (uri == null) return null
        var filename: String? = null

        val resolver = FileOperator.getContext().contentResolver
        val mimeType = resolver.getType(uri)

        if (mimeType == null) {
            filename = getFileNameFromPath(getPathByUri(uri))
        } else {
            resolver.query(uri, null, null, null, null)?.use { c: Cursor ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                filename = c.getString(nameIndex)
            }
        }
        if (FileOperator.isDebug()) {
            FileLogger.i("getFileNameFromUri: $mimeType $filename")
        }
        return filename
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

    fun deleteFile(uri: Uri?): Int =
        getPathByUri(uri)?.run {
            deleteFileWithoutExcludeNames(File(this), null)
        } ?: 0

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
        getPathByUri(uri)?.run {
            deleteFilesNotDir(File(this))
        } ?: false

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