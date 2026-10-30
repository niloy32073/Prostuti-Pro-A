package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupDetail
import com.prostuti.student.data.repository.GroupExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchGroupDetailUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<GroupDetail> =
        withContext(Dispatchers.IO) { repository.groupDetail(groupId) }
}
