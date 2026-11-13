package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.McqDetail

interface MCQRepository {
    suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData>
    suspend fun fetchMcq(mcqId: String): ApiResponse<McqDetail>
}
