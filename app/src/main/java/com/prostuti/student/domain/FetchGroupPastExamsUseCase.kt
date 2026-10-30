package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupPastExam
import com.prostuti.student.data.repository.GroupExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchGroupPastExamsUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<List<GroupPastExam>> =
        withContext(Dispatchers.IO) { repository.pastExams(groupId) }
}
