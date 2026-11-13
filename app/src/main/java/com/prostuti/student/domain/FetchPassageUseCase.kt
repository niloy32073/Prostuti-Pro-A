package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.repository.MCQRepository

class FetchPassageUseCase(private val repository: MCQRepository) {
    suspend operator fun invoke(passageId: String): ApiResponse<FetchPassageData> =
        repository.fetchPassage(passageId)
}
