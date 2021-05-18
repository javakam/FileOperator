package com.ando.file.sample.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import ando.file.core.FileOperator.getContext
import ando.file.core.FileLogger.e
import ando.file.core.FileUri.getPathByUri
import ando.file.core.FileUri.getUriByPath
import java.io.*

/**
 * Bitmap <--> String
 */
object Base64Utils {

    /**
     * 获取图片的 base64 数据
     */
    fun bitmap2Base64(bitmap: Bitmap?): String {
        if (bitmap == null || bitmap.isRecycled) {
            return ""
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val photoString = String(encode(baos.toByteArray()))
        bitmap.recycle()
        return photoString
    }

    /**
     * 将文件转成 base64 字符串
     * <pre>
     *     适配 Android 10
     * <pre>
     *
     * @param path 文件路径
     */
    @Throws(Exception::class)
    fun encodeFileToBase64(path: String?): String {
        if (path == null || path.isBlank()) {
            return ""
        }
        return encodeFileToBase64(getUriByPath(path))
    }

    @Throws(Exception::class)
    fun encodeFileToBase64(uri: Uri?): String =
        uri?.run {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                    String(encode(sb.toString().toByteArray()))
                } else {
                    // Build.VERSION_CODES.O 以下
                    val file = File(getPathByUri(uri) ?: return "")
                    val inputFile = FileInputStream(file)
                    val buffer = ByteArray(file.length().toInt())
                    inputFile.read(buffer)
                    inputFile.close()
                    String(encode(buffer))
                }
            } catch (e: Exception) {
                e("encodeFileToBase64 Exception : $e")
                ""
            }
        } ?: ""

    fun isContentUriExists(
        context: Context?,
        uri: Uri?,
    ): Boolean {
        if (null == context) {
            return false
        }
        val cr = context.contentResolver
        try {
            val afd = cr.openAssetFileDescriptor(uri ?: return false, "r")
            if (null == afd) {
                return false
            } else {
                try {
                    afd.close()
                } catch (e: IOException) {
                }
            }
        } catch (e: FileNotFoundException) {
            return false
        }
        return true
    }

    /**
     * 将base64 转成 Bitmap
     */
    fun base64ToBitmap(base64Data: String): Bitmap? {
        if (base64Data.isBlank()) {
            return null
        }
        val bytes = decode(base64Data.toCharArray())
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * 将 base64 字符保存文本文件
     *
     * @param base64Code
     * @param targetPath
     * @throws Exception
     */
    @Throws(Exception::class)
    fun base64ToFile(base64Code: String, targetPath: String?) {
        val buffer = base64Code.toByteArray()
        val out = FileOutputStream(targetPath ?: return)
        out.write(buffer)
        out.close()
    }

    /**
     * 功能：编码字符串
     *
     * @param data 源字符串
     * @return String
     */
    fun encode(data: String): String = String(encode(data.toByteArray()))

    /**
     * 功能：解码字符串
     *
     * @param data 源字符串
     * @return String
     * @author jiangshuai
     * @date 2016年10月03日
     */
    fun decode(data: String): String = String(decode(data.toCharArray()))

    /**
     * 功能：编码byte[]
     *
     * @param data 源
     * @return char[]
     */
    fun encode(data: ByteArray?): CharArray {
        val out = CharArray((data!!.size + 2) / 3 * 4)
        var i = 0
        var index = 0
        while (i < data.size) {
            var quad = false
            var trip = false
            var value = 0xFF and data[i].toInt()
            value = value shl 8
            if (i + 1 < data.size) {
                value = value or (0xFF and data[i + 1].toInt())
                trip = true
            }
            value = value shl 8
            if (i + 2 < data.size) {
                value = value or (0xFF and data[i + 2].toInt())
                quad = true
            }
            out[index + 3] = alphabet[if (quad) value and 0x3F else 64]
            value = value shr 6
            out[index + 2] = alphabet[if (trip) value and 0x3F else 64]
            value = value shr 6
            out[index + 1] = alphabet[value and 0x3F]
            value = value shr 6
            out[index + 0] = alphabet[value and 0x3F]
            i += 3
            index += 4
        }
        return out
    }

    /**
     * 功能：解码
     *
     * @param data 编码后的字符数组
     * @return byte[]
     */
    fun decode(data: CharArray): ByteArray {
        var tempLen = data.size
        for (ix in data.indices) {
            if (data[ix] > 255.toChar() || codes[data[ix].toInt()] < 0) {
                --tempLen // ignore non-valid chars and padding
            }
        }
        // calculate required length:
        // -- 3 bytes for every 4 valid base64 chars
        // -- plus 2 bytes if there are 3 extra base64 chars,
        // or plus 1 byte if there are 2 extra.
        var len = tempLen / 4 * 3
        if (tempLen % 4 == 3) {
            len += 2
        }
        if (tempLen % 4 == 2) {
            len += 1
        }
        val out = ByteArray(len)
        var shift = 0 // # of excess bits stored in accum
        var accum = 0 // excess bits
        var index = 0

        // we now go through the entire array (NOT using the 'tempLen' value)
        for (ix in data.indices) {
            val value =
                if (data[ix] > 255.toChar()) -1 else codes[data[ix].toInt()].toInt()
            if (value >= 0) { // skip over non-code
                accum = accum shl 6 // bits shift up by 6 each time thru
                shift += 6 // loop, with new bits being put in
                accum = accum or value // at the bottom.
                if (shift >= 8) { // whenever there are 8 or more shifted in,
                    shift -= 8 // write them out (from the top, leaving any
                    out[index++] =  // excess at the bottom for next iteration.
                        (accum shr shift and 0xff).toByte()
                }
            }
        }

        // if there is STILL something wrong we just have to throw up now!
        if (index != out.size) {
            throw Error(
                "Miscalculated data length (wrote " + index
                        + " instead of " + out.size + ")"
            )
        }
        return out
    }

    /**
     * 功能：编码文件
     */
    @Throws(IOException::class)
    fun encode(file: File?) {
        if (file?.exists() == true) {
            val decoded = readBytes(file)
            val encoded = encode(decoded)
            writeChars(file, encoded)
        }
    }

    /**
     * 功能：解码文件。
     */
    @Throws(IOException::class)
    fun decode(file: File?) {
        if (file?.exists() == true) {
            val encoded = readChars(file)
            val decoded = decode(encoded)
            writeBytes(file, decoded)
        }
    }

    // code characters for values 0..63
    private val alphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray()

    // lookup table for converting base64 characters to value in range 0..63
    private val codes = ByteArray(256)

    @Throws(IOException::class)
    private fun readBytes(file: File?): ByteArray? {
        val baos = ByteArrayOutputStream()
        val b: ByteArray?
        var fis: InputStream? = null
        var ins: InputStream? = null
        try {
            fis = FileInputStream(file ?: return null)
            ins = BufferedInputStream(fis)
            var count: Int
            val buf = ByteArray(16384)
            while (ins.read(buf).also { count = it } != -1) {
                if (count > 0) {
                    baos.write(buf, 0, count)
                }
            }
            b = baos.toByteArray()
        } finally {
            try {
                fis?.close()
                ins?.close()
                baos.close()
            } catch (e: Exception) {
                println(e)
            }
        }
        return b
    }

    @Throws(IOException::class)
    private fun readChars(file: File?): CharArray {
        val caw = CharArrayWriter()
        var fr: Reader? = null
        var reader: Reader? = null
        try {
            fr = FileReader(file ?: return caw.toCharArray())
            reader = BufferedReader(fr)
            var count: Int
            val buf = CharArray(16384)
            while (reader.read(buf).also { count = it } != -1) {
                if (count > 0) {
                    caw.write(buf, 0, count)
                }
            }
        } finally {
            try {
                caw.close()
                reader?.close()
                fr?.close()
            } catch (e: Exception) {
                println(e)
            }
        }
        return caw.toCharArray()
    }

    @Throws(IOException::class)
    private fun writeBytes(file: File?, data: ByteArray) {
        var fos: OutputStream? = null
        var os: OutputStream? = null
        try {
            fos = FileOutputStream(file ?: return)
            os = BufferedOutputStream(fos)
            os.write(data)
        } finally {
            try {
                os?.close()
                fos?.close()
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeChars(file: File?, data: CharArray) {
        var fos: Writer? = null
        var os: Writer? = null
        try {
            fos = FileWriter(file ?: return)
            os = BufferedWriter(fos)
            os.write(data)
        } finally {
            try {
                os?.close()
                fos?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        for (i in 0..255) {
            codes[i] = -1
            // LoggerUtil.debug(i + "&" + codes[i] + " ");
        }
        run {
            var i = 'A'.toInt()
            while (i <= 'Z'.toInt()) {
                codes[i] = (i - 'A'.toInt()).toByte()
                i++
            }
        }
        run {
            var i = 'a'.toInt()
            while (i <= 'z'.toInt()) {
                codes[i] =
                    (26 + i - 'a'.toInt()).toByte()
                i++
            }
        }
        var i = '0'.toInt()
        while (i <= '9'.toInt()) {
            codes[i] =
                (52 + i - '0'.toInt()).toByte()
            i++
        }
        codes['+'.toInt()] = 62
        codes['/'.toInt()] = 63
    }
}