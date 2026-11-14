package com.prostuti.student.domain

import com.prostuti.student.data.repository.CodeRunRepository
import com.prostuti.student.domain.code.run.RunEvent
import kotlinx.coroutines.flow.Flow

/** Runs C code with the built-in interpreter. Cancelling the collection stops the program. */
class RunCUseCase(private val repo: CodeRunRepository) {
    operator fun invoke(code: String, trace: Boolean = false): Flow<RunEvent> = repo.runC(code, trace)
}
