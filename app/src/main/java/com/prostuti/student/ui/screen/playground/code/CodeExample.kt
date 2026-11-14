package com.prostuti.student.ui.screen.playground.code

import androidx.annotation.StringRes

/** A ready program: one title, the same algorithm written in C and in Python. */
data class CodeExample(@param:StringRes val titleRes: Int, val c: String, val python: String)
