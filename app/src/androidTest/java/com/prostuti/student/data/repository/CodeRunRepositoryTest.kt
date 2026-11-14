package com.prostuti.student.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.RunFailure
import com.prostuti.student.domain.code.run.TraceCommand
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs real Python in the `:py` process. Needs the on-demand module installed natively, as Play
 * does (e.g. `adb install-multiple` of the bundle's base and pyfeature splits); skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
class CodeRunRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo = CodeRunRepositoryImpl(context)

    @Before fun moduleInstalled() {
        assumeTrue("pyfeature not installed", PythonRuntimeRepositoryImpl(context).state.value == com.prostuti.student.domain.code.run.RuntimeState.Installed)
    }

    private fun output(events: List<RunEvent>) = events.filterIsInstance<RunEvent.Output>().filter { !it.isError }.joinToString("") { it.text }

    @Test fun printsAndFinishes() = runBlocking {
        val events = withTimeout(30_000) { repo.runPython("for i in range(3):\n    print(i * i)").toList() }
        assertEquals("0\n1\n4\n", output(events))
        assertEquals(RunEvent.Finished(null), events.last())
    }

    @Test fun inputGoesThroughTheService() = runBlocking {
        val events = mutableListOf<RunEvent>()
        withTimeout(30_000) {
            repo.runPython("a = int(input('a? '))\nb = int(input('b? '))\nprint(a * b)").collect { e ->
                events += e
                if (e == RunEvent.InputRequested) launch { repo.sendInput(if (events.count { it == RunEvent.InputRequested } == 1) "6" else "7") }
            }
        }
        assertEquals("a? b? 42\n", output(events))
        assertEquals(2, events.count { it == RunEvent.InputRequested })
    }

    @Test fun errorsComeBackAsTracebacks() = runBlocking {
        val last = withTimeout(30_000) { repo.runPython("x = 1\ny = x / 0").toList() }.last() as RunEvent.Finished
        assertTrue(last.error!!, last.error.contains("line 2") && last.error.contains("ZeroDivisionError"))
    }

    @Test fun anInfiniteLoopHitsTheTimeLimitAndTheNextRunStillWorks() = runBlocking {
        val events = withTimeout(40_000) { repo.runPython("while True:\n    pass").toList() }
        assertEquals(RunEvent.Failed(RunFailure.TIME_LIMIT), events.last())
        val again = withTimeout(30_000) { repo.runPython("print('ok')").toList() }
        assertEquals("ok\n", output(again))
    }

    @Test fun floodingOutputHitsTheOutputLimit() = runBlocking {
        val events = withTimeout(40_000) { repo.runPython("while True:\n    print('x' * 100)").toList() }
        assertEquals(RunEvent.Failed(RunFailure.OUTPUT_LIMIT), events.last())
    }

    @Test fun deepRecursionIsARecursionErrorNotACrash() = runBlocking {
        val last = withTimeout(30_000) { repo.runPython("def f(n):\n    return f(n + 1)\nf(0)").toList() }.last()
        assertTrue(last.toString(), last is RunEvent.Finished && last.error!!.contains("RecursionError"))
        assertNull(null)
    }

    @Test fun aTracedRunPausesThroughTheService() = runBlocking {
        val events = mutableListOf<RunEvent>()
        withTimeout(30_000) {
            repo.runPython("total = 0\nfor i in range(3):\n    total += i\nprint(total)", trace = true).collect { e ->
                events += e
                if (e is RunEvent.Paused) {
                    val pauses = events.count { it is RunEvent.Paused }
                    launch { repo.sendTraceCommand(if (pauses < 4) TraceCommand.STEP else TraceCommand.RUN_TO_END) }
                }
            }
        }
        val pauses = events.filterIsInstance<RunEvent.Paused>()
        assertEquals(listOf(1, 2, 3, 2), pauses.map { it.step.line })
        assertEquals(listOf("total" to "0", "i" to "0"), pauses[3].step.frames.single().vars.map { it.name to it.value })
        assertEquals("3\n", output(events))
        assertEquals(RunEvent.Finished(null), events.last())
    }
}
