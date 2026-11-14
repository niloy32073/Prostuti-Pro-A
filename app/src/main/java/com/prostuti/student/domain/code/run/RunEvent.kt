package com.prostuti.student.domain.code.run

/** What a running program reports, in order. The run ends with [Finished] or [Failed]. */
sealed interface RunEvent {
    /** Printed text; [isError] for stderr. */
    data class Output(val text: String, val isError: Boolean) : RunEvent

    /** The program called `input()` and waits for a line (its prompt is already in the output). */
    data object InputRequested : RunEvent

    /** A traced run is paused before [TraceStep.line] and waits for a [TraceCommand]. */
    data class Paused(val step: TraceStep) : RunEvent

    /**
     * The program ended; [error] is the error report (Python traceback, C compile/runtime error)
     * when it failed, null when it ran to the end. [errorLine] is the code line, when known up front.
     */
    data class Finished(val error: String?, val errorLine: Int? = null) : RunEvent

    /** The app stopped the program, or its process died. */
    data class Failed(val reason: RunFailure) : RunEvent
}
