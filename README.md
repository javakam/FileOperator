> **上一篇** 👉 [Android Q & Android 11存储适配(一) 基础知识点梳理](https://juejin.im/post/6854573214447140871)

# [FileOperator](https://github.com/javakam/FileOperator)

> `Android`文件操作库。适用于`Android 4.4`及以上系统, 已兼容`AndroidQ`和`Android11`新的存储策略。包括处理`Android`端文件目录及缓存、文件MimeType、文件打开方式、文件路径和Uri、文件大小、文件常用工具类以及文件选择处理等功能。

## 使用(Usage)
##### 1. 依赖(dependencies)
`mavenCentral` -> Project `build.gradle`

```groovy
repositories {
   mavenCentral()
}

implementation 'com.github.javakam:file.core:1.6.1@aar'      //核心库必选(Core library required)
implementation 'com.github.javakam:file.selector:1.6.1@aar'  //文件选择器(File selector)
implementation 'com.github.javakam:file.compressor:1.6.1@aar'//图片压缩, 核心算法为Luban
implementation 'com.github.javakam:file.android-q:1.6.1@aar' //AndroidQ兼容库,需要: 'androidx.documentfile:documentfile:1.0.1'
```

##### 2. `Application`中初始化(Initialization in Application)
```kotlin
FileOperator.init(this,BuildConfig.DEBUG)
```

##### 3. 混淆(Proguard)

> 未用到反射, 不需要混淆。(No reflection is used, no need to be confused.)

## 预览(Preview)

| 功能列表(Function list) | 缓存目录(Cache directory) |
|:---:|:---:|
| <img src="https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/func.png" width="290" height="600"/> | <img src="https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/cache.png" width="290" height="600"/> |

### 文件选择(File selection)

| 单图+压缩(Single Image+Compress) | 多图+压缩(Multiple images+Compress) | 多文件+多类型(Multiple files+Multiple types) |
|:---:|:---:|:---:|
|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/pick1.png)|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/pick2.png)|![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/pick3.png)|

## 用法(Usage)

### 一、常用文件操作(Common file operations)

> ☘ `FileOperator`提供了`Android`开发常用的一些文件操作工具类,使用方式大多以静态方法为主,需要的同学可以直接CV需要的文件

#### 1. 获取文件MimeType类型👉[FileMimeType.kt](https://github.com/javakam/FileOperator/blob/master/library_core/src/main/java/ando/file/core/FileMimeType.kt)

##### 根据`File Name/Path/Url`获取相应`MimeType`
```kotlin
fun getMimeType(str: String?): String {...}

fun getMimeType(uri: Uri?): String {...}

//MimeTypeMap.getSingleton().getMimeTypeFromExtension(...) 的补充
fun getMimeTypeSupplement(fileName: String): String {...}
```

#### 2. 计算文件或文件夹的大小👉[FileSizeUtils.kt](https://github.com/javakam/FileOperator/blob/master/library_core/src/main/java/ando/file/core/FileSizeUtils.kt)
##### 获取指定`文件/文件夹`大小(Get the size of the specified `file folder`)

```kotlin
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
##### 获取文件大小(Get file size)
```kotlin
fun getFileSize(file: File?): Long{...}

fun getFileSize(uri: Uri?): Long{...}
```
##### 自动计算指定`文件/文件夹`大小(Automatically calculate the size of the specified `file folder`)
自动计算指定文件或指定文件夹的大小 , 返回值带 B、KB、M、GB、TB 单位的字符串

```kotlin
fun getFileOrDirSizeFormatted(path: String?): String {}...}
```
##### 格式化大小(`BigDecimal`实现)
Format size (implemented by `Big Decimal`)

```kotlin
/**
 * @param scale 精确到小数点以后几位 (Accurate to a few decimal places)
 */
fun formatFileSize(size: Long, scale: Int, withUnit: Boolean = false): String {
    val divisor = 1024L
    //ROUND_DOWN 1023 -> 1023B ; ROUND_HALF_UP  1023 -> 1KB
    val kiloByte: BigDecimal = formatSizeByTypeWithDivisor(BigDecimal.valueOf(size), scale, SIZE_TYPE_B, divisor)
    if (kiloByte.toDouble() < 1) {
        return "${kiloByte.toPlainString()}${if (withUnit) SIZE_TYPE_B.unit else ""}"
    }
    //KB
    val megaByte = formatSizeByTypeWithDivisor(kiloByte, scale, SIZE_TYPE_KB, divisor)
    if (megaByte.toDouble() < 1) {
        return "${kiloByte.toPlainString()}${if (withUnit) SIZE_TYPE_KB.unit else ""}"
    }
    //M
    val gigaByte = formatSizeByTypeWithDivisor(megaByte, scale, SIZE_TYPE_MB, divisor)
    if (gigaByte.toDouble() < 1) {
        return "${megaByte.toPlainString()}${if (withUnit) SIZE_TYPE_MB.unit else ""}"
    }
    //GB
    val teraBytes = formatSizeByTypeWithDivisor(gigaByte, scale, SIZE_TYPE_GB, divisor)
    if (teraBytes.toDouble() < 1) {
        return "${gigaByte.toPlainString()}${if (withUnit) SIZE_TYPE_GB.unit else ""}"
    }
    //TB
    return "${teraBytes.toPlainString()}${if (withUnit) SIZE_TYPE_TB.unit else ""}"
}
```
转换文件大小,指定转换的类型(Convert file size, specify the type of conversion):

```kotlin
//scale 精确到小数点以后几位
fun formatSizeByTypeWithoutUnit(size: BigDecimal, scale: Int, sizeType: FileSizeType): BigDecimal =
    size.divide(
        BigDecimal.valueOf(when (sizeType) {
            SIZE_TYPE_B -> 1L
            SIZE_TYPE_KB -> 1024L
            SIZE_TYPE_MB -> 1024L * 1024L
            SIZE_TYPE_GB -> 1024L * 1024L * 1024L
            SIZE_TYPE_TB -> 1024L * 1024L * 1024L * 1024L
        }),
        scale,
        //ROUND_DOWN 1023 -> 1023B ; ROUND_HALF_UP  1023 -> 1KB
        if (sizeType == SIZE_TYPE_B) BigDecimal.ROUND_DOWN else BigDecimal.ROUND_HALF_UP
    )
```

转换文件大小带单位(Convert file size with unit):
```kotlin
fun formatSizeByTypeWithUnit(size: Long, scale: Int, sizeType: FileSizeType): String {
    return "${formatSizeByTypeWithoutUnit(size.toBigDecimal(), scale, sizeType).toPlainString()}${sizeType.unit}"
}
```

#### 3. 直接打开Url/Uri(远程or本地)👉[FileOpener.kt](https://github.com/javakam/FileOperator/blob/master/library_core/src/main/java/ando/file/core/FileOpener.kt)
##### 打开系统分享弹窗(Open the system sharing popup)
```kotlin
fun openShare(context: Context, uri: Uri, title: String = "分享文件") {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    // Put the Uri and MIME type in the result Intent
    intent.setDataAndType(uri, getMimeType(uri))
    context.startActivity(Intent.createChooser(intent, title))
}
```

##### 打开浏览器(Open browser)
```kotlin
@SuppressLint("QueryPermissionsNeeded")
fun openBrowser(
    context: Context, url: String, title: String = "请选择浏览器", newTask: Boolean = false,
    block: ((result: Boolean, msg: String?) -> Unit)? = null,
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        //startActivity(intent)
        //https://developer.android.com/about/versions/11/privacy/package-visibility
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(intent, title))
            block?.invoke(true, null)
        } else {
            block?.invoke(true, "没有可用浏览器")
        }
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        block?.invoke(true, e.toString())
    }
}
```

##### 直接打开`Url`对应的系统应用
Directly open the system application corresponding to `Url`

eg: 如果url是视频地址, 系统会直接用内置的视频播放器打开

```kotlin
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

##### 根据`文件路径`和`类型(后缀判断)`显示支持该格式的程序
According to `file path` and `type (judgment by suffix)` show programs that support the format

```kotlin
fun openFile(context: Any, uri: Uri?, mimeType: String? = null) =
    uri?.let { u ->
        Intent.createChooser(createOpenFileIntent(u, mimeType), "选择程序")?.let {
            startActivity(context, it)
        }
    }
```

#### 4. 获取文件Uri/Path👉[FileUri.kt](https://github.com/javakam/FileOperator/blob/master/library_core/src/main/java/ando/file/core/FileUri.kt)

##### 从`File`路径中获取`Uri`
Obtain `Uri` from `File` path

```kotlin
fun getUriByPath(path: String?): Uri? = if (path.isNullOrBlank()) null else getUriByFile(File(path))

fun getUriByFile(file: File?): Uri? =
    file?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = FileOperator.getContext().packageName + PATH_SUFFIX
            FileProvider.getUriForFile(FileOperator.getContext(), authority, file)
        } else Uri.fromFile(file)
    }
```

##### 获取`Uri`对应的文件路径,兼容`API 26`
Get the file path corresponding to `Uri`, compatible with `API 26`

```kotlin
fun getFilePathByUri(context: Context?, uri: Uri?): String? {
    if (context == null || uri == null) return null
    val scheme = uri.scheme
    // 以 file:// 开头的使用第三方应用打开
    if (ContentResolver.SCHEME_FILE.equals(scheme, ignoreCase = true)) return uri.path
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) getPath(context, uri) else getPathKitkat(context, uri)
}
```

#### 5. 通用文件工具类👉[FileUtils.kt](https://github.com/javakam/FileOperator/blob/master/library_core/src/main/java/ando/file/core//FileUtils.kt)

Method | Remark
:-|:-
`getExtension` | 获取文件后缀`jpg`
`getExtensionFull` | 获取文件完整后缀`.jpg`
`splitFilePath()` | 拆分文件路径 eg: `/xxx/xxx/note.txt` 👉 `path`: `/xxx/xxx`(注:尾部没有`/`)  `name`: note `suffix`: txt
`getFileNameFromPath(path: String?)` | 通过`FilePath`获取文件名
`getFileNameFromUri(uri: Uri?)` | 通过`Uri`获取文件名
`createFile(filePath: String?, fileName: String?, overwrite: Boolean = false):File?` | 创建文件, 同名文件创建多次会跳过已有创建新的文件,如:note.txt已存在,则再次创建会生成note(1).txt
`createDirectory(filePath: String?): Boolean` | 创建目录
`deleteFile` | 删除文件或目录
`deleteFileWithoutExcludeNames(file: File?, vararg excludeDirs: String?)` | 删除文件或目录, `excludeDirs` 指定名称的一些`文件/文件夹`不做删除
`deleteFilesNotDir` | 只删除文件，不删除文件夹
`readFileText(InputStream/Uri): String?` | 读取文本文件中的内容
`readFileBytes(InputStream/Uri): ByteArray?` | 读取文件中的内容并返回`ByteArray`
`copyFile` | 根据文件路径拷贝文件 `java.nio`
`write2File(bitmap:Bitmap, file:File?, overwrite:Boolean=false)` | 把`Bitmap`写到文件中,可通过`BitmapFactory.decodeStream()`读取出来
`write2File(input:InputStream?, file:File?, overwrite:Boolean=false)` | 向文件中写入数据
`isLocal` | 检验是否为本地URI
`isGif()` | 检验是否为 gif

> `copyFile`效率和`kotlin-stdlib-1.4.21.jar`中的`kotlin.io.FilesKt__UtilsKt.copyTo`基本相当 :

```kotlin
fun File.copyTo(target: File, overwrite: Boolean = false,bufferSize: Int = DEFAULT_BUFFER_SIZE): File
```
Usage:
```kotlin
boolean copyResult = FileUtils.copyFile(fileOld, getExternalFilesDir(null).getPath(), "test.txt");
File targetFile = new File(getExternalFilesDir(null).getPath() + "/" + "test.txt");
```

### 二、选择文件(Select File)
> implementation 'com.github.javakam:file.selector:x.x.x@aar'  //文件选择器(File selector)

#### 1. 单选图片(Single selection picture)

```kotlin
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    fileTypeMismatchTip = "文件类型不匹配 !" //File type mismatch
    singleFileMaxSize = 5242880
    singleFileMaxSizeTip = "图片最大不超过5M !" //The largest picture does not exceed 5M
    allFilesMaxSize = 10485760
    allFilesMaxSizeTip = "总图片大小不超过10M !"//The total picture size does not exceed 10M  注:单选条件下无效,只做单个图片大小判断
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setTypeMismatchTip("文件类型不匹配 !") //File type mismatch
    .setMinCount(1, "至少选择一个文件 !") //Choose at least one file
    .setMaxCount(10, "最多选择十个文件 !") //Choose up to ten files  注:单选条件下无效, 只做最少数量判断
    .setOverLimitStrategy(OVER_LIMIT_EXCEPT_OVERFLOW)
    .setSingleFileMaxSize(1048576, "大小不能超过1M !") //The size cannot exceed 1M  注:单选条件下无效, FileSelectOptions.singleFileMaxSize
    .setAllFilesMaxSize(10485760, "总大小不能超过10M !") //The total size cannot exceed 10M 注:单选条件下无效,只做单个图片大小判断 setSingleFileMaxSize
    .setMimeTypes("image/*") //默认不做文件类型约束为"*/*",不同类型系统提供的选择UI不一样 eg:"video/*","audio/*","image/*"
    .applyOptions(optionsImage)
    .filter(object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return when (fileType) {
                FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                FileType.VIDEO -> false
                FileType.AUDIO -> false
                else -> false
            }
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            ResultUtils.resetUI(mTvResult)
            if (results.isNullOrEmpty()) {
                toastLong("没有选取文件") //No file selected
                return
            }
            showSelectResult(results)
        }
        override fun onError(e: Throwable?) {
            FileLogger.e("FileSelectCallBack onError ${e?.message}")
            ResultUtils.setErrorText(mTvError, e)
        }
    })
    .choose()
```

#### 2. 多选图片(多选+单一类型)
Multiple selection pictures (multiple selection + single type)

```kotlin
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    fileTypeMismatchTip = "文件类型不匹配 !" //File type mismatch
    singleFileMaxSize = 5242880
    singleFileMaxSizeTip = "单张图片大小不超过5M !" //The size of a single picture does not exceed 5M
    allFilesMaxSize = 10485760
    allFilesMaxSizeTip = "图片总大小不超过10M !" //The total size of the picture does not exceed 10M
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setMultiSelect()
    .setMinCount(1, "至少选择一个文件 !") //Choose at least one file
    .setMaxCount(2, "最多选两个文件!") //Choose up to two files
    .setSingleFileMaxSize(3145728, "单个大小不能超过3M !") //Single size cannot exceed 3M
    .setAllFilesMaxSize(20971520, "总文件大小不能超过20M !") //The total file size cannot exceed 20M
    .setOverLimitStrategy(this.mOverLimitStrategy)
    .setMimeTypes("image/*")
    .applyOptions(optionsImage)
    .filter(object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return (fileType == FileType.IMAGE) && (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("FileSelectCallBack onSuccess ${results?.size}")
            mAdapter.setData(null)
            if (results.isNullOrEmpty()) {
                toastLong("没有选取文件") //No file selected
                return
            }
            showSelectResult(results)
        }
        override fun onError(e: Throwable?) {
            FileLogger.e("FileSelectCallBack onError ${e?.message}")
            ResultUtils.setErrorText(mTvError, e)
            mAdapter.setData(null)
            mBtSelect.text = "$mShowText (0)"
        }
    })
    .choose()
```

#### 3. 多选文件(多选+多种类型)
Multiple files (multi-select multiple types)

> 🌴适用于处理复杂文件选择情形, 如: 选取图片、音频文件、文本文件, 其中`图片`至少选择一张, 最多选择两张, 每张图片大小不超过5M, 全部图片大小不超过10M;
 `音频文件`至少选择两个, 最多选择三个, 每个音频大小不超过20M, 全部音频大小不超过30M;
 `文本文件`至少选择一个, 最多选择两个, 每个文本文件大小不超过5M, 全部文本文件大小不超过10M

> 🌴It is suitable for processing complex file selection situations, such as: select pictures, audio files, text files, among which, select at least one picture and two at most. The size of each picture does not exceed 5M, and the size of all pictures does not exceed 10M; `audio File `Choose at least two and a maximum of three, each audio size does not exceed 20M, all audio size does not exceed 30M; `text file` select at least one, select at most two, each text file size does not exceed 5M, all The text file size does not exceed 10M

```kotlin
//图片 Image
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    minCount = 1
    maxCount = 2
    minCountTip = "至少选择一张图片" //Select at least one picture
    maxCountTip = "最多选择两张图片" //Select up to two pictures
    singleFileMaxSize = 5242880
    singleFileMaxSizeTip = "单张图片最大不超过5M !" //A single picture does not exceed 5M !
    allFilesMaxSize = 10485760
    allFilesMaxSizeTip = "图片总大小不超过10M !" //The total size of the picture does not exceed 10M !
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return (fileType == FileType.IMAGE && uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
//音频 Audio
val optionsAudio = FileSelectOptions().apply {
    fileType = FileType.AUDIO
    minCount = 2
    maxCount = 3
    minCountTip = "至少选择两个音频文件" //Select at least two audio files
    maxCountTip = "最多选择三个音频文件" //Select up to three audio files
    singleFileMaxSize = 20971520
    singleFileMaxSizeTip = "单音频最大不超过20M !" //Maximum single audio does not exceed 20M !
    allFilesMaxSize = 31457280
    allFilesMaxSizeTip = "音频总大小不超过30M !" //The total audio size does not exceed 30M !
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return (uri != null)
        }
    }
}
//文本文件 txt
val optionsTxt = FileSelectOptions().apply {
    fileType = FileType.TXT
    minCount = 1
    maxCount = 2
    minCountTip = "至少选择一个文本文件" //Select at least one text file
    maxCountTip = "最多选择两个文本文件" //Select at most two text files
    singleFileMaxSize = 5242880
    singleFileMaxSizeTip = "单文本文件最大不超过5M !" //The single biggest text file no more than 5M
    allFilesMaxSize = 10485760
    allFilesMaxSizeTip = "文本文件总大小不超过10M !" //Total size not more than 10M text file
    fileCondition = object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return (uri != null)
        }
    }
}
/*
 注:如果某个FileSelectOptions没通过限定条件, 则该FileSelectOptions不会返回
 eg: 采用上面的限制条件下,图片、音频、文本文件各选一个, 因为音频最小数量设定为`2`不满足设定条件则去除所有音频选择结果
    , 所以返回结果中只有图片和文本文件(限于OVER_LIMIT_EXCEPT_OVERFLOW)

 EN:
 Note: if a FileSelectOptions failed the qualification, then the FileSelectOptions will not return,
 Eg: using the restriction conditions, images, audio, text files, each choose a, because audio set the minimum amount as ` 2 ` set does not meet the conditions the choice of the results to remove all audio
    , Only pictures and text files, so return result (limited to OVER_LIMIT_EXCEPT_OVERFLOW);
 */
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setMultiSelect() //默认是单选false (The default is radio false)

    /*
    实际最少数量限制为 setMinCount 和 (optionsImage.minCount + optionsAudio.minCount +...) 中的最小值
    实际最大数量限制为 setMaxCount 和 (optionsImage.maxCount + optionsAudio.maxCount +...) 中的最大值, 所以此处的最大值限制是无效的
    EN:
    Actual minimum limit for setMinCount and (optionsImage minCount optionsAudio. MinCount... The lowest value of),
    Actual maximum limit for setMaxCount and (optionsImage maxCount optionsAudio. MaxCount... ) the maximum, so the maximum limit here is invalid;
     */
    .setMinCount(1, "设定类型文件至少选择一个!") //Select at least one set type file
    .setMaxCount(4, "最多选四个文件!") //Most alternative four files

    /*
    实际单文件大小限制为 setSingleFileMaxSize 和 (optionsImage.singleFileMaxSize + optionsAudio.singleFileMaxSize +...) 中的最小值
    实际总大小限制为 setAllFilesMaxSize 和 (optionsImage.allFilesMaxSize + optionsAudio.allFilesMaxSize +...) 中的最大值
    EN:
    Actual single file size limit for setSingleFileMaxSize and (optionsImage. SingleFileMaxSize optionsAudio. SingleFileMaxSize... The lowest value of),
    Actual total size limit for setAllFilesMaxSize and (optionsImage allFilesMaxSize optionsAudio. AllFilesMaxSize... The highest value in);
     */
    //优先使用 `自定义FileSelectOptions` 中设置的单文件大小限制, 如果没有设置则采用该值
    //EN:Prefer using ` custom FileSelectOptions ` set in single file size limit, if the value is not set is used
    .setSingleFileMaxSize(2097152, "单文件大小不能超过2M !") //The size of a single file cannot exceed 2M !
    .setAllFilesMaxSize(52428800, "总文件大小不能超过50M !") //The total file size cannot exceed 50M !

    //1. 文件超过数量限制或大小限制
    //2. 单一类型: 保留未超限制的文件并返回, 去掉后面溢出的部分; 多种类型: 保留正确的文件, 去掉错误类型的所有文件
    //EN:
    //1. Documents more than limit or size limit
    //2. Single type: keep not ultra limit file and return, get rid of the overflow part; Multiple types: keep the right file, get rid of the wrong type of all documents
    .setOverLimitStrategy(this.mOverLimitStrategy)

    //eg: ando.file.core.FileMimeType
    //默认不做文件类型约束为"*/*", 不同类型系统提供的选择UI不一样 eg: "video/*","audio/*","image/*"
    //EN:Default do not file type constraints for "/", is not the same as the choice of different types of the system to provide the UI eg: "video/"," audio/", "image/"
    .setMimeTypes("audio/*", "image/*", "text/plain")

    //如果setMimeTypes和applyOptions没对应上会出现`文件类型不匹配问题`
    //EN:If setMimeTypes and applyOptions no corresponding will appear `file type mismatch problems`
    .applyOptions(optionsImage, optionsAudio, optionsTxt)

    //优先使用 FileSelectOptions 中设置的 FileSelectCondition
    //EN:Priority in use FileSelectOptions FileSelectCondition Settings
    .filter(object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return when (fileType) {
                FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                FileType.AUDIO -> true
                FileType.TXT -> true
                else -> false
            }
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("FileSelectCallBack onSuccess ${results?.size}")
            mAdapter.setData(null)
            if (results.isNullOrEmpty()) {
                toastLong("没有选取文件") //No files selected
                return
            }
            showSelectResult(results)
        }
        override fun onError(e: Throwable?) {
            FileLogger.e("FileSelectCallBack onError ${e?.message}")
            ResultUtils.setErrorText(mTvError, e)
            mAdapter.setData(null)
            mBtSelect.text = "$mShowText (0)"
        }
    })
    .choose()
```

#### 4. 自定义FileType(Custom FileType)

##### ①扩展已有的FileType

Extend existing FileType

```kotlin
eg: 
内置(built in): TXT(mutableListOf("txt", "conf", "iml", "ini", "log", "prop", "rc"))

增加(increase): FileType.TXT.supplement("gradle","kt")
结果(result): TXT(mutableListOf("txt", "conf", "iml", "ini", "log", "prop", "rc","gradle","kt"))

移除(remove): FileType.TXT.remove("txt","ini")
结果(result): TXT(mutableListOf("conf", "iml", log", "prop", "rc"))

替换(replace): FileType.XML.replace("xxx")
调试(debugging): FileType.TXT.dump()
```

##### ②通过`IFileType`自定义文件类型

Through ` IFileType ` custom file type

> 🍎下面提供了两种实现的方式 (The following provides two ways):

```kotlin
//1.方式一
object FileTypePhp : IFileType {
    override fun fromUri(uri: Uri?): IFileType {
        return if (parseSuffix(uri).equals("php", true)) FileTypePhp else FileType.UNKNOWN
    }
}
//2.推荐方式 (Recommended way)
enum class FileTypeJson : IFileType {
    JSON;
    override fun fromUri(uri: Uri?): IFileType {
        return resolveFileMatch(uri, "json", JSON)
    }
}
```
用法(Usage) :
```kotlin
val optionsJsonFile = FileSelectOptions().apply {
    fileType = FileTypeJson.JSON
    minCount = 1
    maxCount = 2
    minCountTip = "至少选择一个JSON文件" //Choose at least one JSON file
    maxCountTip = "最多选择两个JSON文件" //Choose up to two JSON files
}

FileSelector.with(this)
    ...
    .setMimeTypes("audio/*", "image/*", "text/*", "application/json")
    .applyOptions(optionsImage, optionsAudio, optionsTxt, optionsJsonFile)
    .filter(object : FileSelectCondition {
        override fun accept(fileType: IFileType, uri: Uri?): Boolean {
            return when (fileType) {
                FileType.IMAGE -> (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                FileType.AUDIO -> true
                FileType.TXT -> true
                FileTypeJson.JSON -> true
                else -> false
            }
        }
    })
    .choose()
```

> 注意: `json`文件无法用`text/*`打开, 对应的`mimeType`为`application/json`

### 三、压缩图片(Compress images) [ImageCompressor.kt](https://github.com/javakam/FileOperator/blob/master/library_compressor/src/main/java/ando/file/compressor/ImageCompressor.kt)

#### 1. 直接压缩不缓存(Direct compression without caching)
```kotlin
val bitmap:Bitmap=ImageCompressEngine.compressPure(uri)
```

#### 2. 压缩图片并缓存(Compress pictures and cache)
```kotlin
/**
 * 压缩图片 1.Luban算法; 2.直接压缩 -> val bitmap:Bitmap=ImageCompressEngine.compressPure(uri)
 *
 * T : String.filePath / Uri / File
 */
fun <T> compressImage(context: Context, photos: List<T>, success: (index: Int, uri: Uri?) -> Unit) {
    ImageCompressor
        .with(context)
        .load(photos)
        .ignoreBy(100)//Byte
        .setTargetDir(getCompressedImageCacheDir())
        .setFocusAlpha(false)
        .enableCache(true)
        .filter(object : ImageCompressPredicate {
            override fun apply(uri: Uri?): Boolean {
                //FileLogger.i("compressImage predicate $uri  ${FileUri.getFilePathByUri(uri)}")
                return if (uri != null) !FileUtils.getExtension(uri).endsWith("gif") else false
            }
        })
        .setRenameListener(object : OnImageRenameListener {
            override fun rename(uri: Uri?): String {
                try {
                    val filePath = FileUri.getFilePathByUri(uri)
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
            override fun onSuccess(index: Int, uri: Uri?) {
                success.invoke(index, uri)
            }

            override fun onError(e: Throwable?) {
                FileLogger.e("OnImageCompressListener onError ${e?.message}")
            }
        }).launch()
}
```

## 总结(Summary)

1. `onActivityResult` 中要把选择文件的结果交给`FileSelector`处理 :

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    //选择结果交给 FileSelector 处理, 可通过`requestCode -> REQUEST_CHOOSE_FILE`进行区分
    mFileSelector?.obtainResult(requestCode, resultCode, data)
}
```

2. 选择文件不满足预设条件时,有两种策略 : 

    - OVER_LIMIT_EXCEPT_ALL 文件超过`数量或大小`限制直接返回失败, 回调 onError

    - OVER_LIMIT_EXCEPT_OVERFLOW ① 文件超过数量限制或大小限制;
    ② 单一类型: 保留未超限制的文件并返回, 去掉后面溢出的部分; 多种类型: 保留正确的文件, 去掉错误类型的所有文件;
    ③ 回调 onSuccess

3. 选择文件数据:单选 Intent.getData ; 多选  Intent.getClipData

4. Android 系统问题 : Intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
开启多选条件下只选择一个文件时,系统是按照单选逻辑走的... Σ( ° △ °|||)︴

5. `Activity`中执行`getExternalFilesDirs(Environment.DIRECTORY_XXX)`和其它获取目录地址的方法时,都会自动创建相应的目录

![](https://raw.githubusercontent.com/javakam/FileOperator/master/screenshot/img1.png)

6. `Uri.fromFile(file)`生成的`file:///...`是不能分享的,所以需要使用`FileProvider`将`App Specific`目录下的文件分享给其他APP读写,
需要通过`FileProvider`解析出的可用于分享的路径: `ando.file.core.FileUri.getUriByFile(file)`

7. 
---

## 更新日志 (Update log)

**README_VERSIONS.md** <a href="https://github.com/javakam/FileOperator/blob/master/README_VERSIONS.md" target="_blank">https://github.com/javakam/FileOperator/blob/master/README_VERSIONS.md</a>

### Fixed Bug
#### 1.Invalid image: ExifInterface got an unsupported image format
```kotlin
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
    dependencies {
        compileOnly "androidx.exifinterface:exifinterface:1.3.2"
        ...
    }

    Then replace `android.media.ExifInterface` with `androidx.exifinterface.media.ExifInterface`
```
#### 2.ImageDecoder$DecodeException: Failed to create image decoder with message
```kotlin
Caused by: android.graphics.ImageDecoder$DecodeException:
Failed to create image decoder with message 'unimplemented'Input contained an error.
```

[What is new in Android P — ImageDecoder & AnimatedImageDrawable](https://medium.com/appnroll-publication/what-is-new-in-android-p-imagedecoder-animatedimagedrawable-a65744bec7c1)

#### 3.SecurityException... you could obtain access using ACTION_OPEN_DOCUMENT or related APIs
```kotlin
java.lang.SecurityException: UID 10483 does not have permission to
    content://com.android.providers.media.documents/document/image%3A16012 [user 0];
    you could obtain access using ACTION_OPEN_DOCUMENT or related APIs
```
> Fixed: `ando.file.core.FileOpener.createChooseIntent`
把 Intent(Intent.ACTION_GET_CONTENT) 改为 Intent(Intent.ACTION_OPEN_DOCUMENT)

#### 4.IllegalArgumentException: column '_data' does not exist

<https://stackoverflow.com/questions/42508383/illegalargumentexception-column-data-does-not-exist>

#### 5.ActivityNotFoundException: No Activity found to handle Intent
```kotlin
android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.intent.action.OPEN_DOCUMENT cat=[android.intent.category.OPENABLE] (has extras) }
at android.app.Instrumentation.checkStartActivityResult(Instrumentation.java:2105)
```
> Fixed: `ando.file.core.FileOpener.createChooseIntent`:

```kotlin
Intent.setType("image / *")
Intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio / *"))
```
#### 6.android.os.FileUriExposedException: file:///storage/emulated/0/Android/data/com.ando.file.sample/cache exposed beyond app through Intent.getData()
> Fixed: `AndroidManifest.xml`没配置`FileProvider`

#### 7.Calling startActivity() from outside of an Activity
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

### 感谢(Thanks)

#### Google

[Storage Samples Repository](https://github.com/android/storage-samples)

[SAF 使用存储访问框架打开文件](https://developer.android.google.cn/guide/topics/providers/document-provider)

[SAF API UseCase](https://developer.android.google.cn/training/data-storage/shared/documents-files)

[管理分区外部存储访问](https://developer.android.google.cn/training/data-storage/files/external-scoped)

[管理分区外部存储访问 - 如何从原生代码访问媒体文件 & MediaStore增删该查API](https://developer.android.google.cn/training/data-storage/shared/media)

[处理外部存储中的媒体文件](https://developer.android.google.cn/training/data-storage/files/media)

[Android 11 中的隐私权](https://developer.android.google.cn/preview/privacy)

[Android 10 中的隐私权](https://developer.android.google.cn/about/versions/10/privacy/changes#scoped-storage)

#### Blog

[LOGO](https://www.easyicon.net/1293281-folders_icon.html)

[FileUtils](https://github.com/coltoscosmin/FileUtils/blob/master/FileUtils.java)

[github/scoped_storage_sample](https://github.com/songlongGithub/scoped_storage_sample)

[掘金-Android Q 适配指南 让你少走一堆弯路](https://juejin.im/post/5cad5b7ce51d456e5a0728b0)

[Android Q 沙箱适配多媒体文件总结](https://segmentfault.com/a/1190000019224425)

[oppo AndroidQ适配指导](https://open.oppomobile.com/wiki/doc#id=10432)

[huawei Google Q版本应用兼容性整改指导](https://developer.huawei.com/consumer/cn/doc/50127)

## 许可(LICENSE)

    Copyright 2019 javakam, FileOperator Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
