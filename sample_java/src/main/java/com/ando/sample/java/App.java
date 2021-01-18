package com.ando.sample.java;

import ando.file.BuildConfig;
import ando.file.FileOperator;

/**
 *
 * @author javakam
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