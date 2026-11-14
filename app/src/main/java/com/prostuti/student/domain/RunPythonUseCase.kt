package com.prostuti.student.domain

import com.prostuti.student.data.repository.CodeRunRepository
import com.prostuti.student.domain.code.run.RunEvent
import kotlinx.coroutines.flow.Flow

/** Runs Python code in its own process. Cancelling the collection stops the program. */
class RunPythonUseCase(private val repo: CodeRunRepository) {
    operator fun invoke(code: String, trace: Boolean = false): Flow<RunEvent> = repo.runPython(code, trace)
}
