package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.repository.CodeRunRepository
import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.TraceCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile

/** Each run reads events the test sends to [events]; the run ends after Finished/Failed. */
class FakeCodeRunRepository : CodeRunRepository {
    var events = Channel<RunEvent>(Channel.UNLIMITED)
    val codes = mutableListOf<String>()
    val inputs = mutableListOf<String>()
    var cancelled = 0

    val languages = mutableListOf<String>()
    /** Whether each run was traced. */
    val traced = mutableListOf<Boolean>()
    val commands = mutableListOf<TraceCommand>()

    override fun runC(code: String, trace: Boolean): Flow<RunEvent> = run(code, "C", trace)

    override fun runPython(code: String, trace: Boolean): Flow<RunEvent> = run(code, "PYTHON", trace)

    private fun run(code: String, language: String, trace: Boolean): Flow<RunEvent> {
        languages += language
        traced += trace
        codes += code
        events = Channel(Channel.UNLIMITED)
        var ended = false
        return events.consumeAsFlow()
            .transformWhile { emit(it); (it !is RunEvent.Finished && it !is RunEvent.Failed).also { more -> ended = !more } }
            .onCompletion { if (!ended) cancelled++ }
    }

    override fun sendInput(line: String) { inputs += line }

    override fun sendTraceCommand(command: TraceCommand) { commands += command }
}
