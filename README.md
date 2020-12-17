> **上一篇** 👉 [Android Q & Android 11存储适配(一) 基础知识点梳理](https://juejin.im/post/6854573214447140871)

# [FileOperator](https://github.com/javakam/FileOperator)

<a href="https://bintray.com/javakam/maven/core/1.0.1/link"><img src="https://api.bintray.com/packages/javakam/maven/core/images/download.svg?version=1.0.1"/></a>

- 🚀[FileOperator GitHub](https://github.com/javakam/FileOperator)
- 🚀更简单的处理Android系统文件操作
- 🚀适用于 Android 4.4 及以上系统 , 兼容AndroidQ新的存储策略
- 🚀图片压缩算法参考 [Luban](https://github.com/Curzibn/Luban)
- 🚀Kotlin 案例 👉 [app](https://github.com/javakam/FileOperator/tree/master/app) & Java 案例 👉 [sample_java](https://github.com/javakam/FileOperator/tree/master/sample_java)

## Gradle:
Project `build.gradle` :
```
repositories {
    maven { url 'https://dl.bintray.com/javakam/maven' }
}
```
> 推荐方式 :

```
implementation 'ando.file:core:1.0.1'         //核心库必选
implementation 'ando.file:android-q:1.0.1'    //AndroidQ & Android 11 兼容库
implementation 'ando.file:compressor:1.0.1'   //图片压缩,核心算法采用 Luban
implementation 'ando.file:selector:1.0.1'     //文件选择器
```
整体引入(不推荐):
```
implementation 'ando.file:FileOperator:0.9.1'
```
`Application`中初始化:
```
FileOperator.init(this,BuildConfig.DEBUG)
```

## 演示

| 功能列表 | 缓存目录 |
|:---:|:---:|
| <img src="https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/img1.png" width="288" height="610"/> | <img src="https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/cache1.png" width="270" height="564"/> |

### API
 
| App Specific | MediaStore | Storage Access Framework|
|:---:|:---:|:---:|
|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/img2.png)|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/img3.png)|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/img4.png)|

### 文件选择

| 单图 + 压缩 | 多图 + 压缩 | 多文件 |
|:---:|:---:|:---:|
|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/pick1.png)|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/pick2.png)|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/pick3.png)|

## Usage:

### 1. 单选图片
```kotlin
/*
说明:
    FileOptions T 为 String.filePath / Uri / File
    3M 3145728 Byte ; 5M 5242880 Byte; 10M 10485760 ; 20M = 20971520 Byte
 */
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    singleFileMaxSize = 2097152
    singleFileMaxSizeTip = "图片最大不超过2M！"
    allFilesMaxSize = 5242880
    allFilesMaxSizeTip = "总图片大小不超过5M！"
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setSelectMode(false)
    .setMinCount(1, "至少选一个文件!")
    .setMaxCount(10, "最多选十个文件!")
    .setSingleFileMaxSize(5242880, "大小不能超过5M！") //5M 5242880 ; 100M = 104857600 Byte
    .setAllFilesMaxSize(10485760, "总大小不能超过10M！")//
    .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同类型系统提供的选择UI不一样 eg:  arrayOf("video/*","audio/*","image/*")
    .applyOptions(optionsImage)
    //优先使用 FileSelectOptions 中设置的 FileSelectCondition
    .filter(object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return when (fileType) {
                FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                FileType.VIDEO -> true
                FileType.AUDIO -> true
                else -> true
            }
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("回调 onSuccess ${results?.size}")
            mTvResult.text = ""
            if (results.isNullOrEmpty()) return
            toastShort("正在压缩图片...")
            showSelectResult(results)
        }
        override fun onError(e: Throwable?) {
            FileLogger.e("回调 onError ${e?.message}")
            mTvResultError.text = mTvResultError.text.toString().plus(" 错误信息: ${e?.message} \n")
        }
    })
    .choose()
```

### 2. 多选图片
```kotlin
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    //maxCount = 2
    singleFileMaxSize = 3145728
    singleFileMaxSizeTip = "单张图片最大不超过3M！"
    allFilesMaxSize = 5242880
    allFilesMaxSizeTip = "图片总大小不超过5M！"
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setSelectMode(true)
    .setMinCount(1, "至少选一个文件!")
    .setMaxCount(10, "最多选十个文件!")
    //优先以自定义的 optionsImage.mSingleFileMaxSize , 单位 Byte
    .setSingleFileMaxSize(2097152, "单个大小不能超过2M！")
    .setAllFilesMaxSize(20971520, "总文件大小不能超过20M！")
    //1.OVER_SIZE_LIMIT_ALL_EXCEPT  超过数量限制和大小限制全部不返回  ; 2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART  超过数量限制和大小限制去掉后面相同类型文件
    .setOverSizeLimitStrategy(this.mOverSizeStrategy)
    .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同类型系统提供的选择UI不一样 eg:  arrayOf("video/*","audio/*","image/*")
    .applyOptions(optionsImage)
    //优先使用 FileSelectOptions 中设置的 FileSelectCondition
    .filter(object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return when (fileType) {
                FileType.IMAGE -> {
                    return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
                FileType.VIDEO -> true
                FileType.AUDIO -> true
                else -> true
            }
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("回调 onSuccess ${results?.size}")
            mTvResult.text = ""
            if (results.isNullOrEmpty()) return
            toastShort("正在压缩图片...")
            showSelectResult(results)
        }
        override fun onError(e: Throwable?) {
            FileLogger.e("回调 onError ${e?.message}")
            mTvResultError.text = mTvResultError.text.toString().plus(" 错误信息: ${e?.message} \n")
        }
    })
    .choose()
```

### 3. 多选文件
> 🌴适用于处理复杂文件选择情形, 如: 选取图片、视频文件,其中图片至少选择一张, 最多选择两张, 每张图片大小不超过3M, 全部图片大小不超过5M ; 
 视频文件只能选择一个, 每个视频大小不超过20M, 全部视频大小不超过30M 。

```kotlin
/*
明:
   FileOptions T 为 String.filePath / Uri / File
   3M 3145728 Byte ; 5M 5242880 Byte; 10M 10485760 ; 20M = 20971520 Byte
   50M 52428800 Byte ; 80M 83886080 ; 100M = 104857600 Byte
*/
//图片
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    maxCount = 2
    minCountTip = "至少选择一张图片"
    maxCountTip = "最多选择两张图片"
    singleFileMaxSize = 3145728
    singleFileMaxSizeTip = "单张图片最大不超过3M！"
    allFilesMaxSize = 5242880
    allFilesMaxSizeTip = "图片总大小不超过5M！"
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
//视频
val optionsVideo = FileSelectOptions().apply {
    fileType = FileType.VIDEO
    maxCount = 1
    minCountTip = "至少选择一个视频文件"
    maxCountTip = "最多选择一个视频文件"
    singleFileMaxSize = 20971520
    singleFileMaxSizeTip = "单视频最大不超过20M！"
    allFilesMaxSize = 31457280
    allFilesMaxSizeTip = "视频总大小不超过30M！"
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return (uri != null)
        }
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setSelectMode(true)
    .setMinCount(1, "至少选一个文件!")
    .setMaxCount(5, "最多选五个文件!")
    // 优先使用自定义 FileSelectOptions 中设置的单文件大小限制,如果没有设置则采用该值
    .setSingleFileMaxSize(2097152, "单文件大小不能超过2M！")
    .setAllFilesMaxSize(52428800, "总文件大小不能超过50M！")
    // 超过数量限制和大小限制两种返回策略: 1.OVER_SIZE_LIMIT_ALL_EXCEPT,超过数量限制和大小限制全部不返回;2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART,超过数量限制和大小限制去掉后面相同类型文件
    .setOverSizeLimitStrategy(OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART)
    .setMimeTypes(null)//默认为 null,*/* 即不做文件类型限定;MIME_MEDIA 媒体文件,不同类型系统提供的选择UI不一样 eg:  arrayOf("video/*","audio/*","image/*")
    .applyOptions(optionsImage, optionsVideo)
    // 优先使用 FileSelectOptions 中设置的 FileSelectCondition,没有的情况下才使用通用的
    .filter(object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return when (fileType) {
                FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                FileType.VIDEO -> true
                FileType.AUDIO -> true
                else -> true
            }
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("回调 onSuccess ${results?.size}")
            mTvResult.text = ""
            if (results.isNullOrEmpty()) return
            showSelectResult(results)
        }
        override fun onError(e: Throwable?) {
            FileLogger.e("回调 onError ${e?.message}")
            mTvResultError.text = mTvResultError.text.toString().plus(" 错误信息: ${e?.message} \n")
        }
    })
    .choose()
```
### 4.压缩图片 [ImageCompressor.kt](https://github.com/javakam/FileOperator/blob/master/library_compressor/src/main/java/ando/file/compressor/ImageCompressor.kt)
```kotlin
/**
 * 压缩图片 1.Luban算法; 2.直接压缩 -> Engine.compress(uri,  100L)
 *
 * T 为 String.filePath / Uri / File
 */
private fun <T> compressImage(photos: List<T>) {
    ImageCompressor
        .with(this)
        .load(photos)
        .ignoreBy(100)//Byte
        .setTargetDir(getPathImageCache())
        .setFocusAlpha(false)
        .enableCache(true)
        .filter(object : ImageCompressPredicate {
            override fun apply(uri: Uri?): Boolean {
                FileLogger.i("image predicate $uri  ${getFilePathByUri(uri)}")
                return if (uri != null) {
                    val path = getFilePathByUri(uri)
                    !(TextUtils.isEmpty(path) || (path?.toLowerCase(Locale.getDefault())?.endsWith(".gif") == true))
                } else false
            }
        })
        .setRenameListener(object : OnImageRenameListener {
            override fun rename(uri: Uri?): String? {
                try {
                    val filePath = getFilePathByUri(uri)
                    val md = MessageDigest.getInstance("MD5")
                    md.update(filePath?.toByteArray() ?: return "")
                    return BigInteger(1, md.digest()).toString(32)
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                }
                return ""
            }
        })
        .setImageCompressListener(object : OnImageCompressListener {
            override fun onStart() {}
            override fun onSuccess(index: Int,uri: Uri?) {
                val path = "$cacheDir/image/"
                FileLogger.i("compress onSuccess  uri=$uri  path=${uri?.path}  压缩图片缓存目录总大小=${FileSizeUtils.getFolderSize(File(path))}")
                val bitmap = getBitmapFromUri(uri)
                dumpMetaData(uri) { displayName: String?, size: String? ->
                    runOnUiThread {
                        mTvResult.text = mTvResult.text.toString().plus(
                            "\n ---------\n👉压缩后 \n Uri : $uri \n 路径: ${uri?.path} \n 文件名称 ：$displayName \n 大小：$size B \n" +
                                    "格式化 : ${FileSizeUtils.formatFileSize(size?.toLong() ?: 0L)}\n ---------"
                        )
                    }
                }
                mIvCompressed.setImageBitmap(bitmap)
            }
            override fun onError(e: Throwable?) {
                FileLogger.e("compress onError ${e?.message}")
            }
        }).launch()
}
```

## 常用文件操作工具类

> ☘ `FileOperator`提供了`Android`开发常用的一些文件操作工具类,使用方式大多以静态方法为主,需要的同学可以直接CV需要的文件

### 1. 获取文件MimeType类型👉[FileMimeType.kt](https://github.com/javakam/FileOperator/blob/master/library/src/main/java/ando/file/core/FileMimeType.kt)

#### 根据`File Name/Path/Url`获取相应`MimeType`
```
fun getMimeType(str: String?): String {...}

fun getMimeType(uri: Uri?): String {...}

//MimeTypeMap.getSingleton().getMimeTypeFromExtension(...) 的补充
fun getMimeTypeSupplement(fileName: String): String {...}
```

### 2. 计算文件或文件夹的大小👉[FileSizeUtils.kt](https://github.com/javakam/FileOperator/blob/master/library/src/main/java/ando/file/core/FileSizeUtils.kt)
#### 获取指定`文件/文件夹`大小
```
@Throws(Exception::class)
fun getFolderSize(file: File?): Long {
    var size = 0L
    if (file == null || !file.exists()) return size
    val files = file.listFiles()
    if (files.isNullOrEmpty()) return size
    for (i in files.indices) {
        size += if (files[i].isDirectory) getFolderSize(files[i]) else getFileSize(files[i])
    }
    return size
}
```
#### 获取文件大小
```
fun getFileSize(file: File?): Long{...}

fun getFileSize(uri: Uri?): Long{...}
```
#### 自动计算指定`文件/文件夹`大小
自动计算指定文件或指定文件夹的大小 , 返回值带 B、KB、M、GB、TB 单位的字符串
```
fun getFileOrDirSizeFormatted(path: String?): String {}...}
```
#### 格式化大小(`BigDecimal`实现)
```
//scale 表示 精确到小数点以后几位
fun formatFileSize(size: Long, scale: Int): String {...}
```
转换文件大小,指定转换的类型:
```
//scale 精确到小数点以后几位
fun formatSizeByType(size: Long, scale: Int, sizeType: FileSizeType): BigDecimal =
        BigDecimal(size.toDouble()).divide(
            BigDecimal(
                when (sizeType) {
                    SIZE_TYPE_B -> 1L
                    SIZE_TYPE_KB -> 1024L
                    SIZE_TYPE_MB -> 1024L * 1024L
                    SIZE_TYPE_GB -> 1024L * 1024L * 1024L
                    SIZE_TYPE_TB -> 1024L * 1024L * 1024L * 1024L
                }
            ),
            scale,
            if (sizeType == SIZE_TYPE_B) BigDecimal.ROUND_DOWN else BigDecimal.ROUND_HALF_UP
        )
```

转换文件大小带单位:
```
fun getFormattedSizeByType(size: Long, scale: Int, sizeType: FileSizeType): String {
    return "${formatSizeByType(size, scale, sizeType).toPlainString()}${sizeType.unit}"
}
```

### 3. 直接打开Url/Uri(远程or本地)👉[FileOpener.kt](https://github.com/javakam/FileOperator/blob/master/library/src/main/java/ando/file/core/FileOpener.kt)
#### 直接打开`Url`对应的系统应用
eg: 如果url是视频地址,则直接用系统的播放器打开
```
fun openUrl(activity: Activity, url: String?) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(url), getMimeType(url))
        activity.startActivity(intent)
    } catch (e: Exception) {
        FileLogger.e("openUrl error : " + e.message)
    }
}
```
#### 根据 文件路径 和 类型(后缀判断) 显示支持该格式的程序
```
fun openFileBySystemChooser(context: Any, uri: Uri?, mimeType: String? = null) =
    uri?.let { u ->
        Intent.createChooser(createOpenFileIntent(u, mimeType), "选择程序")?.let {
            startActivity(context, it)
        }
    }
```
#### 选择文件【调用系统的文件管理】
```
fun createChooseIntent(mimeType: String?, mimeTypes: Array<String>?, multiSelect: Boolean): Intent =
    // Implicitly allow the user to select a particular kind of data. Same as : Intent.ACTION_GET_CONTENT
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiSelect)
        // The MIME data type filter
        //intent.setType("image/*");    //选择图片
        //intent.setType("audio/*");    //选择音频
        //intent.setType("video/*");    //选择视频 （mp4 3gp 是 android支持的视频格式）
        //intent.setType("file/*");     //比 */* 少了一些侧边栏选项
        //intent.setType("video/*;image/*");//错误方式;同时选择视频和图片 ->  https://www.jianshu.com/p/e98c97669af0
        if (mimeType.isNullOrBlank() && mimeTypes.isNullOrEmpty()) type = "*/*"
        else {
            type = if (mimeType.isNullOrEmpty()) "*/*" else mimeType
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        // Only return URIs that can be opened with ContentResolver
        addCategory(Intent.CATEGORY_OPENABLE)
    }
```
> 注: 
<br>&nbsp;&nbsp;&nbsp;&nbsp;1.Intent.setType 不能为空!
<br>&nbsp;&nbsp;&nbsp;&nbsp;2.mimeTypes 会覆盖 mimeType
<br>&nbsp;&nbsp;&nbsp;&nbsp;3.ACTION_GET_CONTENT , ACTION_OPEN_DOCUMENT 效果相同
<br>&nbsp;&nbsp;&nbsp;&nbsp;4.开启多选 resultCode=-1

### 4. 获取文件Uri/Path👉[FileUri.kt](https://github.com/javakam/FileOperator/blob/master/library/src/main/java/ando/file/core/FileUri.kt)

#### 从`File`路径中获取`Uri`

```
fun getUriByPath(path: String?): Uri? = if (path.isNullOrBlank()) null else getUriByFile(File(path))

fun getUriByFile(file: File?): Uri? {
    if (file == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val authority = FileOperator.getContext().packageName + PATH_SUFFIX
        FileProvider.getUriForFile(FileOperator.getContext(), authority, file)
    } else {
        Uri.fromFile(file)
    }
}
```

#### 获取`Uri`对应的文件路径,兼容`API 26`

```
fun getFilePathByUri(context: Context?, uri: Uri?): String? {
    if (context == null || uri == null) return null
    val scheme = uri.scheme
    // 以 file:// 开头的使用第三方应用打开
    if (ContentResolver.SCHEME_FILE.equals(scheme, ignoreCase = true)) return uri.path
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //4.4以后
        getPath(context, uri)
    } else { //4.4以下
        getPathKitkat(context, uri)
    }
}
```

### 5. 通用文件工具类👉[FileUtils.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library/src/main/java/com/ando/file/common/FileUtils.kt)
- getExtension 获取文件后缀 `jpg`
- getExtensionFull 获取文件后缀 `.jpg`
- getExtensionFromUri(uri: Uri?) 获取文件后缀 
- deleteFile 删除文件或目录
- deleteFilesButDir(file: File?, vararg excludeDirs: String?) 删除文件或目录 , `excludeDirs` 跳过指定名称的一些`目录/文件`
- deleteFileDir 只删除文件，不删除文件夹
- readFileText 读取文本文件中的内容 `String`
- readFileBytes 读取文本文件中的内容 `ByteArray`
- copyFile 根据文件路径拷贝文件 `java.nio`

```
eg :boolean copyFile = FileUtils.copyFile(fileOld, "/test_" + i, getExternalFilesDir(null).getPath());
File fileNew =new File( getExternalFilesDir(null).getPath() +"/"+ "test_" + i);
```
- write2File(bitmap: Bitmap, fileName: String?)
- write2File(input: InputStream?, filePath: String?)
- isLocal 检验是否为本地URI
- isGif 检验是否为 gif

## 注意

1. `onActivityResult` 中要把选择文件的结果交给`FileSelector`处理 :

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    mFileSelector?.obtainResult(requestCode, resultCode, data)
}
```

2. 选择文件不满足预设条件时,有两种策略 : 

    - 1.当设置总文件大小限制时,有两种策略 OVER_SIZE_LIMIT_ALL_EXCEPT 只要有一个文件超出直接返回 onError

    - 2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART 去掉超过数量限制和大小限制的溢出部分的文件

3. 选择文件数据:单选 Intent.getData ; 多选  Intent.getClipData


4. Android 系统问题 : Intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
开启多选条件下只选择一个文件时,系统是按照单选逻辑走的... Σ( ° △ °|||)︴

5. 回调处理

多选模式下,建议使用统一的 CallBack 回调;<br>
单选模式下,如果配置了自定义的 CallBack , 则优先使用该回调;否则使用统一的 CallBack

## 未来任务
```
1.做一个自定义UI的文件管理器
2.增加Fragment使用案例 , 视频压缩-郭笑醒 , 清除缓存功能  , 外置存储适配
3.整理更详细的文档 配合 `com.liulishuo.okdownload` 做文件下载 👉 `library_file_downloader`
4.
```
---

## v1.0.2
1.加入文件不匹配时的判断;
2.开启多选: FileSelector.setSelectMode(true) 改为 setMultiSelect() , 默认为单选模式
3.
```
W/ExifInterface: Invalid image: ExifInterface got an unsupported image format
    file(ExifInterface supports JPEG and some RAW image formats only) or a corrupted JPEG file to ExifInterface.
     java.io.IOException: Invalid byte order: 0
         at android.media.ExifInterface.readByteOrder(ExifInterface.java:3134)
         at android.media.ExifInterface.isOrfFormat(ExifInterface.java:2449)
         at android.media.ExifInterface.getMimeType(ExifInterface.java:2327)
         at android.media.ExifInterface.loadAttributes(ExifInterface.java:1755)
         at android.media.ExifInterface.<init>(ExifInterface.java:1449)
      ...

Fixed :
    compileOnly "androidx.exifinterface:exifinterface:1.3.2"
    replace `android.media.ExifInterface` with `androidx.exifinterface.media.ExifInterface`
```
4.增加清理压缩图片缓存方法
5.单选 setMinCount 提示问题
6.修改`FileSizeUtils.kt`算法
7.FileSelectResult 加入MimeType
```
Caused by: android.graphics.ImageDecoder$DecodeException: Failed to create image decoder with message 'unimplemented'Input contained an error.
```
[What is new in Android P — ImageDecoder & AnimatedImageDrawable](https://medium.com/appnroll-publication/what-is-new-in-android-p-imagedecoder-animatedimagedrawable-a65744bec7c1)

8.
```
java.lang.SecurityException: UID 10483 does not have permission to content://com.android.providers.media.documents/document/image%3A16012 [user 0];
you could obtain access using ACTION_OPEN_DOCUMENT or related APIs
```
Fixed: `ando.file.core.FileOpener.createChooseIntent`
```kotlin
把 Intent(Intent.ACTION_GET_CONTENT) 改为 Intent(Intent.ACTION_OPEN_DOCUMENT)
```
9.多选图片改为列表展示
10.增加数量限制, 最小数量处理,minCount最小为0,maxCount最小为1,minCount必须小于maxCount
11.

## 参考

- Google

1. [Storage Samples Repository](https://github.com/android/storage-samples)

2. [SAF 使用存储访问框架打开文件](https://developer.android.google.cn/guide/topics/providers/document-provider)

3. [SAF API UseCase](https://developer.android.google.cn/training/data-storage/shared/documents-files)


[管理分区外部存储访问](https://developer.android.google.cn/training/data-storage/files/external-scoped)
[管理分区外部存储访问 - 如何从原生代码访问媒体文件 & MediaStore增删该查API](https://developer.android.google.cn/training/data-storage/shared/media)

[处理外部存储中的媒体文件](https://developer.android.google.cn/training/data-storage/files/media)

[Android 11 中的隐私权](https://developer.android.google.cn/preview/privacy)

[Android 10 中的隐私权](https://developer.android.google.cn/about/versions/10/privacy/changes#scoped-storage)

- Other

[github/scoped_storage_sample](https://github.com/songlongGithub/scoped_storage_sample)

[掘金-Android Q 适配指南 让你少走一堆弯路](https://juejin.im/post/5cad5b7ce51d456e5a0728b0)

[Android Q 沙箱适配多媒体文件总结](https://segmentfault.com/a/1190000019224425)

[oppo AndroidQ适配指导](https://open.oppomobile.com/wiki/doc#id=10432)

[huawei Google Q版本应用兼容性整改指导](https://developer.huawei.com/consumer/cn/doc/50127)

- 参考项目

[MaterialFiles](https://github.com/zhanghai/MaterialFiles)

[Shelter](https://github.com/PeterCxy/Shelter)

[FileUtils](https://github.com/coltoscosmin/FileUtils/blob/master/FileUtils.java)

[cloud-player-android-sdk](https://github.com/codeages/cloud-player-android-sdk/blob/master/app/src/main/java/com/edusoho/playerdemo/util/FileUtils.java)

## library_file_downloader

> 项目基于 [OkDownload](https://github.com/lingochamp/okdownload) 实现

- 断点异常的BUG <https://github.com/lingochamp/okdownload/issues/39>

- 中文文档 <https://github.com/lingochamp/okdownload/blob/master/README-zh.md>

- Simple <https://github.com/lingochamp/okdownload/wiki/Simple-Use-Guideline>

- Advanced <https://github.com/lingochamp/okdownload/wiki/Advanced-Use-Guideline>

- AndroidFilePicker <https://github.com/rosuH/AndroidFilePicker/blob/master/README_CN.md>

- FilePicker <https://github.com/chsmy/FilePicker>

## bintrayUpload
[novoda](https://github.com/novoda/bintray-release)

`gradlew clean build bintrayUpload -PbintrayUser=javakam -PbintrayKey=xxx -PdryRun=false`
