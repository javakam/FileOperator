package com.ando.file.sample

import android.app.Application
import ando.file.core.FileOperator
import com.ando.file.sample.utils.CrashHandler
import com.ando.file.sample.utils.MethodSignKotlinUtils

/**
 * # App
 *
 * @author javakam
 * @date 2020/5/9  14:08
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FileOperator.init(this, true)
        CrashHandler.init(this, "${externalCacheDir?.path}/Crash/")
        //MethodSignKotlinUtils.dumpMethods(this)
    }

}