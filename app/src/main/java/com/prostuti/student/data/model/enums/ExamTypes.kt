package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class ExamTypes {
    BOARD,
    COLLEGE,
    UNIVERSITY,
    MEDICAL,
    ENGINEERING,
    CUSTOM,
    LIVE,
    PRACTICE,
    EXTRA_LIVE
}
