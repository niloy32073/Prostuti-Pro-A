package com.prostuti.student.util

fun getNormalizedPhone(input: String?): String {
    if (input.isNullOrBlank()) return ""

    // Extract only the numbers to eliminate spaces, dashes, or an existing '+'
    val digitsOnly = input.filter { it.isDigit() }

    return when {
        // Handles: "8801521577964" or "+8801521577964"
        digitsOnly.startsWith("880") && digitsOnly.length == 13 -> "+$digitsOnly"

        // Handles: "01521577964"
        digitsOnly.startsWith("01") && digitsOnly.length == 11 -> "+88$digitsOnly"

        // Handles: "1521577964" (10 digits missing the leading 0)
        digitsOnly.startsWith("1") && digitsOnly.length == 10 -> "+880$digitsOnly"

        else -> input.trim()
    }
}
