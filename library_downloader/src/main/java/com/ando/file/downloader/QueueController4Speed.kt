package ando.file.downloader

import android.text.TextUtils
import android.widget.Button
import android.widget.FrameLayout
import ando.file.downloader.view.FileBaseViewHolder
import ando.file.downloader.view.FileRecyclerAdapter
import ando.file.common.FileLogger
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import java.io.File

class QueueController4Speed {
    companion object {
        const val STATUS_LOOK_OVER = "查看"
    }

    private var adapter: FileRecyclerAdapter<DownloadTask>? = null
    private val taskList = arrayListOf<DownloadTask>()
    private var context: DownloadContext? = null
    private val listener = ando.file.downloader.QueueDownloadListener4Speed()
    private var queueDir: File? = null

    private var urlList: List<DownLoadTaskBean>? = null


    interface CallBack {
        /**
         * 查看
         */
        fun lookOver(holder: FileBaseViewHolder, task: DownloadTask)
    }

    private var callBack: CallBack? = null

    fun setCallBack(callBack: CallBack) {
        this.callBack = callBack
    }

    fun getTaskList(): List<DownloadTask> {
        return taskList
    }

    fun initTasks(
        urlList: List<DownLoadTaskBean>,
        parentFile: File,
        listener: DownloadContextListener
    ) {

        val set = DownloadContext.QueueSet()
        this.queueDir = parentFile
        this.urlList = urlList

        set.setParentPathFile(parentFile)
        set.minIntervalMillisCallbackProcess = 300

        val builder = set.commit()

        for (taskBean in urlList) {
            val boundTask = builder.bind(taskBean.url)

            //注:断点下载 根据的是 任务名
            //断点续传失效问题
            ando.file.downloader.QueueTagUtils.saveTaskName(
                boundTask,
                taskBean.tname
            )

        }

        builder.setListener(listener)

        //1 builder.build() 生成 DownloadContext 对象
        //2 把 DownloadContext.tasks 放 taskList 中一份
        this.context = builder.build().also { this.taskList.addAll(it.tasks) }

    }

    //delete /storage/emulated/0/Android/data/com.ando.download/cache/queue failed!
    fun deleteFiles() {
        if (queueDir == null) {
            return
        }

        val children = queueDir?.list()
        if (children != null) {
            for (child in children) {
                if (!File(queueDir, child).delete()) {
                    FileLogger.w("delete $child failed!")
                }
            }
        }

        if (queueDir?.delete() == false) {
            FileLogger.w("delete $queueDir failed!")
        }


        for (task in taskList) {
            ando.file.downloader.QueueTagUtils.clearProceedTask(task)
        }

        FileLogger.i("deleteFiles $queueDir Success!")
    }

    fun deleteFile(task: DownloadTask) {


        if (queueDir == null) {
            return
        }

        FileLogger.e("deleteFile Task: ${task.id}")

        val children = queueDir?.list()
        if (children != null) {
            for (child in children) {

                var realDeleteFile: File? = null
                if (task.filename != null) {
                    if (TextUtils.equals(child, task.filename)) {
                        realDeleteFile = task.file
                    }
                } else {
                    val fileName = task.url.substring(task.url.lastIndexOf('/') + 1);

                    if (TextUtils.equals(child, fileName)) {
                        realDeleteFile = File(task.parentFile, fileName)
                    }

                    FileLogger.w(
                        "fileName : $fileName  realDeleteFile : ${realDeleteFile?.absolutePath}"
                    )

                }

                if (realDeleteFile == null) {
                    continue
                }

                if (!(realDeleteFile.delete())) {
                    FileLogger.w("delete ${realDeleteFile.name} failed!")
                } else {
                    break
                }
            }
        }

        for (t in taskList) {
            if (t == task) {
                ando.file.downloader.QueueTagUtils.clearProceedTask(t)
                break
            }
        }

        //不从列表删除
        //taskList.remove(task)

        FileLogger.i("deleteFile task.id=${task.id}  task.filename=${task.filename} Success!")

    }

    fun setPriority(task: DownloadTask, priority: Int) {
        val newTask = task.toBuilder().setPriority(priority).build()
        this.context = context?.toBuilder()
            ?.bindSetTask(newTask)
            ?.build()
            ?.also {
                //修改优先级之后,重新设置 taskList 数据
                taskList.clear()
                taskList.addAll(it.tasks)
            }
        newTask.setTags(task)
        ando.file.downloader.QueueTagUtils.savePriority(newTask, priority)
    }

    fun start(isSerial: Boolean) {
        if (isSerial) {
            this.context?.start(listener, true)
        } else {
            this.context?.startOnParallel(listener)
        }
    }

    fun stop() {
        if (this.context?.isStarted == true) {
            this.context?.stop()
        }
    }

    fun startTask(holder: FileBaseViewHolder, task: DownloadTask) {

        val status = ando.file.downloader.QueueTagUtils.getStatus(task)
        FileLogger.w("Action...." + task.id + "  " + task.url + "   " + status)

        if (status == EndCause.COMPLETED.name || status == DownloadListener1Status.PROGRESS) {
            FileLogger.w("暂停....")
            task.cancel()
        } else if (status == STATUS_LOOK_OVER) {
            FileLogger.w("$STATUS_LOOK_OVER......")

            callBack?.lookOver(holder, task)
//            deleteFile(task)
//            adapter?.replaceData(taskList)

        } else {
            FileLogger.w("继续....")
            task.enqueue(listener)
        }

//        // priority
//        val priority = TagUtil.getPriority(task)
//        holder.priorityTv.text = holder.priorityTv.context.getString(R.string.priority, priority)
//        holder.prioritySb.progress = priority
//
//        if (this.context?.isStarted == true) {
//            holder.prioritySb.isEnabled = false
//        } else {
//            holder.prioritySb.isEnabled = true
//            println(2)
//
//        }

    }

    fun bind(
        adapter: FileRecyclerAdapter<DownloadTask>,
        holder: FileBaseViewHolder,
        task: DownloadTask
    ) {
        this.adapter = adapter

        //DownloadTask.Id 对应 ViewHolder
        listener.bind(task, holder)
        //ViewHolder 设置数据
        listener.resetInfo(task, holder)

        //Holder View
        val flDelete: FrameLayout = holder.getView(R.id.fl_down_delete)
        val btnAction: Button = holder.getView(R.id.bt_down_action)
        //val tvStatus: TextView = holder.getView(R.id.tv_down_status)
        //val progressBar: ProgressBar = holder.getView(R.id.progressbar_down)

        // 开始
        btnAction.setOnClickListener {
            if (TextUtils.equals(
                    btnAction.text,
                    STATUS_LOOK_OVER
                )
            ) {
                //flDelete.performClick()
                callBack?.lookOver(holder, task)
            } else {
                startTask(holder, task)
            }
        }

        // 删除
        flDelete.setOnClickListener {
            val status = ando.file.downloader.QueueTagUtils.getStatus(task)
            if (status == DownloadListener1Status.PROGRESS) {
                task.cancel()
                //deleteTask=task

                it.postDelayed({
                    //延时删除
                    deleteFile(task)
                    adapter.refresh(taskList)

                }, 300)

            } else {
                deleteFile(task)
                adapter.refresh(taskList)

            }

        }

        /*// priority
        val priority = TagUtil.getPriority(task)
        holder.priorityTv.text = holder.priorityTv.context.getString(R.string.priority, priority)
        holder.prioritySb.progress = priority
        if (this.context?.isStarted == true) {
            holder.prioritySb.isEnabled = false
        } else {

            holder.prioritySb.isEnabled = true

            //非下载中状态下,可以调整优先级
            holder.prioritySb.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        var isFromUser: Boolean = false

                        override fun onProgressChanged(
                                seekBar: SeekBar,
                                progress: Int,
                                fromUser: Boolean
                        ) {
                            isFromUser = fromUser
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            if (isFromUser) {
                                val taskPriority = seekBar.progress
                                setPriority(task, taskPriority)
                                holder.priorityTv.text =
                                    seekBar.context.getString(R.string.priority, taskPriority)
                            }
                        }
                    })
        }*/

    }

    fun size(): Int = taskList.size

}