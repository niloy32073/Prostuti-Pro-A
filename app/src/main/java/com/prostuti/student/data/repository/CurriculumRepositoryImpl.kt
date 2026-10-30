package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.service.local.ChapterDao
import com.prostuti.student.data.service.local.SubjectDao
import com.prostuti.student.data.service.local.TopicDao
import com.prostuti.student.data.service.remote.CurriculumApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Cache-first: Room is the single source of truth; a failed refresh keeps showing cached rows. */
class CurriculumRepositoryImpl(
    private val apiService: CurriculumApiService,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao
) : CurriculumRepository {

    override fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>> = flow {
        if (refresh || subjectDao.getAll().isEmpty()) {
            when (val api = apiService.fetchSubjects()) {
                is ApiResponse.Success -> subjectDao.replaceAll(api.data)
                is ApiResponse.Failure -> {
                    emit(api)
                    if (subjectDao.getAll().isEmpty()) return@flow
                }
            }
        }
        emitAll(subjectDao.observeAll().map { ApiResponse.Success(it) })
    }

    override fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>> = flow {
        if (refresh || chapterDao.getBySubjectId(subjectId).isEmpty()) {
            when (val api = apiService.fetchChapters(subjectId)) {
                is ApiResponse.Success -> chapterDao.replaceBySubjectId(subjectId, api.data)
                is ApiResponse.Failure -> {
                    emit(api)
                    if (chapterDao.getBySubjectId(subjectId).isEmpty()) return@flow
                }
            }
        }
        emitAll(chapterDao.observeBySubjectId(subjectId).map { ApiResponse.Success(it) })
    }

    override fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>> = flow {
        if (refresh || topicDao.getByChapterId(chapterId).isEmpty()) {
            when (val api = apiService.fetchTopics(chapterId)) {
                is ApiResponse.Success -> topicDao.replaceByChapterId(chapterId, api.data)
                is ApiResponse.Failure -> {
                    emit(api)
                    if (topicDao.getByChapterId(chapterId).isEmpty()) return@flow
                }
            }
        }
        emitAll(topicDao.observeByChapterId(chapterId).map { ApiResponse.Success(it) })
    }
}
