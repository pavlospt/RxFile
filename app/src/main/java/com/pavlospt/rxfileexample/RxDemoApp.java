package com.pavlospt.rxfileexample;

import android.app.Application;
import timber.log.Timber;

public class RxDemoApp extends Application {

  @Override public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) Timber.plant(new Timber.DebugTree());
  }
}
