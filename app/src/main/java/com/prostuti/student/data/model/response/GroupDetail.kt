package com.prostuti.student.data.model.response

import kotlinx.serialization.Serializable

/** Everything GroupDetailScreen needs in one fetch. [routine] null means no routine exists yet
 * (the admin-CTA / waiting-for-admin card); [currentExam] null with a non-null [routine] means
 * the routine exists but today isn't one of its exam days. */
@Serializable
data class GroupDetail(
    val group: Group,
    val routine: GroupRoutineSummary?,
    val currentExam: TimedExam?,
    val mySubmission: MyTimedExamSubmission?,
    val recentPastExams: List<GroupPastExam>,
    val memberPreview: List<GroupMember>
)
