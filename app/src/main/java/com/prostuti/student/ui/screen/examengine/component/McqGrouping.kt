package com.prostuti.student.ui.screen.examengine.component

import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Question

/** One cell of an MCQ grid/list: either a standalone question, or a set of questions sharing a passage. */
sealed class McqGridItem {
    abstract val key: String

    data class Standalone(val index: Int, val question: Question) : McqGridItem() {
        override val key get() = question.mcqId
    }

    data class PassageGroup(
        val passageId: String,
        val passage: FetchPassageData?,
        val items: List<IndexedValue<Question>>
    ) : McqGridItem() {
        override val key get() = "passage_$passageId"
    }
}

/**
 * Groups [questions] so that every MCQ sharing the same `passageId` is shown
 * together under one [McqGridItem.PassageGroup], in the order its first
 * question appears; MCQs without a passage stay [McqGridItem.Standalone].
 */
fun groupQuestionsByPassage(
    questions: List<Question>,
    passages: Map<String, FetchPassageData>
): List<McqGridItem> {
    val handledPassageIds = mutableSetOf<String>()
    val result = mutableListOf<McqGridItem>()

    questions.forEachIndexed { index, question ->
        val passageId = question.passageId
        when {
            passageId == null -> result += McqGridItem.Standalone(index, question)
            passageId in handledPassageIds -> Unit // already emitted as part of its group
            else -> {
                handledPassageIds += passageId
                val groupedQuestions = questions.withIndex()
                    .filter { it.value.passageId == passageId }
                result += McqGridItem.PassageGroup(passageId, passages[passageId], groupedQuestions)
            }
        }
    }

    return result
}
