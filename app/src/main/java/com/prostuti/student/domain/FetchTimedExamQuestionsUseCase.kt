package com.prostuti.student.domain

import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.repository.TimedExamRepository

class FetchTimedExamQuestionsUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(): List<Question> =
        repository.questions()
}
