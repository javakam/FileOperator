package ando.file.downloader

import android.view.View
import ando.file.common.FileType
import ando.file.downloader.file.FileTransformHelper
import ando.file.downloader.view.FileBaseViewHolder
import ando.file.downloader.view.FileRecyclerAdapter
import com.liulishuo.okdownload.DownloadTask

/**
 * Title: QueueTaskAdapter4WithSpeed
 *
 * Description:
 *
 * @author javakam
 * @date 2020/1/15  16:47
 */
class QueueTaskAdapter4Speed(private val controller: QueueController4Speed) :
    FileRecyclerAdapter<DownloadTask>() {

    override fun bindData(holder: FileBaseViewHolder, position: Int, bean: DownloadTask?) {
        if (bean != null) {
            controller.bind(this, holder, bean)

            //final int visibility = BuildConfig.DEBUG ? View.VISIBLE : View.GONE;
            holder.getView<View>(R.id.tv_down_status).visibility = View.GONE
            holder.getView<View>(R.id.tv_down_speed).visibility = View.GONE
            holder.getView<View>(R.id.tv_down_percent).visibility = View.GONE

            //下载的文件显示对应的图标
            if (FileTransformHelper.isHttp(bean.url)) {
                val fileType = FileType.INSTANCE.typeByFileName(bean.url)
                if (fileType != FileType.UNKNOWN) {
                    holder.setImageResource(
                        R.id.iv_detail_icon,
                        //if (fileType.background <= 0) R.mipmap.ic_point_other else fileType.background
                        R.mipmap.ic_point_other
                    )
//                    ImageLoader.get().loadImage(
//                        holder.getView(R.id.iv_detail_icon), fileType.background,
//                        GlideRequestOptionsProvider.noAnimate(R.mipmap.ic_other)
//                    )
                }
            }

            holder.addOnClickListener(R.id.iv_detail_icon)
        }
    }


    override fun getItemCount(): Int {
        return controller.size()
    }

    override fun getItemLayoutId(viewType: Int): Int {
        return R.layout.item_point_detail_download
    }

}