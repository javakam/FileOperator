> **下一篇** 👉 [Android Q & Android 11存储适配(二) FileOperator文件管理框架](https://juejin.im/post/6854573214451335175)

# 分区存储（Scoped Storage）

- 沙盒存储(App-specific directory) 本应用专有的目录（通过 Context.getExternalFilesDir() 访问）
- 公共目录(Public Directory)  MediaStore/SAF(Storage Access Framework) with ContentResolver

<p>分区存储如何影响文件访问：</p>
<table>
      <tbody><tr>
        <th>文件位置</th>
        <th>所需权限</th>
        <th>访问方法 (*)</th>
        <th>卸载应用时是否移除文件？</th>
      </tr>
      <tr>
        <td>特定于应用的目录</td>
        <td>无</td>
        <td><a href="https://developer.android.com/reference/android/content/Context#getExternalFilesDir(java.lang.String)">
        getExternalFilesDir()</a></td>
        <td>是</td>
      </tr>
      <tr>
        <td>媒体集合<br />（照片、视频、音频）</td>
        <td><a href="https://developer.android.com/reference/android/Manifest.permission#READ_EXTERNAL_STORAGE">
       READ_EXTERNAL_STORAGE</a><br /><font size="2" color="red">（仅当访问其他应用的文件时）</font></td>
        <td><a href="https://developer.android.com/reference/android/provider/MediaStore">
        MediaStore</a></td>
        <td>否</td>
      </tr>
      <tr>
        <td>下载内容<br />（文档和<br />电子书籍）</td>
        <td>无</td>
        <td><a href="https://developer.android.com/guide/topics/providers/create-document-provider">SAF存储访问框架</a><br />（加载系统的文件选择器）</td>
        <td>否</td>
      </tr>
    </tbody></table>

>  对应于`MediaStore` 类中仅包含五种文件类型 `Image/Video/Audio`以及`Files`和`Download` , 其中 `Image/Video/Audio` 直接使用`MediaStore`+`ContentResolver` API即可访问 , 而`Files`和`Download`则是使用 `SAF`存储访问框架访问。

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/445b640c4e174ca59eefff512cc8524c~tplv-k3u1fbpfcp-zoom-1.image)

> ⭐ 注意：使用分区存储的应用对于 /sdcard/DCIM/IMG1024.JPG 这类路径不具有直接内核访问权限。要访问此类文件，应用必须使用 `MediaStore`，并调用 `ContentResolver.openFile()` 等方法。

## App Specific 沙盒目录

- 如果配置了 `FileProvider` 并且配置了`external-files-path`和`external-cache-path`, 应用会在启动时自动创建 `cache`和`files`目录:

```
 <!--context.getExternalFilesDirs()-->
 <external-files-path
     name="ando_file_external_files"
     path="." />
 <!-- getExternalCacheDirs() 此标签需要 support 25.0.0以上才可以使用-->
 <external-cache-path
     name="ando_file_external_cache"
     path="." />
```
`FileProvider` :
```
<provider
    android:name=".common.FileProvider"
    android:authorities="${applicationId}.fileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## MediaStore

- 媒体数据限制

分区存储会施加以下媒体数据限制：

若您的应用未获得 ACCESS_MEDIA_LOCATION 权限，照片文件中的 Exif 元数据会被修改。要了解详情，请参阅介绍如何访问照片中的位置信息的部分。

MediaStore.Files 表格本身会经过过滤，仅显示照片、视频和音频文件。例如，表格中不显示 PDF 文件。
必须使用 MediaStore 在 Java 或 Kotlin 代码中访问媒体文件。请参阅有关`如何从原生代码访问媒体文件`的指南。
该指南介绍了如何处理媒体文件，并提供了有关访问 MediaStore 内的单个文档和文档树的最佳做法。如果您的应用使用分区存储，则需要使用这些方法来访问媒体。

- 如何从原生代码访问媒体文件

系统会自动扫描外部存储，并将媒体文件添加到以下定义好的集合中：


<ul>
<li><strong>Images</strong>, including photographs and screenshots, which are stored in the
<code translate="no" dir="ltr">DCIM/</code> and <code translate="no" dir="ltr">Pictures/</code> directories. The system adds these files to the
<a href="/reference/android/provider/MediaStore.Images"><code translate="no" dir="ltr">MediaStore.Images</code></a> table.</li>
<li><strong>Videos</strong>, which are stored in the <code translate="no" dir="ltr">DCIM/</code>, <code translate="no" dir="ltr">Movies/</code>, and <code translate="no" dir="ltr">Pictures/</code>
directories. The system adds these files to the
<a href="/reference/android/provider/MediaStore.Video"><code translate="no" dir="ltr">MediaStore.Video</code></a> table.</li>
<li><strong>Audio files</strong>, which are stored in the <code translate="no" dir="ltr">Alarms/</code>, <code translate="no" dir="ltr">Audiobooks/</code>, <code translate="no" dir="ltr">Music/</code>,
<code translate="no" dir="ltr">Notifications/</code>, <code translate="no" dir="ltr">Podcasts/</code>, and <code translate="no" dir="ltr">Ringtones/</code> directories, as well as audio
playlists that are in the <code translate="no" dir="ltr">Music/</code> or <code translate="no" dir="ltr">Movies/</code> directories. The system adds
these files to the
<a href="/reference/android/provider/MediaStore.Audio"><code translate="no" dir="ltr">MediaStore.Audio</code></a> table.</li>
<li><strong>Downloaded files</strong>, which are stored in the <code translate="no" dir="ltr">Download/</code> directory. On
devices that run Android&nbsp;10 (API level 29) and higher, these files are stored in the
<a href="/reference/android/provider/MediaStore.Downloads"><code translate="no" dir="ltr">MediaStore.Downloads</code></a>
table. <em>This table isn&#39;t available on Android 9 (API level 28) and lower.</em></li>
</ul>

- 权限管理

如果您的应用使用范围存储，则它仅应针对运行Android 9（API级别28）或更低版本的设备请求与存储相关的权限。 您可以通过在应用清单文件中的权限声明中添加android：maxSdkVersion属性来应用此条件：
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="28" />
```
**不要为运行Android 10或更高版本的设备不必要地请求与存储相关的权限。** 您的应用程序可以参与定义明确的媒体集合，包括MediaStore.Downloads集合，而无需请求任何与存储相关的权限。 例如，如果您正在开发相机应用程序，则无需请求与存储相关的权限，因为您的应用程序拥有您要写入媒体存储区的图像。


- MediaStore API 提供访问以下类型的媒体文件的接口：

```
照片：存储在 MediaStore.Images 中。
视频：存储在 MediaStore.Video 中。
音频文件：存储在 MediaStore.Audio 中。
MediaStore 还包含一个名为 MediaStore.Files 的集合，该集合提供访问所有类型的媒体文件的接口。其他文件，例如 PDF 文件，无法访问到。
```
> 注意：如果您的应用使用分区存储，MediaStore.Files 集合将仅显示照片、视频和音频文件。

- 若要加载媒体文件，请从 ContentResolver 调用以下方法之一：

    - 对于单个媒体文件，请使用 `openFileDescriptor()`。
    - 对于单个媒体文件的缩略图，请使用 `loadThumbnail()`，并传入要加载的缩略图的大小。
    - 对于媒体文件的集合，请使用 `ContentResolver.query()`。


- 🌰查询一个媒体文件集合

```
// Need the READ_EXTERNAL_STORAGE permission if accessing video files that your app didnt create.

// Container for information about each video.
data class Video(val uri: Uri,
    val name: String,
    val duration: Int,
    val size: Int
)
val videoList = mutableListOf<Video>()

val projection = arrayOf(
    MediaStore.Video.Media._ID,
    MediaStore.Video.Media.DISPLAY_NAME,
    MediaStore.Video.Media.DURATION,
    MediaStore.Video.Media.SIZE
)

// Show only videos that are at least 5 minutes in duration.
val selection = "${MediaStore.Video.Media.DURATION} >= ?"
val selectionArgs = arrayOf(
    TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
)

// Display videos in alphabetical order based on their display name.
val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

val query = ContentResolver.query(
    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
    projection,
    selection,
    selectionArgs,
    sortOrder
)
query?.use { cursor ->
    // Cache column indices.
    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
    val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
    val durationColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

    while (cursor.moveToNext()) {
        // Get values of columns for a given video.
        val id = cursor.getLong(idColumn)
        val name = cursor.getString(nameColumn)
        val duration = cursor.getInt(durationColumn)
        val size = cursor.getInt(sizeColumn)

        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            id
        )

        // Stores column values and the contentUri in a local object
        // that represents the media file.
        videoList += Video(contentUri, name, duration, size)
    }
}
```

- 媒体文件的挂起状态

如果您的应用程序执行一些可能非常耗时的操作，比如写入媒体文件，那么在文件被处理时对其进行独占访问是非常有用的。在运行Android 10或更高版本的设备上，您的应用程序可以通过将`IS_PENDING`标志的值设置为`1`来获得这种独占访问。只有您的应用程序可以查看该文件，直到您的应用程序将`IS_PENDING`的值更改回0。

```
为正在存储的媒体文件提供待处理状态
在搭载 Android 10（API 级别 29）及更高版本的设备上，您的应用可以通过使用 IS_PENDING 标记在媒体文件写入磁盘时获得对文件的独占访问权限。

以下代码段展示了在将图片存储到 MediaStore.Images 集合所对应的目录时如何使用 IS_PENDING 标记：

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG1024.JPG")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.getContentResolver()
    val collection = MediaStore.Images.Media
            .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val item = resolver.insert(collection, values)

    resolver.openFileDescriptor(item, "w", null).use { pfd ->
        // Write data into the pending image.
    }

    // Now that we're finished, release the "pending" status, and allow other apps
    // to view the image.
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(item, values, null, null)

```

Android Q 上，MediaStore 中添加了一个 IS_PENDING Flag，用于标记当前文件是 Pending 状态。

其他 APP 通过 MediaStore 查询文件，如果没有设置 setIncludePending 接口，就查询不到设置为 Pending 状态的文件，这就能使 APP 专享此文件。

这个 flag 在一些应用场景下可以使用，例如在下载文件的时候：下载中，文件设置为 Pending 状态；下载完成，把文件 Pending 状态置为 0。
```
ContentValues values = new ContentValues();
values.put(MediaStore.Images.Media.DISPLAY_NAME, "myImage.PNG");
values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
values.put(MediaStore.Images.Media.IS_PENDING, 1);

ContentResolver resolver = context.getContentResolver();
Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
Uri item = resolver.insert(uri, values);

try {
    ParcelFileDescriptor pfd = resolver.openFileDescriptor(item, "w", null);
    // write data into the pending image.
} catch (IOException e) {
    LogUtil.log("write image fail");
}

// clear IS_PENDING flag after writing finished.
values.clear();
values.put(MediaStore.Images.Media.IS_PENDING, 0);
resolver.update(item, values, null, null);
```

- 文件移动

```
Note: You can move files on disk during a call to update() by changing MediaColumns.RELATIVE_PATH or MediaColumns.DISPLAY_NAME.

注意：您可以在调用update 的过程中通过更改 MediaColumns.RELATIVE_PATH  或MediaColumns.DISPLAY_NAME 在磁盘上移动文件。
```

## Storage Access Framework

### `相关视频 (Youtube)：`
<ul>
    <li><a href="http://www.youtube.com/watch?v=zxHVeXbK1P4">DevBytes：Android 4.4 存储访问框架：提供程序</a></li>
     <li><a href="http://www.youtube.com/watch?v=UFj9AEz0DHQ">DevBytes：Android 4.4 存储访问框架：客户端</a></li>
     <li><a href="https://www.youtube.com/watch?v=4h7yCZt231Y">存储访问框架中的虚拟文件</a></li>
</ul>

> Android 4.4（API 级别 19）引入了存储访问框架 (SAF)。借助 SAF，用户可轻松在其所有首选文档存储提供程序中浏览并打开文档、图像及其他文件。用户可通过易用的标准界面，以统一方式在所有应用和提供程序中浏览文件，以及访问最近使用的文件。


![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4c58f1ee27614ab485957e2b993d9012~tplv-k3u1fbpfcp-zoom-1.image)

### 编辑文档

> ⭐ Note: The DocumentFile class's  `canWrite() ` method doesn't necessarily indicate that your app can edit a document. That's because this method returns true if `Document.COLUMN_FLAGS` contains either `FLAG_SUPPORTS_DELETE` or `FLAG_SUPPORTS_WRITE`. To determine whether your app can edit a given document, query the value of `FLAG_SUPPORTS_WRITE` directly.

### 虚拟文件 👉 [视频](https://www.youtube.com/watch?v=4h7yCZt231Y)

Android 7.0 在存储访问框架中加入了虚拟文件的概念。即使虚拟文件没有二进制表示形式，客户端应用也可将其强制转换为其他文件类型，或使用 ACTION_VIEW Intent 查看这些文件，从而打开文件中的内容。

如要打开虚拟文件，您的客户端应用需包含可处理此类文件的特殊逻辑。若想获取文件的字节表示形式（例如为了预览文件），则需从文档提供程序请求另一种 MIME 类型。

为获得应用中虚拟文件的 URI，您首先需创建 Intent 来打开文件选择器界面（如先前搜索文档中的代码所示）。

> ⭐ 重要说明：由于应用不能使用 openInputStream() 方法直接打开虚拟文件，因此如果您在 ACTION_OPEN_DOCUMENT Intent 中加入 CATEGORY_OPENABLE 类别，则您的应用不会收到任何虚拟文件。


### SAF 使用情形 👉 [官方文档](https://developer.android.google.cn/training/data-storage/shared/documents-files)

通过上面的分析可以看出, MediaStore 仅可以处理公共目录中的 `图片/视频/音频` 文件, 当涉及到分组文件和其它类型文件的时候显得捉襟见肘。

    - [操作一组文件](https://developer.android.google.cn/training/data-storage/shared/media#manage-groups-files)
    - [操作文档和其他文件](https://developer.android.google.cn/training/data-storage/shared/media#other-file-types)
    - [把数据分享给其它应用](https://developer.android.google.cn/training/data-storage/shared/media#companion-apps)


- 查看剩余空间

如果您提前知道要存储多少数据，则可以通过调用getAllocatableBytes（）找出设备可以为应用程序提供多少空间。 getAllocatableBytes（）的返回值可能大于设备上当前的可用空间量。 这是因为系统已识别出可以从其他应用程序的缓存目录中删除的文件。
```
// App needs 10 MB within internal storage.
const val NUM_BYTES_NEEDED_FOR_MY_APP = 1024 * 1024 * 10L;

val storageManager = applicationContext.getSystemService<StorageManager>()!!
val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(filesDir)
val availableBytes: Long =
        storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
if (availableBytes >= NUM_BYTES_NEEDED_FOR_MY_APP) {
    storageManager.allocateBytes(
        appSpecificInternalDirUuid, NUM_BYTES_NEEDED_FOR_MY_APP)
} else {
    val storageIntent = Intent().apply {
        action = ACTION_MANAGE_STORAGE
    }
    // Display prompt to user, requesting that they choose files to remove.
}
```
> ⭐ 保存文件之前，不需要检查可用空间量。 相反，您可以尝试立即写入文件，然后在发生异常时捕获IOException。

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8353bae487d74141b7d0c865b8144cce~tplv-k3u1fbpfcp-zoom-1.image)

#### 卸载应用

`AndroidManifest.xml`中声明：`android:hasFragileUserData="true"`, 卸载应用会有提示是否保留APP数据。默认应用卸载时`Appspecific`目录下的数据被删除，但用户可以选择保留。

#### 共享文件 👉 https://developer.android.com/training/secure-file-sharing/share-file

#### 分享文件 👉 https://developer.android.com/training/secure-file-sharing/setup-sharing

#### FileProvider 👉 https://developer.android.google.cn/reference/androidx/core/content/FileProvider

### 参考资料

#### 文档
[ContentProvider官方文档](https://developer.android.google.cn/guide/topics/providers/content-providers)

[DocumentsProvider官方文档]()

[唯一标识符最佳做法](https://developer.android.google.cn/training/articles/user-data-ids#signed-out-user-prefs-between-apps)

#### 视频

Youtube 👉 <https://www.youtube.com/watch?v=UnJ3amzJM94>

Bilibili 👉 <https://www.bilibili.com/video/BV1NE41117eR>