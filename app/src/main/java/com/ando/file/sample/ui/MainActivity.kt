package com.ando.file.sample.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R
import com.ando.file.sample.harmony.FileHarmonyActivity
import com.ando.file.sample.ui.core.FileCoreActivity
import com.ando.file.sample.ui.core.FileUtilsActivity
import com.ando.file.sample.ui.selector.FileSelectCustomFileTypeActivity
import com.ando.file.sample.ui.selector.FileSelectMultiFilesActivity
import com.ando.file.sample.ui.selector.FileSelectMultiImageActivity
import com.ando.file.sample.ui.selector.FileSelectSingleImageActivity
import com.ando.file.sample.ui.selector.fragment.FileSelectFragmentUsageActivity
import com.ando.file.sample.ui.storage.MediaStoreActivity
import com.ando.file.sample.ui.storage.StorageAccessFrameworkActivity

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity() {

    /////////////////////////////// 核心库(Core library) ///////////////////////////////////////

    //FileMimeType -> 获取文件MimeType (Get file Mime Type)
    fun caseGetFileMimeType(view: View) = FileCoreActivity.openMimeType(this)

    fun caseGetFileSize(view: View) = FileCoreActivity.openFileSize(this)

    fun caseGetFileUriOrPath(view: View) = FileCoreActivity.openFileUriAndPath(this)

    fun caseOpenFileByUri(view: View) {}

    fun caseFileUtils(view: View) {
        startActivity(Intent(this, FileUtilsActivity::class.java))
    }

    /////////////////////////////// 文件选择器(File selector) ///////////////////////////////////////

    //选择单张图片 (Select a single picture)
    fun caseSelectSingleImageWithCompress(view: View) {
        startActivity(Intent(this, FileSelectSingleImageActivity::class.java))
    }

    //选择多张图片 (Select multiple pictures)
    fun caseSelectMultiImageWithCompress(view: View) {
        startActivity(Intent(this, FileSelectMultiImageActivity::class.java))
    }

    //选择多个文件 (Select multiple files)
    fun caseSelectMultiFilesWithCompress(view: View) {
        startActivity(Intent(this, FileSelectMultiFilesActivity::class.java))
    }

    //自定义文件类型 (Custom file type)
    fun caseSelectCustomFileType(view: View) {
        startActivity(Intent(this, FileSelectCustomFileTypeActivity::class.java))
    }

    fun caseFragmentSimpleUsage(view: View) {
        startActivity(Intent(this, FileSelectFragmentUsageActivity::class.java))
    }

    fun caseClearCache(view: View) {
        startActivity(Intent(this, FileInfoActivity::class.java))
    }

    /////////////////////////////// 区别(The difference) ///////////////////////////////////////

    //AppSpecific(沙盒)
    //@see FileUtilsActivity

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

    //HarmonyOS
    fun caseHarmony(view: View) {
        startActivity(Intent(this, FileHarmonyActivity::class.java))
    }

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

}