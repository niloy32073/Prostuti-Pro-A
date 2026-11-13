package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.request.RescheduleRequest
import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineDeleteResult
import com.prostuti.student.data.model.response.RoutinePlan
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.model.response.SlotStart
import com.prostuti.student.util.safeApiCall
import com.prostuti.student.util.safeApiCallNoData
import com.prostuti.student.util.safeApiCallNullable
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post

/** `/routines` endpoints (scope `routine:use`). Dates are `yyyy-MM-dd` Asia/Dhaka days. */
class RoutineApiService(private val client: HttpClient) {

    suspend fun preview(body: RoutineCreateRequest): ApiResponse<RoutinePlan> = safeApiCall {
        client.post("routines/preview") { json(body) }
    }

    suspend fun create(body: RoutineCreateRequest): ApiResponse<RoutineWithSlots> = safeApiCall {
        client.post("routines") { json(body) }
    }

    /** The active routine of [groupId], or the personal one when null. `Success(null)` = none. */
    suspend fun active(groupId: String?): ApiResponse<RoutineWithSlots?> = safeApiCallNullable {
        client.get("routines/active") { groupId?.let { parameter("group_id", it) } }
    }

    suspend fun delete(routineId: String): ApiResponse<RoutineDeleteResult> = safeApiCall {
        client.delete("routines/$routineId")
    }

    /** My schedule: personal slots plus every slot of my groups, by opening time. [limit] is 1-100. */
    suspend fun slots(
        dateFrom: String?,
        dateTo: String?,
        groupId: String?,
        page: Int,
        limit: Int,
    ): ApiResponse<List<RoutineSlot>> = safeApiCall {
        client.get("routines/slots") {
            dateFrom?.let { parameter("date_from", it) }
            dateTo?.let { parameter("date_to", it) }
            groupId?.let { parameter("group_id", it) }
            paging(page, limit)
        }
    }

    suspend fun createAdhoc(body: AdhocExamRequest): ApiResponse<RoutineSlot> = safeApiCall {
        client.post("routines/slots") { json(body) }
    }

    suspend fun start(slotId: String): ApiResponse<SlotStart> = safeApiCall {
        client.post("routines/slots/$slotId/start")
    }

    suspend fun reschedule(slotId: String, slotDate: String): ApiResponse<RoutineSlot> = safeApiCall {
        client.patch("routines/slots/$slotId/reschedule") { json(RescheduleRequest(slotDate)) }
    }

    suspend fun deleteAdhoc(slotId: String): ApiResponse<Unit> = safeApiCallNoData {
        client.delete("routines/slots/$slotId")
    }
}
