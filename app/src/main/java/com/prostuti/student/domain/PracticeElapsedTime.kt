package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ExamResponseAckResponse

/**
 * Time already used on a practice attempt, in milliseconds. Practice time pauses while the app
 * is closed, so this is not "now − start_at" but the larger of:
 *  - the server's view: the biggest `response_time` saved (it is cumulative — the moment in the
 *    exam each answer was given), and
 *  - this device's saved value, which also covers time spent after the last answer.
 * The server wins when it is ahead (or the local value is missing, e.g. after a reinstall), so
 * clearing app data can never give back time.
 */
fun practiceElapsedMs(savedResponses: List<ExamResponseAckResponse>, localElapsedMs: Long?): Long {
    val serverElapsed = savedResponses.maxOfOrNull { it.responseTimeMs.toLong() } ?: 0L
    return maxOf(serverElapsed, localElapsedMs ?: 0L)
}
