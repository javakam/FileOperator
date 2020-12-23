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
package com.ando.file.sample.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ando.file.sample.R
import com.ando.file.sample.ui.core.FileCoreActivity
import com.ando.file.sample.ui.selector.FileSelectMultiFilesActivity
import com.ando.file.sample.ui.selector.FileSelectMultiImageActivity
import com.ando.file.sample.ui.selector.FileSelectSingleImageActivity
import com.ando.file.sample.ui.storage.AppSpecificActivity
import com.ando.file.sample.ui.storage.MediaStoreActivity
import com.ando.file.sample.ui.storage.StorageAccessFrameworkActivity

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity() {

    /////////////////////////////// 核心库(CORE) ///////////////////////////////////////

    //获取文件MimeType(FileMimeType)
    fun caseGetFileMimeType(view: View) = FileCoreActivity.openMimeType(this)

    fun caseGetFileSize(view: View) = FileCoreActivity.openFileSize(this)

    fun caseGetFileUriOrPath(view: View) = FileCoreActivity.openFileUriAndPath(this)

    fun caseOpenFileByUri(view: View) {}
    fun caseFileUtils(view: View) {}

    /////////////////////////////// 文件选择器(SELECTOR) ///////////////////////////////////////

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

    /////////////////////////////// 区别 ///////////////////////////////////////

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