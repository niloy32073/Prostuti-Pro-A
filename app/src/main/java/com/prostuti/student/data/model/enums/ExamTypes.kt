package com.prostuti.student.data.model.enums

import com.prostuti.student.util.FallbackEnumSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ExamTypesSerializer::class)
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
    GENERATED,

    /** A routine or group exam (private; created when its slot is first started). */
    SCHEDULED,

    /** App-side only: any `exam_type` the server sends that this build doesn't know. Never sent. */
    UNKNOWN
}

object ExamTypesSerializer : FallbackEnumSerializer<ExamTypes>("ExamTypes", ExamTypes.entries, ExamTypes.UNKNOWN)
