package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable


/** Routine / one-off exam difficulty band, mapped by the server onto the 0-100 MCQ difficulty score. */
@Serializable
enum class QuestionDifficulty {
    EASY, MEDIUM, HARD
}
