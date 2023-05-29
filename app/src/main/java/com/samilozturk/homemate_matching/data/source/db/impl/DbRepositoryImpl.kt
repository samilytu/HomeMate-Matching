package com.samilozturk.homemate_matching.data.source.db.impl

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.samilozturk.homemate_matching.data.model.ImagePath
import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.NoRequest
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.ReceivedRequest
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.SentRequest
import com.samilozturk.homemate_matching.data.model.UniqueId
import com.samilozturk.homemate_matching.data.model.db.MatchRequestDbModel
import com.samilozturk.homemate_matching.data.model.db.UserDbModel
import com.samilozturk.homemate_matching.data.model.db.toMatchRequest
import com.samilozturk.homemate_matching.data.model.db.toStudent
import com.samilozturk.homemate_matching.data.model.toUserDbModel
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository
import com.samilozturk.homemate_matching.data.source.notification.NotificationRepository
import com.samilozturk.homemate_matching.data.source.remotestorage.RemoteStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DbRepositoryImpl(
    private val db: FirebaseFirestore,
    private val remoteStorageRepository: RemoteStorageRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val localStorageRepository: LocalStorageRepository,
) : DbRepository {
    override suspend fun insertStudent(student: Student) {
        val user = student.toUserDbModel()
        db.collection("users")
            .document(user.uid)
            .set(user)
            .await()
        // update user
        db.collection("users")
            .document(user.uid)
            .update("requestRefs", FieldValue.arrayUnion())
            .await()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getStudent(uid: UniqueId): Student {
        return try {
            withContext(Dispatchers.IO) {
                val userId = authRepository.getCurrentStudentId()!!
                val doc = db.collection("users")
                    .document(uid.value)
                    .get()
                    .await()
                val imagePath = doc["imagePath"] as String?
                val imageUrl = imagePath?.let {
                    remoteStorageRepository.getDownloadUrl(ImagePath(it))
                }
                val requestRefs = doc["requestRefs"] as List<DocumentReference>
                // go to requestRefs and get all requests whose sender is current user or receiver is current user
                val requests = requestRefs.map { ref ->
                    async { ref.get().await() }
                }.awaitAll().filter { snapshot ->
                    val senderRef = snapshot["fromRef"] as DocumentReference
                    val receiverRef = snapshot["toRef"] as DocumentReference
                    senderRef.id == userId.value || receiverRef.id == userId.value
                }
                val matchRequest = requests
                    .singleOrNull()
                    ?.let { snapshot ->
                        val senderRef = snapshot["fromRef"] as DocumentReference
                        val receiverRef = snapshot["toRef"] as DocumentReference
                        val isAccepted = snapshot["isAccepted"] as Boolean
                        MatchRequestDbModel(
                            uid = snapshot.id,
                            status = when {
                                isAccepted -> MatchRequestDbModel.Status.MATCHED
                                senderRef.id == userId.value -> MatchRequestDbModel.Status.SENT
                                receiverRef.id == userId.value -> MatchRequestDbModel.Status.RECEIVED
                                else -> error("This should not happen")
                            }
                        )
                    }
                doc.toObject<UserDbModel>()!!
                    .copy(imagePath = imageUrl, matchRequest = matchRequest).toStudent()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Kullanıcı getirilemedi")
        }

    }

    override suspend fun getCurrentStudent(): Student {
        try {
            val userId = authRepository.getCurrentStudentId()!!
            val doc = db.collection("users")
                .document(userId.value)
                .get()
                .await()
            val imagePath = doc["imagePath"] as String?
            val imageUrl = imagePath?.let {
                remoteStorageRepository.getDownloadUrl(ImagePath(it))
            }
            return doc.toObject<UserDbModel>()!!.copy(imagePath = imageUrl).toStudent()
        } catch (e: Exception) {
            e.printStackTrace()
            error("Kullanıcı bulunamadı")
        }
    }

    override suspend fun updateStudent(student: Student) {
        try {
            val user = student.toUserDbModel()
            // get request refs array
            val snapshot = db.collection("users")
                .document(user.uid)
                .get()
                .await()
            val requestRefs = snapshot["requestRefs"]
            val fcmToken = snapshot["fcmToken"]
            // update whole user
            db.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            // update user request refs
            db.collection("users")
                .document(user.uid)
                .update(mapOf(
                    "requestRefs" to requestRefs,
                    "fcmToken" to fcmToken
                ))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            error("Kullanıcı güncellenemedi")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAllStudents(): List<Student> {
        try {
            val userId = authRepository.getCurrentStudentId()!!
            val result = db.collection("users")
                .whereNotEqualTo(FieldPath.documentId(), userId.value)
                .get()
                .await()
            return withContext(Dispatchers.Default) {
                result.documents.map { doc ->
                    async {
                        val imagePath = doc["imagePath"] as String?
                        val imageUrl = imagePath?.let {
                            remoteStorageRepository.getDownloadUrl(ImagePath(it))
                        }
                        val requestRefs = doc["requestRefs"] as List<DocumentReference>
                        // go to requestRefs and get all requests whose sender is current user or receiver is current user
                        val requests = requestRefs.map { ref ->
                            async { ref.get().await() }
                        }.awaitAll().filter { snapshot ->
                            val senderRef = snapshot["fromRef"] as DocumentReference
                            val receiverRef = snapshot["toRef"] as DocumentReference
                            senderRef.id == userId.value || receiverRef.id == userId.value
                        }
                        val matchRequest = requests
                            .singleOrNull()
                            ?.let { snapshot ->
                                val senderRef = snapshot["fromRef"] as DocumentReference
                                val receiverRef = snapshot["toRef"] as DocumentReference
                                val isAccepted = snapshot["isAccepted"] as Boolean
                                MatchRequestDbModel(
                                    uid = snapshot.id,
                                    status = when {
                                        isAccepted -> MatchRequestDbModel.Status.MATCHED
                                        senderRef.id == userId.value -> MatchRequestDbModel.Status.SENT
                                        receiverRef.id == userId.value -> MatchRequestDbModel.Status.RECEIVED
                                        else -> error("This should not happen")
                                    }
                                )
                            }
                        doc.toObject<UserDbModel>()!!
                            .copy(imagePath = imageUrl, matchRequest = matchRequest).toStudent()
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Kullanıcılar getirilemedi")
        }
    }

    override suspend fun sendMatchRequestTo(targetStudentId: UniqueId) {
        withContext(Dispatchers.IO) {
            try {
                val targetStudent = getStudent(targetStudentId)
                if (targetStudent.matchingStatus != NoRequest) {
                    error("Bu kullanıcıya zaten eşleşme talebi gönderilmiş")
                }

                val senderId = authRepository.getCurrentStudentId()!!
                val senderRef = db.collection("users").document(senderId.value)

                val receiverRef = db.collection("users").document(targetStudentId.value)
                // generate firestore request id
                val requestRef = db.collection("requests").document()

                val request = mapOf(
                    "fromRef" to senderRef,
                    "toRef" to receiverRef,
                    "isAccepted" to false
                )

                awaitAll(
                    async {
                        // create request
                        requestRef
                            .set(request)
                            .await()
                    },
                    async {
                        // update sender's request refs
                        senderRef
                            .update("requestRefs", FieldValue.arrayUnion(requestRef))
                            .await()
                    },
                    async {
                        // update receiver's request refs
                        receiverRef
                            .update("requestRefs", FieldValue.arrayUnion(requestRef))
                            .await()
                    }
                )

                val fcmToken = getFcmToken(targetStudentId)
                    ?: return@withContext // if target student has no fcm token, do not send notification
                // send notification to receiver
                notificationRepository.notifyMatchRequestSent(fcmToken)
            } catch (e: Exception) {
                e.printStackTrace()
                error("Eşleşme talebi gönderilemedi")
            }
        }
    }

    override suspend fun revokeMatchRequest(requestId: UniqueId) {
        withContext(Dispatchers.IO) {
            try {
                val requestRef = db.collection("requests").document(requestId.value)
                val request = requestRef.get().await()

                val targetStudentRef = request["toRef"] as DocumentReference
                val targetStudent = getStudent(UniqueId(targetStudentRef.id))
                if (targetStudent.matchingStatus !is SentRequest) {
                    error("Bu kullanıcı henüz eşleşme talebi göndermemiş")
                }

                val senderId = authRepository.getCurrentStudentId()!!
                val senderRef = db.collection("users").document(senderId.value)

                awaitAll(
                    async {
                        // delete request
                        requestRef
                            .delete()
                            .await()
                    },
                    async {
                        // remove request from sender's request refs
                        senderRef
                            .update("requestRefs", FieldValue.arrayRemove(requestRef))
                            .await()
                    },
                    async {
                        // remove request from receiver's request refs
                        targetStudentRef
                            .update("requestRefs", FieldValue.arrayRemove(requestRef))
                            .await()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                error("Eşleşme talebi geri çekilemedi")
            }
        }
    }

    override suspend fun acceptMatchRequest(requestId: UniqueId) {
        withContext(Dispatchers.IO) {
            try {
                val requestRef = db.collection("requests").document(requestId.value)
                val request = requestRef.get().await()

                val targetStudentRef = request["fromRef"] as DocumentReference
                val targetStudent = getStudent(UniqueId(targetStudentRef.id))
                if (targetStudent.matchingStatus !is ReceivedRequest) {
                    error("Bu kullanıcıya henüz bir talep gönderilmemiş")
                }

                requestRef
                    .update("isAccepted", true)
                    .await()

                val fcmToken = getFcmToken(targetStudent.uid)
                    ?: return@withContext // if target student has no fcm token, do not send notification

                // send notification to targetStudent
                notificationRepository.notifyMatchRequestAccepted(fcmToken)
            } catch (e: Exception) {
                e.printStackTrace()
                error("Eşleşme talebi kabul edilemedi")
            }
        }
    }

    override suspend fun rejectMatchRequest(requestId: UniqueId) {
        withContext(Dispatchers.IO) {
            try {
                val requestRef = db.collection("requests").document(requestId.value)
                val request = requestRef.get().await()

                val targetStudentRef = request["fromRef"] as DocumentReference
                val targetStudent = getStudent(UniqueId(targetStudentRef.id))
                if (targetStudent.matchingStatus !is ReceivedRequest) {
                    error("Bu kullanıcı henüz eşleşme talebi göndermemiş")
                }

                val receiverId = authRepository.getCurrentStudentId()!!
                val receiverRef = db.collection("users").document(receiverId.value)

                awaitAll(
                    async {
                        // delete request
                        requestRef
                            .delete()
                            .await()
                    },
                    async {
                        // remove request from sender's request refs
                        targetStudentRef
                            .update("requestRefs", FieldValue.arrayRemove(requestRef))
                            .await()
                    },
                    async {
                        // remove request from receiver's request refs
                        receiverRef
                            .update("requestRefs", FieldValue.arrayRemove(requestRef))
                            .await()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                error("Eşleşme talebi reddedilemedi")
            }
        }
    }

    override suspend fun agreeMatchRequest(requestId: UniqueId) {
        withContext(Dispatchers.IO) {
            try {
                val userId = authRepository.getCurrentStudentId()!!

                val requestRef = db.collection("requests").document(requestId.value)
                val request = requestRef.get().await()

                val senderRef = request["fromRef"] as DocumentReference
                val receiverRef = request["toRef"] as DocumentReference

                val targetStudentRef = if (senderRef.id == userId.value) receiverRef else senderRef
                val targetStudent = getStudent(UniqueId(targetStudentRef.id))
                if (targetStudent.matchingStatus !is Student.MatchingStatus.MatchedRequest) {
                    error("Bu kullanıcıyla henüz eşleşme sağlanmamış")
                }

                awaitAll(
                    async {
                        // delete request
                        requestRef
                            .delete()
                            .await()
                    },
                    async {
                        // remove request from sender's request refs
                        senderRef
                            .update(
                                mapOf(
                                    "requestRefs" to FieldValue.arrayRemove(requestRef),
                                    "type" to UserDbModel.StudentType.IDLE,
                                    "availability" to null,
                                    "homeAddress" to null,
                                )
                            )
                            .await()
                    },
                    async {
                        // remove request from receiver's request refs
                        receiverRef
                            .update(
                                mapOf(
                                    "requestRefs" to FieldValue.arrayRemove(requestRef),
                                    "type" to UserDbModel.StudentType.IDLE,
                                    "availability" to null,
                                    "homeAddress" to null,
                                )
                            )
                            .await()
                    },
                )

                // update local user
                val localUser = localStorageRepository.getStudent()!!
                localStorageRepository.saveStudent(
                    Student.Factory.create(
                        uid = localUser.uid.value,
                        firstName = localUser.firstName,
                        lastName = localUser.lastName,
                        email = localUser.email,
                        imageUrl = localUser.imageUrl,
                        phone = localUser.phone,
                        department = localUser.education?.department,
                        grade = localUser.education?.grade,
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                error("Eşleşme talebi reddedilemedi")
            }
        }
    }

    override suspend fun disagreeMatchRequest(requestId: UniqueId) {
        withContext(Dispatchers.IO) {
            try {
                val userId = authRepository.getCurrentStudentId()!!

                val requestRef = db.collection("requests").document(requestId.value)
                val request = requestRef.get().await()

                val senderRef = request["fromRef"] as DocumentReference
                val receiverRef = request["toRef"] as DocumentReference

                val targetStudentRef = if (senderRef.id == userId.value) receiverRef else senderRef
                val targetStudent = getStudent(UniqueId(targetStudentRef.id))
                if (targetStudent.matchingStatus !is Student.MatchingStatus.MatchedRequest) {
                    error("Bu kullanıcıyla henüz eşleşme sağlanmamış")
                }

                awaitAll(
                    async {
                        // delete request
                        requestRef
                            .delete()
                            .await()
                    },
                    async {
                        // remove request from sender's request refs
                        senderRef
                            .update("requestRefs", FieldValue.arrayRemove(requestRef))
                            .await()
                    },
                    async {
                        // remove request from receiver's request refs
                        receiverRef
                            .update("requestRefs", FieldValue.arrayRemove(requestRef))
                            .await()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                error("Eşleşme talebi reddedilemedi")
            }
        }
    }

    override suspend fun getReceivedMatchRequests(): List<MatchRequest.Received> {
        try {
            val userId = authRepository.getCurrentStudentId()!!
            val userDoc = db.collection("users").document(userId.value)
            val result = db.collection("requests")
                .whereEqualTo("toRef", userDoc)
                .whereNotEqualTo("isAccepted", true)
                .get()
                .await()
            return withContext(Dispatchers.Default) {
                result.documents.map { doc ->
                    async {
                        val senderRef = doc["fromRef"] as DocumentReference
                        val senderDoc = senderRef.get().await()
                        val senderImagePath = senderDoc["imagePath"] as String?
                        val senderImageUrl = senderImagePath?.let {
                            remoteStorageRepository.getDownloadUrl(ImagePath(it))
                        }
                        val sender = senderDoc.toObject<UserDbModel>()!!
                            .copy(
                                imagePath = senderImageUrl,
                                matchRequest = MatchRequestDbModel(
                                    uid = doc.id,
                                    status = MatchRequestDbModel.Status.RECEIVED
                                )
                            )
                        MatchRequestDbModel(
                            uid = doc.id,
                            status = MatchRequestDbModel.Status.RECEIVED,
                            targetUser = sender,
                        ).toMatchRequest() as MatchRequest.Received
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Alınan eşleşme talepleri getirilemedi")
        }
    }

    override suspend fun getSentMatchRequests(): List<MatchRequest.Sent> {
        try {
            val userId = authRepository.getCurrentStudentId()!!
            val userDoc = db.collection("users").document(userId.value)
            val result = db.collection("requests")
                .whereEqualTo("fromRef", userDoc)
                .whereNotEqualTo("isAccepted", true)
                .get()
                .await()
            return withContext(Dispatchers.Default) {
                result.documents.map { doc ->
                    async {
                        val receiverRef = doc["toRef"] as DocumentReference
                        val receiverDoc = receiverRef.get().await()
                        val receiverImagePath = receiverDoc["imagePath"] as String?
                        val receiverImageUrl = receiverImagePath?.let {
                            remoteStorageRepository.getDownloadUrl(ImagePath(it))
                        }
                        val receiver = receiverDoc.toObject<UserDbModel>()!!
                            .copy(
                                imagePath = receiverImageUrl,
                                matchRequest = MatchRequestDbModel(
                                    uid = doc.id,
                                    status = MatchRequestDbModel.Status.SENT
                                )
                            )
                        MatchRequestDbModel(
                            uid = doc.id,
                            status = MatchRequestDbModel.Status.SENT,
                            targetUser = receiver,
                        ).toMatchRequest() as MatchRequest.Sent
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Alınan eşleşme talepleri getirilemedi")
        }
    }

    override suspend fun getMatchedRequests(): List<MatchRequest.Matched> {
        try {
            val userId = authRepository.getCurrentStudentId()!!
            val userDoc = db.collection("users").document(userId.value)
            val result = db.collection("requests")
                .where(
                    Filter.or(
                        Filter.equalTo("fromRef", userDoc),
                        Filter.equalTo("toRef", userDoc)
                    )
                )
                .whereEqualTo("isAccepted", true)
                .get()
                .await()
            return withContext(Dispatchers.Default) {
                result.documents.map { doc ->
                    async {
                        val senderRef = doc["fromRef"] as DocumentReference
                        val receiverRef = doc["toRef"] as DocumentReference
                        val targetRef = if (senderRef.id == userId.value) receiverRef else senderRef
                        val targetDoc = targetRef.get().await()
                        val targetImagePath = targetDoc["imagePath"] as String?
                        val targetImageUrl = targetImagePath?.let {
                            remoteStorageRepository.getDownloadUrl(ImagePath(it))
                        }
                        val target = targetDoc.toObject<UserDbModel>()!!
                            .copy(
                                imagePath = targetImageUrl,
                                matchRequest = MatchRequestDbModel(
                                    uid = doc.id,
                                    status = MatchRequestDbModel.Status.MATCHED
                                )
                            )
                        MatchRequestDbModel(
                            uid = doc.id,
                            status = MatchRequestDbModel.Status.MATCHED,
                            targetUser = target,
                        ).toMatchRequest() as MatchRequest.Matched
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Alınan eşleşme talepleri getirilemedi")
        }

    }

    override suspend fun updateFcmToken(token: String) {
        val userId = authRepository.getCurrentStudentId()!!
        db.collection("users")
            .document(userId.value)
            .update("fcmToken", token)
            .await()
    }

    override suspend fun getFcmToken(targetStudentId: UniqueId): String? {
        return db.collection("users")
            .document(targetStudentId.value)
            .get()
            .await()
            .get("fcmToken") as String?
    }

    override suspend fun removeFcmToken() {
        val userId = authRepository.getCurrentStudentId()!!
        db.collection("users")
            .document(userId.value)
            .update("fcmToken", FieldValue.delete())
            .await()
    }

}