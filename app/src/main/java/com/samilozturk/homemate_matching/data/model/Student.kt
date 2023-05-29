package com.samilozturk.homemate_matching.data.model

import android.os.Parcelable
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.samilozturk.homemate_matching.data.model.db.UserDbModel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json

@Serializable
sealed interface Student : Parcelable {
    val uid: UniqueId
    val firstName: String
    val lastName: String
    val email: String
    val imageUrl: String?
    val phone: String?
    val education: Education?
    val homeAddress: HomeAddress?
    val availability: Availability?
    val type: StudentType
    val matchingStatus: MatchingStatus

    val fullName get() = "$firstName $lastName"

    fun clone(
        uid: UniqueId = this.uid,
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        email: String = this.email,
        imageUrl: String? = this.imageUrl,
        phone: String? = this.phone,
        education: Education? = this.education,
        homeAddress: HomeAddress? = this.homeAddress,
        availability: Availability? = this.availability,
        matchingStatus: MatchingStatus = this.matchingStatus,
    ): Student

    companion object {
        fun fromJson(json: String): Student {
            return Json.decodeFromString(json)
        }
    }

    @Serializable
    sealed class MatchingStatus : Parcelable {

        @Serializable
        @Parcelize
        object NoRequest : MatchingStatus() {
            override fun toString() = "Eşleşme Talebi Gönder"
        }

        @Serializable
        @Parcelize
        data class SentRequest(val uid: UniqueId) : MatchingStatus() {
            override fun toString() = SentRequest.toString()

            companion object {
                override fun toString() = "Eşleşme Talebi Gönderildi"
            }
        }

        @Serializable
        @Parcelize
        data class ReceivedRequest(val uid: UniqueId) : MatchingStatus() {
            override fun toString() = ReceivedRequest.toString()

            companion object {
                override fun toString() = "Eşleşme Talebi Alındı"
            }
        }

        @Serializable
        @Parcelize
        data class MatchedRequest(val uid: UniqueId) : MatchingStatus() {
            override fun toString() = MatchedRequest.toString()

            companion object {
                override fun toString() = "Eşleşme Sağlandı"
            }
        }

    }

    @Serializable
    @Parcelize
    data class Education(
        val department: String,
        val grade: Int,
    ) : Parcelable {
        override fun toString() = "$department / $grade. Sınıf"
    }

    @Serializable
    @Parcelize
    data class Availability(
        val availableTime: Int,
        val distanceToUniversity: Float,
    ) : Parcelable

    class LatLngSerializer : KSerializer<LatLng> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LatLng") {
            element<Double>("latitude")
            element<Double>("longitude")
        }

        override fun serialize(encoder: Encoder, value: LatLng) {
            encoder.encodeStructure(descriptor) {
                encodeDoubleElement(descriptor, 0, value.latitude)
                encodeDoubleElement(descriptor, 1, value.longitude)
            }
        }

        override fun deserialize(decoder: Decoder): LatLng {
            return decoder.decodeStructure(descriptor) {
                var latitude = 0.0
                var longitude = 0.0
                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> latitude = decodeDoubleElement(descriptor, 0)
                        1 -> longitude = decodeDoubleElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break@loop
                        else -> error("Unexpected index: $index")
                    }
                }
                LatLng(latitude, longitude)
            }
        }
    }

    @Serializable
    @Parcelize
    data class HomeAddress(
        val address: String,
        @Serializable(with = LatLngSerializer::class)
        val location: LatLng,
    ) : Parcelable

    enum class StudentType {
        PROVIDER {
            override fun toString() = "Ev Paylaşmak İstiyor"
        },
        SEEKER {
            override fun toString() = "Ev Arıyor"
        },
        IDLE {
            override fun toString() = "Aramıyor"
        }
    }

    // class for home provider for home seeker,
    @Serializable
    @Parcelize
    data class Provider(
        override val uid: UniqueId,
        override val firstName: String,
        override val lastName: String,
        override val email: String,
        override val imageUrl: String?,
        override val phone: String?,
        override val education: Education?,
        override val availability: Availability,
        override val homeAddress: HomeAddress,
        override val matchingStatus: MatchingStatus,
    ) : Student {
        override val type get() = StudentType.PROVIDER

        override fun clone(
            uid: UniqueId,
            firstName: String,
            lastName: String,
            email: String,
            imageUrl: String?,
            phone: String?,
            education: Education?,
            homeAddress: HomeAddress?,
            availability: Availability?,
            matchingStatus: MatchingStatus,
        ): Student {
            return copy(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                imageUrl = imageUrl,
                phone = phone,
                education = education,
                homeAddress = homeAddress!!,
                availability = availability!!,
                matchingStatus = matchingStatus,
            )
        }
    }

    // class for home seeker for home provider
    @Serializable
    @Parcelize
    data class Seeker(
        override val uid: UniqueId,
        override val firstName: String,
        override val lastName: String,
        override val email: String,
        override val imageUrl: String?,
        override val phone: String?,
        override val education: Education?,
        override val availability: Availability,
        override val matchingStatus: MatchingStatus,
    ) : Student {
        override val type get() = StudentType.SEEKER
        override val homeAddress: HomeAddress? get() = null

        override fun clone(
            uid: UniqueId,
            firstName: String,
            lastName: String,
            email: String,
            imageUrl: String?,
            phone: String?,
            education: Education?,
            homeAddress: HomeAddress?,
            availability: Availability?,
            matchingStatus: MatchingStatus,
        ): Student {
            return copy(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                imageUrl = imageUrl,
                phone = phone,
                education = education,
                availability = availability!!,
                matchingStatus = matchingStatus,
            )
        }
    }

    // class for non provider and non seeker
    @Serializable
    @Parcelize
    data class Idle(
        override val uid: UniqueId,
        override val firstName: String,
        override val lastName: String,
        override val email: String,
        override val imageUrl: String?,
        override val phone: String?,
        override val education: Education?,
    ) : Student {
        @IgnoredOnParcel
        override val availability: Availability? = null
        override val type get() = StudentType.IDLE
        override val homeAddress: HomeAddress? get() = null
        override val matchingStatus: MatchingStatus get() = MatchingStatus.NoRequest

        override fun clone(
            uid: UniqueId,
            firstName: String,
            lastName: String,
            email: String,
            imageUrl: String?,
            phone: String?,
            education: Education?,
            homeAddress: HomeAddress?,
            availability: Availability?,
            matchingStatus: MatchingStatus,
        ): Student {
            return copy(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                imageUrl = imageUrl,
                phone = phone,
                education = education,
            )
        }
    }

    object Factory {
        fun create(
            uid: String,
            firstName: String,
            lastName: String,
            email: String,
            imageUrl: String?,
            phone: String? = null,
            department: String? = null,
            @IntRange(from = 1, to = 4)
            grade: Int? = null,
            homeAddress: String? = null,
            homeLocation: LatLng? = null,
            @FloatRange(from = 0.0)
            distanceToUniversity: Float? = null, // in km
            availableTime: Int? = null, // in hours
            isProvider: Boolean = false,
            isSeeker: Boolean = false,
            matchingStatus: MatchingStatus = MatchingStatus.NoRequest,
        ): Student {
            require(!(isProvider and isSeeker)) {
                "Student $uid must not be both provider and seeker"
            }
            return when {
                isProvider -> Provider(
                    uid = UniqueId(uid),
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    imageUrl = imageUrl,
                    phone = phone,
                    education = department?.let {
                        Education(
                            department = department,
                            grade = grade!!
                        )
                    },
                    availability = Availability(
                        availableTime = availableTime!!,
                        distanceToUniversity = distanceToUniversity!!,
                    ),
                    homeAddress = HomeAddress(
                        address = homeAddress!!,
                        location = homeLocation!!,
                    ),
                    matchingStatus = matchingStatus,
                )

                isSeeker -> Seeker(
                    uid = UniqueId(uid),
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    imageUrl = imageUrl,
                    phone = phone,
                    education = department?.let {
                        Education(
                            department = department,
                            grade = grade!!
                        )
                    },
                    availability = Availability(
                        availableTime = availableTime!!,
                        distanceToUniversity = distanceToUniversity!!
                    ),
                    matchingStatus = matchingStatus,
                )

                else -> Idle(
                    uid = UniqueId(uid),
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    imageUrl = imageUrl,
                    phone = phone,
                    education = department?.let {
                        Education(
                            department = department,
                            grade = grade!!
                        )
                    }
                )
            }
        }
    }
}

fun Student.isOppositeType(other: Student): Boolean {
    // if this student is idle there is no opposite type
    if (this.type == Student.StudentType.IDLE || other.type == Student.StudentType.IDLE) {
        return false
    }

    // if this student is provider and other is seeker or vice versa return true
    return this.type != other.type
}

fun Student.toJson(): String {
    return Json.encodeToString(this)
}

fun Student.toUserDbModel(): UserDbModel {
    return UserDbModel(
        uid = uid.value,
        firstName = firstName,
        lastName = lastName,
        email = email,
        imagePath = imageUrl,
        phone = phone,
        homeAddress = homeAddress?.let {
            UserDbModel.HomeAddress(
                address = it.address,
                location = GeoPoint(it.location.latitude, it.location.longitude)
            )
        },
        education = education?.let {
            UserDbModel.Education(
                department = it.department,
                grade = it.grade
            )
        },
        availability = availability?.let {
            UserDbModel.Availability(
                availableTime = it.availableTime,
                distanceToUniversity = it.distanceToUniversity
            )
        },
        type = when (type) {
            Student.StudentType.PROVIDER -> UserDbModel.StudentType.PROVIDER
            Student.StudentType.SEEKER -> UserDbModel.StudentType.SEEKER
            Student.StudentType.IDLE -> UserDbModel.StudentType.IDLE
        },
    )
}