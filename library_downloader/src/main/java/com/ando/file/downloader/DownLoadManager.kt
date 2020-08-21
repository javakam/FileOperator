package ando.file.downloader

import com.liulishuo.okdownload.OkDownload

/**
 * Title: DownLoadManager
 *
 *
 * Description:
 *
 *
 * @author javakam
 * @date 2020/4/20  14:35
 */
object DownLoadManager {

    fun cancelAll() {
        OkDownload.with().downloadDispatcher().cancelAll()
    }
}