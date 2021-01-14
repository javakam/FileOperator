package com.ando.file.sample

import android.app.Application
import ando.file.FileOperator
import ando.file.core.FileDirectory.getExternalFilesDirDOCUMENTS
import com.ando.file.sample.utils.CrashHandler

/**
 * App
 *
 * Description:
 *
 * @author javakam
 * @date 2020/5/9  14:08
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FileOperator.init(this, true)
        CrashHandler.init(this, "${getExternalFilesDirDOCUMENTS()?.absolutePath}/Crash/")

    }

}