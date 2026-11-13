package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.model.request.McqReportRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.local.ReportedResourceDao
import com.prostuti.student.data.service.local.ReportedResourceEntity
import com.prostuti.student.data.service.remote.MCQApiService
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReportRepositoryImpl(
    private val mcqApiService: MCQApiService,
    private val dao: ReportedResourceDao,
    private val timeProvider: TimeProvider
) : ReportRepository {

    override fun observeReportedIds(type: ReportResourceType): Flow<Set<String>> =
        dao.observeIdsByType(type).map { it.toSet() }

    override suspend fun reportMcq(mcqId: String, reason: String, description: String?): ApiResponse<Unit> {
        if (dao.exists(ReportResourceType.MCQ, mcqId)) return ApiResponse.Success(Unit)
        return when (val r = mcqApiService.reportMcq(mcqId, McqReportRequest(reason = reason, description = description))) {
            is ApiResponse.Success -> {
                dao.insert(
                    ReportedResourceEntity(
                        resourceType = ReportResourceType.MCQ,
                        resourceId = mcqId,
                        reportId = r.data.id,
                        reason = reason,
                        reportedAt = timeProvider.nowEpochMillis()
                    )
                )
                ApiResponse.Success(Unit, r.message)
            }
            is ApiResponse.Failure -> r
        }
    }

    override suspend fun clearReports() = dao.deleteAll()
}
