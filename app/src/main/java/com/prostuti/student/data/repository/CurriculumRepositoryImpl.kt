package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.CurriculumLookup
import com.prostuti.student.data.model.response.CurriculumName
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.service.local.ChapterDao
import com.prostuti.student.data.service.local.SubjectDao
import com.prostuti.student.data.service.local.TopicDao
import com.prostuti.student.data.service.remote.CurriculumApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** Cache-first: Room is the single source of truth; a failed refresh keeps showing cached rows. */
class CurriculumRepositoryImpl(
    private val apiService: CurriculumApiService,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val topicDao: TopicDao
) : CurriculumRepository {

    override suspend fun cachedNames(subjectIds: Set<String>, chapterIds: Set<String>, topicIds: Set<String>): CurriculumNames {
        // Room caps a query's parameters, so look ids up in chunks.
        suspend fun <T> lookup(ids: Set<String>, get: suspend (List<String>) -> List<T>): List<T> = ids.chunked(ID_CHUNK).flatMap { get(it) }
        val chapters = lookup(chapterIds, chapterDao::getByIds)
        val topics = lookup(topicIds, topicDao::getByIds)
        return CurriculumNames(
            subjects = lookup(subjectIds, subjectDao::getByIds).associate { it.id to CurriculumName(it.nameBn, it.nameEn) },
            chapters = chapters.associate { it.id to CurriculumName(it.chapterNameBn, it.chapterNameEn) },
            topics = topics.associate { it.id to CurriculumName(it.nameBn, it.nameEn) },
            chapterSubjects = chapters.associate { it.id to it.subjectId },
            topicChapters = topics.associate { it.id to it.chapterId },
        )
    }

    override suspend fun fillNames(lookup: CurriculumLookup): Unit = coroutineScope {
        suspend fun <T> cached(ids: Set<String>, get: suspend (List<String>) -> List<T>): List<T> = ids.chunked(ID_CHUNK).flatMap { get(it) }
        fun <T> ApiResponse<T>.orNull(what: String): T? = when (this) {
            is ApiResponse.Success -> data
            is ApiResponse.Failure -> null.also { Timber.w("Curriculum %s lookup failed: %s", what, errorCode) }
        }

        // Fetch first, write last: a topic row needs its chapter row, and a chapter its subject
        // (foreign keys), so nothing is saved until the parents are known.

        // 1. Topics: a whole chapter's list where the chapter is known, else one by one.
        val knownTopics = cached(lookup.allTopicIds, topicDao::getByIds)
        val knownTopicIds = knownTopics.map { it.id }.toSet()
        val byChapter = lookup.topicsByChapter.filterValues { ids -> ids.any { it !in knownTopicIds } }.keys
            .map { id -> async { apiService.fetchTopics(id).orNull("topics") } }
        val loose = (lookup.looseTopicIds - knownTopicIds).take(MAX_SINGLE_FETCH)
            .map { id -> async { apiService.fetchTopic(id).orNull("topic")?.let(::listOf) } }
        val newTopics = (byChapter + loose).awaitAll().filterNotNull().flatten()

        // 2. Chapters: the asked ones plus the chapters of every loose topic.
        val looseChapters = (knownTopics + newTopics).filter { it.id in lookup.looseTopicIds }.map { it.chapterId }.toSet()
        val wantedChapters = lookup.allChapterIds + looseChapters + newTopics.map { it.chapterId }
        val knownChapters = cached(wantedChapters, chapterDao::getByIds)
        val knownChapterIds = knownChapters.map { it.id }.toSet()
        val bySubject = lookup.chaptersBySubject.filterValues { ids -> ids.any { it !in knownChapterIds } }.keys
            .map { id -> async { apiService.fetchChapters(id).orNull("chapters") } }
        val fetchedBySubject = bySubject.awaitAll().filterNotNull().flatten()
        val stillMissing = wantedChapters - knownChapterIds - fetchedBySubject.map { it.id }.toSet()
        val single = stillMissing.take(MAX_SINGLE_FETCH)
            .map { id -> async { apiService.fetchChapter(id).orNull("chapter")?.let(::listOf) } }
        val newChapters = fetchedBySubject + single.awaitAll().filterNotNull().flatten()

        // 3. Subjects: one list covers every subject the chapters belong to.
        val wantedSubjects = lookup.subjectIds + (knownChapters + newChapters).map { it.subjectId }
        val knownSubjectIds = cached(wantedSubjects, subjectDao::getByIds).map { it.id }.toSet()
        val newSubjects = if (wantedSubjects.all { it in knownSubjectIds }) emptyList()
        else apiService.fetchSubjects().orNull("subjects").orEmpty()

        // 4. Save parents before children, and only rows whose parent is there.
        if (newSubjects.isNotEmpty()) subjectDao.upsertAll(newSubjects)
        val subjectIds = knownSubjectIds + newSubjects.map { it.id }
        val chapters = newChapters.distinctBy { it.id }.filter { it.subjectId in subjectIds }
        if (chapters.isNotEmpty()) chapterDao.upsertAll(chapters)
        val chapterIds = cached(newTopics.map { it.chapterId }.toSet(), chapterDao::getByIds).map { it.id }.toSet()
        val topics = newTopics.distinctBy { it.id }.filter { it.chapterId in chapterIds }
        if (topics.isNotEmpty()) topicDao.upsertAll(topics)
        if (topics.size < newTopics.distinctBy { it.id }.size || chapters.size < newChapters.distinctBy { it.id }.size) {
            Timber.w("Curriculum names: skipped rows whose parent couldn't be fetched")
        }
    }

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

    private companion object {
        const val ID_CHUNK = 500
        /** Caps one-by-one lookups so a huge exam can't flood the server. */
        const val MAX_SINGLE_FETCH = 30
    }
}
