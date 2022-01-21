package ando.file.core

import ando.file.core.FileMimeType.getMimeType
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * # FileOpener
 *
 * 打开该 Uri 对应文件类型的所有软件, 通常情况是个底部弹窗
 *
 * Open all the software corresponding to the file type of the Uri, usually it is a pop-up window at the bottom
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.setDataAndType(Uri.parse(url), getMimeType(url))
            activity.startActivity(intent)
        } catch (e: Exception) {
            FileLogger.e("OpenUrl Error : " + e.message)
        }
    }

    /**
     * 打开系统分享弹窗 (Open the system sharing popup)
     */
    fun openShare(context: Context, uri: Uri, title: String = "分享文件") {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        // Put the Uri and MIME type in the result Intent
        intent.setDataAndType(uri, getMimeType(uri))

        //https://stackoverflow.com/questions/3918517/calling-startactivity-from-outside-of-an-activity-context
        val chooserIntent: Intent = Intent.createChooser(intent, title)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    /**
     * 打开浏览器 (Open browser)
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun openBrowser(
        context: Context, url: String, title: String = "请选择浏览器", newTask: Boolean = false,
        block: ((result: Boolean, msg: String?) -> Unit)? = null,
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (newTask) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            //startActivity(intent)
            //https://developer.android.com/about/versions/11/privacy/package-visibility
            if (intent.resolveActivity(context.packageManager) != null) {
                val chooserIntent: Intent = Intent.createChooser(intent, title)
                if (newTask) {
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                block?.invoke(true, null)
            } else {
                block?.invoke(true, "没有可用的浏览器")
            }
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            block?.invoke(true, e.toString())
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
    fun openChooser(context: Any, uri: Uri?, mimeType: String? = null, title: String? = "选择程序") =
        uri?.let { u ->
            Intent.createChooser(createOpenFileIntent(u, mimeType), title)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(context, it)
            }
        }

    fun openChooser(context: Any, uri: Uri?, title: String? = "选择程序") =
        openChooser(context, uri, getMimeType(uri), title)

    /**
     * ### 选择文件【调用系统的文件管理】 (Select file [call system file management])
     *
     * 注:
     *
     * #### 1. Intent.setType 不能为空(Can not be empty) !
     * ```
     * android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.intent.action.OPEN_DOCUMENT cat=[android.intent.category.OPENABLE] (has extras) }
     * at android.app.Instrumentation.checkStartActivityResult(Instrumentation.java:2105)
     * ```
     *
     * #### 2. mimeTypes 会覆盖 mimeType (mimeTypes will override mimeType)
     * ```
     * eg:
     *      Intent.setType("image / *")
     *      Intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio / *"))
     * 🍎 最终可选文件类型变为音频
     * ```
     *
     * #### 3. ACTION_GET_CONTENT, ACTION_OPEN_DOCUMENT 效果相同, Android Q 上使用 `ACTION_GET_CONTENT` 会出现:
     * ```
     *      java.lang.SecurityException: UID 10483 does not have permission to content://com.android.providers.media.documents/document/image%3A16012 [user 0];
     *      you could obtain access using ACTION_OPEN_DOCUMENT or related APIs
     * ```
     *
     * #### 4. 开启多选(Open multiple selection) resultCode = -1
     *
     * #### 5. 无论是`ACTION_OPEN_DOCUMENT`还是`ACTION_GET_CONTENT`都只是负责打开和选择,
     * 具体的文件操作如查看文件内容,删除,分享,复制,重命名等操作需要在`onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)`中的`data:Intent`中提取
     *
     * #### 6. 不使用ACTION_GET_CONTENT的另外一个原因: https://stackoverflow.com/questions/50386916/select-specific-file-types-using-action-get-content-and-settype-or-intent-extra
     *
     * #### 7. 可以使用返回的Intent设置临时权限 Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
     *
     * #### 8. extraMimeTypes 必须要用 Array 类型的, 否则打开的文件管理器后只显示"最近"一个页面 !
     */
    fun createChooseIntent(@NonNull mimeType: String?, @Nullable extraMimeTypes: Array<out String>?, multiSelect: Boolean): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiSelect)
            setTypeAndNormalize(if (mimeType.isNullOrBlank()) "*/*" else mimeType)
            if (!extraMimeTypes.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
            }
            addCategory(Intent.CATEGORY_OPENABLE)
        }

}