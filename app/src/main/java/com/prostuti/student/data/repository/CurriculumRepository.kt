package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import kotlinx.coroutines.flow.Flow

interface CurriculumRepository {
    fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>>
    fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>>
    fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>>
}
