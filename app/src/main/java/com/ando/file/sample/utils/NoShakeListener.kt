package com.ando.file.sample.utils

import android.view.View
import kotlin.math.abs

fun View.noShake(interval: Long = 500L, block: (v: View) -> Unit) {
    this.apply {
        setOnClickListener(object : NoShakeClickListener(interval) {
            override fun onSingleClick(v: View) {
                block.invoke(v)
            }
        })
    }
}

abstract class NoShakeClickListener @JvmOverloads constructor(interval: Long = 500L) : View.OnClickListener {

    private var mTimeInterval = 500L
    private var mLastClickTime: Long = 0   //最近一次点击的时间
    private var mLastClickViewId = 0       //最近一次点击的控件ID

    init {
        mTimeInterval = interval
    }

    override fun onClick(v: View) {
        if (isFastDoubleClick(v, mTimeInterval)) onFastClick(v) else onSingleClick(v)
    }

    /**
     * 是否是快速点击
     *
     * @param v        点击的控件
     * @param interval 时间间期（毫秒）
     * @return true:是，false:不是
     */
    private fun isFastDoubleClick(v: View, interval: Long): Boolean {
        val viewId = v.id
        val nowTime = System.currentTimeMillis()
        val timeInterval = abs(nowTime - mLastClickTime)
        return if (timeInterval < interval && viewId == mLastClickViewId) {
            // 快速点击事件
            true
        } else {
            // 单次点击事件
            mLastClickTime = nowTime
            mLastClickViewId = viewId
            false
        }
    }

    protected open fun onFastClick(v: View) {}
    protected abstract fun onSingleClick(v: View)
}
