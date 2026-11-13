package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.MCQRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * The answer sheet of a submitted attempt with question text, images and passages: the frozen
 * answer sheet first (it names the exam), then the exam's questions-with-options, joined by mcq_id.
 * A passage that the questions call only names by ID is fetched on its own (in parallel); if that
 * fails the question is still shown, just without its passage.
 * Works for exams whose explanations are hidden too — those just come without explanation text.
 */
class GetAttemptAnswerSheetUseCase(
    private val examRepository: ExamRepository,
    private val mcqRepository: MCQRepository
) {
    suspend operator fun invoke(attemptId: String): ApiResponse<List<AnsweredQuestion>> {
        val sheet = when (val r = examRepository.getAnswerSheet(attemptId)) {
            is ApiResponse.Success -> r.data
            is ApiResponse.Failure -> return r
        }
        val questions = when (val q = examRepository.getExamQuestionsWithOptions(sheet.examId)) {
            is ApiResponse.Success -> q.data
            is ApiResponse.Failure -> return q
        }
        val inSheet = sheet.questions.map { it.mcqId }.toSet()
        val missingPassageIds = questions
            .filter { it.mcqId in inSheet && it.passageId != null && it.passage?.hasContent() != true }
            .mapNotNull { it.passageId }
            .distinct()
        val passages: Map<String, FetchPassageData> = coroutineScope {
            missingPassageIds.map { id -> async { (mcqRepository.fetchPassage(id) as? ApiResponse.Success)?.data } }
                .awaitAll()
                .filterNotNull()
                .associateBy { it.id }
        }
        return ApiResponse.Success(mergeAnswerSheet(sheet, questions, passages))
    }
}
