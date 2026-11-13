package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.model.response.ApiResponse
import kotlinx.coroutines.flow.Flow

/** Content the signed-in user has reported. Reports are final: there is no un-report. */
interface ReportRepository {
    /** Ids of reported resources of [type], from Room. */
    fun observeReportedIds(type: ReportResourceType): Flow<Set<String>>

    /** Sends the report, then remembers it in Room. Already reported counts as success (no second call). */
    suspend fun reportMcq(mcqId: String, reason: String, description: String?): ApiResponse<Unit>

    /** On logout. */
    suspend fun clearReports()
}
