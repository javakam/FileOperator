package com.ando.file.sample.utils

import ando.file.core.FileGlobal
import ando.file.core.FileLogger
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile

/**
 * Storage Access Framework (SAF)
 *
 * - https://developer.android.google.cn/training/data-storage/shared/documents-files
 *
 * - 需要 👉 implementation 'androidx.documentfile:documentfile:1.0.1'
 *
 * @author javakam
 * @date 2021-09-09  15:18
 */
object DocumentFileUtils {

    /**
     * 获取目录的访问权限, 并访问文件列表
     */
    fun getDocumentTree(activity: Activity, uri: Uri?, requestCode: Int): DocumentFile? {
        var root: DocumentFile? = null
        if (uri != null) {
            try {
                val takeFlags: Int = activity.intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                // Check for the freshest data.
                activity.contentResolver.takePersistableUriPermission(uri, takeFlags)

                // todo  activity.contentResolver.persistedUriPermissions
                FileLogger.d("已经获得永久访问权限")
                root = DocumentFile.fromTreeUri(activity, uri)
                return root
            } catch (e: SecurityException) {
                FileLogger.d("uri 权限失效，调用目录获取")
                activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), requestCode)
            }
        } else {
            FileLogger.d("没有永久访问权限，调用目录获取")
            activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), requestCode)
        }
        return root
    }

    fun getDocumentTree(activity: Activity, requestCode: Int): DocumentFile? {
        val sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
        val uriString = sp.getString("uri", "")
        val treeUri = Uri.parse(uriString)
        return getDocumentTree(activity, treeUri, requestCode)
    }

    /**
     * 永久保留权限
     */
    fun saveDocTreePersistablePermission(activity: Activity, uri: Uri) {
        val sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE)
        sp.edit {
            this.putString("uri", uri.toString())
            this.apply()
        }
        val takeFlags: Int = activity.intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    //Dump
    //------------------------------------------------------------------------------------------------

    /**
     * 获取文档元数据
     */
    fun dumpDocumentFileTree(root: DocumentFile?) {
        root?.listFiles()?.forEach loop@{ it ->
            //FileLogger.d( "目录下文件名称：${it.name}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                FileGlobal.dumpMetaData(it.uri)
            }
        }
    }

}