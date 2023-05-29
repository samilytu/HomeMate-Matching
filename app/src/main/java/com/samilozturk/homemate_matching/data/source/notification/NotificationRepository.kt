package com.samilozturk.homemate_matching.data.source.notification

import com.samilozturk.homemate_matching.data.model.UniqueId

interface NotificationRepository {
    suspend fun notifyMatchRequestSent(targetStudentFcmToken: String)
    suspend fun notifyMatchRequestAccepted(targetStudentFcmToken: String)
}