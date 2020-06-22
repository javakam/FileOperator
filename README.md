# FileOperator

- 🔥更简单的处理Android系统文件操作
- 🔥适用于 Android 4.4 及以上系统 , 兼容AndroidQ新的存储策略

<a href='https://bintray.com/javakam/maven/FileOperator?source=watch' alt='Get automatic notifications about new "FileOperator" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a><a href='https://bintray.com/javakam/maven/FileOperator?source=watch' alt='Get automatic notifications about new "FileOperator" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>

<a href='https://bintray.com/javakam/maven/FileOperator/_latestVersion'><img src='https://api.bintray.com/packages/javakam/maven/FileOperator/images/download.svg'></a>

- Gradle:

```
implementation 'com.ando.file:FileOperator:0.8.0'
```

## 选择文件功能
## 打开文件功能
## SAF API 封装
## MediaStore 封装
## ...

### 参考

- Google

> 推荐👉

1.[Storage Samples Repository](https://github.com/android/storage-samples)

2.[SAF 使用存储访问框架打开文件](https://developer.android.google.cn/guide/topics/providers/document-provider
    - [SAF API UseCase](https://developer.android.google.cn/training/data-storage/shared/documents-files)


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

### library_file_downloader

> 项目基于 [OkDownload](https://github.com/lingochamp/okdownload) 实现

- 断点异常的BUG <https://github.com/lingochamp/okdownload/issues/39>

- 中文文档 <https://github.com/lingochamp/okdownload/blob/master/README-zh.md>

- Simple <https://github.com/lingochamp/okdownload/wiki/Simple-Use-Guideline>

- Advanced <https://github.com/lingochamp/okdownload/wiki/Advanced-Use-Guideline>

- AndroidFilePicker <https://github.com/rosuH/AndroidFilePicker/blob/master/README_CN.md>

- FilePicker <https://github.com/chsmy/FilePicker>


### 任务
```

### 优先级 自定义FileSelectOptions > 统一的 CallBack

###
1.当设置总文件大小限制时,有两种策略 
    OVER_SIZE_LIMIT_ALL_DONT 只要有一个文件超出直接返回 onErroe null ;  OVER_SIZE_LIMIT_EXCEPT_OVERFLOW_PART 去掉超过限制大小后面相同类型文件
2.图片/音频/视频 同时选择
3.简单的ui模板
4.Fragment使用案例 , 视频压缩-郭笑醒 , 清除缓存功能  , 外置存储适配
5.整理文档

## 注意的点
1.单选 Intent.getData ; 多选  Intent.getClipData
2.Android 系统问题 : Intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
开启多选条件下只选择一个文件时,需要安装单选逻辑走... Σ( ° △ °|||)︴

## 回调处理
多选模式下,建议使用 统一的 CallBack ,如果配置了自定义的 CallBack , 则会根据文件类型分开回调 , 并且统一的 CallBack 也会回调;
单选模式下,如果配置了自定义的 CallBack , 则优先使用该回调;否则使用统一的 CallBack

```

```
Fixed BUGS:
     Caused by: java.lang.IllegalArgumentException: Unknown URI: content://downloads/public_downloads/1
     Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,null);
Fixed : https://github.com/flutter/flutter/issues/21863
```
