package com.prostuti.student.data.repository

/**
 * One group exam's summary numbers that the API doesn't give yet: submissions, average score and
 * the score spread. [histogram] holds 10 buckets (0–10 %, …, 90–100 %), each a share of 0..100.
 */
data class GroupExamStats(
    val submittedCount: Int,
    val averagePercent: Int,
    val histogram: List<Int>,
)

/** Source of [GroupExamStats]; today only [DummyGroupExamStatsProvider] (GROUP_ROUTINE_PLAN.md §14 B1). */
interface GroupExamStatsProvider {
    fun statsFor(examId: String, participants: Int, memberCount: Int): GroupExamStats
}
