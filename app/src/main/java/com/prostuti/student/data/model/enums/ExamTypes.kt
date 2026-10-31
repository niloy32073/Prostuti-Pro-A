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
    EXTRA_LIVE,
    /** A practice exam the student built with POST /exams/generate. */
    GENERATED
}
