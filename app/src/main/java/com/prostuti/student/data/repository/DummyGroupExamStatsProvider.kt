package com.prostuti.student.data.repository

import kotlin.random.Random

/**
 * DUMMY: replace when `GET /routines/slots/{slot_id}/stats` exists (GROUP_ROUTINE_PLAN.md §14 B1) —
 * swap the binding in DI, nothing else changes. Seeded by exam id, so an exam always shows the
 * same made-up numbers. [participants] is real when the leaderboard has loaded; otherwise a share
 * of the members is made up.
 */
class DummyGroupExamStatsProvider : GroupExamStatsProvider {
    override fun statsFor(examId: String, participants: Int, memberCount: Int): GroupExamStats {
        val random = Random(examId.hashCode())
        val submitted = if (participants > 0) participants else (memberCount * random.nextInt(55, 95) / 100).coerceAtLeast(0)
        val histogram = List(10) { i -> (10 + 80 * bell(i) + random.nextInt(0, 15)).toInt().coerceIn(4, 100) }
        return GroupExamStats(submittedCount = submitted, averagePercent = random.nextInt(55, 82), histogram = histogram)
    }

    /** A rough bump around 60–80 %, so the made-up spread looks like a real class. */
    private fun bell(i: Int): Double = kotlin.math.exp(-((i - 6.5) * (i - 6.5)) / 6.0)
}
