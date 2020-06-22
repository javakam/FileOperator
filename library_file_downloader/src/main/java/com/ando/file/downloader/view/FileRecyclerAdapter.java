package com.ando.file.downloader.view;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * 通用的RecyclerView适配器
 *
 * @author javakam
 * @date 2017/9/10 18:30
 */
public abstract class FileRecyclerAdapter<T> extends FileBaseRecyclerAdapter<T, FileBaseViewHolder> {

    public FileRecyclerAdapter() {
        super();
    }

    public FileRecyclerAdapter(List<T> list) {
        super(list);
    }

    public FileRecyclerAdapter(T[] data) {
        super(data);
    }

    /**
     * 适配的布局
     */
    public abstract int getItemLayoutId(int viewType);

    @NonNull
    @Override
    public FileBaseViewHolder getViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileBaseViewHolder(inflateView(parent, getItemLayoutId(viewType)));
    }

}