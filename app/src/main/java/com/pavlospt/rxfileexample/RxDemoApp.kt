package com.pavlospt.rxfileexample

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class RxDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(DebugTree())
    }
}