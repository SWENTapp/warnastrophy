package com.github.warnastrophy.core.model

data class Contact(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val relationship: String
)

const val fakeNumber: String = "1234567890"
