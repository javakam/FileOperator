package com.ando.file.sample.ui.upload

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient

/**
 * Title: # FileUploadActivity
 * <p>
 * Description:
 * </p>
 * @author javakam
 * @date 2021/3/2  13:52
 */
class FileUploadActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = OkHttpClient()
        val uploader=FileUploader(client,this)
        //uploader.uploadFromUri()

    }

}