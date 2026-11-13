package com.prostuti.student.data.repository

import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.service.remote.RoutineApiService
import com.prostuti.student.util.retryWhileAuthzUnavailable

/** Every call retries with backoff while the server's permission check is down (§6.20). */
class RoutineRepositoryImpl(private val api: RoutineApiService) : RoutineRepository {
    override suspend fun preview(body: RoutineCreateRequest) = retryWhileAuthzUnavailable { api.preview(body) }
    override suspend fun create(body: RoutineCreateRequest) = retryWhileAuthzUnavailable { api.create(body) }
    override suspend fun active(groupId: String?) = retryWhileAuthzUnavailable { api.active(groupId) }
    override suspend fun delete(routineId: String) = retryWhileAuthzUnavailable { api.delete(routineId) }
    override suspend fun slots(dateFrom: String?, dateTo: String?, groupId: String?, page: Int, limit: Int) = retryWhileAuthzUnavailable { api.slots(dateFrom, dateTo, groupId, page, limit) }
    override suspend fun createAdhoc(body: AdhocExamRequest) = retryWhileAuthzUnavailable { api.createAdhoc(body) }
    override suspend fun start(slotId: String) = retryWhileAuthzUnavailable { api.start(slotId) }
    override suspend fun reschedule(slotId: String, slotDate: String) = retryWhileAuthzUnavailable { api.reschedule(slotId, slotDate) }
    override suspend fun deleteAdhoc(slotId: String) = retryWhileAuthzUnavailable { api.deleteAdhoc(slotId) }
}
