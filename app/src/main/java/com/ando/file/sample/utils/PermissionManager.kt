package com.ando.file.sample.utils

import android.Manifest
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.ando.file.sample.toastLong
import com.permissionx.guolindev.PermissionX

/**
 * 权限框架 (Permissions framework)
 *
 * com.permissionx.guolindev.PermissionX
 */
object PermissionManager {

    /**
     * 相应的清单文件中配置 (The corresponding listing file configuration):
     *
     * <!-- Apps on devices running Android 4.4 (API level 19) or higher cannot
     *      access external storage outside their own "sandboxed" directory, so
     *      the READ_EXTERNAL_STORAGE (and WRITE_EXTERNAL_STORAGE) permissions
     *      aren't necessary. -->
     *
     * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
     *
     * <uses-permission
     *      android:name="android.permission.WRITE_EXTERNAL_STORAGE"
     *      tools:ignore="ScopedStorage" />
     */
    private val STORAGE_PERMISSION = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun requestStoragePermission(
        fragment: Fragment,
        block: (allGranted: Boolean) -> Unit? = {},
    ) {
        requestStoragePermission(fragment.requireActivity(), block)
    }

    fun requestStoragePermission(
        activity: FragmentActivity,
        block: (allGranted: Boolean) -> Unit? = {},
    ) {
        PermissionX.init(activity)
            .permissions(*STORAGE_PERMISSION)
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList, "请在设置中手动开启以下权限", "允许", "取消")
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    //activity.toastShort("已授予所有权限")
                } else {
                    activity.toastLong("以下权限被拒绝：$deniedList")
                }
                block.invoke(allGranted)
            }
    }
}