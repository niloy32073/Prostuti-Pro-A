package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExamAnswerSheet
import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Always my own answer sheet — there is no other student's to fetch (see the no-peeking-at-
 * others'-answers decision). */
class FetchMyAnswerSheetUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<TimedExamAnswerSheet> =
        withContext(Dispatchers.IO) { repository.myAnswerSheet(examId) }
}
