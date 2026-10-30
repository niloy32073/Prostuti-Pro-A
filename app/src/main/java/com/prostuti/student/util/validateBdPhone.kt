package com.prostuti.student.util

fun validateBdPhone(phone: String?): Boolean {
    if (phone.isNullOrBlank()) return false
    val bdPhoneRegex = Regex("^\\+?8801[3-9]\\d{8}$")
    return getNormalizedPhone(phone).trim().matches(bdPhoneRegex)
}
