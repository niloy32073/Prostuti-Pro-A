package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupPastExam
import com.prostuti.student.data.repository.GroupExamRepository

class FetchGroupPastExamsUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<List<GroupPastExam>> =
        repository.pastExams(groupId)
}
