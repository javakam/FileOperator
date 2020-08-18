package com.ando.file.sample

import android.app.Application
import android.content.Context
import com.ando.file.BuildConfig
import com.ando.file.FileOperator
import com.ando.file.common.getExternalFilesDirDOCUMENTS
import com.ando.file.sample.utils.CrashHandler

/**
 * Title: App
 *
 * Description:
 *
 * @author javakam
 * @date 2020/5/9  14:08
 */
class App : Application() {

    companion object {
        lateinit var instance: App

        fun get(): App {
            return instance
        }

        fun getApplicationContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        FileOperator.init(
            this,
            BuildConfig.DEBUG
        )

        CrashHandler.init(this, "${getExternalFilesDirDOCUMENTS()?.absolutePath}/Crash/")

    }

}