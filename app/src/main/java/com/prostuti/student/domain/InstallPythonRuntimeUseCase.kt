package com.prostuti.student.domain

import com.prostuti.student.data.repository.PythonRuntimeRepository

/** Starts downloading the Python runtime; progress arrives through [ObservePythonRuntimeUseCase]. */
class InstallPythonRuntimeUseCase(private val repo: PythonRuntimeRepository) {
    operator fun invoke() = repo.install()
}
