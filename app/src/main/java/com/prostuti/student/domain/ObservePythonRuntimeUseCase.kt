package com.prostuti.student.domain

import com.prostuti.student.data.repository.PythonRuntimeRepository
import com.prostuti.student.domain.code.run.RuntimeState
import kotlinx.coroutines.flow.StateFlow

class ObservePythonRuntimeUseCase(private val repo: PythonRuntimeRepository) {
    operator fun invoke(): StateFlow<RuntimeState> = repo.state
}
