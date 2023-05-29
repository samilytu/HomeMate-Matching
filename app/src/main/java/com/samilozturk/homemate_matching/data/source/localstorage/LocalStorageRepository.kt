package com.samilozturk.homemate_matching.data.source.localstorage

import com.samilozturk.homemate_matching.data.model.Student

interface LocalStorageRepository {
    fun saveStudent(student: Student)
    fun getStudent(): Student?
    fun clearStudent()
}