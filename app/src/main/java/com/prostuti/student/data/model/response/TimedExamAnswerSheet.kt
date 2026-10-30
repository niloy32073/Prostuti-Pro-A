package com.prostuti.student.data.model.response

import kotlinx.serialization.Serializable

/** My own answer-sheet for a timed exam, once its window has closed — reuses the same [Question]
 * model the attempt screen shows, now carrying the correct option and an explanation. Only ever
 * built for the signed-in student's own submission (no one else's sheet is ever fetchable). */
@Serializable
data class TimedExamAnswerSheet(
    val questions: List<Question>,
    /** mcqId -> the option id the student picked (missing = left unanswered). */
    val myAnswers: Map<String, String>,
    /** mcqId -> the correct option id. */
    val correctAnswers: Map<String, String>,
    /** mcqId -> a short explanation. */
    val explanations: Map<String, String>,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val totalMarks: Float
)
