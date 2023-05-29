package com.samilozturk.homemate_matching.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.samilozturk.homemate_matching.R


@RequiresApi(Build.VERSION_CODES.O)
class NotificationUtils(base: Context) : ContextWrapper(base) {

    fun createChannels() {
        val notificationManager = getSystemService<NotificationManager>()!!

        createChannelByDefaultSettings(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            notificationManager
        )
    }

    private fun createChannelByDefaultSettings(id: String, name: String, notificationManager: NotificationManager) {
        val announcementChannel = NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        announcementChannel.enableLights(true)
        announcementChannel.lightColor = Color.WHITE

        notificationManager.createNotificationChannel(announcementChannel)
    }

}