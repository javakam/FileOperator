/**
 * Copyright (C)  javakam, FileOperator Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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