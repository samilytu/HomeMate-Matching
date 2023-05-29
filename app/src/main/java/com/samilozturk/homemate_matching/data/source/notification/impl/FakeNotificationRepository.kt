package com.samilozturk.homemate_matching.data.source.notification.impl

import com.samilozturk.homemate_matching.data.model.UniqueId
import com.samilozturk.homemate_matching.data.source.notification.NotificationRepository
import kotlinx.coroutines.delay

class FakeNotificationRepository : NotificationRepository {
    override suspend fun notifyMatchRequestSent(targetStudentFcmToken: String) {
        delay(250)
        // do nothing
    }

    override suspend fun notifyMatchRequestAccepted(targetStudentFcmToken: String) {
        delay(250)
        // do nothing
    }

}