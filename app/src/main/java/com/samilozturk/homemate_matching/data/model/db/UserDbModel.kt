package com.samilozturk.homemate_matching.data.model.db

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.UniqueId

data class UserDbModel(
    @DocumentId
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val imagePath: String? = null,
    val phone: String? = null,
    val homeAddress: HomeAddress? = null,
    val education: Education? = null,
    val availability: Availability? = null,
    val type: StudentType = StudentType.IDLE,
    @get:Exclude
    val matchRequest: MatchRequestDbModel? = null,
) {
    data class Education(
        val department: String = "",
        val grade: Int = 1,
    )

    data class HomeAddress(
        val address: String = "",
        val location: GeoPoint = GeoPoint(0.0, 0.0),
    )

    data class Availability(
        val availableTime: Int = 0,
        val distanceToUniversity: Float = 0f,
    )

    enum class StudentType {
        @PropertyName("provider")
        PROVIDER,

        @PropertyName("seeker")
        SEEKER,

        @PropertyName("idle")
        IDLE,
    }
}

fun UserDbModel.toStudent(): Student {
    return Student.Factory.create(
        uid = uid,
        firstName = firstName,
        lastName = lastName,
        email = email,
        imageUrl = imagePath,
        phone = phone,
        department = education?.department,
        grade = education?.grade,
        homeAddress = homeAddress?.address,
        homeLocation = homeAddress?.location?.let { LatLng(it.latitude, it.longitude) },
        distanceToUniversity = availability?.distanceToUniversity,
        availableTime = availability?.availableTime,
        isProvider = type == UserDbModel.StudentType.PROVIDER,
        isSeeker = type == UserDbModel.StudentType.SEEKER,
        matchingStatus = when(matchRequest?.status) {
            null -> Student.MatchingStatus.NoRequest
            MatchRequestDbModel.Status.SENT -> Student.MatchingStatus.SentRequest(UniqueId(matchRequest.uid))
            MatchRequestDbModel.Status.RECEIVED -> Student.MatchingStatus.ReceivedRequest(UniqueId(matchRequest.uid))
            MatchRequestDbModel.Status.MATCHED -> Student.MatchingStatus.MatchedRequest(UniqueId(matchRequest.uid))
        }
    )
}