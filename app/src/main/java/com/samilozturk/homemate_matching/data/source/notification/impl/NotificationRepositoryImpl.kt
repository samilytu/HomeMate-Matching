package com.samilozturk.homemate_matching.data.source.notification.impl

import com.samilozturk.homemate_matching.BuildConfig
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository
import com.samilozturk.homemate_matching.data.source.notification.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.util.InternalAPI
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NotificationRepositoryImpl(
    private val client: HttpClient,
    private val localStorageRepository: LocalStorageRepository,
) : NotificationRepository {

    @OptIn(InternalAPI::class)
    private suspend fun sendNotification(
        fcmToken: String,
        notificationTitle: String,
        notificationBody: String,
    ) {
        val response = client.post {
            url("https://fcm.googleapis.com/fcm/send")
            // get server key from local.properties
            val serverKey = BuildConfig.FCM_SERVER_KEY
            header("Authorization", "key=${serverKey}")
            header("Content-Type", "application/json")
            body = buildJsonObject {
                put("to", fcmToken)
                put("notification", buildJsonObject {
                    put("title", notificationTitle)
                    put("body", notificationBody)
                })
            }.toString()
        }
        response.content
    }

    override suspend fun notifyMatchRequestSent(targetStudentFcmToken: String) {
        val currentStudent = localStorageRepository.getStudent()!!
        sendNotification(
            fcmToken = targetStudentFcmToken,
            notificationTitle = "Eşleşme İsteği",
            notificationBody = "${currentStudent.fullName} adlı öğrenci sizinle eşleşmek istiyor."
        )
    }

    override suspend fun notifyMatchRequestAccepted(targetStudentFcmToken: String) {
        val currentStudent = localStorageRepository.getStudent()!!
        sendNotification(
            fcmToken = targetStudentFcmToken,
            notificationTitle = "Eşleşme İsteğiniz Kabul Edildi",
            notificationBody = "${currentStudent.fullName} adlı öğrenci eşleşme talebinizi kabul etti."
        )
    }
}