package com.ando.file.sample.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.ando.file.sample.R
import com.ando.file.sample.ui.selector.FileSelectMultiFilesActivity
import com.ando.file.sample.ui.selector.FileSelectSingleImageActivity
import com.ando.file.sample.ui.selector.FileSelectMultiImageActivity
import com.ando.file.sample.ui.storage.AppSpecificActivity
import com.ando.file.sample.ui.storage.MediaStoreActivity
import com.ando.file.sample.ui.storage.StorageAccessFrameworkActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    //AppSpecific
    fun caseAppSpecific(view: View) {
        val intent = Intent(this, AppSpecificActivity::class.java)
        startActivity(intent)
    }

    //MediaStore
    fun caseMediaStore(view: View) {
        val intent = Intent(this, MediaStoreActivity::class.java)
        startActivity(intent)
    }

    //SAF
    fun caseSAF(view: View) {
        val intent = Intent(this, StorageAccessFrameworkActivity::class.java)
        startActivity(intent)
    }

    //选择单张图片
    fun caseSelectSingleImageWithCompress(view: View) {
        startActivity(Intent(this, FileSelectSingleImageActivity::class.java))
    }

    //选择多张图片
    fun caseSelectMultiImageWithCompress(view: View) {
        startActivity(Intent(this, FileSelectMultiImageActivity::class.java))
    }

    //选择多个文件
    fun caseSelectMultiFilesWithCompress(view: View) {
        startActivity(Intent(this, FileSelectMultiFilesActivity::class.java))
    }

    fun caseFragmentSimpleUsage(view: View) {
    }

    fun caseClearCache(view: View) {
        startActivity(Intent(this, FileClearCacheActivity::class.java))
    }

}