package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.RoutineRepository
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.fetchAllPages
import java.time.Instant

/**
 * A group's exams whose window has closed, newest first (GROUP_ROUTINE_PLAN.md §6.14). The API lists
 * oldest first, so every page up to today is read and sorted here.
 */
class FetchPastSlotsUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(groupId: String, now: Instant): ApiResponse<List<RoutineSlot>> {
        val today = DhakaTime.dateAt(now.toEpochMilli()).toString()
        return when (val r = fetchAllPages(PAGE_LIMIT, MAX_PAGES) { page, limit -> repository.slots(null, today, groupId, page, limit) }) {
            is ApiResponse.Success -> ApiResponse.Success(
                r.data.filter { s -> DhakaTime.parseInstant(s.closesAt)?.let { !now.isBefore(it) } ?: false }
                    .sortedByDescending { it.closesAt },
                r.message, r.metaData,
            )
            is ApiResponse.Failure -> r
        }
    }

    private companion object {
        const val PAGE_LIMIT = 100
        const val MAX_PAGES = 10
    }
}
