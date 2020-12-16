package ando.file.compressor

import android.net.Uri

/**
 * Created on 2018/1/3 19:43
 *
 * @author andy
 *
 * A functional interface (callback) that returns true or false for the given input path should be compressed.
 */
interface ImageCompressPredicate {
    /**
     * Determine the given input path should be compressed and return a boolean.
     * @param uri input uri
     * @return the boolean result
     */
    fun apply(uri: Uri?): Boolean
}

interface OnImageCompressListener {
    /**
     * Fired when the compression is started, override to handle in your own code
     */
    fun onStart() {}

    /**
     * Fired when a compression returns successfully, override to handle in your own code
     */
    fun onSuccess(index: Int = 0, uri: Uri?) {}

    /**
     * Fired when a compression fails to complete, override to handle in your own code
     */
    fun onError(e: Throwable?) {}
}

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