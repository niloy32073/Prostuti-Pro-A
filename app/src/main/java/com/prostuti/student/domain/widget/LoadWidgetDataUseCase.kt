package com.prostuti.student.domain.widget

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.StorageRepository
import com.prostuti.student.domain.FeatureAccess
import com.prostuti.student.domain.FetchLiveExamListUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.fetchAllPages
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Everything the home screen widget needs, in one go (calls run side by side). Signed out → no
 * network at all. Features the account can't use (routine/group scopes) aren't asked for.
 */
class LoadWidgetDataUseCase(
    private val storage: StorageRepository,
    private val getMe: GetMeUseCase,
    private val fetchLiveExams: FetchLiveExamListUseCase,
    private val fetchSlots: FetchSlotsUseCase,
    private val fetchActiveRoutine: FetchActiveRoutineUseCase,
    private val fetchMyGroups: FetchMyGroupsUseCase,
    private val describeTopics: DescribeTopicGroupingUseCase,
) {
    suspend operator fun invoke(now: Instant): WidgetData {
        if (storage.getJWTToken().isNullOrBlank()) return WidgetData.SignedOut
        // The cached profile is enough for the scopes; unknown → both on (the server still guards).
        val access = FeatureAccess.of((getMe(refresh = false).first() as? ApiResponse.Success)?.data)
        val today = DhakaTime.dateAt(now.toEpochMilli())

        return coroutineScope {
            val live = async { fetchLiveExams() }
            val slots = async {
                if (access.routine) fetchSlots(dateFrom = today, dateTo = today.plusDays(SLOT_DAYS - 1), limit = SLOT_LIMIT) else null
            }
            val personal = async { if (access.routine) fetchActiveRoutine(null) else null }
            val groups = async {
                if (access.group) fetchAllPages(GROUP_PAGE) { page, limit -> fetchMyGroups(page, limit) } else null
            }

            val liveExams = (live.await() as? ApiResponse.Success)?.data
            val slotList = (slots.await() as? ApiResponse.Success)?.data
            val upcoming = when {
                liveExams == null && slotList == null -> null
                else -> pickUpcoming(liveExams.orEmpty(), slotList.orEmpty(), now)
            }
            val todaySlots = slotList?.let { pickToday(it, today) }
            val shownSlots = todaySlots.orEmpty() + upcoming.orEmpty().filterIsInstance<UpcomingExam.Group>().map { it.slot }

            WidgetData.SignedIn(
                access = access,
                upcoming = upcoming,
                today = todaySlots,
                hasPersonalRoutine = (personal.await() as? ApiResponse.Success)?.let { it.data?.routine != null },
                groupNames = (groups.await() as? ApiResponse.Success)?.data.orEmpty().associate { it.id to it.name },
                labels = describeTopics(shownSlots.distinctBy { it.id }),
            )
        }
    }

    private companion object {
        /** Two weeks ahead is plenty to find the next three group exams. */
        const val SLOT_DAYS = 14L
        const val SLOT_LIMIT = 100
        const val GROUP_PAGE = 50
    }
}
