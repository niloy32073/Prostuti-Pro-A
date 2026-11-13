package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

/** Which kind of timed exam the attempt/result/ranking flow is showing. Group exams now use the real exam engine, so only LIVE is left. */
@Serializable
enum class TimedExamKind {
    LIVE
}
