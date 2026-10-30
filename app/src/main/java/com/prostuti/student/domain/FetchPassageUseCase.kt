package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.repository.MCQExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchPassageUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(passageId: String): ApiResponse<FetchPassageData> =
        withContext(Dispatchers.IO) { repository.fetchPassage(passageId) }
}
