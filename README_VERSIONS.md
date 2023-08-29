# 更新日志(Update log)

## v3.9.0 & v3.8.0
```
优化媒体信息用例, 显示图片/音频/视频文件的全部信息
更新 gradle dependencies
```

## v3.7.0
```
v3.7.0 之前不支持`minCount = 0`的限制并且在`OVER_LIMIT_EXCEPT_ALL`策略下报错返回不具体。

1. 多选模式下错误提醒具体到是哪几种类型文件选择出了问题(FileSelector.obtainResult)
2. FileType.HTML 并入 FileType.TXT
```

## v3.6.0
```
增加了一些常用功能, 获取媒体文件的创建时间,修改时间等/重命名文件,会覆盖原文件/删除过期文件(具体保质期可以自定义Long)
1. 获取文件add,modify,expires时间, getMediaShotTime(targetBucketId: Long? = null, block: (Long, Long, Long) -> Unit),返回值为 dateAdded, dateModified, dateExpires;
2. renameFile(oldFile: File, newFileDirectory: String? = null, newFileName: String, newFileNameSuffix: String? = null): File? {};
3. deleteFilesOutDate(directoryPath: String, maxFileAge: Long = 2678400000L) 移除超过指定期限(Long)的文件
```

## v3.5.0
```
1.setMimeTypes 更名为 setExtraMimeTypes , 更符合 Intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes) 的语义
2.Intent.setType 改用为 Intent.setTypeAndNormalize, 后者会将 "IMAGE/*" 转为 "image/*", 因为Android系统仅支持小写的MimeType, 并非正式的 RFC MIME
3.完善 FileOpener.createChooseIntent 注释
4.修改 FileMimeType.kt 中 MimeType 获取方式, Fixed #73 👉 https://github.com/javakam/FileOperator/issues/73
```

## v3.0.0
```
支持 ActivityResultLauncher 跳转页面, 同时兼容旧的 startActivityForResult + onActivityResult 方式
```

## v2.4.0
```
Fixed #62 👉 https://github.com/javakam/FileOperator/issues/62
```

## v2.3.0
```
1.移除`FileUri`中多于代码;
2.升级`gradle`配置, 小幅修改`README`
```

## v2.1.0 & v2.2.0
```
优化 `FileSelector`
```

## v2.0.0
```
1.移除 `library_android_q`, 改用`MediaStoreUtils`, 最后一个版本为: implementation 'com.github.javakam:file.android-q:1.9.0@aar'
2.调整部分代码结构
```

## v1.9.0
```
1.当处于"多选"+"不设置 applyOptions"情况下 FileSelectCondition.accept 不回调问题
2.优化了类型匹配算法, 会优先匹配`自定义类型`,  避免文件类型不匹配的问题
```

## v1.8.0
```
修复API30压缩图片失败的问题
```

## v1.7.0

```
1.FileUtils 加入获取拍摄时间,打印媒体信息,检查Uri,ByteArray写入文件方法以及用例
2.Android 文件系统会显示一些不存在的文件, 但是仍对应有Uri并且可以选取, 不过大小为0, 当我们把这个Uri当做正常文件处理时候, 会报错:
Caused by: java.io.FileNotFoundException: open failed: ENOENT (No such file or directory)
解决方式是使用 try..catch 进行异常捕获, 保证程序正常运行
```

## v1.6.2

```
Modify FileGlobal.giveUriPermission & use
```

## v1.6.0

```
1.修复Android Q上路径获取问题
2.修改并增加注释
```

## v1.5.0

```
1.修改FileUri.kt文件
2.升级gradle插件
```

## v1.4.+

```
1.移除 bintray, jcenter, 改用 MavenCentral
2.移除Java使用案例 sample_java
```

## v1.3.8

```
1.移除 FileUri 中的复制文件获取路径的方案
2.app中加入上传案例
```

## v1.3.7

```
1.修复 Android 11 文件类型不匹配问题 (Fix Android 11 "File type mismatch" problem)
```

## v1.3.6

```
1.internationalization
2.Fragment使用方式和案例 (Fragments using methods and cases) #13
```

## v1.3.5

```
1.重要: 增加自定义FileType
2.移除AppSpecific(沙盒)演示Demo AppSpecificActivity,因为沙盒目录(AppSpecific)操作直接沿用旧的 File API操作,
    所以直接可以用 ando.file.core.FileUtils 替代,详见: FileUtilsActivity
3.FileOpener.openFileBySystemChooser 改名为 FileOpener.openFile, 语义更明确
4.如果筛选`txt`文本文件,`MimeType`建议设置为`text/*`相较于`text/plain`在系统文件管理器页面多一个`文档`字样的筛选更好一些,
  eg: setExtraMimeTypes("audio/*", "image/*", "text/*")
5.修复访问`Public`目录路径异常问题
```

## v1.3.2

```
1.修复`FileOpener.createChooseIntent`问题
2.更新`FileUtils`并上传相应的用法示例
3.优化了一些方法
```

## v1.1.0

```
1.增加文件类型不匹配判断;
2.开启多选: FileSelector.setSelectMode(true) 改为 setMultiSelect() , 默认为单选模式
3.增加清理压缩图片缓存方法
4.单选 setMinCount 提示问题
5.修改`FileSizeUtils.kt`算法
6.FileSelectResult 加入MimeType
7.多选图片和多选文件改为RecyclerView进行结果展示
8.增加数量限制
9.增加更多注释, 重要注释为汉英双译
10.增加 LICENSE
11.修复了一些BUG
```
