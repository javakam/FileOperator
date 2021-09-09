package com.ando.file.sample.ui.storage

import android.annotation.SuppressLint
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
import ando.file.core.FileGlobal.MODE_WRITE_ONLY_ERASING
import ando.file.core.FileGlobal.dumpMetaData
import ando.file.core.FileGlobal.openFileDescriptor
import ando.file.core.MediaStoreUtils.getBitmapFromUri
import ando.file.core.MediaStoreUtils.readTextFromUri
import ando.file.core.MediaStoreUtils.selectFile
import ando.file.core.MediaStoreUtils.selectImage
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.ando.file.sample.R
import com.ando.file.sample.utils.DocumentFileUtils
import com.ando.file.sample.utils.DocumentFileUtils.dumpDocumentFileTree
import com.ando.file.sample.utils.DocumentFileUtils.saveDocTreePersistablePermission
import java.io.*

class StorageAccessFrameworkActivity : AppCompatActivity() {

    private val REQUEST_CREATE_FILE: Int = 1
    private val REQUEST_EDIT_FILE: Int = 2
    private val REQUEST_CHOOSE_DOCUMENT_DIR: Int = 3
    private val REQUEST_SELECT_SINGLE_IMAGE: Int = 4

    private lateinit var safSelectSingleFile: Button
    private lateinit var createFileBtn: Button
    private lateinit var deleteFileBtn: Button
    private lateinit var renameFileBtn: Button
    private lateinit var editDocumentBtn: Button
    private lateinit var getDocumentTreeBtn: Button
    private lateinit var showIv: ImageView
    private lateinit var createFileUriTv: TextView
    private lateinit var tvDocumentTreeFiles: TextView

    private var mCreateUri: Uri? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_access_framework)
        safSelectSingleFile = findViewById(R.id.safSelectSingleFile)
        createFileBtn = findViewById(R.id.createFileBtn)
        deleteFileBtn = findViewById(R.id.deleteFileBtn)
        renameFileBtn = findViewById(R.id.renameFileBtn)
        editDocumentBtn = findViewById(R.id.editDocumentBtn)
        createFileUriTv = findViewById(R.id.createFileUriTv)
        getDocumentTreeBtn = findViewById(R.id.getDocumentTreeBtn)
        tvDocumentTreeFiles = findViewById(R.id.tvDocumentTreeFiles)
        showIv = findViewById(R.id.showIv)

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

        //1.选择一个图片文件 (Choose a picture file)
        safSelectSingleFile.setOnClickListener {
            selectImage(this@StorageAccessFrameworkActivity, REQUEST_SELECT_SINGLE_IMAGE)
        }
        //2.新建一个 txt 文件 (Create a new txt file)
        //选择一个文件，这里打开一个图片作为演示 (Choose a file, open a picture here as a demo)
        createFileBtn.setOnClickListener {
            MediaStoreUtils.createFile(this@StorageAccessFrameworkActivity, null,
                "新建文本文档.txt", "text/plain", REQUEST_CREATE_FILE)
        }
        //3.删除一个文件 (Delete a file)
        //如果您获得了文档的 URI，并且文档的 Document.COLUMN_FLAGS 包含 FLAG_SUPPORTS_DELETE，则便可删除该文档
        deleteFileBtn.setOnClickListener {
            val string = createFileUriTv.text.toString()
            if (string.isNotEmpty()) {
                val uri = Uri.parse(string)
                val deleted = MediaStoreUtils.deleteFile(uri)
                if (deleted) {
                    @SuppressLint("SetTextI18n")
                    createFileUriTv.text = "已删除文件 $uri"
                }
            }
        }
        //Rename
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            renameFileBtn.setOnClickListener {
                val uri = mCreateUri

                if (uri != null) {
                    MediaStoreUtils.renameFile(uri, "smlz.txt") { isSuccess: Boolean, msg: String ->
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

        //3.编辑一个文件 (Edit a file)
        editDocumentBtn.setOnClickListener {
            selectFile(this, "text/plain", requestCode = REQUEST_EDIT_FILE)
        }
        //4.获取文件树 (Get file tree)
        //使用 SAF 选择目录 -> 获取该目录的读取权限
        getDocumentTreeBtn.setOnClickListener {
            val root = DocumentFileUtils.getDocumentTree(this, requestCode = REQUEST_CHOOSE_DOCUMENT_DIR)
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

        //5.MediaStore获取文件 (MediaStore get files)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(this).forEach { volumeName ->
                FileLogger.d("volumeName：${MediaStore.Images.Media.getContentUri(volumeName)}")
                FileLogger.d("getExternalStorageState：${Environment.getExternalStorageState()}")
                FileLogger.d("EXTERNAL_CONTENT_URI：${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}")
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_SELECT_SINGLE_IMAGE) {
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
        } else if (requestCode == REQUEST_CREATE_FILE) {
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
                mCreateUri = uri
            }
        } else if (requestCode == REQUEST_EDIT_FILE) {
            //编辑文档
            createFileUriTv.visibility = View.VISIBLE

            alterDocument(data?.data)
        } else if (requestCode == REQUEST_CHOOSE_DOCUMENT_DIR) {
            //选择目录
            val treeUri = data?.data
            if (treeUri != null) {
                saveDocTreePersistablePermission(this, treeUri)
                //Log
                dumpDocumentFileTree(DocumentFile.fromTreeUri(this, treeUri))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun alterDocument(uri: Uri?) {
        try {
            openFileDescriptor(uri ?: return, MODE_WRITE_ONLY_ERASING)?.use {
                // use{} lets the document provider know you're done by automatically closing the stream
                FileOutputStream(it.fileDescriptor).use { fos ->
                    fos.write(
                        ("(*^▽^*) ${System.currentTimeMillis()}\n").toByteArray()
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

    /**
     * 通过 Uri 获取 Bitmap
     */
    @SuppressLint("StaticFieldLeak")
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

}