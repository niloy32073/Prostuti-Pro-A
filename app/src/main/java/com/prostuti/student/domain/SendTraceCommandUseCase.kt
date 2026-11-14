package com.prostuti.student.domain

import com.prostuti.student.data.repository.CodeRunRepository
import com.prostuti.student.domain.code.run.TraceCommand

/** Tells a paused, traced program to take one step or to run to the end. */
class SendTraceCommandUseCase(private val repo: CodeRunRepository) {
    operator fun invoke(command: TraceCommand) = repo.sendTraceCommand(command)
}
