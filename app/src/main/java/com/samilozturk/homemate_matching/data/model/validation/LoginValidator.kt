package com.samilozturk.homemate_matching.data.model.validation

import android.util.Patterns
import com.samilozturk.homemate_matching.data.model.credentials.LoginCredentials
import javax.inject.Inject

class LoginValidator @Inject constructor() : Validator<LoginCredentials> {

    override fun validate(args: LoginCredentials) {
        if(args.email.isEmpty()) {
            error("E-posta boş olamaz")
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(args.email).matches()) {
            error("Geçersiz e-posta")
        }
        if(args.password.isEmpty()) {
            error("Şifre boş olamaz")
        }
    }

}