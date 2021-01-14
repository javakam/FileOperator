package ando.file.downloader

import android.util.SparseArray
import android.view.View
import android.widget.*
import ando.file.downloader.QueueController4Speed.Companion.STATUS_LOOK_OVER
import ando.file.downloader.view.FileBaseViewHolder
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.StatusUtil
import com.liulishuo.okdownload.core.Util
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend.Listener4SpeedModel
import ando.file.common.FileLogger

/**
 * QueueDownloadListener4WithSpeed
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2020/3/2  14:38
 */
class QueueDownloadListener4Speed : DownloadListener4WithSpeed() {
    companion object {
        private const val TAG = "123"
    }

    private val holderMap = SparseArray<FileBaseViewHolder>()
    private val totalLengthMap = SparseArray<Long>()
    private val readableTotalLengthMap = SparseArray<String>()

    fun bind(task: DownloadTask, holder: FileBaseViewHolder) {
        //L.i(TAG, "bind " + task.id + " with " + holder)
        // replace.
        val size = holderMap.size()
        for (i in 0 until size) {
            if (holderMap.valueAt(i) === holder) {
                holderMap.removeAt(i)
                //holderMap.put(i, null)
                break
            }
        }
        holderMap.put(task.id, holder)
    }

    fun resetInfo(task: DownloadTask, holder: FileBaseViewHolder) {
        //Holder View
        val flDelete: FrameLayout = holder.getView(R.id.fl_down_delete)
        val btnAction: Button = holder.getView(R.id.bt_down_action)
        val tvStatus: TextView = holder.getView(R.id.tv_down_status)
        val progressBar: ProgressBar = holder.getView(R.id.progressbar_down)

        val tvTotal: TextView = holder.getView(R.id.tv_down_written_total)
        val tvSpeed: TextView = holder.getView(R.id.tv_down_speed)
        val tvPercent: TextView = holder.getView(R.id.tv_down_percent)

        // task name
        val taskName = ando.file.downloader.QueueTagUtils.getTaskName(task)
        holder.setText(R.id.tv_down_name, taskName)

        // process references
        val status = ando.file.downloader.QueueTagUtils.getStatus(task)

        //Fixed : StatusUtil.getStatus(task).name  应为 COMPLETED 而不是 UNKOWN
        FileLogger.i(TAG, "setProgress ${task.id}  $status ${StatusUtil.getStatus(task).name}")

        if (status != null) {
            //  started
            tvStatus.text = status

            tvTotal.visibility = View.VISIBLE
            tvSpeed.visibility = View.VISIBLE
            tvPercent.visibility = View.VISIBLE

            tvTotal.text = ""
            tvSpeed.text = ""
            tvPercent.text = ""

            if (status == EndCause.COMPLETED.name) {
                progressBar.progress = progressBar.max
                btnAction.text = STATUS_LOOK_OVER

                flDelete.visibility = View.VISIBLE
            } else {

                holder.setText(R.id.tv_down_status_text, "")

                val total = ando.file.downloader.QueueTagUtils.getTotal(task)
                FileLogger.e(
                    TAG, "total= $total   ${ando.file.downloader.QueueTagUtils.getOffset(
                        task
                    )}"
                )
                if (total == 0L) {
                    progressBar.progress = 0

                    btnAction.setText(R.string.start)
                    flDelete.visibility = View.GONE
                } else {
                    btnAction.setText(R.string.cancel)
                    flDelete.visibility = View.VISIBLE

                    //向 ProgressBar 设置进度
                    DownloadProgressUtils.calcProgressToViewAndMark(
                        progressBar,
                        ando.file.downloader.QueueTagUtils.getOffset(task), total, false
                    )
                }
            }

        } else {
            // non-started
            val statusOnStore = StatusUtil.getStatus(task)
            ando.file.downloader.QueueTagUtils.saveStatus(
                task,
                statusOnStore.toString()
            )

            tvTotal.visibility = View.GONE
            tvSpeed.visibility = View.GONE
            tvPercent.visibility = View.GONE
            flDelete.visibility = View.VISIBLE

            if (statusOnStore == StatusUtil.Status.COMPLETED) {
                tvStatus.text = EndCause.COMPLETED.name
                progressBar.progress = progressBar.max

                FileLogger.w(TAG, "statusOnStore= ${task.id}  ${statusOnStore.name}")
                btnAction.text = STATUS_LOOK_OVER
                flDelete.visibility = View.VISIBLE
            } else {
                when (statusOnStore) {
                    StatusUtil.Status.IDLE -> {
                        tvStatus.setText(R.string.state_idle);
                    }
                    StatusUtil.Status.PENDING -> {
                        tvStatus.setText(R.string.state_pending);
                    }
                    StatusUtil.Status.RUNNING -> {
                        tvStatus.setText(R.string.state_running);
                    }
                    else -> {//UNKNOWN
                        tvStatus.setText(R.string.state_unknown);
                    }
                }

                if (statusOnStore == StatusUtil.Status.UNKNOWN) {
                    val info = StatusUtil.getCurrentInfo(task)
                    FileLogger.e(
                        TAG,
                        "statusOnStore UNKNOWN = ${task.id}  ${task.filename} ${task.url}  ${statusOnStore.name} info= ${info?.url}" +
                                " info.totalOffset${info?.totalOffset}, info.totalLength=${info?.totalLength}"
                    )

                    progressBar.progress = 0
                    FileLogger.w("${task.id}  ${statusOnStore.name}")
                    btnAction.setText(R.string.start)
                    holder.setText(R.id.tv_down_status_text, "")

                    flDelete.visibility = View.GONE
                } else {
                    btnAction.setText(R.string.start)
                    holder.setText(R.id.tv_down_status_text, "")

                    //断点信息
                    val info = StatusUtil.getCurrentInfo(task)
                    FileLogger.e(
                        TAG,
                        "statusOnStore not UNKNOWN = ${task.id}  ${task.filename} ${task.url}  ${statusOnStore.name} info= ${info?.url}"
                    )

                    if (info != null) {
                        flDelete.visibility = View.VISIBLE

                        ando.file.downloader.QueueTagUtils.saveTotal(
                            task,
                            info.totalLength
                        )
                        ando.file.downloader.QueueTagUtils.saveOffset(
                            task,
                            info.totalOffset
                        )
                        //向 ProgressBar 设置进度
                        DownloadProgressUtils.calcProgressToViewAndMark(
                            progressBar,
                            info.totalOffset, info.totalLength, false
                        )
                    } else {
                        progressBar.progress = 0

                        flDelete.visibility = View.GONE
                    }
                }

            }

        }
    }

    fun clearBoundHolder() = holderMap.clear()

    override fun taskStart(task: DownloadTask) {
        //L.i(TAG, "【1、taskStart】")

        val status = DownloadListener1Status.TASKSTART
        ando.file.downloader.QueueTagUtils.saveStatus(task, status)

        val holder = holderMap.get(task.id) ?: return

        holder.setText(R.id.tv_down_status, status)
        holder.setText(R.id.bt_down_action, R.string.cancel)
        holder.setText(R.id.tv_down_status_text, "")
    }

    override fun infoReady(
        task: DownloadTask,
        info: BreakpointInfo,
        fromBreakpoint: Boolean,
        model: Listener4SpeedModel
    ) {
//        totalLength = info.totalLength
//        readableTotalLength = Util.humanReadableBytes(totalLength, true)

        totalLengthMap.put(task.id, info.totalLength)
        readableTotalLengthMap.put(task.id, Util.humanReadableBytes(info.totalLength, true))
        //L.i(TAG,"【2、infoReady】当前进度" + info.totalOffset.toFloat() / totalLength * 100 + "%" + "，" + info.toString())
    }

    override fun connectStart(
        task: DownloadTask,
        blockIndex: Int,
        requestHeaders: Map<String?, List<String?>?>
    ) {
        //L.i( TAG,"【3、connectStart】$blockIndex")
    }

    override fun connectEnd(
        task: DownloadTask,
        blockIndex: Int,
        responseCode: Int,
        responseHeaders: Map<String?, List<String?>?>
    ) {
        //L.i( TAG,"【4、connectEnd】$blockIndex，$responseCode")

        val status = DownloadListener1Status.CONNECTED
        ando.file.downloader.QueueTagUtils.saveStatus(task, status)
        ando.file.downloader.QueueTagUtils.saveTotal(
            task,
            totalLengthMap.get(task.id)
        )

    }

    override fun progressBlock(
        task: DownloadTask,
        blockIndex: Int,
        currentBlockOffset: Long,
        blockSpeed: SpeedCalculator
    ) {
//        val readableOffset = Util.humanReadableBytes(currentBlockOffset, true)
//        val progressStatus = "$readableOffset/$readableTotalLength"
//        val speed = blockSpeed.speed()
//        val percent = currentBlockOffset.toFloat() / totalLength * 100
        FileLogger.w(TAG, "【5、progressBlock】$blockIndex，$currentBlockOffset")

//        L.w( "【5、progressBlock】  ${task.id}  blockIndex={$blockIndex}  currentOffset=[$progressStatus]，" +
//                "totalLength=$totalLength , speed=${speed}，进度：$percent%")

        val status = DownloadListener1Status.PROGRESS
        ando.file.downloader.QueueTagUtils.saveStatus(task, status)

        val holder = holderMap.get(task.id) ?: return
        DownloadProgressUtils.calcProgressToViewAndMark(
            holder.getView(R.id.progressbar_down),
            currentBlockOffset,
            totalLengthMap.get(task.id),// totalLength
            false
        )

        //手动更新断点信息到数据库 , 解决小文件没有计入断点信息的问题
        task.info?.let { OkDownload.with().breakpointStore().update(it) }

    }

    override fun progress(task: DownloadTask, currentOffset: Long, taskSpeed: SpeedCalculator) {
        val readableOffset = Util.humanReadableBytes(currentOffset, true)
        val progressStatus = "$readableOffset/${readableTotalLengthMap.get(task.id)}"
        val speed = taskSpeed.speed()
        val percent = currentOffset.toFloat() / totalLengthMap.get(task.id) * 100

//        val readableOffset = Util.humanReadableBytes(currentOffset, true)
//        val progressStatus = "$readableOffset/$readableTotalLength"
//        val speed = taskSpeed.speed()
//        val percent = currentOffset.toFloat() / totalLength * 100

        //eg: 【6、progress】13195049[13.2 MB/43.4 MB]，速度：667.4 kB/s，进度：30.385971%
        FileLogger.i(
            TAG,
            "【6、progress】${task.id} $readableOffset $currentOffset[$progressStatus]，速度：$speed，进度：$percent%"
        )

        val status = DownloadListener1Status.PROGRESS
        ando.file.downloader.QueueTagUtils.saveStatus(task, status)
        ando.file.downloader.QueueTagUtils.saveOffset(task, currentOffset)

        val holder = holderMap.get(task.id) ?: return

        holder.setText(R.id.tv_down_status, status)
        holder.setText(R.id.bt_down_action, R.string.cancel)
        holder.setText(R.id.tv_down_status_text, "")


        val tvTotal: TextView = holder.getView(R.id.tv_down_written_total)
        //val tvSpeed: TextView = holder.getView(R.id.tv_down_speed)
        //val tvPercent: TextView = holder.getView(R.id.tv_down_percent)
        val flDelete: FrameLayout = holder.getView(R.id.fl_down_delete)

        if (tvTotal.visibility == View.GONE) {
            tvTotal.visibility = View.VISIBLE
        }

//        if (DownloadFileHelper.isDebug()) {
//            if (tvSpeed.visibility == View.GONE) {
//                tvSpeed.visibility = View.VISIBLE
//                tvSpeed.text = speed;
//            }
//            if (tvPercent.visibility == View.GONE) {
//                tvPercent.visibility = View.VISIBLE
//                tvPercent.text = "$percent%";
//            }
//        }

        if (flDelete.visibility == View.GONE) {
            flDelete.visibility = View.VISIBLE
        }

        tvTotal.text = progressStatus;

        DownloadProgressUtils.updateProgressToViewWithMark(
            holder.getView(R.id.progressbar_down),
            currentOffset,
            false
        )

    }

    override fun blockEnd(
        task: DownloadTask,
        blockIndex: Int,
        info: BlockInfo?,
        blockSpeed: SpeedCalculator
    ) {
        FileLogger.i(TAG, "【7、blockEnd】$blockIndex")

        //手动更新断点信息到数据库 , 解决小文件没有计入断点信息的问题
        task.info?.let { OkDownload.with().breakpointStore().update(it) }

    }

    override fun taskEnd(
        task: DownloadTask,
        cause: EndCause,
        realCause: Exception?,
        taskSpeed: SpeedCalculator
    ) {
        FileLogger.i(
            TAG,
            "【8、taskEnd】${task.file?.absolutePath} " + cause.name + "：" + if (realCause != null) realCause.message else "无异常"
        )
//        if (task.id== QueueController4WithSpeed.deleteTask?.id){
//            val tastStatus = StatusUtil.getStatus(task)
//            QueueController4WithSpeed.deleteTask
//        }

        val status = cause.toString()
        ando.file.downloader.QueueTagUtils.saveStatus(task, status)

        //手动更新断点信息到数据库 , 解决小文件没有计入断点信息的问题
        task.info?.let { OkDownload.with().breakpointStore().update(it) }

        //L.w( "${task.file?.absolutePath} end with: $cause ]---===---[  Exception : ${realCause?.message}")
        val holder = holderMap.get(task.id) ?: return

        holder.setText(R.id.tv_down_status, status)
        //DownloadTask.cancel 已完成->delete else 未完成->继续 / 开始
        val taskStatus = StatusUtil.getStatus(task)
        when {
            taskStatus == StatusUtil.Status.COMPLETED -> {
                holder.setText(R.id.bt_down_action, STATUS_LOOK_OVER)
                holder.setText(R.id.tv_down_status_text, R.string.down_paused)
                val flDelete: FrameLayout = holder.getView(R.id.fl_down_delete)
                flDelete.visibility = View.VISIBLE

                holder.setText(R.id.tv_down_written_total, "")
            }
            task.info?.totalOffset ?: -1 > 0L -> {
                holder.setText(R.id.bt_down_action, R.string.goon)
                holder.setText(R.id.tv_down_status_text, R.string.down_paused)
            }
            else -> {
                holder.setText(R.id.bt_down_action, R.string.start)
                holder.setText(R.id.tv_down_status_text, "")
            }
        }

        if (cause == EndCause.COMPLETED) {
            val progressBar: ProgressBar = holder.getView(R.id.progressbar_down)
            progressBar.progress = progressBar.max

            val flDelete: FrameLayout = holder.getView(R.id.fl_down_delete)
            flDelete.visibility = View.VISIBLE
        }
    }

}