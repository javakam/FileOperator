package com.ando.file.sample.ui.selector.fragment

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R

/**
 * Fragment Container
 *
 * @author javakam
 * @date 2021-1-25 09:51:54
 */
class FileSelectFragmentUsageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Fragment Usage"

        //ContentView
        val container: FrameLayout = FrameLayout(this)
        container.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        container.setBackgroundColor(resources.getColor(R.color.color_container_bg))
        container.id = R.id.fl_container
        setContentView(container)

        //Add Fragment
        val fragment: FileSelectFragment = FileSelectFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(container.id, fragment, "tag")
            .commitAllowingStateLoss()

    }

    /*
    Note:

    v1.3.5 以下版本需要把 onActivityResult 数据传给 Fragment

    Versions below v1.3.5 need to pass onActivityResult data to Fragment

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //找到相应的`Fragment`并把数据通过主动调用`Fragment.onActivityResult`传过去
        //Find the corresponding `Fragment` and pass the data by actively calling `Fragment.onActivityResult`
        val fragment: FileSelectFragment? = supportFragmentManager.findFragmentByTag("tag") as? FileSelectFragment?
        fragment?.onActivityResult(requestCode, resultCode, data)
    }
    */

}