package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.request.McqReportRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.model.response.McqReportResponse
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Calls about a single MCQ or its content (passage, report), independent of any exam attempt. */
class MCQApiService(private val client: HttpClient) {

    suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> = safeApiCall {
        client.get("questions/passages/$passageId")
    }

    suspend fun fetchMcq(mcqId: String): ApiResponse<McqDetail> = safeApiCall {
        client.get("questions/mcq/$mcqId")
    }

    suspend fun reportMcq(mcqId: String, request: McqReportRequest): ApiResponse<McqReportResponse> = safeApiCall {
        client.post("questions/mcq/$mcqId/report") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
