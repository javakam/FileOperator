package ando.file.downloader

import com.liulishuo.okdownload.DownloadTask

object QueueTagUtils {
    private const val KEY_STATUS = 0
    private const val KEY_OFFSET = 1
    private const val KEY_TOTAL = 2
    private const val KEY_TASK_NAME = 3
    private const val KEY_PRIORITY = 4

    fun saveStatus(task: DownloadTask, status: String) {
        task.addTag(KEY_STATUS, status)
    }

    fun getStatus(task: DownloadTask): String? {
        val status = task.getTag(KEY_STATUS)
        return if (status != null) status as? String else null
    }

    fun saveOffset(task: DownloadTask, offset: Long) {
        task.addTag(KEY_OFFSET, offset)
    }

    fun getOffset(task: DownloadTask): Long {
        val offset = task.getTag(KEY_OFFSET)
        return if (offset != null) offset as Long else 0
    }

    fun saveTotal(task: DownloadTask, total: Long) {
        task.addTag(KEY_TOTAL, total)
    }

    fun getTotal(task: DownloadTask): Long {
        val total = task.getTag(KEY_TOTAL)
        return if (total != null) total as Long else 0
    }

    fun saveTaskName(task: DownloadTask, name: String) {
        task.addTag(KEY_TASK_NAME, name)
    }

    fun getTaskName(task: DownloadTask): String? {
        val taskName = task.getTag(KEY_TASK_NAME)
        return if (taskName != null) taskName as String else null
    }

    fun savePriority(task: DownloadTask, priority: Int) {
        task.addTag(KEY_PRIORITY, priority)
    }

    fun getPriority(task: DownloadTask): Int {
        val priority = task.getTag(KEY_PRIORITY)
        return if (priority != null) priority as Int else 0
    }

    fun clearProceedTask(task: DownloadTask) {
        task.removeTag(KEY_STATUS)
        task.removeTag(KEY_OFFSET)
        task.removeTag(KEY_TOTAL)
        task.removeTag(KEY_OFFSET)
    }
}