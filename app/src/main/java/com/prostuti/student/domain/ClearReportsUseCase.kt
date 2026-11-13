package com.prostuti.student.domain

import com.prostuti.student.data.repository.ReportRepository

class ClearReportsUseCase(private val repository: ReportRepository) {
    suspend operator fun invoke() = repository.clearReports()
}
