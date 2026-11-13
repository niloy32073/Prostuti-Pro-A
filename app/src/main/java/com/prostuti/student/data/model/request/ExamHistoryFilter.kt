package com.prostuti.student.data.model.request

/**
 * Query filters for `GET exams/attempts/history`. Null = not sent.
 * [dateFrom]/[dateTo] are UTC ISO-8601 date-times without an offset; [status] is a `HistoryStatus` value.
 */
data class ExamHistoryFilter(
    val subjectId: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val status: String? = null
)
