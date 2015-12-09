package com.pavlospt.rxfileexample;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by PavlosPT13 on 09/12/15.
 */
public class RxDemoApp extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        if(BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());
    }
}
