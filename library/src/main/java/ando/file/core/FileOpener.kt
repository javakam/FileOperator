package ando.file.core

import ando.file.core.FileMimeType.getMimeType
import android.app.Activity
import android.content.Intent
import android.net.Uri

/**
 * Title: FileOpener
 * <p>
 * Description: 打开该 Uri 对应文件类型的所有软件, 通常情况下是底部弹窗
 * </p>
 * @author javakam
 * @date 2020/8/24 11:20
 */
object FileOpener {

    /**
     * 直接打开 Url 对应的系统应用
     * <pre>
     * eg: 如果url是视频地址,则直接用系统的播放器打开
     * </pre>
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
     * 根据 文件路径 和 类型(后缀判断) 显示支持该格式的程序
     *
     * @param uri
     * @param mimeType 指定打开文件的 MimeType 类型
     * [√] /storage/emulated/0/Pictures/sl2/BitmapImage.png
     * [X] /data/user/0/xxx.cn.app/cache/documents/microMsg.1579490868646(2).jpg
     */
    fun openFileBySystemChooser(context: Any, uri: Uri?, mimeType: String? = null) =
        uri?.let { u ->
            Intent.createChooser(createOpenFileIntent(u, mimeType), "选择程序")?.let {
                startActivity(context, it)
            }
        }


    /**
     * 选择文件【调用系统的文件管理】
     *
     * <pre>
     *  注:
     *      1.Intent.setType 不能为空!
     *      2.mimeTypes 会覆盖 mimeType
     *      3.ACTION_GET_CONTENT , ACTION_OPEN_DOCUMENT 效果相同
     *      4.开启多选 resultCode=-1
     * </pre>
     */
    fun createChooseIntent(mimeType: String?, mimeTypes: Array<String>?, multiSelect: Boolean): Intent =
        // Implicitly allow the user to select a particular kind of data. Same as : Intent.ACTION_GET_CONTENT
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiSelect)
            // The MIME data type filter
            //intent.setType("image/*");    //选择图片
            //intent.setType("audio/*");    //选择音频
            //intent.setType("video/*");    //选择视频 （mp4 3gp 是 android支持的视频格式）
            //intent.setType("file/*");     //比 */* 少了一些侧边栏选项
            //intent.setType("video/*;image/*");//错误方式;同时选择视频和图片 ->  https://www.jianshu.com/p/e98c97669af0
            if (mimeType.isNullOrBlank() && mimeTypes.isNullOrEmpty()) type = "*/*"
            else {
                type = if (mimeType.isNullOrEmpty()) "*/*" else mimeType
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            // Only return URIs that can be opened with ContentResolver
            addCategory(Intent.CATEGORY_OPENABLE)
        }

}