## Q & A

### 🔥 获取路径为空的问题(Path return null problem) good first issue

长期置顶 👉 https://github.com/javakam/FileOperator/issues/49

### 1.Invalid image: ExifInterface got an unsupported image format

```kotlin
W / ExifInterface: Invalid image : ExifInterface got an unsupported image format
file(ExifInterface supports JPEG and some RAW image formats only) or a corrupted JPEG file to ExifInterface .
java.io.IOException: Invalid byte order: 0
at android . media . ExifInterface . readByteOrder (ExifInterface.java:3134)
at android . media . ExifInterface . isOrfFormat (ExifInterface.java:2449)
at android . media . ExifInterface . getMimeType (ExifInterface.java:2327)
at android . media . ExifInterface . loadAttributes (ExifInterface.java:1755)
at android . media . ExifInterface .<init>(ExifInterface.java:1449)
...

Fixed :
dependencies {
    compileOnly "androidx.exifinterface:exifinterface:1.3.2"
    ...
}

Then replace `android.media.ExifInterface` with `androidx.exifinterface.media.ExifInterface`
```

### 2.ImageDecoder$DecodeException: Failed to create image decoder with message

```kotlin
Caused by : android . graphics . ImageDecoder $DecodeException:
Failed to create image decoder with message 'unimplemented' Input contained an error.
```

[What is new in Android P — ImageDecoder & AnimatedImageDrawable](https://medium.com/appnroll-publication/what-is-new-in-android-p-imagedecoder-animatedimagedrawable-a65744bec7c1)

### 3.SecurityException... you could obtain access using ACTION_OPEN_DOCUMENT or related APIs

```kotlin
java.lang.SecurityException: UID 10483 does not have permission to
        content://com.android.providers.media.documents/document/image%3A16012 [user 0];
you could obtain access using ACTION_OPEN_DOCUMENT or related APIs
```

> Fixed: `ando.file.core.FileOpener.createChooseIntent`
把 Intent(Intent.ACTION_GET_CONTENT) 改为 Intent(Intent.ACTION_OPEN_DOCUMENT)

### 4.IllegalArgumentException: column '_data' does not exist

<https://stackoverflow.com/questions/42508383/illegalargumentexception-column-data-does-not-exist>

### 5.ActivityNotFoundException: No Activity found to handle Intent

```kotlin
android.content.ActivityNotFoundException: No Activity found to handle Intent {
    act = android.intent.action.OPEN_DOCUMENT cat =[android.intent.category.OPENABLE](has extras)
}
at android . app . Instrumentation . checkStartActivityResult (Instrumentation.java:2105)
```

> Fixed: `ando.file.core.FileOpener.createChooseIntent`:

```kotlin
Intent.setType("image / *")
Intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio / *"))
```

### 6.android.os.FileUriExposedException: file:///storage/emulated/0/Android/data/com.ando.file.sample/cache exposed beyond app through Intent.getData()

> Fixed: `AndroidManifest.xml`没配置`FileProvider`

### 7.Calling startActivity() from outside of an Activity

<https://stackoverflow.com/questions/3918517/calling-startactivity-from-outside-of-an-activity-context>

> Fixed: `Intent.createChooser`要添加两次`FLAG_ACTIVITY_NEW_TASK`:

```kotlin
val intent = Intent(Intent.ACTION_SEND)
intent.putExtra(Intent.EXTRA_STREAM, uri)
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

val chooserIntent: Intent = Intent.createChooser(intent, title)
chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
context.startActivity(chooserIntent)
```

## 感谢(Thanks)

### Google

[Storage Samples Repository](https://github.com/android/storage-samples)

[SAF 使用存储访问框架打开文件](https://developer.android.google.cn/guide/topics/providers/document-provider)

[SAF API UseCase](https://developer.android.google.cn/training/data-storage/shared/documents-files)

[管理分区外部存储访问](https://developer.android.google.cn/training/data-storage/files/external-scoped)

[管理分区外部存储访问 - 如何从原生代码访问媒体文件 & MediaStore增删该查API](https://developer.android.google.cn/training/data-storage/shared/media)

[处理外部存储中的媒体文件](https://developer.android.google.cn/training/data-storage/files/media)

[Android 11 中的隐私权](https://developer.android.google.cn/preview/privacy)

[Android 10 中的隐私权](https://developer.android.google.cn/about/versions/10/privacy/changes#scoped-storage)

### Blog

[LOGO](https://www.easyicon.net/1293281-folders_icon.html)

[FileUtils](https://github.com/coltoscosmin/FileUtils/blob/master/FileUtils.java)

[github/scoped_storage_sample](https://github.com/songlongGithub/scoped_storage_sample)

[掘金-Android Q 适配指南 让你少走一堆弯路](https://juejin.im/post/5cad5b7ce51d456e5a0728b0)

[Android Q 沙箱适配多媒体文件总结](https://segmentfault.com/a/1190000019224425)

[oppo AndroidQ适配指导](https://open.oppomobile.com/wiki/doc#id=10432)

[huawei Google Q版本应用兼容性整改指导](https://developer.huawei.com/consumer/cn/doc/50127)
