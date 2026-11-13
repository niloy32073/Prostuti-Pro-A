package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamRoutine
import com.prostuti.student.data.model.response.PlannedSlot
import com.prostuti.student.data.model.response.RoutineDeleteResult
import com.prostuti.student.data.model.response.RoutinePlan
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.model.response.SlotStart
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.data.repository.RoutineRepository

/** Configurable [RoutineRepository] for ViewModel tests; records what was sent. */
class FakeRoutineRepository : RoutineRepository {
    var previewResult: ApiResponse<RoutinePlan> = ApiResponse.Success(plan())
    var createResult: ApiResponse<RoutineWithSlots> = ApiResponse.Success(RoutineWithSlots(routine()))
    var activeResult: ApiResponse<RoutineWithSlots?> = ApiResponse.Success(null)
    var adhocResult: ApiResponse<RoutineSlot> = ApiResponse.Success(slot())
    /** (dateFrom, dateTo, groupId) -> slots. */
    var slotsResult: (String?, String?, String?) -> ApiResponse<List<RoutineSlot>> = { _, _, _ -> ApiResponse.Success(emptyList()) }
    var rescheduleResult: ApiResponse<RoutineSlot> = ApiResponse.Success(slot())
    var deleteAdhocResult: ApiResponse<Unit> = ApiResponse.Success(Unit)
    var deleteResult: ApiResponse<RoutineDeleteResult> = ApiResponse.Success(RoutineDeleteResult("r1", 3))

    val previews = mutableListOf<RoutineCreateRequest>()
    val creates = mutableListOf<RoutineCreateRequest>()
    val adhocs = mutableListOf<AdhocExamRequest>()
    val activeAsked = mutableListOf<String?>()
    val slotsAsked = mutableListOf<Triple<String?, String?, String?>>()
    val rescheduled = mutableListOf<Pair<String, String>>()
    val deletedAdhoc = mutableListOf<String>()
    val deletedRoutines = mutableListOf<String>()

    override suspend fun preview(body: RoutineCreateRequest) = previewResult.also { previews += body }
    override suspend fun create(body: RoutineCreateRequest) = createResult.also { creates += body }
    override suspend fun active(groupId: String?) = activeResult.also { activeAsked += groupId }
    override suspend fun delete(routineId: String) = deleteResult.also { deletedRoutines += routineId }
    override suspend fun slots(dateFrom: String?, dateTo: String?, groupId: String?, page: Int, limit: Int): ApiResponse<List<RoutineSlot>> {
        slotsAsked += Triple(dateFrom, dateTo, groupId)
        return slotsResult(dateFrom, dateTo, groupId)
    }
    override suspend fun createAdhoc(body: AdhocExamRequest) = adhocResult.also { adhocs += body }
    override suspend fun start(slotId: String): ApiResponse<SlotStart> = error("not used")
    override suspend fun reschedule(slotId: String, slotDate: String) = rescheduleResult.also { rescheduled += slotId to slotDate }
    override suspend fun deleteAdhoc(slotId: String) = deleteAdhocResult.also { deletedAdhoc += slotId }

    companion object {
        fun plan(examDays: Int = 1, warnings: List<String> = emptyList()) = RoutinePlan(
            examDays = examDays,
            slots = List(examDays) { i ->
                PlannedSlot(
                    slotDate = "2026-11-${11 + i}", opensAt = "2026-11-${11 + i}T13:00:00", closesAt = "2026-11-${11 + i}T17:00:00",
                    topicIds = listOf("t1"), topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS),
                )
            },
            warnings = warnings,
        )

        fun routine(groupId: String? = null) = ExamRoutine(
            id = "r1", groupId = groupId, status = "ACTIVE", startDate = "2026-11-08", durationDays = 30,
            windowOpenMinute = 0, windowCloseMinute = 1440, questionsCount = 25, durationMinutes = 25,
            markPerQuestion = 1.0, negativeMarkValue = 0.25, createdAt = "2026-11-08T05:00:00",
        )

        /** Dates are Dhaka; [opensAt]/[closesAt] are UTC like the API (Dhaka 19:00–23:00 by default). */
        fun slot(
            id: String = "s1", kind: SlotKind = SlotKind.ADHOC, groupId: String? = "g1", date: String = "2026-11-10",
            opensAt: String = "${date}T13:00:00", closesAt: String = "${date}T17:00:00", status: SlotMyStatus = SlotMyStatus.UPCOMING,
            title: String? = null, topicIds: List<String> = listOf("t1"),
        ) = RoutineSlot(
            id = id, kind = kind, groupId = groupId, title = title, slotDate = date, opensAt = opensAt, closesAt = closesAt, topicIds = topicIds,
            topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS), questionsCount = 25,
            durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = status,
        )
    }
}
