# 更新日志(Update log)

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

## v1.3.2

```
1.修复`FileOpener.createChooseIntent`问题
2.更新`FileUtils`并上传相应的用法示例
3.优化了一些方法
```

## v1.3.5

```
1.重要: 增加自定义FileType
2.移除AppSpecific(沙盒)演示Demo AppSpecificActivity,因为沙盒目录(AppSpecific)操作直接沿用旧的 File API操作,
    所以直接可以用 ando.file.core.FileUtils 替代,详见: FileUtilsActivity
3.FileOpener.openFileBySystemChooser 改名为 FileOpener.openFile, 语义更明确
4.如果筛选`txt`文本文件,`MimeType`建议设置为`text/*`相较于`text/plain`在系统文件管理器页面多一个`文档`字样的筛选更好一些,
  eg: setMimeTypes("audio/*", "image/*", "text/*")
5.修复访问`Public`目录路径异常问题
```

## v1.3.6

```
1.internationalization
2.Fragment使用方式和案例 (Fragments using methods and cases) #13
```

## v1.3.7

```
1.修复 Android 11 文件类型不匹配问题 (Fix Android 11 "File type mismatch" problem)
```

## v1.3.8

```
1.移除 FileUri 中的复制文件获取路径的方案
2.app中加入上传案例
```

## v1.4.+

```
1.移除 bintray, jcenter, 改用 MavenCentral
2.移除Java使用案例 sample_java
```

## v1.5.0

```
1.修改FileUri.kt文件
2.升级gradle插件
```

## v1.6.0

```
1.修复Android Q上路径获取问题
2.修改并增加注释
```

## v1.6.2

```
Modify FileGlobal.giveUriPermission & use
```

## v1.7.0

```
1.FileUtils 加入获取拍摄时间,打印媒体信息,检查Uri,ByteArray写入文件方法以及用例
2.Android 文件系统会显示一些不存在的文件, 但是仍对应有Uri并且可以选取, 不过大小为0, 当我们把这个Uri当做正常文件处理时候, 会报错:
Caused by: java.io.FileNotFoundException: open failed: ENOENT (No such file or directory)
解决方式是使用 try..catch 进行异常捕获, 保证程序正常运行
```