package com.prostuti.student.domain

import com.prostuti.student.data.model.response.CurriculumName

/**
 * One topic of an attempt's analysis with names resolved from the curriculum.
 * [topicId] null = questions without a topic. Names are null when the curriculum couldn't be loaded.
 */
data class TopicAnalysisRow(
    val topicId: String?,
    val topicName: CurriculumName?,
    val chapterName: CurriculumName?,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val skipped: Int
) {
    /** Correct answers as a whole percentage of this topic's questions (0 when it has none). */
    val correctPercent: Int get() = if (total > 0) correct * 100 / total else 0
}
