package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

/** Live and group exams share every screen in the attempt/result/ranking flow; this is the one
 * thing that differs between them (back-destination, a few labels). */
@Serializable
enum class TimedExamKind {
    LIVE, GROUP
}
