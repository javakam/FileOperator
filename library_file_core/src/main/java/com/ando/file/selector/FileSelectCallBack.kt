package com.ando.file.selector

/**
 * Title: FileSelectCallBack
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/5/21  11:19
 */
interface FileSelectCallBack {

    fun onSuccess(results: List<FileSelectResult>?)

    fun onError(e: Throwable?)

}