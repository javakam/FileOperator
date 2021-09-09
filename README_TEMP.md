### FileUri.kt

```kotlin
private fun testPath(uri: Uri): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getAllPdf()
    }
    /*
      todo 2021年9月7日 17:00:11
      getDataColumn -> Volume content://com.android.providers.downloads.documents/document/msf%3A51 not found
      https://developer.android.com/reference/android/provider/MediaStore.Downloads

      content://com.android.providers.downloads.documents/document/msf%3A51
      转换为
      content://media/content%3A%2F%2Fcom.android.providers.downloads.documents%2Fdocument%2Fmsf%253A51/downloads
     */
    var path = getDataColumn(uri)
    if (path.isNullOrBlank()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            /*
            侧边栏->Download :
                __________________
                content://com.android.providers.downloads.documents/document/msf:51
                content://media/external/downloads
                msf:51
                content://media/external/downloads/document/msf:51
                content://media/document/msf:51
                __________________
                com.ando.file.sample E/ActivityThread: Failed to find provider info for media/external/downloads

            侧边栏->手机图标进入:
                Uri:       content://com.android.externalstorage.documents/document/primary:Download/AAA/[高清 720P] 川普优选又来了？.flv
                Authority: com.android.externalstorage.documents
                Segments:  [document, primary:Download/AAA/[高清 720P] 川普优选又来了？.flv]
             */
            FileLogger.e("""
                __________________
                $uri                                   
                ${MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)}
                ${DocumentsContract.getDocumentId(uri)}
            }
                __________________
                """.trimIndent())
            path =
                getDataColumn(DocumentsContract.buildDocumentUri("com.android.externalstorage.documents" + "/document/primary:Download",
                    DocumentsContract.getDocumentId(uri)))
        }
    }
    return path
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getAllPdf() { //查询sd卡所有pdf文件
    val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val projection: Array<String>? = null
    val sortOrder: String? = null // unordered
    // only pdf
    val selectionMimeType = "${MediaStore.Files.FileColumns.MIME_TYPE}=?"
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf")
    val selectionArgsPdf = arrayOf(mimeType)
    val cursor = FileOperator.getContext().contentResolver.query(uri, projection, selectionMimeType, selectionArgsPdf, sortOrder)
    while (cursor != null && cursor.moveToNext()) {
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        val filePath = cursor.getString(column_index) //所有pdf文件路径
        FileLogger.d("getAllPdf() filePath=$filePath")
    }
    cursor?.close()
}
```