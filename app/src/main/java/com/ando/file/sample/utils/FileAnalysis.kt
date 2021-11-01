package com.ando.file.sample.utils

import ando.file.core.FileSizeUtils
import ando.file.core.FileUtils
import android.util.ArrayMap
import java.io.File

/**
 * todo 2021年11月1日 10:37:55
 *
 * @author javakam
 * @date 2021-09-24  15:53
 */
object FileAnalysis {

    /**
     * content://com.android.providers.media.documents/document/image:53283
     * /storage/emulated/0/DCIM/Screenshots/Games/Screenshot_2021_0828_220956_com.tencent.tmgp.sgame.jpg
     *
     * 路飞
     * content://com.android.externalstorage.documents/document/primary:aaaaa/1548551182723.png
     * /storage/emulated/0/Android/data/com.ando.file.sample/files/Documents/aaaaa/1548551182723.png
     */
    fun proceedFileDir(dirPath: String, vararg suffixTypes: String): Map<String, Long>? {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return null

        val suffixSizeMap = ArrayMap<String, Long>()
        val dirFiles = dir.listFiles()
        if (dirFiles.isNullOrEmpty()) return null
        dirFiles.forEach { f: File ->
            val suffix = FileUtils.getExtension(f.absolutePath)
            if (suffixTypes.contains(suffix)) {
                if (!suffixSizeMap.keys.contains(suffix)) {
                    suffixSizeMap.keys.add(suffix)
                }
                suffixSizeMap[suffix] = suffixSizeMap[suffix]?.plus(FileSizeUtils.getFileSize(f))
            }
        }
        return suffixSizeMap
    }

}