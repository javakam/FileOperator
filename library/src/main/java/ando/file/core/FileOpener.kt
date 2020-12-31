package ando.file.core

import ando.file.core.FileMimeType.getMimeType
import android.app.Activity
import android.content.Intent
import android.net.Uri

/**
 * Title: FileOpener
 *
 * Description: 打开该 Uri 对应文件类型的所有软件, 通常情况下是个部弹窗
 *
 * Open all the software corresponding to the Uri file type, usually a pop-up window
 *
 * @author javakam
 * @date 2020/8/24 11:20
 */
object FileOpener {

    /**
     * ### 直接打开 Url 对应的系统应用 (Open the system application corresponding to the URL directly)
     *
     * eg: 如果url是视频地址,则直接用系统的播放器打开 (If the url is the video address, open it directly with the system player)
     */
    fun openUrl(activity: Activity, url: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), getMimeType(url))
            activity.startActivity(intent)
        } catch (e: Exception) {
            FileLogger.e("openUrl error : " + e.message)
        }
    }

    fun createOpenFileIntent(uri: Uri, mimeType: String?): Intent = Intent(Intent.ACTION_VIEW).run {
        addCategory(Intent.CATEGORY_DEFAULT)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        setDataAndType(uri, if (mimeType.isNullOrBlank()) getMimeType(uri) else mimeType)
    }

    /**
     * ### 根据 文件路径 和 类型(后缀判断) 显示支持该格式的程序
     *
     * According to the file path and type (judging by suffix), show the programs that support the format
     *
     * (√) /storage/emulated/0/Pictures/sl2/BitmapImage.png
     *
     * (X) /data/user/0/xxx.xxx.app/cache/documents/microMsg.15798.jpg
     *
     * @param context  Activity/Fragment/Context
     * @param mimeType 指定打开文件的 MimeType 类型 (Specify the MimeType of the opened file)
     *
     */
    fun openFileBySystemChooser(context: Any, uri: Uri?, mimeType: String? = null, title: String? = "选择程序") =
        uri?.let { u ->
            Intent.createChooser(createOpenFileIntent(u, mimeType), title)?.let {
                startActivity(context, it)
            }
        }

    /**
     * ### 选择文件【调用系统的文件管理】 (Select file [call system file management])
     *
     * 注:
     *
     * 1. Intent.setType 不能为空(Can not be empty) !
     *
     * 2. mimeTypes 会覆盖(Will overwrite) mimeType
     *
     * 3. ACTION_GET_CONTENT, ACTION_OPEN_DOCUMENT 效果相同, Android Q 上使用 `ACTION_GET_CONTENT` 会出现:
     * ```
     *      java.lang.SecurityException: UID 10483 does not have permission to content://com.android.providers.media.documents/document/image%3A16012 [user 0];
     *      you could obtain access using ACTION_OPEN_DOCUMENT or related APIs
     * ```
     *
     * 4. 开启多选(Open multiple selection) resultCode = -1
     */
    fun createChooseIntent(mimeType: String?, mimeTypes: Array<String>?, multiSelect: Boolean): Intent =
        /*
         * 隐式允许用户选择一种特定类型的数据。 Implicitly allow the user to select a particular kind of data.
         *
         * Same as : ACTION_GET_CONTENT , ACTION_OPEN_DOCUMENT
         */
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiSelect)
            // The MIME data type filter
            // Tip: type = "file/*" 比 */* 少了一些侧边栏选项(There are fewer sidebar options than */*)
            if (mimeType.isNullOrBlank() && mimeTypes.isNullOrEmpty()) type = "*/*"
            else {
                type = if (mimeType.isNullOrEmpty()) "*/*" else mimeType
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            // Only return URIs that can be opened with ContentResolver
            addCategory(Intent.CATEGORY_OPENABLE)
        }

}