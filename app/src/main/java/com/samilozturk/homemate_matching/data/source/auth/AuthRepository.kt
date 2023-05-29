package com.samilozturk.homemate_matching.data.source.auth

import com.samilozturk.homemate_matching.data.model.UniqueId

interface AuthRepository {
    fun getCurrentStudentId(): UniqueId?
    suspend fun login(email: String, password: String): UniqueId
    suspend fun signup(email: String, password: String): UniqueId
    suspend fun sendResetPasswordEmail(email: String)
    fun logout()
}