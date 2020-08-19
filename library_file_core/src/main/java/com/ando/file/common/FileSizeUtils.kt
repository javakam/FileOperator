package com.ando.file.common

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.TextUtils
import com.ando.file.FileOperator.getContext
import com.ando.file.common.FileSizeUtils.FileSizeType.*
import com.ando.file.common.FileLogger.e
import com.ando.file.common.FileLogger.i
import java.io.File
import java.math.BigDecimal

/**
 * FileSizeUtils 计算文件大小 👉 BigDecimal
 * <p>
 * https://developer.android.com/training/secure-file-sharing/setup-sharing
 * <pre>
 *      计算文件大小的核心方法只有两种:
 *          1.File.length
 *          2.ContentResolver.query()
 * </pre>
 */
object FileSizeUtils {

    enum class FileSizeType(val id: Int, val unit: String) {
        SIZE_TYPE_B(1, "B"),
        SIZE_TYPE_KB(2, "KB"),
        SIZE_TYPE_MB(3, "M"),
        SIZE_TYPE_GB(4, "GB"),
        SIZE_TYPE_TB(5, "TB")
    }

    // File/Dir Size
    //-----------------------------------------------------------------------

    /**
     * 获取指定 `文件/文件夹` 大小
     */
    @Throws(Exception::class)
    fun getFolderSize(file: File?): Long {
        var size = 0L
        if (file == null || !file.exists()) return size
        val files = file.listFiles()
        if (files.isNullOrEmpty()) return size

        for (i in files.indices) {
            size += if (files[i].isDirectory) getFolderSize(files[i]) else getFileSize(files[i])
        }
        return size
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(file: File?): Long = if (file?.exists() == true) file.length() else 0L

    fun getFileSize(uri: Uri?): Long = getFileSize(getContext(), uri) ?: 0L

    /**
     * ContentResolver.query 获取 `文件/文件夹` 大小
     */
    private fun getFileSize(context: Context, uri: Uri?): Long? =
        uri?.let {
            val zero = 0L
            val uriScheme = uri.scheme
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || TextUtils.equals(
                    "content",
                    uriScheme
                )
            ) {
                cursor?.use {
                    val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                    // 1.Technically the column stores an int, but cursor.getString() will do the conversion automatically.
                    // it.getString(sizeIndex)
                    // 2.it.moveToFirst() -> Caused by: android.database.CursorIndexOutOfBoundsException: Index -1 requested, with a size of 1
                    if (it.moveToFirst() && !it.isNull(sizeIndex)) it.getLong(sizeIndex) else zero
                }
            } else if (TextUtils.equals("file", uriScheme)) File(
                getFilePathByUri(uri) ?: return zero
            ).length() else zero
        }


    /**
     * 计算`文件/文件夹`的大小
     *
     * @param path 文件/文件夹的路径
     * @param sizeType 指定要转换的单位类型
     * @return 大小 double
     */
    fun calculateFileOrDirSize(path: String?, sizeType: FileSizeType): Double {
        if (path.isNullOrBlank()) return 0.00

        val file = File(path)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) getFolderSize(file) else getFileSize(file)
        } catch (e: Exception) {
            e.printStackTrace()
            e("获取文件大小 获取失败!")
        }
        return formatSizeByType(blockSize, 2, sizeType).toDouble()
    }

    /**
     * 计算`文件/文件夹`的大小
     *
     * @param path 文件/文件夹的路径
     * @return 大小
     */
    fun calculateFileOrDirSize(path: String?): Long {
        if (path.isNullOrBlank()) return 0L

        val file = File(path)
        var blockSize = 0L
        try {
            blockSize = if (file.isDirectory) getFolderSize(file) else getFileSize(
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            e("获取文件大小 获取失败!")
        }
        i("获取文件大小 =$blockSize")
        return blockSize
    }

    /**
     * 自动计算指定文件或指定文件夹的大小
     *
     * @param path 文件路径
     * @return 带 B、KB、M、GB、TB 单位的字符串
     */
    fun getFileOrDirSizeFormatted(path: String?): String = formatFileSize(
        calculateFileOrDirSize(path)
    )

    // format size
    //-----------------------------------------------------------------------

    fun formatFileSize(size: Long): String = formatFileSize(size, 2)

    /**
     * @param scale 精确到小数点以后几位
     */
    fun formatFileSize(size: Long, scale: Int): String {
        val dividend = 1024L
        //ROUND_DOWN 1023 -> 1023B ; ROUND_HALF_UP  1023 -> 1KB
        val kiloByte =
            BigDecimal(size.toDouble()).divide(BigDecimal(dividend), scale, BigDecimal.ROUND_DOWN)
        if (kiloByte.toDouble() < 1) {
            return "${kiloByte.toPlainString()}B"
        }
        val megaByte = BigDecimal(kiloByte.toDouble()).divide(
            BigDecimal(dividend),
            scale,
            BigDecimal.ROUND_HALF_UP
        )
        if (megaByte.toDouble() < 1) {
            return "${kiloByte.toPlainString()}KB"
        }
        val gigaByte = BigDecimal(megaByte.toDouble()).divide(
            BigDecimal(dividend),
            scale,
            BigDecimal.ROUND_HALF_UP
        )
        if (gigaByte.toDouble() < 1) {
            return "${megaByte.toPlainString()}M"
        }
        val teraBytes = BigDecimal(gigaByte.toDouble()).divide(
            BigDecimal(dividend),
            scale,
            BigDecimal.ROUND_HALF_UP
        )
        if (teraBytes.toDouble() < 1) {
            return "${gigaByte.toPlainString()}GB"
        }
        return "${teraBytes.toPlainString()}TB"
    }

    /**
     * 转换文件大小,指定转换的类型
     *
     * BigDecimal 实现提供（相对）精确的除法运算。当发生除不尽的情况时(ArithmeticException)，由scale参数指定精度，以后的数字四舍五入
     * <p>
     * https://www.liaoxuefeng.com/wiki/1252599548343744/1279768011997217
     * https://zhuanlan.zhihu.com/p/75780642
     * <pre>
     *      注: 禁止使用构造方法BigDecimal(double)的方式把double值转化为BigDecimal对象
     *      说明：反编译出的字节码文件显示每次循环都会new出一个StringBuilder对象，然后进行append操作，最后通过toString方法返回String对象，造成内存资源浪费。
     *      BigDecimal result = new BigDecimal(Double.toString(megaByte));
     * </pre>
     *
     * @param size 大小 Byte
     * @param scale 精确到小数点以后几位
     */
    fun formatSizeByType(size: Long, scale: Int, sizeType: FileSizeType): BigDecimal =
        BigDecimal(size.toDouble()).divide(
            BigDecimal(
                when (sizeType) {
                    SIZE_TYPE_B -> 1L
                    SIZE_TYPE_KB -> 1024L
                    SIZE_TYPE_MB -> 1024L * 1024L
                    SIZE_TYPE_GB -> 1024L * 1024L * 1024L
                    SIZE_TYPE_TB -> 1024L * 1024L * 1024L * 1024L
                }
            ),
            scale,
            if (sizeType == SIZE_TYPE_B) BigDecimal.ROUND_DOWN else BigDecimal.ROUND_HALF_UP
        )

    /**
     * 转换文件大小带单位
     */
    fun getFormattedSizeByType(size: Long, scale: Int, sizeType: FileSizeType): String {
        return "${formatSizeByType(size, scale, sizeType).toPlainString()}${sizeType.unit}"
    }

}