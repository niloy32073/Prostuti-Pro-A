package com.prostuti.student.domain

import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchTimedExamQuestionsUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(): List<Question> =
        withContext(Dispatchers.IO) { repository.questions() }
}
