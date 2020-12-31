package com.ando.file.sample.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val FILE_NAME = "crash"
    private const val FILE_NAME_SUFFIX = ".trace"

    private var mDeviceInfo: String? = null
    private var mCrashFileDir: String? = null
    private var mCrashHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context, path: String?) {
        this. mDeviceInfo = collectDeviceInfo(context)
        this.mCrashFileDir = path
        val saveErrorDirFile = File(mCrashFileDir ?: return)
        if (!saveErrorDirFile.exists()) {
            saveErrorDirFile.mkdirs()
        }
        mCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!handleException(thread, throwable) && mCrashHandler != null) {
            mCrashHandler?.uncaughtException(thread, throwable)
        } else {
            Process.killProcess(Process.myPid())
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun handleException(thread: Thread?, throwable: Throwable?): Boolean {
        if (thread == null || throwable == null) {
            return false
        }
        Thread {
            try {
                val current = System.currentTimeMillis()
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(current))
                val file = File(mCrashFileDir + FILE_NAME + time + FILE_NAME_SUFFIX)
                file.createNewFile()
                val pw = PrintWriter(BufferedWriter(FileWriter(file)))
                pw.println(time)
                pw.println(mDeviceInfo)
                throwable.printStackTrace(pw)
                pw.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        return true
    }


    @Suppress("DEPRECATION")
    private fun collectDeviceInfo(context: Context): String {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return StringBuffer().append("APP Version:")
            .append(packageInfo?.versionName + '_')
            .append("${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo?.longVersionCode else packageInfo?.versionCode} \n")
            .append("OS Version:")
            .append(Build.VERSION.RELEASE + '_')
            .append("${Build.VERSION.SDK_INT} \n")
            .append("Vendor:")
            .append(Build.MANUFACTURER + "\n")
            .append("Model:")
            .append(Build.MODEL + "\n")
            .append("CUP ABI: ${Build.SUPPORTED_ABIS?.forEach { " ${it?.toString()} ; " }} \n")
            .toString()
    }

}