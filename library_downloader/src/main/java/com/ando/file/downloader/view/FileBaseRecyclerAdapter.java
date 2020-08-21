package ando.file.downloader.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 基础的 RecyclerView 适配器
 *
 * @author javakam
 * @date 2019-08-11 16:12
 */
public abstract class FileBaseRecyclerAdapter<T, VH extends FileBaseViewHolder> extends RecyclerView.Adapter<VH> {
    /**
     * 数据源
     */
    protected final List<T> mData = new ArrayList<>();
    /**
     * 点击监听
     */
    private OnItemClickListener mClickListener;
    /**
     * 点击监听 子View
     */
    private OnItemChildClickListener mChildClickListener;
    /**
     * 长按监听
     */
    private OnItemLongClickListener mLongClickListener;
    /**
     * 长按监听 子View
     */
    private OnItemChildLongClickListener mChildLongClickListener;

    /**
     * 当前点击的条目
     */
    private int mSelectPosition = -1;

    public interface OnItemLongClickListener {
        boolean onItemLongClick(FileBaseViewHolder viewHolder, int position);
    }

    public interface OnItemChildLongClickListener {
        boolean onItemLongClick(@NonNull View v, FileBaseViewHolder viewHolder, int position, int positionChild);
    }

    public interface OnItemClickListener {
        void onItemClick(FileBaseViewHolder viewHolder, int position);
    }

    public interface OnItemChildClickListener {
        void onItemClick(@NonNull View v, FileBaseViewHolder viewHolder, int position, int positionChild);
    }

    public FileBaseRecyclerAdapter() {
    }

    public FileBaseRecyclerAdapter(List<T> list) {
        if (list != null) {
            mData.addAll(list);
        }
    }

    public FileBaseRecyclerAdapter(T[] data) {
        if (data != null && data.length > 0) {
            mData.addAll(Arrays.asList(data));
        }
    }

    /**
     * 构建自定义的ViewHolder
     *
     * @param parent
     * @param viewType
     * @return
     */
    @NonNull
    protected abstract VH getViewHolder(@NonNull ViewGroup parent, int viewType);

    /**
     * 绑定数据
     *
     * @param holder
     * @param position 索引
     * @param item     列表项
     */
    protected abstract void bindData(@NonNull VH holder, int position, T item);

    /**
     * 加载布局获取控件
     *
     * @param parent   父布局
     * @param layoutId 布局ID
     * @return
     */
    protected View inflateView(ViewGroup parent, @LayoutRes int layoutId) {
        return LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH holder, int position) {
        bindData(holder, position, mData.get(position));

        if (mClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(holder, holder.getLayoutPosition());
                }
            });
        }
        if (mLongClickListener != null) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mLongClickListener.onItemLongClick(holder, holder.getLayoutPosition());
                    return true;
                }
            });
        }

        if (mChildClickListener != null) {
            ArraySet<Integer> clickViewIds = holder.getChildClickViewIds();
            for (int i = 0; i < clickViewIds.size(); i++) {
                final int cvPos = i;
                final Integer cvId = clickViewIds.valueAt(i);
                if (cvId == null || cvId < 0) {
                    continue;
                }
                View viewById = holder.itemView.findViewById(cvId);
                if (viewById != null) {
                    viewById.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mChildClickListener.onItemClick(v, holder, holder.getLayoutPosition(), cvPos);
                        }
                    });
                }
            }
        }

        if (mChildLongClickListener != null) {
            ArraySet<Integer> clickViewIds = holder.getChildClickViewIds();
            for (int i = 0; i < clickViewIds.size(); i++) {
                final int cvPos = i;
                final Integer cvId = clickViewIds.valueAt(i);
                if (cvId == null || cvId < 0) {
                    continue;
                }
                View viewById = holder.itemView.findViewById(cvId);
                if (viewById != null) {
                    viewById.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            mChildLongClickListener.onItemLongClick(v, holder, holder.getLayoutPosition(), cvPos);
                            return true;
                        }
                    });
                }
            }
        }
    }

    /**
     * 获取列表项
     *
     * @param position
     * @return
     */
    public T getItem(int position) {
        return checkPosition(position) ? mData.get(position) : null;
    }

    private boolean checkPosition(int position) {
        return position >= 0 && position <= mData.size() - 1;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * 给指定位置添加一项
     *
     * @param pos
     * @param item
     * @return
     */
    public FileBaseRecyclerAdapter add(int pos, T item) {
        mData.add(pos, item);
        notifyItemInserted(pos);
        return this;
    }

    /**
     * 在列表末端增加一项
     *
     * @param item
     * @return
     */
    public FileBaseRecyclerAdapter add(T item) {
        mData.add(item);
        notifyItemInserted(mData.size() - 1);
        return this;
    }

    /**
     * 删除列表中指定索引的数据
     *
     * @param pos
     * @return
     */
    public FileBaseRecyclerAdapter delete(int pos) {
        mData.remove(pos);
        notifyItemRemoved(pos);
        return this;
    }

    /**
     * 刷新列表中指定位置的数据
     *
     * @param pos
     * @param item
     * @return
     */
    public FileBaseRecyclerAdapter refresh(int pos, T item) {
        mData.set(pos, item);
        notifyItemChanged(pos);
        return this;
    }

    /**
     * 刷新列表数据
     *
     * @param collection
     * @return
     */
    public FileBaseRecyclerAdapter refresh(Collection<T> collection) {
        if (collection != null) {
            mData.clear();
            mData.addAll(collection);
            mSelectPosition = -1;
            notifyDataSetChanged();
        }
        return this;
    }

    /**
     * 刷新列表数据
     *
     * @param array
     * @return
     */
    public FileBaseRecyclerAdapter refresh(T[] array) {
        if (array != null && array.length > 0) {
            mData.clear();
            mData.addAll(Arrays.asList(array));
            mSelectPosition = -1;
            notifyDataSetChanged();
        }
        return this;
    }

    /**
     * 加载更多
     *
     * @param collection
     * @return
     */
    public FileBaseRecyclerAdapter loadMore(Collection<T> collection) {
        if (collection != null) {
            mData.addAll(collection);
            notifyDataSetChanged();
        }
        return this;
    }

    /**
     * 加载更多
     *
     * @param array
     * @return
     */
    public FileBaseRecyclerAdapter loadMore(T[] array) {
        if (array != null && array.length > 0) {
            mData.addAll(Arrays.asList(array));
            notifyDataSetChanged();
        }
        return this;
    }

    /**
     * 添加一个
     *
     * @param item
     * @return
     */
    public FileBaseRecyclerAdapter load(T item) {
        if (item != null) {
            mData.add(item);
            notifyDataSetChanged();
        }
        return this;
    }

    /**
     * 设置列表项点击监听
     *
     * @param listener
     * @return
     */
    public FileBaseRecyclerAdapter setOnItemClickListener(OnItemClickListener listener) {
        mClickListener = listener;
        return this;
    }

    public FileBaseRecyclerAdapter setOnItemChildClickListener(OnItemChildClickListener listener) {
        mChildClickListener = listener;
        return this;
    }

    /**
     * 设置列表项长按监听
     *
     * @param listener
     * @return
     */
    public FileBaseRecyclerAdapter setOnItemLongClickListener(OnItemLongClickListener listener) {
        mLongClickListener = listener;
        return this;
    }

    public FileBaseRecyclerAdapter setOnItemChildLongClickListener(OnItemChildLongClickListener listener) {
        mChildLongClickListener = listener;
        return this;
    }

    /**
     * @return 当前列表的选中项
     */
    public int getSelectPosition() {
        return mSelectPosition;
    }

    /**
     * 设置当前列表的选中项
     *
     * @param selectPosition
     * @return
     */
    public FileBaseRecyclerAdapter setSelectPosition(int selectPosition) {
        mSelectPosition = selectPosition;
        notifyDataSetChanged();
        return this;
    }

    /**
     * 获取当前列表选中项
     *
     * @return 当前列表选中项
     */
    public T getSelectItem() {
        return getItem(mSelectPosition);
    }

    /**
     * 清除数据
     */
    public void clear() {
        if (!isEmpty()) {
            mData.clear();
            mSelectPosition = -1;
            notifyDataSetChanged();
        }
    }

}
