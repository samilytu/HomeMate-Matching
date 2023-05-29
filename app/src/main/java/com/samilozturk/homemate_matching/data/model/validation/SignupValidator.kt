package com.samilozturk.homemate_matching.data.model.validation

import android.util.Patterns
import com.samilozturk.homemate_matching.data.model.credentials.SignupCredentials
import javax.inject.Inject

class SignupValidator @Inject constructor() : Validator<SignupCredentials> {

    override fun validate(args: SignupCredentials) {
        if (args.firstName.length < 2) {
            error("İsim en az 2 karakter olmalıdır")
        }
        if (args.lastName.length < 2) {
            error("Soyisim en az 2 karakter olmalıdır")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(args.email).matches()) {
            error("Geçersiz e-posta")
        }
        if (args.password.length < 6) {
            error("Şifre en az 6 karakter olmalıdır")
        }
        if (!args.password.contains(Regex("[0-9]"))) {
            error("Şifre en az 1 rakam içermelidir")
        }
        if (!args.password.contains(Regex("[a-z]"))) {
            error("Şifre en az 1 küçük harf içermelidir")
        }
        if (!args.password.contains(Regex("[A-Z]"))) {
            error("Şifre en az 1 büyük harf içermelidir")
        }
    }

}