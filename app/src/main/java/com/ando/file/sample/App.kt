/**
 * Copyright (C)  javakam, FileOperator Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ando.file.sample

import android.app.Application
import ando.file.FileOperator
import ando.file.core.FileDirectory.getExternalFilesDirDOCUMENTS
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

    override fun onCreate() {
        super.onCreate()
        FileOperator.init(this, true)
        CrashHandler.init(this, "${getExternalFilesDirDOCUMENTS()?.absolutePath}/Crash/")

    }

}