package com.ando.file.downloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ando.file.downloader.view.FileBaseRecyclerAdapter;
import com.ando.file.downloader.view.FileBaseViewHolder;
import com.liulishuo.okdownload.DownloadContext;
import com.liulishuo.okdownload.DownloadContextListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;


/**
 * <pre>
 *     Activity 中 :
 *
 *    @Override
 *     public void onDestroy() {
 *         super.onDestroy();
 *         OkDownload.with().downloadDispatcher().cancelAll();
 *     }
 * </pre>
 */
public class DownloadView extends FrameLayout {

    private QueueController4Speed controller;
    private RecyclerView mRvTasks;
    private QueueTaskAdapter4Speed mAdapter;

    private List<DownLoadTaskBean> downLoadTaskBeans;

    private OnDownloadItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnDownloadItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public DownloadView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public DownloadView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public DownloadView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DownloadView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        controller = new QueueController4Speed();

        final View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_download_view, this, false);

        mRvTasks = view.findViewById(R.id.rv_tasks);
        mRvTasks.setItemAnimator(null);
        mRvTasks.setHasFixedSize(true);
        mRvTasks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        mAdapter = new QueueTaskAdapter4Speed(controller);
        mRvTasks.setAdapter(mAdapter);


        controller.setCallBack(new QueueController4Speed.CallBack() {
            @Override
            public void lookOver(@NotNull FileBaseViewHolder holder, @NotNull DownloadTask task) {

                if (isDownloadActionLookOver(holder)) {
                    final DownLoadTaskBean downLoadTaskBean = downLoadTaskBeans.get(holder.getAdapterPosition());
                    if (downLoadTaskBean != null) {
                        onItemClickListener.onClick(downLoadTaskBean);
                    }
                }

            }
        });

        mAdapter.setOnItemClickListener(new FileBaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FileBaseViewHolder vh, int position) {
                if (onItemClickListener == null || downLoadTaskBeans == null || downLoadTaskBeans.isEmpty()) {
                    return;
                }

                final RecyclerView.ViewHolder viewHolder = mRvTasks.findViewHolderForAdapterPosition(position);
                if (!(viewHolder instanceof FileBaseViewHolder)) {
                    return;
                }

                final FileBaseViewHolder holder = (FileBaseViewHolder) viewHolder;
                if (isDownloadActionLookOver(holder)) {

                    final int id = view.getId();
                    if (id == R.id.iv_detail_icon) {
                        final DownLoadTaskBean downLoadTaskBean = downLoadTaskBeans.get(position);
                        if (downLoadTaskBean != null) {
                            onItemClickListener.onClick(downLoadTaskBean);
                        }
                    }
                }
            }
        });

        addView(view);

    }

    private boolean isDownloadActionLookOver(FileBaseViewHolder holder) {
        if (holder == null) {
            return false;
        }
        final TextView btDownAction = holder.getView(R.id.bt_down_action);
        if (TextUtils.equals(btDownAction.getText().toString().trim(), QueueController4Speed.STATUS_LOOK_OVER)) {
            return true;
        }
        return false;
    }

    public QueueController4Speed getController() {
        return controller;
    }

    public void notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setData(List<DownLoadTaskBean> downLoadTaskBeans, File parentFile) {
        this.downLoadTaskBeans = downLoadTaskBeans;

        //快速初始化 use -> createDownloadContextListener
        controller.initTasks(this.downLoadTaskBeans, parentFile, downloadContextListener);

        if (mAdapter != null) {
            mAdapter.refresh(controller.getTaskList());
        }

    }

    final DownloadContextListener downloadContextListener = new DownloadContextListener() {
        @Override
        public void taskEnd(@NonNull DownloadContext context, @NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, int remainCount) {
        }

        @Override
        public void queueEnd(@NonNull DownloadContext context) {
            // to cancel
            controller.stop();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

}