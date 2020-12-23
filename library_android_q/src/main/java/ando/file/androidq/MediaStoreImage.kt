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
package ando.file.androidq

import android.net.Uri
import java.util.*

/**
 * mapping ->
 *  MediaStore.Image.Media._ID,
 *  MediaStore.Image.Media.DISPLAY_NAME,
 */
data class MediaStoreImage(
    override var id: Long, override var uri: Uri?, override var displayName: String?, override var size: Long?,
    val description: String?, override var title: String?, override var mimeType: String?,
    override var dateAdded: Date?,
) : BaseMediaColumnsData() {

    @Suppress("UNUSED")
    constructor(uri: Uri?, displayName: String?, size: Long?) : this(0L, uri, displayName, size, null, null, null, null)
}