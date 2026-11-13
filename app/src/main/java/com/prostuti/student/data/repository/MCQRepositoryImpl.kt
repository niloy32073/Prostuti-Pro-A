package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.service.remote.MCQApiService

class MCQRepositoryImpl(
    private val apiService: MCQApiService
) : MCQRepository {

    override suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> =
        apiService.fetchPassage(passageId)

    override suspend fun fetchMcq(mcqId: String): ApiResponse<McqDetail> =
        apiService.fetchMcq(mcqId)
}
