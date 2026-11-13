package com.prostuti.student.domain

import com.prostuti.student.data.model.response.CurriculumName
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.CurriculumRepository
import com.prostuti.student.data.repository.ExamRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Topic-wise analysis of a submitted attempt with topic and chapter names.
 * The analysis API returns topic IDs only; the answer sheet maps each topic to its chapter and
 * subject, and the cached curriculum (Room first, API on a miss) gives the names. A name that
 * can't be found stays null — the analysis itself is still returned. Server order is kept.
 */
class GetAttemptAnalysisUseCase(
    private val examRepository: ExamRepository,
    private val curriculumRepository: CurriculumRepository
) {
    suspend operator fun invoke(attemptId: String): ApiResponse<List<TopicAnalysisRow>> = coroutineScope {
        // The two attempt calls, and then every curriculum lookup, run in parallel.
        val analysisCall = async { examRepository.getAttemptAnalysis(attemptId) }
        val sheetCall = async { examRepository.getAnswerSheet(attemptId) }
        val analysis = when (val r = analysisCall.await()) {
            is ApiResponse.Success -> r.data
            is ApiResponse.Failure -> {
                sheetCall.cancel()
                return@coroutineScope r
            }
        }
        val sheetQuestions = (sheetCall.await() as? ApiResponse.Success)?.data?.questions.orEmpty()
        val chapterByTopic = sheetQuestions.mapNotNull { q -> q.topicId?.let { t -> q.chapterId?.let { t to it } } }.toMap()
        val subjectByChapter = sheetQuestions.mapNotNull { q -> q.chapterId?.let { c -> q.subjectId?.let { c to it } } }.toMap()

        // Chapters before topics: cached topic rows reference their chapter row in Room.
        val chapterNames = subjectByChapter.values.distinct()
            .map { subjectId -> async { curriculumRepository.observeChapters(subjectId, refresh = false).firstSuccess().orEmpty() } }
            .awaitAll().flatten().associate { it.id to CurriculumName(it.chapterNameBn, it.chapterNameEn) }
        val topicNames = chapterByTopic.values.distinct()
            .map { chapterId -> async { curriculumRepository.observeTopics(chapterId, refresh = false).firstSuccess().orEmpty() } }
            .awaitAll().flatten().associate { it.id to CurriculumName(it.nameBn, it.nameEn) }

        ApiResponse.Success(
            analysis.topics.map { t ->
                TopicAnalysisRow(
                    topicId = t.topicId,
                    topicName = t.topicId?.let { topicNames[it] },
                    chapterName = t.topicId?.let { chapterByTopic[it] }?.let { chapterNames[it] },
                    total = t.totalQuestions,
                    correct = t.correct,
                    wrong = t.wrong,
                    skipped = t.skipped
                )
            }
        )
    }
}

/** The first successful emission of a cache-first curriculum flow, or null if it only fails. */
private suspend fun <T> Flow<ApiResponse<T>>.firstSuccess(): T? =
    (firstOrNull { it is ApiResponse.Success } as? ApiResponse.Success)?.data
