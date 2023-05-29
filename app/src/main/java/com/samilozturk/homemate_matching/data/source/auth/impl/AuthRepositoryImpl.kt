package com.samilozturk.homemate_matching.data.source.auth.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.samilozturk.homemate_matching.data.model.UniqueId
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
) : AuthRepository {
    override fun getCurrentStudentId(): UniqueId? {
        return auth.currentUser?.uid?.let { UniqueId(it) }
    }

    override suspend fun login(email: String, password: String): UniqueId {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user == null) {
                error("Giriş yapılamadı")
            }
            return UniqueId(result.user!!.uid)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException,
                -> {
                    error("E-posta veya şifre hatalı")
                }

                else -> {
                    error("Giriş yapılırken bir hata oluştu")
                }
            }
        }
    }

    override suspend fun signup(email: String, password: String): UniqueId {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            if (result.user == null) {
                error("Kayıt yapılamadı")
            }
            return UniqueId(result.user!!.uid)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException,
                -> {
                    error("E-posta veya şifre hatalı")
                }

                is FirebaseAuthUserCollisionException -> {
                    error("Bu e-posta adresi zaten kullanılıyor")
                }

                else -> {
                    error("Kayıt olunurken bir hata oluştu")
                }
            }
        }
    }

    override suspend fun sendResetPasswordEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidUserException -> {
                    error("Bu e-posta adresi ile kayıtlı bir kullanıcı bulunamadı")
                }

                else -> {
                    error("Şifre sıfırlanırken bir hata oluştu")
                }
            }
        }

    }

    override fun logout() {
        auth.signOut()
    }
}