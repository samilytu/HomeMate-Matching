package com.samilozturk.homemate_matching.data.source.localstorage.impl

import com.google.android.gms.maps.model.LatLng
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository

class FakeLocalStorageRepository : LocalStorageRepository {
    private var currentStudent: Student? = Student.Factory.create(
        uid = "123",
        firstName = "Kristin",
        lastName = "Jones",
        email = "bridget.bonner@example.com",
        imageUrl = "https://picsum.photos/id/237/500/500",
        phone = null,
        department = "Bilgisayar Mühendisliği",
        grade = 3,
        homeAddress = "İstanbul Teknik Üniversitesi, Maslak, Sarıyer/İstanbul, Türkiye",
        homeLocation = LatLng(41.104444, 29.022222),
        distanceToUniversity = 5f,
        availableTime = 5,
        isProvider = true,
    )

    override fun saveStudent(student: Student) {
        currentStudent = student
    }

    override fun getStudent(): Student? {
        return currentStudent
    }

    override fun clearStudent() {
        currentStudent = null
    }
}