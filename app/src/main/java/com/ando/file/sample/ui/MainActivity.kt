package com.ando.file.sample.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R
import com.ando.file.sample.ui.selector.FileSelectMultiFilesActivity
import com.ando.file.sample.ui.selector.FileSelectMultiImageActivity
import com.ando.file.sample.ui.selector.FileSelectSingleImageActivity
import com.ando.file.sample.ui.storage.AppSpecificActivity
import com.ando.file.sample.ui.storage.MediaStoreActivity
import com.ando.file.sample.ui.storage.StorageAccessFrameworkActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!this.isTaskRoot) {
            intent?.apply {
                if (hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == action) {
                    finish()
                    return
                }
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    //AppSpecific
    @Suppress("UNUSED_PARAMETER")
    fun caseAppSpecific(view: View) {
        val intent = Intent(this, AppSpecificActivity::class.java)
        startActivity(intent)
    }

    //MediaStore
    @Suppress("UNUSED_PARAMETER")
    fun caseMediaStore(view: View) {
        val intent = Intent(this, MediaStoreActivity::class.java)
        startActivity(intent)
    }

    //SAF
    @Suppress("UNUSED_PARAMETER")
    fun caseSAF(view: View) {
        val intent = Intent(this, StorageAccessFrameworkActivity::class.java)
        startActivity(intent)
    }

    //选择单张图片
    @Suppress("UNUSED_PARAMETER")
    fun caseSelectSingleImageWithCompress(view: View) {
        startActivity(Intent(this, FileSelectSingleImageActivity::class.java))
    }

    //选择多张图片
    @Suppress("UNUSED_PARAMETER")
    fun caseSelectMultiImageWithCompress(view: View) {
        startActivity(Intent(this, FileSelectMultiImageActivity::class.java))
    }

    //选择多个文件
    @Suppress("UNUSED_PARAMETER")
    fun caseSelectMultiFilesWithCompress(view: View) {
        startActivity(Intent(this, FileSelectMultiFilesActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun caseFragmentSimpleUsage(view: View) {
    }

    @Suppress("UNUSED_PARAMETER")
    fun caseClearCache(view: View) {
        startActivity(Intent(this, FileClearCacheActivity::class.java))
    }

}