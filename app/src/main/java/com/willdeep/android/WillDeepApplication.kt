package com.willdeep.android

import android.app.Application
import com.willdeep.android.push.MobilePushManager

class WillDeepApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobilePushManager.preInit(this)
        MobilePushManager.register(this)
    }
}
