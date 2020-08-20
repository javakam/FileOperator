# [FileOperator](https://github.com/javakam/FileOperator)

<a href='https://bintray.com/javakam/maven/FileOperator/_latestVersion'><img src='https://api.bintray.com/packages/javakam/maven/FileOperator/images/download.svg'></a>

- 🚀[FileOperator GitHub](https://github.com/javakam/FileOperator)
- 🚀更简单的处理Android系统文件操作
- 🚀适用于 Android 4.4 及以上系统 , 兼容AndroidQ新的存储策略
- 🚀图片压缩算法参考 [Luban](https://github.com/Curzibn/Luban)
- 🚀Kotlin 案例 👉 [app](https://github.com/javakam/FileOperator/tree/master/app) 
- 🚀Java 案例 👉 [sample_java](https://github.com/javakam/FileOperator/tree/master/sample_java)

## Gradle:

```
implementation 'com.ando.file:FileOperator:0.9.1'
```

> 然后在`Application`中初始化:

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
```
val optionsImage = FileSelectOptions()
optionsImage.fileType = FileType.IMAGE
options.mMinCount = 0
options.mMaxCount = 10
optionsImage.mSingleFileMaxSize = 2097152  // 20M = 20971520 B
optionsImage.mSingleFileMaxSizeTip = "图片最大不超过2M！"
optionsImage.mAllFilesMaxSize = 5242880  //5M 5242880 ; 20M = 20971520 B
optionsImage.mAllFilesMaxSizeTip = "总图片大小不超过5M！"
optionsImage.mFileCondition = object : FileSelectCondition {
    override fun accept(fileType: FileType, uri: Uri?): Boolean {
        return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setSelectMode(false)
    .setMinCount(1, "至少选一个文件!")
    .setMaxCount(10, "最多选十个文件!")
    .setSingleFileMaxSize(5242880, "大小不能超过5M！") //5M 5242880 ; 100M = 104857600 KB
    .setAllFilesMaxSize(10485760, "总大小不能超过10M！")//
    .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同 arrayOf("video/*","audio/*","image/*") 系统提供的选择UI不一样
    .applyOptions(optionsImage)
    //优先使用 FileOptions 中设置的 FileSelectCondition
    .filter(object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            when (fileType) {
                FileType.IMAGE -> {
                    return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
                FileType.VIDEO -> true
                FileType.AUDIO -> true
                else -> true
            }
            return true
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("回调 onSuccess ${results?.size}")
            mTvResult.text = ""
            if (results.isNullOrEmpty()) return
            shortToast("正在压缩图片...")
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
```
val optionsImage = FileSelectOptions()
optionsImage.fileType = FileType.IMAGE
options.mMinCount = 0
options.mMaxCount = 10
optionsImage.mSingleFileMaxSize = 3145728  // 20M = 20971520 B
optionsImage.mSingleFileMaxSizeTip = "单张图片最大不超过3M！"
optionsImage.mAllFilesMaxSize = 5242880  //3M 3145728 ; 5M 5242880 ; 10M 10485760 ; 20M = 20971520 B
optionsImage.mAllFilesMaxSizeTip = "图片总大小不超过5M！"
optionsImage.mFileCondition = object : FileSelectCondition {
    override fun accept(fileType: FileType, uri: Uri?): Boolean {
        return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
    }
}
mFileSelector = FileSelector
    .with(this)
    .setRequestCode(REQUEST_CHOOSE_FILE)
    .setSelectMode(true)
    .setMinCount(1, "至少选一个文件!")
    .setMaxCount(10, "最多选十个文件!")
    //优先以自定义的 optionsImage.mSingleFileMaxSize 为准5M 5242880 ; 100M = 104857600 KB
    .setSingleFileMaxSize(2097152, "大小不能超过2M！")
    .setAllFilesMaxSize(20971520, "总文件大小不能超过20M！")
    //1.OVER_SIZE_LIMIT_ALL_DONT  超过限制大小全部不返回  ;2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART  超过限制大小去掉后面相同类型文件
    .setOverSizeLimitStrategy(this.mOverSizeStrategy)
    .setMimeTypes(MIME_MEDIA)//默认全部文件, 不同 arrayOf("video/*","audio/*","image/*") 系统提供的选择UI不一样
    .applyOptions(optionsImage)
    //优先使用 FileOptions 中设置的 FileSelectCondition
    .filter(object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            when (fileType) {
                FileType.IMAGE -> {
                    return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
                FileType.VIDEO -> true
                FileType.AUDIO -> true
                else -> true
            }
            return true
        }
    })
    .callback(object : FileSelectCallBack {
        override fun onSuccess(results: List<FileSelectResult>?) {
            FileLogger.w("回调 onSuccess ${results?.size}")
            mTvResult.text = ""
            if (results.isNullOrEmpty()) return
            shortToast("正在压缩图片...")
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
> 🌴适用于处理复杂文件选择情形,如: 选取图片、视频文件,其中图片至少选择一张,最多选择两张,每张图片大小不超过3M,全部图片大小不超过5M ; 
视频文件只能选择一个, 每个视频大小不超过20M,全部视频大小不超过30M。

```
//图片
val optionsImage = FileSelectOptions().apply {
    fileType = FileType.IMAGE
    mMinCount = 1
    mMaxCount = 2
    mMinCountTip = "至少选择一张图片"
    mMaxCountTip = "最多选择两张图片"
    mSingleFileMaxSize = 3145728  // 20M = 20971520 B
    mSingleFileMaxSizeTip = "单张图片最大不超过3M！"
    mAllFilesMaxSize = 5242880  // 5M 5242880 
    mAllFilesMaxSizeTip = "图片总大小不超过5M！"
    mFileCondition = object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
        }
    }
}
//视频
val optionsVideo = FileSelectOptions().apply {
    fileType = FileType.VIDEO
    mMinCount = 1
    mMaxCount = 1
    mMinCountTip = "至少选择一个视频文件"
    mMaxCountTip = "最多选择一个视频文件"
    mSingleFileMaxSize = 20971520  // 20M = 20971520 B
    mSingleFileMaxSizeTip = "单视频最大不超过20M！"
    mAllFilesMaxSize = 31457280  //3M 3145728
    mAllFilesMaxSizeTip = "视频总大小不超过30M！"
    mFileCondition = object : FileSelectCondition {
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
    // 100M = 104857600 KB  ;80M 83886080 ;50M 52428800 ; 20M 20971520  ;5M 5242880 ;
    .setSingleFileMaxSize(2097152, "单文件大小不能超过2M！")
    .setAllFilesMaxSize(52428800, "总文件大小不能超过50M！")
    // 超过限制大小两种返回策略: 1.OVER_SIZE_LIMIT_ALL_DONT,超过限制大小全部不返回;2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART,超过限制大小去掉后面相同类型文件
    .setOverSizeLimitStrategy(OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART)
    .setMimeTypes(null)//默认为 null,*/* 即不做文件类型限定;  MIME_MEDIA 媒体文件, 不同 arrayOf("video/*","audio/*","image/*") 系统提供的选择UI不一样
    .applyOptions(optionsImage, optionsVideo)
    // 优先使用 FileOptions 中设置的 FileSelectCondition , 没有的情况下才使用通用的
    .filter(object : FileSelectCondition {
        override fun accept(fileType: FileType, uri: Uri?): Boolean {
            when (fileType) {
                FileType.IMAGE -> {
                    return (uri != null && !uri.path.isNullOrBlank() && !FileUtils.isGif(uri))
                }
                FileType.VIDEO -> true
                FileType.AUDIO -> true
                else -> true
            }
            return true
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
### 4.压缩图片 [ImageCompressor.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library_file_core/src/main/java/com/ando/file/compress/ImageCompressor.kt)
```
//T 为 String.filePath / Uri / File
fun <T> compressImage(photos: List<T>) {
    ImageCompressor
        .with(this)
        .load(photos)
        .ignoreBy(100)//B
        .setTargetDir(getPathImageCache())
        .setFocusAlpha(false)
        .enableCache(true)
        .filter(object : ImageCompressPredicate {
            override fun apply(uri: Uri?): Boolean {
                //getFilePathByUri(uri)
                FileLogger.i("image predicate $uri  ${getFilePathByUri(uri)}")
                return if (uri != null) {
                    val path = getFilePathByUri(uri)
                    !(TextUtils.isEmpty(path) || (path?.toLowerCase()
                        ?.endsWith(".gif") == true))
                } else {
                    false
                }
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
            override fun onSuccess(uri: Uri?) {
                val path = "$cacheDir/image/"
                FileLogger.i("compress onSuccess  uri=$uri  path=${uri?.path}  缓存目录总大小=${FileSizeUtils.getFolderSize(File(path))}")
              
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

## 直接使用静态方法

### 1. 获取文件MimeType类型👉[FileMimeType.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library_file_core/src/main/java/com/ando/file/common/FileMimeType.kt)

### 2. 计算文件或文件夹的大小👉[FileSizeUtils.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library_file_core/src/main/java/com/ando/file/common/FileSizeUtils.kt)

### 3. 直接打开Url/Uri(远程or本地)👉[FileOpener.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library_file_core/src/main/java/com/ando/file/common/FileOpener.kt)

### 4. 获取文件Uri/Path👉[FileUri.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library_file_core/src/main/java/com/ando/file/common/FileUri.kt)

- 从File路径中获取 Uri

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

- 获取Uri对应的文件路径,兼容API 26

```
fun getFilePathByUri(context: Context?, uri: Uri?): String? {
    if (context == null || uri == null) return null
    val scheme = uri.scheme
    // 以 file:// 开头的
    if (ContentResolver.SCHEME_FILE.equals(scheme, ignoreCase = true)) {//使用第三方应用打开
        uri.path
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //4.4以后
        getPath(context, uri)
    } else { //4.4以下
        getPathKitkat(context, uri)
    }
}
```

### 5. 通用文件工具类👉[FileUtils.kt](https://raw.githubusercontent.com/javakam/FileOperator/master/library_file_core/src/main/java/com/ando/file/common/FileUtils.kt)
- getExtension 获取文件后缀 `jpg`
- getExtensionFull 获取文件后缀 `.jpg`
- getExtensionFromUri(uri: Uri?) 获取文件后缀 
- deleteFile 删除文件或目录
- deleteFilesButDir(file: File?, vararg excludeDirs: String?) 删除文件或目录 , excludeDirs 跳过指定名称的一些`目录/文件`
- deleteFileDir 只删除文件，不删除文件夹
- readFileText 读取文本文件中的内容 `String`
- readFileBytes 读取文本文件中的内容 `ByteArray`
- copyFile 根据文件路径拷贝文件 java.nio

```
eg :boolean copyFile = FileUtils.copyFile(fileOld, "/test_" + i, getExternalFilesDir(null).getPath());
File fileNew =new File( getExternalFilesDir(null).getPath() +"/"+ "test_" + i);
```
- write2File(bitmap: Bitmap, fileName: String?)
- write2File(input: InputStream?, filePath: String?)
- isLocal 检验是否为本地URI
- isGif 检验是否为 gif

## 注意的点

1. `onActivityResult` 中要把选择文件的结果交给`FileSelector`处理`mFileSelector?.obtainResult(requestCode, resultCode, data)` 

2. 选择文件不满足预设条件时,有两种策略 : 

    - 1.当设置总文件大小限制时,有两种策略 OVER_SIZE_LIMIT_ALL_DONT 只要有一个文件超出直接返回 onError  

    - 2.OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART 去掉超过限制大小的溢出部分的文件

3. 选择文件数据:单选 Intent.getData ; 多选  Intent.getClipData


4. Android 系统问题 : Intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
开启多选条件下只选择一个文件时,需要安装单选逻辑走... Σ( ° △ °|||)︴

5. 回调处理

多选模式下,建议使用统一的 CallBack 回调;<br>
单选模式下,如果配置了自定义的 CallBack , 则优先使用该回调;否则使用统一的 CallBack

## 未来任务
```
1.做一个自定义UI的文件管理器
2.增加Fragment使用案例 , 视频压缩-郭笑醒 , 清除缓存功能  , 外置存储适配
3.整理更详细的文档 配合 com.liulishuo.okdownload 做文件下载 👉 library_file_downloader
4.
```
---

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
[novoda](ttps://github.com/novoda/bintray-release)

`gradlew clean build bintrayUpload -PbintrayUser=javakam -PbintrayKey=xxx -PdryRun=false`
