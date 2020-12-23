/**
 * Copyright (C)  javakam, FileOperator Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ando.file.sample.ui.selector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ando.file.sample.R
import com.ando.file.sample.utils.ResultUtils
import com.ando.file.sample.utils.ResultUtils.ResultShowBean

class FileSelectResultAdapter : RecyclerView.Adapter<FileSelectResultAdapter.SelectResultHolder>() {

    private var mData: MutableList<ResultShowBean>? = null

    fun setData(data: MutableList<ResultShowBean>?) {
        if (this.mData?.isNotEmpty() == true) this.mData?.clear()
        this.mData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectResultHolder {
        return SelectResultHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_select_image_result, parent, false))
    }

    override fun onBindViewHolder(holder: SelectResultHolder, position: Int) {
        mData?.get(position)?.let { b: ResultShowBean ->
            holder.tvResult.text = b.originResult
            holder.tvCompressedResult.text = b.compressedResult
            val visibility = if (b.compressedResult.isBlank()) View.GONE else View.VISIBLE
            holder.lineResult.visibility = visibility
            holder.tvCompressedResult.visibility = visibility

            //Event
            ResultUtils.setItemEvent(holder.tvResult, b.originUri, "确定打开该文件?")
            ResultUtils.setItemEvent(holder.tvCompressedResult, b.compressedUri, "确定打开压缩后的图片?")
        }
    }

    override fun getItemCount(): Int = mData?.size ?: 0

    inner class SelectResultHolder(v: View) : RecyclerView.ViewHolder(v) {
        var lineResult: View = v.findViewById(R.id.line_result)
        var tvResult: TextView = v.findViewById(R.id.tv_result)
        var tvCompressedResult: TextView = v.findViewById(R.id.tv_result_compressed)
    }
}