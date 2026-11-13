package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow

class ObserveReportedIdsUseCase(private val repository: ReportRepository) {
    operator fun invoke(type: ReportResourceType): Flow<Set<String>> = repository.observeReportedIds(type)
}
