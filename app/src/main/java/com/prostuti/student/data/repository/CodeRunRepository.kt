package com.prostuti.student.data.repository

import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.TraceCommand
import kotlinx.coroutines.flow.Flow

/** Runs student code away from the app's own process. */
interface CodeRunRepository {
    /**
     * Ends after [RunEvent.Finished] or [RunEvent.Failed]; cancelling it kills the program. With
     * [trace] it pauses before each line ([RunEvent.Paused]) until [sendTraceCommand].
     */
    fun runPython(code: String, trace: Boolean = false): Flow<RunEvent>

    /** Runs C code with the built-in interpreter (no download); same events and limits as Python. */
    fun runC(code: String, trace: Boolean = false): Flow<RunEvent>

    /** The line for a pending [RunEvent.InputRequested]. */
    fun sendInput(line: String)

    /** What a [RunEvent.Paused] program does next. */
    fun sendTraceCommand(command: TraceCommand)
}
