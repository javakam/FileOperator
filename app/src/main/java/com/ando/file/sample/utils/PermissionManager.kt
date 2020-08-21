package com.ando.file.sample.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ando.file.core.FileLogger

/**
 * <pre>
 *      shouldShowRequestPermissionRationale -> https://blog.csdn.net/wangpf2011/article/details/80589648
 *
 *      如果返回true表示用户点了禁止获取权限，但没有勾选不再提示。
 *      返回false表示用户点了禁止获取权限，并勾选不再提示
 *
 *      val shouldShow =ActivityCompat.shouldShowRequestPermissionRationale(fragment.activity,Manifest.permission.CAMERA)
 *
 *      L.w("shouldShow  $shouldShow")
 * </pre>
 * <pre>
 *     shouldShowRequestPermissionRationale
 *       1. 第一次请求权限时 ActivityCompat -> false;
 *       2. 第一次请求权限被禁止，但未选择【不再提醒】 -> true;
 *       3. 允许某权限后 -> false;
 *       4. 禁止权限，并选中【禁止后不再询问】 -> false；
 * </pre>
 */
object PermissionManager {

    const val REQUEST_EXTERNAL_STORAGE = 21
    const val REQUEST_EXTERNAL_CAMERA = 22

    var PERMISSIONS_STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) else arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )


    var PERMISSIONS_CAMERA = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        for (permission in PERMISSIONS_STORAGE) {
            val granted = ContextCompat.checkSelfPermission(activity, permission)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
                break
            }
        }
    }

    fun verifyCameraPermissions(activity: Activity) {
        // Check if we have write permission
        for (permission in PERMISSIONS_CAMERA) {
            val granted = ContextCompat.checkSelfPermission(activity, permission)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_CAMERA,
                    REQUEST_EXTERNAL_CAMERA
                )
                break
            }
        }
    }

    fun checkShowRationale(
        activity: Activity,
        vararg permissions: String
    ): Boolean {
        var showRationale: Boolean = true
        if (permissions.isNotEmpty()) {
            for (permission in permissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
                ) {
                    showRationale = false
                    break
                }
            }
        }
        return showRationale
    }

    /**
     * 1.采用该策略申请权限 <必须> 在进入页面时候申请权限  PermissionManager.verifyStoragePermissions(this)
     * 然后在 Click 事件中再次处理, 避开 ` 第一次请求权限时 ActivityCompat 返回为 false;` 的问题;
     * 2.此方式不需要处理 onRequestPermissionsResult 回调
     * <pre>
     *      val  showRationale = PermissionManager.checkShowRationaleAndGoToSetting(this,"请您到系统权限页面申请存储权限!",*PERMISSIONS_STORAGE)
     *      if (showRationale){
     *          PermissionManager.verifyStoragePermissions(this)
     *      }
     *      return@setOnClickListener
     * </pre>
     */
    fun checkShowRationaleAndGoToSetting(
        activity: Activity,
        notice: String? = "请申请存储权限!",
        vararg permissions: String
    ): Boolean {
        var showRationale: Boolean = true
        if (permissions.isNotEmpty()) {
            for (permission in permissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
                ) {
                    showRationale = false
                    break
                }
            }
        }
        if (!showRationale) { //用户点了禁止获取权限，并勾选不再提示
            Toast.makeText(activity, notice, Toast.LENGTH_LONG).show()
            goToSettings(activity)
            return false
        }
        return true
    }

    fun handleRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        block: (result: Boolean, showRationale: Boolean) -> Unit
    ) {
        // If request is cancelled, the result arrays are empty.
        if (havePermissions(grantResults)) {
            //showImages()
            block.invoke(true, false)
        } else {
            // If we weren't granted the permission, check to see if we should show
            // rationale for the permission.

            val showRationale = checkShowRationale(activity, *permissions)

            permissions.forEach {
                FileLogger.i( "权限 : $it")
            }
            FileLogger.e("handleRequestPermissionsResult showRationale=$showRationale ")
            block.invoke(false, showRationale)
        }
    }

    fun havePermissions(grantResults: IntArray): Boolean {
        if (grantResults.isNotEmpty()) {
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED)
                    return false
            }
        }
        return true
    }

    fun havePermissions(context: Context?, vararg permissions: String?): Boolean {
        if (context != null && permissions.isNotEmpty()) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        permission ?: continue
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 获取应用权限详情页面 Intent
     */
    fun goToSettings(activity: Activity) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${activity.packageName}")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            activity.startActivity(intent)
        }
    }

}