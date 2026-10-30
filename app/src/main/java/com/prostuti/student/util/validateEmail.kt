package com.prostuti.student.util

fun validateEmail(value: String?): Boolean {
    if (value.isNullOrBlank()) return false

    val emailRegex = Regex(
        """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
    )

    return value.trim().matches(emailRegex)
}
