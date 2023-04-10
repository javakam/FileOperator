package com.ando.file.sample.ui.upload

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * @author javakam
 * @date 2021/3/2  10:06
 */
internal class Upload {

    /**
     * http://jessehu.cn/2019/01/09/okhttp3/okhttp04/
     */
    fun uploadFile(url: String, inputStream: InputStream) {
        val client = OkHttpClient()
        val mediaType: MediaType? = "application/octet-stream".toMediaTypeOrNull()


        val file = File("File path")
        //inputStream.readBytes().toRequestBody(mediaType)

        val requestBody: RequestBody = file.asProgressRequestBody2(mediaType) {

        }
        val multipartBody: MultipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("param1", "value1")
            .addFormDataPart("param2", "value2")
            .addFormDataPart("param3", "value3")
            .addFormDataPart("pic", "1.png", requestBody)
            .build()

        val request: Request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                //val result = Objects.requireNonNull(response.body)!!.string()
            }
        })
    }
}