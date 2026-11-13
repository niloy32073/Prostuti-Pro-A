package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory reports. Set [reportResult] to make a report fail. */
class FakeReportRepository : ReportRepository {
    val reported = MutableStateFlow<Set<Pair<ReportResourceType, String>>>(emptySet())

    var reportResult: ApiResponse<Unit> = ApiResponse.Success(Unit)
    val reportCalls = mutableListOf<Triple<String, String, String?>>()
    var clearCount = 0

    override fun observeReportedIds(type: ReportResourceType): Flow<Set<String>> =
        reported.map { set -> set.filter { it.first == type }.map { it.second }.toSet() }

    override suspend fun reportMcq(mcqId: String, reason: String, description: String?): ApiResponse<Unit> {
        reportCalls += Triple(mcqId, reason, description)
        if (reportResult is ApiResponse.Success) reported.value = reported.value + (ReportResourceType.MCQ to mcqId)
        return reportResult
    }

    override suspend fun clearReports() {
        clearCount++
        reported.value = emptySet()
    }
}
