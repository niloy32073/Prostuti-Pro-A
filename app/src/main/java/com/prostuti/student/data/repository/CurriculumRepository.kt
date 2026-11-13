package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.CurriculumLookup
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import kotlinx.coroutines.flow.Flow

interface CurriculumRepository {
    fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>>
    fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>>
    fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>>

    /** Names of whatever of these ids is already cached; nothing is fetched. */
    suspend fun cachedNames(subjectIds: Set<String>, chapterIds: Set<String>, topicIds: Set<String>): CurriculumNames = CurriculumNames()

    /**
     * Fetches whatever of [lookup] isn't cached yet and caches it, so a group member who never opened
     * these subjects still sees names. Best effort: a failed call just leaves those names out.
     */
    suspend fun fillNames(lookup: CurriculumLookup) {}
}
