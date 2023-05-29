package com.samilozturk.homemate_matching.data.model.validation

interface Validator<T> {
    fun validate(args: T)
}