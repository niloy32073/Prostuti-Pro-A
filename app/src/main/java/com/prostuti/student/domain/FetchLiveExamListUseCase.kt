package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.data.repository.LiveExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchLiveExamListUseCase(private val repository: LiveExamRepository) {
    suspend operator fun invoke(): ApiResponse<List<TimedExam>> =
        withContext(Dispatchers.IO) { repository.liveExamList() }
}
