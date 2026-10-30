package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class CurriculumApiService(private val client: HttpClient) {
    suspend fun fetchSubjects(): ApiResponse<List<Subject>> =
        safeApiCall { client.get("curriculum/subjects") }

    suspend fun fetchChapters(subjectId: String): ApiResponse<List<Chapter>> =
        safeApiCall { client.get("curriculum/subjects/$subjectId/chapters") }

    suspend fun fetchTopics(chapterId: String): ApiResponse<List<Topic>> =
        safeApiCall { client.get("curriculum/chapters/$chapterId/topics") }
}
