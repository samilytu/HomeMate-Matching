package com.samilozturk.homemate_matching.data.source.auth.impl

import com.samilozturk.homemate_matching.data.model.UniqueId
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeAuthRepository : AuthRepository {

    private var currentStudentId: UniqueId? = UniqueId("123")

    override fun getCurrentStudentId(): UniqueId? {
        return currentStudentId
    }

    override suspend fun login(email: String, password: String): UniqueId {
        delay(1000)
        if (email == "admin@admin.com" && password == "admin") {
            val randomUniqueId = UniqueId("123")
            currentStudentId = randomUniqueId
            return randomUniqueId
        }
        error("Kullanıcı adı veya şifre hatalı")
    }

    override suspend fun signup(email: String, password: String): UniqueId {
        delay(1000)
        if (email == "admin@admin.com") {
            error("E-posta zaten kullanılıyor")
        }
        val randomUniqueId = UniqueId(Random.nextInt(1000).toString())
        currentStudentId = randomUniqueId
        return randomUniqueId
    }

    override suspend fun sendResetPasswordEmail(email: String) {
        delay(1000)
        if (email == "admin@admin.com") {
            return
        }
        error("E-posta bulunamadı")
    }

    override fun logout() {
        currentStudentId = null
    }
}