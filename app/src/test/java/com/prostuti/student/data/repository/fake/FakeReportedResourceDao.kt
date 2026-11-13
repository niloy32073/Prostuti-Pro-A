package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.service.local.ReportedResourceDao
import com.prostuti.student.data.service.local.ReportedResourceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room [ReportedResourceDao]. */
class FakeReportedResourceDao : ReportedResourceDao {
    val rows = MutableStateFlow<Map<Pair<ReportResourceType, String>, ReportedResourceEntity>>(emptyMap())

    override suspend fun insert(report: ReportedResourceEntity) { rows.value = rows.value + ((report.resourceType to report.resourceId) to report) }
    override fun observeIdsByType(type: ReportResourceType): Flow<List<String>> =
        rows.map { m -> m.keys.filter { it.first == type }.map { it.second } }
    override suspend fun exists(type: ReportResourceType, resourceId: String): Boolean = (type to resourceId) in rows.value
    override suspend fun deleteAll() { rows.value = emptyMap() }
}
