package com.samilozturk.homemate_matching.data.source.db

import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.UniqueId

interface DbRepository {
    suspend fun insertStudent(student: Student)
    suspend fun getStudent(uid: UniqueId): Student
    suspend fun getCurrentStudent(): Student
    suspend fun updateStudent(student: Student)
    suspend fun getAllStudents(): List<Student>
    suspend fun sendMatchRequestTo(targetStudentId: UniqueId)
    suspend fun revokeMatchRequest(requestId: UniqueId)
    suspend fun acceptMatchRequest(requestId: UniqueId)
    suspend fun rejectMatchRequest(requestId: UniqueId)
    suspend fun agreeMatchRequest(requestId: UniqueId)
    suspend fun disagreeMatchRequest(requestId: UniqueId)
    suspend fun getReceivedMatchRequests(): List<MatchRequest.Received>
    suspend fun getSentMatchRequests(): List<MatchRequest.Sent>
    suspend fun getMatchedRequests(): List<MatchRequest.Matched>
    suspend fun updateFcmToken(token: String)
    suspend fun getFcmToken(targetStudentId: UniqueId): String?
    suspend fun removeFcmToken()
}