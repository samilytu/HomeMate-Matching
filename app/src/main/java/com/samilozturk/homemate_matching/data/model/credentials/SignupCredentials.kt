package com.samilozturk.homemate_matching.data.model.credentials

import android.net.Uri

data class SignupCredentials(
    val imageUri: Uri?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
)
