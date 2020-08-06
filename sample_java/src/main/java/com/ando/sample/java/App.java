package com.ando.sample.java;

import com.ando.file.FileOperator;

/**
 * Title:
 * <p>
 * Description:
 * </p>
 *
 * @author Changbao
 * @date 2020/8/6  17:32
 */
public class App extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FileOperator.INSTANCE.init(
                this,
                BuildConfig.DEBUG
        );

    }
}