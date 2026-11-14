package com.prostuti.student.data.repository

import android.content.Context
import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.TraceCommand
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The C path of [CodeRunRepositoryImpl] (in-process, no Android service involved). */
class CodeRunRepositoryCTest {

    private val repo = CodeRunRepositoryImpl(mockk<Context>(relaxed = true))

    private fun out(events: List<RunEvent>) = events.filterIsInstance<RunEvent.Output>().joinToString("") { it.text }

    @Test fun runsWithInputAndFinishes() = runBlocking {
        val events = mutableListOf<RunEvent>()
        withTimeout(10_000) {
            repo.runC("#include <stdio.h>\nint main() { int a, b; scanf(\"%d %d\", &a, &b); printf(\"%d\\n\", a + b); return 0; }").collect { e ->
                events += e
                if (e == RunEvent.InputRequested) launch { repo.sendInput("20 22") }
            }
        }
        assertEquals("42\n", out(events))
        assertEquals(RunEvent.Finished(null), events.last())
    }

    @Test fun errorsCarryTheirLine() = runBlocking {
        val last = withTimeout(10_000) { repo.runC("int main() {\n  int x = 1 / 0;\n  return 0;\n}").toList() }.last() as RunEvent.Finished
        assertEquals(2, last.errorLine)
        assertTrue(last.error!!, last.error.startsWith("runtime error: line 2"))
    }

    @Test fun cancellingStopsAnEndlessLoop() = runBlocking {
        val flow = repo.runC("int main() { long i = 0; while (1) { i++; } }")
        val job = launch { flow.toList() }
        delay(300)
        job.cancel()
        // The next run still works.
        val again = withTimeout(10_000) { repo.runC("#include <stdio.h>\nint main() { printf(\"ok\"); return 0; }").toList() }
        assertEquals("ok", out(again))
    }

    @Test fun floodingOutputHitsTheLimit() = runBlocking {
        val last = withTimeout(20_000) { repo.runC("#include <stdio.h>\nint main() { while (1) printf(\"xxxxxxxxxx\\n\"); }").toList() }.last()
        assertEquals(RunEvent.Failed(com.prostuti.student.domain.code.run.RunFailure.OUTPUT_LIMIT), last)
    }

    @Test fun aTracedRunPausesUntilToldAndCanRunToTheEnd() = runBlocking {
        val events = mutableListOf<RunEvent>()
        withTimeout(10_000) {
            repo.runC("#include <stdio.h>\nint main() {\n  int a = 1;\n  a = a + 1;\n  printf(\"%d\\n\", a);\n  return 0;\n}", trace = true).collect { e ->
                events += e
                if (e is RunEvent.Paused) {
                    val pauses = events.count { it is RunEvent.Paused }
                    launch { repo.sendTraceCommand(if (pauses < 2) TraceCommand.STEP else TraceCommand.RUN_TO_END) }
                }
            }
        }
        assertEquals(listOf(3, 4), events.filterIsInstance<RunEvent.Paused>().map { it.step.line })
        assertEquals("2\n", out(events))
        assertEquals(RunEvent.Finished(null), events.last())
    }

    @Test fun stoppingAPausedRunEndsIt() = runBlocking {
        val flow = repo.runC("int main() {\n  int a = 1;\n  return a;\n}", trace = true)
        val first = withTimeout(10_000) { flow.first { it is RunEvent.Paused } } as RunEvent.Paused
        assertEquals(2, first.step.line)
        // Leaving the flow (first()) cancelled the run; the next one is unaffected.
        val again = withTimeout(10_000) { repo.runC("#include <stdio.h>\nint main() { printf(\"ok\"); return 0; }").toList() }
        assertEquals("ok", out(again))
    }
}
