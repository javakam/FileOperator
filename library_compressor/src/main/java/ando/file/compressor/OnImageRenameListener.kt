package ando.file.compressor

import android.net.Uri

/**
 * 提供修改压缩图片命名接口
 *
 * A functional interface (callback) that used to rename the file after compress.
 */
interface OnImageRenameListener {
    /**
     * 压缩前调用该方法用于修改压缩后文件名
     *
     *
     * Call before compression begins.
     *
     * @param uri 传入文件路径/ file uri
     * @return 返回重命名后的字符串/ file name
     */
    fun rename(uri: Uri?): String?
}