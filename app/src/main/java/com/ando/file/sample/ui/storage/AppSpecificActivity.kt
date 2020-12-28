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
package com.ando.file.sample.ui.storage

import ando.file.androidq.FileOperatorQ.createFileInAppSpecific
import ando.file.androidq.FileOperatorQ.readTextFromUri
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import ando.file.core.FileLogger
import ando.file.core.FileUri.getUriByFile
import android.widget.Button
import android.widget.TextView
import com.ando.file.sample.R
import java.io.File

/**
 * Title: AppSpecificActivity
 * <p>
 * Description: 沙盒 -> APP卸载,数据删除
 * </p>
 * <pre>
 * 1.共享文件  https://developer.android.com/training/secure-file-sharing/share-file
 * 2.设置文件共享 https://developer.android.com/training/secure-file-sharing/setup-sharing
 * 3.FileProvider https://developer.android.google.cn/reference/androidx/core/content/FileProvider
 * </pre>
 * @author javakam
 * @date 2020/6/2  15:12
 */
class AppSpecificActivity : AppCompatActivity() {

    private lateinit var tvAppSpecificTip: TextView
    private lateinit var tvDocumentsFilesInfo: TextView
    private lateinit var tvAppSpecific: TextView
    private lateinit var getDocuments: Button
    private lateinit var createDocumentsDirs: Button
    private lateinit var createFileInDocuments: Button
    private lateinit var shareFileInDocuments: Button
    private lateinit var deleteFileInDocuments: Button

    private var mJustCreatedFile: File? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_specific)
        tvAppSpecific = findViewById(R.id.tvAppSpecific)
        tvAppSpecificTip = findViewById(R.id.tvAppSpecificTip)
        tvDocumentsFilesInfo = findViewById(R.id.tvDocumentsFilesInfo)
        getDocuments = findViewById(R.id.getDocuments)
        createDocumentsDirs = findViewById(R.id.createDocumentsDirs)
        createFileInDocuments = findViewById(R.id.createFileInDocuments)
        shareFileInDocuments = findViewById(R.id.shareFileInDocuments)
        deleteFileInDocuments = findViewById(R.id.deleteFileInDocuments)
        title = "App Specific"

        tvAppSpecificTip.text = "⭐沙盒目录(AppSpecific)操作直接沿用旧的 File API操作"

        //批量创建目录
        createDocumentsDirs.setOnClickListener {
            getExternalFilesDirs(Environment.DIRECTORY_MUSIC)
            getExternalFilesDirs(Environment.DIRECTORY_PODCASTS)
            getExternalFilesDirs(Environment.DIRECTORY_RINGTONES)
            getExternalFilesDirs(Environment.DIRECTORY_ALARMS)
            getExternalFilesDirs(Environment.DIRECTORY_NOTIFICATIONS)
            getExternalFilesDirs(Environment.DIRECTORY_PICTURES)
            getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
            getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)

            Toast.makeText(this, "创建目录成功", Toast.LENGTH_SHORT).show()
        }

        //文件列表  Environment.DIRECTORY_DOCUMENTS
        getDocuments.setOnClickListener {
            getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)
                .let { dir ->
                    val sb = StringBuilder()
                    val line = "--------------------------------------------------- \n"
                    dir.forEach { file ->
                        sb.append(line)
                        sb.append("${Environment.DIRECTORY_DOCUMENTS}：${file.name} \n ${file.path} \n ${file.toUri()} \n")
                        if (file.isDirectory) {
                            file.listFiles()?.forEach { fl ->
                                sb.append("\n ${fl.name} \n ${fl.path} \n ${fl.toUri()} \n ${getUriByFile(fl)} \n")
                            }
                        }
                        sb.append(line)
                    }

                    tvDocumentsFilesInfo.text = sb.toString()
                }
        }

        //新建文件  Environment.DIRECTORY_DOCUMENTS
        createFileInDocuments.setOnClickListener {
            createFileInAppSpecific(
                Environment.DIRECTORY_DOCUMENTS,
                "文件.txt",
                "hello world"
            ) { file ->
                if (file != null) {
                    // MyDocument /storage/emulated/0/Android/data/com.xxx.xxx/files/Documents/MyDocument
                    FileLogger.d(
                        "${Environment.DIRECTORY_DOCUMENTS}下的文件名和路径：" + file.name + " " + file.path + " \n "
                                + readTextFromUri(file.toUri())
                    )

                    mJustCreatedFile = file

                    runOnUiThread {
                        tvAppSpecific.text = " 👉${file.name}  \n 👉path=${file.path} \n 👉uri=${file.toUri()} " +
                                "\n 👉因为 Uri.fromFile(file)生成的 file:///... 是不能分享的,所以需要使用FileProvider将App Specific目录下的文件分享给其他APP读写" +
                                "\n 👉FileProvider解析出的可用于分享的路径 : \n ${getUriByFile(file)}"
                    }
                }
            }
        }

        //删除文件
        deleteFileInDocuments.setOnClickListener {
            val delete = mJustCreatedFile?.delete()
            Toast.makeText(
                this,
                "删除${if (delete == true) "成功" else "失败"}!", Toast.LENGTH_SHORT
            ).show()
        }

        //分享文件
        //todo test 2020年6月2日 17:04:58l getFilePathByUri()
        shareFileInDocuments.setOnClickListener {
            //val filePath =  "${getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)}${File.separator}${mJustCreatedFile.name}"
            val fileUri: Uri? = getUriByFile(mJustCreatedFile)
            if (fileUri != null) {
                FileLogger.i(fileUri.toString() + "  " + contentResolver.getType(fileUri))

                val intent = Intent(Intent.ACTION_SEND)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Put the Uri and MIME type in the result Intent
                intent.setDataAndType(fileUri, contentResolver.getType(fileUri))
                // Set the result
                // setResult(RESULT_OK, intent)
                startActivity(Intent.createChooser(intent, "分享文件"))
            }
        }

    }

}