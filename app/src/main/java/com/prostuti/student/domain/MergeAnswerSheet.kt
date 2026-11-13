package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.AnswerResult
import com.prostuti.student.data.model.response.AnswerSheetData
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.MCQReviewOptionResponse
import com.prostuti.student.data.model.response.MCQReviewQuestionResponse

/**
 * Joins the frozen answer sheet (result, correct option, explanation) with the exam's question
 * text and options by `mcq_id`. The answer sheet decides which questions appear and their order;
 * a question missing from [questions] (deleted since) keeps its result but has no text or options.
 * `isCorrect` follows the old review shape: true = correct, false = wrong, null = skipped.
 * A question's passage comes with its content, or from [passages] (by passage ID) when that
 * content only names the passage.
 */
fun mergeAnswerSheet(
    sheet: AnswerSheetData,
    questions: List<ExamQuestionWithOptions>,
    passages: Map<String, FetchPassageData> = emptyMap()
): List<AnsweredQuestion> {
    val byId = questions.associateBy { it.mcqId }
    return sheet.questions.sortedBy { it.order }.map { q ->
        val content = byId[q.mcqId]
        val isCorrect = when (q.result) {
            AnswerResult.CORRECT -> true
            AnswerResult.WRONG -> false
            AnswerResult.SKIPPED -> null
            else -> if (q.selectedOptionId == null) null else q.isCorrect
        }
        AnsweredQuestion(
            review = MCQReviewQuestionResponse(
                mcqId = q.mcqId,
                order = q.order,
                questionText = content?.questionText,
                questionImageLink = content?.questionImageLink,
                explanationText = q.explanationText,
                explanationImageLink = q.explanationImageLink,
                options = content?.options.orEmpty().sortedBy { it.order }.map { o ->
                    MCQReviewOptionResponse(
                        id = o.id,
                        optionText = o.optionText,
                        optionImageLink = o.optionImageLink,
                        order = o.order,
                        isCorrect = o.id == q.correctOptionId
                    )
                },
                selectedOptionId = q.selectedOptionId,
                isCorrect = isCorrect,
                passage = content?.passage?.takeIf { it.hasContent() } ?: content?.passageId?.let { passages[it] }
            ),
            subjectId = q.subjectId,
            chapterId = q.chapterId,
            topicId = q.topicId
        )
    }
}

/** True when the passage has text or an image worth showing. */
internal fun FetchPassageData.hasContent(): Boolean = !passageText.isNullOrBlank() || !passageImage.isNullOrBlank()
