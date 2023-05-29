package com.samilozturk.homemate_matching.data.model.validation

import android.util.Patterns
import com.samilozturk.homemate_matching.data.model.credentials.ResetPasswordCredentials
import javax.inject.Inject

class ResetPasswordValidator @Inject constructor() : Validator<ResetPasswordCredentials> {

    override fun validate(args: ResetPasswordCredentials) {
        if(args.email.isEmpty()) {
            error("E-posta boş olamaz")
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(args.email).matches()) {
            error("Geçersiz e-posta")
        }
    }

}