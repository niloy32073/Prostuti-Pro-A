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
 * Inside a group, questions follow `passageOrder` (then their order in [questions]).
 *
 * Each index is the question's position **on screen** (0-based), not in [questions]:
 * with Q1–Q3 standalone and then a two-question passage, the passage questions are
 * numbered 4 and 5 and the next standalone question 6 — no gaps or jumps.
 */
fun groupQuestionsByPassage(
    questions: List<Question>,
    passages: Map<String, FetchPassageData>
): List<McqGridItem> {
    val handledPassageIds = mutableSetOf<String>()
    val result = mutableListOf<McqGridItem>()
    var nextNumber = 0

    questions.forEach { question ->
        val passageId = question.passageId
        when {
            passageId == null -> result += McqGridItem.Standalone(nextNumber++, question)
            passageId in handledPassageIds -> Unit // already emitted as part of its group
            else -> {
                handledPassageIds += passageId
                val groupedQuestions = questions
                    .filter { it.passageId == passageId }
                    .sortedBy { it.passageOrder ?: Int.MAX_VALUE } // stable: ties keep list order
                    .map { IndexedValue(nextNumber++, it) }
                result += McqGridItem.PassageGroup(passageId, passages[passageId], groupedQuestions)
            }
        }
    }

    return result
}
