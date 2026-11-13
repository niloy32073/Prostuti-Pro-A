package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.SlotStart

/** A started (or resumed) attempt on a scheduled exam, with the slot info the exam screens need. */
data class ScheduledExamStart(
    val slot: SlotStart,
    val attempt: ExamAttemptData,
)
