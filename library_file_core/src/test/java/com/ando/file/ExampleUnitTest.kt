package com.ando.file

import com.ando.file.common.FileSizeUtils
import org.junit.Test
import java.math.BigDecimal

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        //assertEquals(4, 2 + 2)
        //26543121 Byte -> 25921.0166 KB -> 25.31349 M
        println("大小 : ${testFormatFileSize(1023, 2)}")
        println("大小 : ${testFormattedSizeByType(10995116256, 2, FileSizeUtils.FileSizeType.SIZE_TYPE_KB)}")
    }

    /**
     * @param scale 精确到小数点以后几位
     */
    fun testFormatFileSize(size: Long, scale: Int): String {
        val dividend = 1024L
        val kiloByte = BigDecimal(size.toDouble()).divide(BigDecimal(dividend), scale, BigDecimal.ROUND_HALF_UP)
        if (kiloByte.toDouble() < 1) {
            return "${kiloByte.toPlainString()}Byte"  //"0KB"
        }
        val megaByte = BigDecimal(kiloByte.toDouble()).divide(BigDecimal(dividend), scale, BigDecimal.ROUND_HALF_UP)
        if (megaByte.toDouble() < 1) {
            return "${kiloByte.toPlainString()}KB" //"1M"
        }
        val gigaByte = BigDecimal(megaByte.toDouble()).divide(BigDecimal(dividend), scale, BigDecimal.ROUND_HALF_UP)
        if (gigaByte.toDouble() < 1) {
            return "${megaByte.toPlainString()}M"
        }
        val teraBytes = BigDecimal(gigaByte.toDouble()).divide(BigDecimal(dividend), scale, BigDecimal.ROUND_HALF_UP)
        if (teraBytes.toDouble() < 1) {
            return "${gigaByte.toPlainString()}GB"
        }
        return "${teraBytes.toPlainString()}TB"
    }

    /**
     * 转换文件大小,指定转换的类型
     */
    fun testFormatSizeByType(size: Long, scale: Int, sizeType: FileSizeUtils.FileSizeType): BigDecimal =
        BigDecimal(size.toDouble()).divide(
            BigDecimal(
                when (sizeType) {
                    FileSizeUtils.FileSizeType.SIZE_TYPE_B -> 1L
                    FileSizeUtils.FileSizeType.SIZE_TYPE_KB -> 1024L
                    FileSizeUtils.FileSizeType.SIZE_TYPE_MB -> 1024L * 1024L
                    FileSizeUtils.FileSizeType.SIZE_TYPE_GB -> 1024L * 1024L * 1024L
                    FileSizeUtils.FileSizeType.SIZE_TYPE_TB -> 1024L * 1024L * 1024L * 1024L
                }
            ), scale, BigDecimal.ROUND_HALF_UP
        )

    /**
     * 转换文件大小,指定转换的类型 ,带单位
     */
    fun testFormattedSizeByType(size: Long, scale: Int, sizeType: FileSizeUtils.FileSizeType): String {
        return "${testFormatSizeByType(size, scale, sizeType).toPlainString()}${sizeType.unit}"
    }

}