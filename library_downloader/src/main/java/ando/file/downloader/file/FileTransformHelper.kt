package ando.file.downloader.file

import ando.file.core.FileSizeUtils
import ando.file.downloader.DownLoadTaskBean
import java.io.File
import java.util.*

/**
 * # FileTransformHelper
 *
 * FileBean & TaskBean 相互转换
 */
object FileTransformHelper {
    fun task2File(downLoadTask: DownLoadTaskBean?): FileBean {
        //父目录绝对路径
        val parentFileAbsolutePath = downLoadTask?.parentFile
        val fileBean = FileBean()
        fileBean.id = generateUUID()
        fileBean.path = parentFileAbsolutePath
        fileBean.mimeType = ""

        //文件大小
        val fileSize = FileSizeUtils.formatFileSize(
            FileSizeUtils.getFileSize(File(parentFileAbsolutePath)).toLong()
        )
        fileBean.size = fileSize
        //fileBean.setName(FileChooser.getFileName(parentFileAbsolutePath));
        fileBean.name = downLoadTask?.tname
        return fileBean
    }

    fun taskList2FileList(downLoadTaskList: List<DownLoadTaskBean>): List<FileBean> {
        if (downLoadTaskList.isEmpty()) {
            return ArrayList()
        }
        val fileList: MutableList<FileBean> = ArrayList()
        for (task in downLoadTaskList) {
            val fileBean =
                task2File(task)
            fileList.add(fileBean)
        }
        return fileList
    }

    /**
     * 自动生成32位的UUid，对应数据库的主键id进行插入用。
     */
    fun generateUUID(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

}