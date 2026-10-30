package com.prostuti.student.util

private val bengaliDigitChars = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

/** Renders any Latin digits in this string as Bengali digits — dates, counts, percentages
 * everywhere outside the JetBrains-Mono numeral UI (timers, scores) which stays Latin by design. */
fun String.toBengaliDigits(): String = map { c -> if (c in '0'..'9') bengaliDigitChars[c - '0'] else c }.joinToString("")

fun Int.toBengaliDigits(): String = toString().toBengaliDigits()
