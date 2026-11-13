package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

/** What a report points at (`GET enums/report-resource-types`). Only MCQs have a report endpoint so far. */
@Serializable
enum class ReportResourceType {
    MCQ,
    CONCEPTUAL_QUESTION,
    DESCRIPTIVE_QUESTION
}
