package com.ando.sample.java;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import ando.file.compressor.ImageCompressPredicate;
import ando.file.compressor.ImageCompressor;
import ando.file.compressor.OnImageCompressListener;
import ando.file.compressor.OnImageRenameListener;
import ando.file.core.FileLogger;
import ando.file.core.FileSizeUtils;
import ando.file.core.FileType;
import ando.file.core.FileUtils;
import ando.file.operator.FileSelectCallBack;
import ando.file.operator.FileSelectCondition;
import ando.file.operator.FileSelectOptions;
import ando.file.operator.FileSelectResult;
import ando.file.operator.FileSelector;

import static ando.file.androidq.FileOperatorQKt.getBitmapFromUri;
import static ando.file.androidq.FileOperatorQKt.loadThumbnail;
import static ando.file.core.FileUriKt.getFilePathByUri;

/**
 * Title:MainActivity
 * <p>
 * Description:
 * </p>
 *
 * @author javakam
 * @date 2020/8/6 16:42
 */
public class MainActivity extends Activity {

    private static final int REQUEST_CHOOSE_FILE = 10;
    private FileSelector mFileSelector;
    //
    private TextView mTvResult, mTvResultError;
    private ImageView mIvOrigin, mIvCompressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 21);

        setContentView(R.layout.activity_main);

        setTitle("单选图片");

        mTvResult = findViewById(R.id.mTvResult);
        mTvResultError = findViewById(R.id.mTvResultError);
        mIvOrigin = findViewById(R.id.mIvOrigin);
        mIvCompressed = findViewById(R.id.mIvCompressed);
        findViewById(R.id.mBtChooseSingle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mFileSelector != null) {
            mTvResultError.setText("");
            mTvResult.setText("");
            mIvOrigin.setImageBitmap(null);
            mIvCompressed.setImageBitmap(null);

            mFileSelector.obtainResult(requestCode, resultCode, data);
        }
    }

    private void chooseFile() {

        FileSelectOptions options = new FileSelectOptions();
        options.setFileType(FileType.IMAGE);
        options.setMSingleFileMaxSize(2097152); // 20M = 20971520 B
        options.setMSingleFileMaxSizeTip("图片最大不超过2M！");
        options.setMAllFilesMaxSize(5242880);//5M 5242880 ; 20M = 20971520 B
        options.setMAllFilesMaxSizeTip("总图片大小不超过5M！");
        options.setMFileCondition(new FileSelectCondition() {
            @Override
            public boolean accept(@NonNull FileType fileType, Uri uri) {
                return (uri != null && !TextUtils.isEmpty(uri.getPath()) && !FileUtils.INSTANCE.isGif(uri));
            }
        });

        mFileSelector = FileSelector.Companion.with(this)

                .setRequestCode(REQUEST_CHOOSE_FILE)
                .setSelectMode(false)
                .setMinCount(1, "至少选一个文件!")
                .setMaxCount(10, "最多选十个文件!")
                .setSingleFileMaxSize(5242880, "大小不能超过5M！") //5M 5242880 ; 100M = 104857600 KB
                .setAllFilesMaxSize(10485760, "总大小不能超过10M！")//
                .setMimeTypes(new String[]{"image/*"})//默认为 */* 可以选择任何文件类型, 不同 arrayOf("video/*","audio/*","image/*") 系统提供的选择UI不一样

                .applyOptions(options)

                //优先使用 FileOptions 中设置的 FileSelectCondition
                .filter(new FileSelectCondition() {
                    @Override
                    public boolean accept(@NonNull FileType fileType, Uri uri) {
                        switch (fileType) {
                            case IMAGE:
                                return (uri != null && !TextUtils.isEmpty(uri.getPath()) && !FileUtils.INSTANCE.isGif(uri));
                            case VIDEO:
                            case AUDIO:
                                break;
                            default:
                        }
                        return true;
                    }
                })
                .callback(new FileSelectCallBack() {
                    @Override
                    public void onSuccess(List<FileSelectResult> results) {
                        if (results != null && !results.isEmpty()) {
                            FileLogger.INSTANCE.w("回调 onSuccess " + results.size());
                            mTvResult.setText("");

                            showSelectResult(results);

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        FileLogger.INSTANCE.e("回调 onError " + e.getMessage());
                        mTvResultError.setText(String.format("%s%s", mTvResultError.getText().toString(), " 错误信息: " + e.getMessage() + "   \n "));
                    }
                })
                .choose();
    }

    private void showSelectResult(List<FileSelectResult> results) {
        mTvResult.setText("");

        for (FileSelectResult result : results) {

            //1.打印Log
            final String info = result.toString() + "  格式化 :  " + FileSizeUtils.INSTANCE.formatFileSize(result.getFileSize()) + " \n";
            FileLogger.INSTANCE.w("FileOptions onSuccess  " + info);
            //Caused by: java.util.MissingFormatArgumentException: Format specifier '%3A'
            mTvResult.setText(String.format("%s选择结果 : %s |--------- \n\uD83D\uDC49压缩前 \n%s",
                    mTvResult.getText().toString(), FileType.INSTANCE.typeByUri(result.getUri()), info));


            //2.压缩图片
            final FileType fileType = result.getFileType();
            if (fileType != null) {
                switch (fileType) {
                    case IMAGE:
                        try {
                            final Bitmap bitmap = getBitmapFromUri(result.getUri());
                            //原图
                            mIvOrigin.setImageBitmap(bitmap);
                            //压缩(Luban)
                            List<Uri> photos = new ArrayList<Uri>();
                            photos.add(result.getUri());
                            compressImage(photos);
                            //or
                            //Engine.compress(uri,  100L)
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case VIDEO:
                        final Bitmap thumbnail = loadThumbnail(result.getUri(), 100, 200);
                        if (thumbnail != null && !thumbnail.isRecycled()) {
                            mIvOrigin.setImageBitmap(thumbnail);
                        }
                        break;
                    default:
                }
            }
        }

    }

    /**
     * 压缩图片
     */
    private void compressImage(List<Uri> photos) {

        ImageCompressor.Companion
                .with(this)
                .load(photos)
                .ignoreBy(100)//B
                .setTargetDir(getPathImageCache())
                .setFocusAlpha(false)
                .enableCache(true)
                .filter(new ImageCompressPredicate() {
                    @Override
                    public boolean apply(Uri uri) {
                        final String path = getFilePathByUri(uri);
                        FileLogger.INSTANCE.i("image predicate " + uri + "  " + path);
                        if (uri != null) {
                            return !TextUtils.isEmpty(path) && !path.toLowerCase().endsWith(".gif");
                        }
                        return false;
                    }
                })
                .setRenameListener(new OnImageRenameListener() {
                    @Override
                    public String rename(Uri uri) {
                        try {
                            String filePath = getFilePathByUri(uri);
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            md.update(filePath.getBytes());
                            return new BigInteger(1, md.digest()).toString(32);
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                        return "";
                    }
                })
                .setImageCompressListener(new OnImageCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(Uri uri) {
                        final String path = getCacheDir() + "/image/";
                        try {
                            FileLogger.INSTANCE.i("compress onSuccess  uri=" + uri + "  path= " + path
                                    + " 缓存目录总大小= " + FileSizeUtils.INSTANCE.getFolderSize(new File(path)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            //1.Log
                            String displayName = "", size = "";
                            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null) {
                                while (cursor.moveToNext()) {
                                    displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                                    size = cursor.getString(sizeIndex);
                                }
                            }

                            mTvResult.setText(String.format(
                                    "%s\n ---------\n \uD83D\uDC49压缩后 \n Uri : %s \n   文件名称 ：%s \n 大小：%s B \n格式化 : %s\n ---------",
                                    mTvResult.getText().toString(), uri, displayName, size, FileSizeUtils.INSTANCE.formatFileSize(Long.parseLong(size))));

                            //2.设置压缩过的图片
                            final Bitmap bitmap = getBitmapFromUri(uri);
                            mIvCompressed.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        FileLogger.INSTANCE.e("compress onError " + e.getMessage());
                    }
                })
                .launch();

    }

    private String getPathImageCache() {
        String path = getCacheDir().getAbsolutePath() + "/image/";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
