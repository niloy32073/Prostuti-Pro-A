package com.prostuti.student.domain

import com.prostuti.student.data.model.response.MCQReviewQuestionResponse

/**
 * One answer-sheet question ready to show: text/options/images in [review] plus where it sits in
 * the curriculum (the answer-sheet snapshot always carries subject and chapter; topic may be null).
 */
data class AnsweredQuestion(
    val review: MCQReviewQuestionResponse,
    val subjectId: String?,
    val chapterId: String?,
    val topicId: String?
)
