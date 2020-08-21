package com.ando.file.sample.ui.storage

import ando.file.androidq.*
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import ando.file.core.*
import ando.file.androidq.BaseMediaColumnsData
import com.ando.file.sample.R
import kotlinx.android.synthetic.main.activity_storage_access_framework.*
import java.io.*

class StorageAccessFrameworkActivity : AppCompatActivity() {

    private var mCreateFile: BaseMediaColumnsData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_access_framework)

        title = "SAF - Storage Access Framework"

        FileLogger.i("externalMediaDirs :${externalMediaDirs[0]}")
        externalCacheDirs.forEach {
            FileLogger.w("externalCacheDirs :${it} ")
        }
        FileLogger.i("externalCacheDir :$externalCacheDir")
        FileLogger.i("obbDir :${obbDir} ")
        FileLogger.i(" ------------------------------------------------ ")

        getExternalFilesDirs(null).forEach {
            FileLogger.w("getExternalFilesDirs :${it} ")
        }
        FileLogger.i("getExternalFilesDir :${getExternalFilesDir(null)}")

        //1.选择一个图片文件
        selectSingleFile()
        //2.新建一个 txt 文件
        createFile("新建文本文档.txt", "text/plain")
        //3.删除一个文件
        deleteFile()
        //重命名
        renameFile()

        //3.编辑一个文件
        editDocument()
        //4.获取文件树
        getDocumentTree()

        //5.MediaStore 获取文件
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(this).forEach { volumeName ->
                FileLogger.d("volumeName：${MediaStore.Images.Media.getContentUri(volumeName)}")
                FileLogger.d("getExternalStorageState：${Environment.getExternalStorageState()}")
                FileLogger.d("EXTERNAL_CONTENT_URI：${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}")
            }
        }
    }

    /**
     * 选择一个文件，这里打开一个图片作为演示
     */
    private fun selectSingleFile() {
        safSelectSingleFile.setOnClickListener {
            selectSingleImage(this@StorageAccessFrameworkActivity)
        }
    }

    private fun createFile(fileName: String, mimeType: String) {
        createFileBtn.setOnClickListener {
            createFileSAF(this@StorageAccessFrameworkActivity, null, fileName, mimeType)
        }
    }

    /**
     * 如果您获得了文档的 URI，并且文档的 Document.COLUMN_FLAGS 包含 FLAG_SUPPORTS_DELETE，则便可删除该文档
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun deleteFile() {
        deleteFileBtn.setOnClickListener {
            val string = createFileUriTv.text.toString()
            if (string.isNotEmpty()) {
                val uri = Uri.parse(string)
                val deleted = deleteFileSAF(uri)
                if (deleted) {
                    createFileUriTv.text = "已删除文件 $uri"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun renameFile() {
        renameFileBtn.setOnClickListener {
            val uri = mCreateFile?.uri

            if (uri != null) {
                renameFileSAF(uri, "smlz.txt") { isSuccess: Boolean, msg: String ->
                    if (isSuccess) {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            dumpMetaData(uri) { displayName: String?, size: String? ->
                                runOnUiThread {
                                    createFileUriTv.text =
                                        "👉$msg \n👉 Uri : $uri \n 文件名称 ：$displayName \n Size：$size B"
                                }
                            }
                        }
                    } else
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Uri 为空!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun editDocument() {
        editDocumentBtn.setOnClickListener {
            selectSingleFile(this, "text/plain", REQUEST_CODE_SAF_EDIT_FILE)
        }
    }

    /**
     * 使用saf选择目录 -> 获取该目录的读取权限
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private fun getDocumentTree() {
        getDocumentTreeBtn.setOnClickListener {
            val root =
                getDocumentTreeSAF(this, REQUEST_CODE_SAF_CHOOSE_DOCUMENT_DIR)
            dumpDocumentFileTree(root)

            val sb = StringBuilder("${root?.listFiles()?.size} \n")
            root?.listFiles()?.forEach loop@{
                //FileLogger.d( "目录下文件名称：${it.name}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sb.append("${it.uri}  ${it.name}  ${it.length()}  \n\n ")
                }
            }

            tvDocumentTreeFiles.text = sb.toString()

        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_CODE_SAF_SELECT_SINGLE_IMAGE) {
            //获取文档
            val uri = data?.data
            if (uri != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dumpMetaData(uri)
                }
                //异步加载图片
                GetBitmapFromUriAsyncTask().execute(uri)

                FileLogger.d("图片的line :$uri  ${readTextFromUri(uri)}")
            }
        } else if (requestCode == REQUEST_CODE_SAF_CREATE_FILE) {
            //创建文档
            val uri = data?.data
            if (uri != null) {
                createFileUriTv.visibility = View.VISIBLE

                Toast.makeText(this, "创建文件成功", Toast.LENGTH_SHORT).show()
                FileLogger.d("创建文件成功")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dumpMetaData(uri) { displayName: String?, size: String? ->
                        runOnUiThread {
                            createFileUriTv.text =
                                "👉 Uri : $uri \n 文件名称 ：$displayName \n Size：$size B"
                        }
                    }
                } else {
                    createFileUriTv.text = uri.toString()
                }
                mCreateFile = BaseMediaColumnsData()
                mCreateFile?.uri = uri
            }
        } else if (requestCode == REQUEST_CODE_SAF_EDIT_FILE) {
            //编辑文档
            createFileUriTv.visibility = View.VISIBLE

            alterDocument(data?.data)
        } else if (requestCode == REQUEST_CODE_SAF_CHOOSE_DOCUMENT_DIR) {
            //选择目录
            val treeUri = data?.data
            if (treeUri != null) {
                saveDocTreePersistablePermissionSAF(this, treeUri)
                //Log
                dumpDocumentFileTree(DocumentFile.fromTreeUri(this, treeUri))
            }
        }
    }

    /**
     * 通过Uri获取Bitmap
     */
    @Suppress("DEPRECATION")
    internal inner class GetBitmapFromUriAsyncTask : AsyncTask<Uri, Void, Bitmap>() {
        override fun doInBackground(vararg params: Uri): Bitmap? {
            val uri = params[0]
            return getBitmapFromUri(uri)
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            super.onPostExecute(bitmap)
            showIv.visibility = View.VISIBLE
            showIv.setImageBitmap(bitmap)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun alterDocument(uri: Uri?) {
        try {
            openFileDescriptor(uri ?: return, MODE_WRITE_ONLY_ERASING)?.use {
                // use{} lets the document provider know you're done by automatically closing the stream
                FileOutputStream(it.fileDescriptor).use { fos ->
                    fos.write(
                        ("Overwritten by MyCloud at ${System.currentTimeMillis()}\n").toByteArray()
                    )
                    fos.flush()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        readTextFromUri(uri) { content ->
                            dumpMetaData(uri) { displayName: String?, size: String? ->
                                val editResult =
                                    "👉编辑成功 \n👉 Uri : $uri \n 文件名称 ：$displayName \n Size：$size B \n 内容: $content"
                                FileLogger.d(editResult)
                                runOnUiThread {
                                    createFileUriTv.text = editResult
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FileLogger.e("编辑失败: $e")
            createFileUriTv.text = "编辑失败: $e"
        }
    }


}
