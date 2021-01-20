# `ando.file:android-q`

> 目前共四个类, 其中`FileOperatorQ`为核心类:
```
ando
  └─file
      └─androidq
              BaseMediaColumnsData.kt
              FileOperatorQ.kt
              MediaStoreImage.kt
              MediaStoreVideo.kt
```

## 依赖(dependencies)
```
//Q和11兼容库,需要额外的库:'androidx.documentfile:documentfile:1.0.1'
implementation 'ando.file:android-q:xxx'
implementation 'androidx.documentfile:documentfile:1.0.1'
```

## `FileOperatorQ`

### 方法(Methods)

```kotlin
fun buildQuerySelectionStatement(kotlin.String, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.Boolean): ando.file.core.FileGlobal.QuerySelectionStatement
fun checkUriFlagSAF(android.net.Uri, kotlin.Int): kotlin.Boolean
fun closeIO(java.io.Closeable?): kotlin.Unit
fun createContentValues(kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.Int?): android.content.ContentValues
fun createFileSAF(android.app.Activity, android.net.Uri?, kotlin.String, kotlin.String, kotlin.Int): kotlin.Unit
fun deleteFileSAF(android.net.Uri): kotlin.Boolean
fun deleteUri(android.app.Activity, android.net.Uri?, kotlin.Int): kotlin.Boolean
fun deleteUri(android.app.Activity, android.net.Uri?, kotlin.String?, kotlin.Array<kotlin.String>?, kotlin.Int): kotlin.Boolean
fun deleteUriDirectory(android.app.Activity, kotlin.Int, kotlin.String): kotlin.Boolean
fun deleteUriMediaStoreImage(android.app.Activity, ando.file.androidq.MediaStoreImage, kotlin.Int): kotlin.Boolean
fun dumpDocumentFileTree(androidx.documentfile.provider.DocumentFile?): kotlin.Unit
fun getBitmapFromUri(android.net.Uri?): android.graphics.Bitmap?
fun getDocumentTreeSAF(android.app.Activity, android.net.Uri?, kotlin.Int): androidx.documentfile.provider.DocumentFile?
fun getDocumentTreeSAF(android.app.Activity, kotlin.Int): androidx.documentfile.provider.DocumentFile?
fun getInputStreamForVirtualFile(android.net.Uri, kotlin.String): java.io.InputStream?
fun getMediaCursor(android.net.Uri, kotlin.Array<kotlin.String>?, kotlin.String?, ando.file.core.FileGlobal.QuerySelectionStatement?): android.database.Cursor?
fun getMediaLocation(android.net.Uri, (latLong: kotlin.FloatArray) -> kotlin.Unit): kotlin.Unit
fun insertAudio(kotlin.String?): kotlin.Unit
fun insertBitmap(android.graphics.Bitmap?, android.content.ContentValues): android.net.Uri?
fun insertMediaFile(android.net.Uri?, android.content.Context, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?): kotlin.String?
fun isVirtualFile(android.net.Uri): kotlin.Boolean
fun loadThumbnail(android.net.Uri?, kotlin.Int, kotlin.Int): android.graphics.Bitmap?
fun moveFileSAF(android.net.Uri, android.net.Uri, android.net.Uri): kotlin.Unit
fun openDirectorySAF(android.app.Activity, android.net.Uri?, kotlin.Int): kotlin.Unit
fun openFileSAF(android.app.Activity, android.net.Uri?, kotlin.String, kotlin.Int): kotlin.Unit
fun performFileSearch(android.app.Activity, kotlin.String, kotlin.Int): kotlin.Unit
fun queryMediaStoreImages(): kotlin.collections.MutableList<ando.file.androidq.MediaStoreImage>?
fun queryMediaStoreImages(kotlin.Array<kotlin.String>?, kotlin.String?, ando.file.core.FileGlobal.QuerySelectionStatement?): kotlin.collections.MutableList<ando.file.androidq.MediaStoreImage>?
fun queryMediaStoreImages(kotlin.Array<kotlin.String>?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.String?, kotlin.Boolean): kotlin.collections.MutableList<ando.file.androidq.MediaStoreImage>?
fun queryMediaStoreImages(kotlin.String): android.net.Uri?
fun queryMediaStoreImages(kotlin.String, kotlin.Boolean): android.net.Uri?
fun queryMediaStoreVideo(kotlin.Array<kotlin.String>?, kotlin.String?, kotlin.Long, java.util.concurrent.TimeUnit): kotlin.collections.MutableList<ando.file.androidq.MediaStoreVideo>?
fun readTextFromUri(android.net.Uri): kotlin.String
fun readTextFromUri(android.net.Uri, (result: kotlin.String?) -> kotlin.Unit): kotlin.Unit
fun renameFileSAF(android.net.Uri, kotlin.String?, (isSuccess: kotlin.Boolean, msg: kotlin.String) -> kotlin.Unit): kotlin.Unit
fun saveDocTreePersistablePermissionSAF(android.app.Activity, android.net.Uri): kotlin.Unit
fun selectSingleFile(android.app.Activity, kotlin.String, kotlin.Int): kotlin.Unit
fun selectSingleImage(android.app.Activity): kotlin.Unit
fun testQueryMediaVideoByUri(): kotlin.Unit
fun writeTextToUri(android.net.Uri, kotlin.String?): kotlin.Unit
```
