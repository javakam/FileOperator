package ando.file.downloader.file

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import ando.file.common.FileType
import java.util.*

/**
 * Title: FileChooserHelper
 *
 *
 * Description:
 *
 *
 * @author javakam
 * @date 2020-02-21
 */
class FileChooserHelper {
    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var isFragment: Boolean
    private var mFiles: MutableList<FileBean>
    var maxSize = 10

    val files: List<FileBean>
        get() = mFiles

    val validFiles: List<FileBean>
        get() {
            if (mFiles.isNotEmpty()) {
                val realFiles: MutableList<FileBean> = ArrayList()
                for (file in mFiles) {
                    if (!file.isInvalid) {
                        realFiles.add(file)
                    }
                }
                return realFiles
            }
            return mFiles
        }

    //有效的文件个数
    val validFilesNumber: Int
        get() {
            if (mFiles.isEmpty()) {
                return 0
            }
            var size = 0
            for (fileBean in mFiles) {
                if (!fileBean.isInvalid) {
                    size++
                }
            }
            return size
        }

    constructor(activity: Activity?) {
        this.activity = activity
        mFiles = ArrayList()
        isFragment = false
    }

    constructor(fragment: Fragment?) {
        this.fragment = fragment
        activity = fragment?.activity
        mFiles = ArrayList()
        isFragment = true
    }

    fun getFile(position: Int): FileBean {
        return mFiles[position]
    }

    //有效的文件总大小
    fun getValidFilesTotalLength(lastSelectedFileLength: Long): Long {
        var lastLength = lastSelectedFileLength
        if (mFiles.isEmpty()) {
            return 0
        }
        if (lastLength < 0) {
            lastLength = 0
        }
        var totalSize: Long = 0
        for (fileBean in mFiles) {
            if (!fileBean.isInvalid) {
                totalSize += fileBean.sizeBytes //统计之前选的文件大小
            }
        }
        totalSize += lastLength //刚选的文件大小
        //L.w("选择文件返回 $fileType  totalSize = " + totalSize);
        return totalSize
    }


    /**
     * 有效的文件个数 -> 指定 FileType(文件类型) 的数量 eg: 指定数量限定 : 图片 30 张 ; 音视频 3 个
     */
    fun getValidFilesNumberByFileType(selectedNum: Int, vararg fileTypes: FileType): Int {
        if (mFiles.isEmpty()) {
            return 0
        }
        var size = selectedNum //注意:刚选则文件也要计入统计! 目前项目仅支持单选文件,所以 selectedNum=1
        for (fileBean in mFiles) {
            if (!fileBean.isInvalid) {
                for (fileType in fileTypes) {
                    if (fileBean.fileType == fileType) {
                        size++
                    }
                }
            }
        }
        return size
    }

    fun addFile(fileBean: FileBean?) {
        if (validFilesNumber >= maxSize) {
            //ToastUtils.shortToast(String.format(Locale.getDefault(), "最多支持%d个文件", maxSize));
            return
        }
        if (fileBean == null) {
            return
        }
        mFiles.add(fileBean)
    }

    fun addFiles(fileList: List<FileBean>?) {
        if (validFilesNumber >= maxSize) {
            //ToastUtils.shortToast(String.format(Locale.getDefault(), "最多支持%d个文件", maxSize));
            return
        }
        if (fileList == null || fileList.isEmpty()) {
            return
        }
        mFiles.addAll(fileList)
    }

    fun removeValidFile() {
        if (mFiles.isNotEmpty()) {
            val iterator = mFiles.iterator()
            while (iterator.hasNext()) {
                val fileBean = iterator.next()
                if (fileBean.isInvalid) {
                    iterator.remove()
                }
            }
        }
    }

    fun deleteFiles() {
        mFiles.clear()
    }

    fun deleteFile(fileBean: FileBean) {
        mFiles.remove(fileBean)
    }

    private fun startActivity(intent: Intent) {
        if (isFragment) {
            fragment?.startActivity(intent)
        } else {
            activity?.startActivity(intent)
        }
    }

    private fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (isFragment) {
            fragment?.startActivityForResult(intent, requestCode)
        } else {
            activity?.startActivityForResult(intent, requestCode)
        }
    }
}