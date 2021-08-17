package ando.file.compressor

import ando.file.core.FileGlobal.MODE_READ_ONLY
import ando.file.core.FileGlobal.openFileDescriptor
import ando.file.core.FileLogger
import ando.file.core.FileSizeUtils
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.experimental.and

object ImageChecker {

    private const val JPG = ".jpg"
    private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    /**
     * Determine if it is JPG.
     *
     * @param ins image file input stream
     */
    fun isJPG(ins: InputStream?): Boolean {
        return isJPG(toByteArray(ins))
    }

    private fun isJPG(data: ByteArray?): Boolean {
        if (data == null || data.size < 3) {
            return false
        }
        val signatureB = byteArrayOf(data[0], data[1], data[2])
        return JPEG_SIGNATURE.contentEquals(signatureB)
    }

    fun extSuffix(uri: Uri?): String {
        return try {
            BitmapFactory.Options().run {
                inJustDecodeBounds = true
                BitmapFactory.decodeFileDescriptor(openFileDescriptor(uri, MODE_READ_ONLY)?.fileDescriptor ?: return JPG, null, this)
                outMimeType.replace("image/", ".")
            }
        } catch (e: Exception) {
            JPG
        }
    }

    fun needCompress(leastCompressSize: Int, uri: Uri?): Boolean =
        if (leastCompressSize > 0) {
            FileSizeUtils.getFileSize(uri) > leastCompressSize shl 10
        } else true

    /**
     * 获取图片的旋转角度
     * 只能通过原始文件获取，如果已经进行过 bitmap 操作无法获取。
     */
    fun getRotateDegree(exif: ExifInterface): Int {
        var result = 0
        try {
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> result = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> result = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> result = 270
            }
        } catch (ignore: IOException) {
            FileLogger.e("Orientation not found")
            return 0
        }
        return result
    }

    /**
     * Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
     */
    fun getOrientation(ins: InputStream?): Int {
        return getOrientation(toByteArray(ins))
    }

    fun getOrientation(jpeg: ByteArray?): Int {
        if (jpeg == null) {
            return 0
        }
        var offset = 0
        var length = 0

        // ISO/IEC 10918-1:1993(E)
        while (offset + 3 < jpeg.size && (jpeg[offset++] and (0xFF).toByte()) == (0xFF).toByte()) {
            val marker: Int = (jpeg[offset] and (0xFF).toByte()).toInt()

            // Check if the marker is a padding.
            if (marker == 0xFF) {
                continue
            }
            offset++

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                break
            }

            // Get the length and check if it is reasonable.
            length = pack(jpeg, offset, 2, false)
            if (length < 2 || offset + length > jpeg.size) {
                FileLogger.e("Invalid length")
                return 0
            }

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 8 && pack(
                    jpeg,
                    offset + 2,
                    4,
                    false
                ) == 0x45786966 && pack(jpeg, offset + 6, 2, false) == 0
            ) {
                offset += 8
                length -= 8
                break
            }

            // Skip other markers.
            offset += length
            length = 0
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            // Identify the byte order.
            var tag = pack(jpeg, offset, 4, false)
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                FileLogger.e("Invalid byte order")
                return 0
            }
            val littleEndian = tag == 0x49492A00

            // Get the offset and check if it is reasonable.
            var count = pack(jpeg, offset + 4, 4, littleEndian) + 2
            if (count < 10 || count > length) {
                FileLogger.e("Invalid offset")
                return 0
            }
            offset += count
            length -= count

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian)
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian)
                if (tag == 0x0112) {
                    when (pack(jpeg, offset + 8, 2, littleEndian)) {
                        1 -> return 0
                        3 -> return 180
                        6 -> return 90
                        8 -> return 270
                        else -> {
                        }
                    }
                    FileLogger.e("Unsupported orientation")
                    return 0
                }
                offset += 12
                length -= 12
            }
        }
        FileLogger.e("Orientation not found")
        return 0
    }

    private fun pack(
        bytes: ByteArray,
        off: Int,
        len: Int,
        littleEndian: Boolean,
    ): Int {
        var offset = off
        var length = len
        var step = 1
        if (littleEndian) {
            offset += length - 1
            step = -1
        }
        var value = 0
        while (length-- > 0) {
            value = value shl 8 or (bytes[offset] and (0xFF).toByte()).toInt()
            offset += step
        }
        return value
    }

    private fun toByteArray(inputStream: InputStream?): ByteArray {
        if (inputStream == null) {
            return ByteArray(0)
        }
        val buffer = ByteArrayOutputStream()
        var read: Int
        val data = ByteArray(4096)
        try {
            while (inputStream.read(data, 0, data.size).also { read = it } != -1) {
                buffer.write(data, 0, read)
            }
        } catch (ignored: Exception) {
            return ByteArray(0)
        } finally {
            try {
                buffer.close()
            } catch (ignored: IOException) {
            }
        }
        return buffer.toByteArray()
    }

}