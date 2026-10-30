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

class CurriculumRepositoryImpl(
    private val apiService: CurriculumApiService,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao
) : CurriculumRepository {

    override fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>> = flow {
        val shouldFetch = refresh || subjectDao.getAll().isEmpty()
        if (shouldFetch) {
            when (val api = apiService.fetchSubjects()) {
                is ApiResponse.Success -> {
                    subjectDao.deleteAll()
                    subjectDao.insertAll(api.data)
                }
                is ApiResponse.Failure -> {
                    emit(api)
                    return@flow
                }
            }
        }
        emitAll(subjectDao.observeAll().map { ApiResponse.Success(it) })
    }

    override fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>> = flow {
        val shouldFetch = refresh || chapterDao.getBySubjectId(subjectId).isEmpty()
        if (shouldFetch) {
            when (val api = apiService.fetchChapters(subjectId)) {
                is ApiResponse.Success -> {
                    chapterDao.deleteBySubjectId(subjectId)
                    chapterDao.insertAll(api.data)
                }
                is ApiResponse.Failure -> {
                    emit(api)
                    return@flow
                }
            }
        }
        emitAll(chapterDao.observeBySubjectId(subjectId).map { ApiResponse.Success(it) })
    }

    override fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>> = flow {
        val shouldFetch = refresh || topicDao.getByChapterId(chapterId).isEmpty()
        if (shouldFetch) {
            when (val api = apiService.fetchTopics(chapterId)) {
                is ApiResponse.Success -> {
                    topicDao.deleteByChapterId(chapterId)
                    topicDao.insertAll(api.data)
                }
                is ApiResponse.Failure -> {
                    emit(api)
                    return@flow
                }
            }
        }
        emitAll(topicDao.observeByChapterId(chapterId).map { ApiResponse.Success(it) })
    }
}
