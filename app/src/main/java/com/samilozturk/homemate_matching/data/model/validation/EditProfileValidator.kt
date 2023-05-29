package com.samilozturk.homemate_matching.data.model.validation

import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.credentials.EditProfileData
import javax.inject.Inject

class EditProfileValidator @Inject constructor() : Validator<EditProfileData> {

    override fun validate(args: EditProfileData) {
        if (args.firstName.length < 2) {
            error("İsim en az 2 karakter olmalıdır")
        }
        if (args.lastName.length < 2) {
            error("Soyisim en az 2 karakter olmalıdır")
        }
        if (args.department.length < 2) {
            error("Bölüm en az 2 karakter olmalıdır")
        }
        if (args.grade.isBlank()) {
            error("Sınıf boş bırakılamaz")
        }
        if (args.grade.toIntOrNull() == null) {
            error("Sınıf sayı olmalıdır")
        }
        if (args.grade.toInt() !in 1..4) {
            error("Sınıf 1 ile 4 arasında olmalıdır")
        }
        when(args.state) {
            Student.StudentType.PROVIDER, Student.StudentType.SEEKER -> {
                if (args.distanceToUniversity.isBlank()) {
                    error("Üniversiteye uzaklık boş bırakılamaz")
                }
                if (args.distanceToUniversity.toFloatOrNull() == null) {
                    error("Üniversiteye uzaklık sayı olmalıdır")
                }
                if (args.distanceToUniversity.toFloat() < 0) {
                    error("Üniversiteye uzaklık 0'dan büyük olmalıdır")
                }
                if(args.state == Student.StudentType.PROVIDER && args.homeAddress == null) {
                    error("Ev konumu boş bırakılamaz")
                }
            }
            Student.StudentType.IDLE -> Unit
        }
        // phone is must be blank or in format +90 5XX XXX XXXX
        val phoneRegex = "^\\+90 5[0-9]{2} [0-9]{3} [0-9]{4}$".toRegex()
        if (args.phone.isNotBlank() && !args.phone.matches(phoneRegex)) {
            error("Telefon numarası geçersiz")
        }
    }

}