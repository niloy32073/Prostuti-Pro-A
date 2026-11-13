package com.prostuti.student.data.repository

import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineDeleteResult
import com.prostuti.student.data.model.response.RoutinePlan
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.model.response.SlotStart

/** Personal and group exam routines and their scheduled exams (slots). Online only. */
interface RoutineRepository {
    suspend fun preview(body: RoutineCreateRequest): ApiResponse<RoutinePlan>
    suspend fun create(body: RoutineCreateRequest): ApiResponse<RoutineWithSlots>
    /** [groupId] null = the personal routine. `Success(null)` = no active routine. */
    suspend fun active(groupId: String?): ApiResponse<RoutineWithSlots?>
    suspend fun delete(routineId: String): ApiResponse<RoutineDeleteResult>
    suspend fun slots(dateFrom: String?, dateTo: String?, groupId: String?, page: Int, limit: Int): ApiResponse<List<RoutineSlot>>
    suspend fun createAdhoc(body: AdhocExamRequest): ApiResponse<RoutineSlot>
    suspend fun start(slotId: String): ApiResponse<SlotStart>
    suspend fun reschedule(slotId: String, slotDate: String): ApiResponse<RoutineSlot>
    suspend fun deleteAdhoc(slotId: String): ApiResponse<Unit>
}
