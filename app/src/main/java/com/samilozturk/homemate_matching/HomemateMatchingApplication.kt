package com.samilozturk.homemate_matching

import android.app.Application
import android.os.Build
import com.samilozturk.homemate_matching.util.NotificationUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HomemateMatchingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // create required notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils(this).createChannels()
        }
    }
}