package com.prostuti.student.ui.screen.playground.code

import androidx.lifecycle.SavedStateHandle
import com.prostuti.student.data.repository.fake.FakeCodeRunRepository
import com.prostuti.student.data.repository.fake.FakePythonRuntimeRepository
import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.InstallPythonRuntimeUseCase
import com.prostuti.student.domain.ObservePythonRuntimeUseCase
import com.prostuti.student.domain.RunCUseCase
import com.prostuti.student.domain.RunPythonUseCase
import com.prostuti.student.domain.SendRunInputUseCase
import com.prostuti.student.domain.SendTraceCommandUseCase
import com.prostuti.student.domain.code.run.InstallFailure
import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.RunFailure
import com.prostuti.student.domain.code.run.RuntimeState
import com.prostuti.student.domain.code.run.TraceCommand
import com.prostuti.student.domain.code.run.TraceFrame
import com.prostuti.student.domain.code.run.TraceStep
import com.prostuti.student.domain.code.run.TraceVar
import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.domain.code.CodeLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CodeVisualizerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    private val runtime = FakePythonRuntimeRepository()
    private val runner = FakeCodeRunRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = CodeVisualizerViewModel(
        BuildCodeFlowUseCase(), dispatcher, handle,
        ObservePythonRuntimeUseCase(runtime), InstallPythonRuntimeUseCase(runtime), RunPythonUseCase(runner), SendRunInputUseCase(runner),
        RunCUseCase(runner), SendTraceCommandUseCase(runner),
    )

    private fun pythonViewModel(): CodeVisualizerViewModel = viewModel().also { it.selectLanguage(CodeLanguage.PYTHON) }

    @Test fun cRunsAtOnceWithoutADownload() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onRun()
        advanceUntilIdle()
        assertFalse(vm.state.value.askDownload)
        assertEquals(listOf("C"), runner.languages)
        assertEquals(0, runtime.installs)
        runner.events.send(RunEvent.Finished("error: line 4: 'x' is not declared\n", errorLine = 4))
        advanceUntilIdle()
        assertEquals(RunStatus.ERROR, vm.state.value.runStatus)
        assertEquals(4, vm.state.value.runErrorLine)
    }

    @Test fun firstRunAsksToDownloadThenRunsWhenInstalled() = runTest(dispatcher) {
        val vm = pythonViewModel()
        vm.onRun()
        assertTrue(vm.state.value.askDownload)
        vm.onDismissDownload()
        assertFalse(vm.state.value.askDownload)
        assertEquals(0, runtime.installs)

        vm.onRun()
        vm.onConfirmDownload()
        assertEquals(1, runtime.installs)
        assertEquals(RunStatus.PREPARING, vm.state.value.runStatus)
        runtime.state.value = RuntimeState.Downloading(5, 10)
        advanceUntilIdle()
        assertEquals(RuntimeState.Downloading(5, 10), vm.state.value.runtime)
        assertTrue(runner.codes.isEmpty())
        runtime.state.value = RuntimeState.Installed
        advanceUntilIdle()
        assertEquals(listOf(vm.state.value.code), runner.codes)
        assertEquals(RunStatus.STARTING, vm.state.value.runStatus)
    }

    @Test fun aFailedDownloadReturnsToIdle() = runTest(dispatcher) {
        val vm = pythonViewModel()
        vm.onRun(); vm.onConfirmDownload()
        runtime.state.value = RuntimeState.Failed(InstallFailure.NO_NETWORK, -6)
        advanceUntilIdle()
        assertEquals(RunStatus.IDLE, vm.state.value.runStatus)
        assertTrue(runner.codes.isEmpty())
    }

    @Test fun outputInputAndErrorLine() = runTest(dispatcher) {
        runtime.state.value = RuntimeState.Installed
        val vm = pythonViewModel()
        advanceUntilIdle()
        val request = vm.state.value.consoleRequest
        vm.onRun()
        advanceUntilIdle()
        assertEquals(request + 1, vm.state.value.consoleRequest)
        assertNull(vm.state.value.undoCode)
        runner.events.send(RunEvent.Output("n = ", false))
        runner.events.send(RunEvent.InputRequested)
        advanceUntilIdle()
        assertEquals(RunStatus.WAITING_INPUT, vm.state.value.runStatus)
        vm.onSubmitInput("5")
        assertEquals(listOf("5"), runner.inputs)
        runner.events.send(RunEvent.Output("25\n", false))
        runner.events.send(RunEvent.Output("more\n", false))
        runner.events.send(RunEvent.Finished("Traceback (most recent call last):\n  File \"main.py\", line 3, in <module>\nZeroDivisionError: division by zero\n"))
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(RunStatus.ERROR, s.runStatus)
        assertEquals(3, s.runErrorLine)
        assertEquals(listOf(ConsoleKind.OUT, ConsoleKind.INPUT, ConsoleKind.OUT, ConsoleKind.ERR), s.console.map { it.kind })
        assertEquals("25\nmore\n", s.console[2].text)
        vm.onCodeChange(s.code + "\n", 1)
        assertNull(vm.state.value.runErrorLine)
    }

    @Test fun stopCancelsTheRunAndLimitsShowTheirStatus() = runTest(dispatcher) {
        runtime.state.value = RuntimeState.Installed
        val vm = pythonViewModel()
        advanceUntilIdle()
        vm.onRun()
        advanceUntilIdle()
        vm.onStop()
        advanceUntilIdle()
        assertEquals(RunStatus.STOPPED, vm.state.value.runStatus)
        assertEquals(1, runner.cancelled)

        vm.onRun()
        advanceUntilIdle()
        runner.events.send(RunEvent.Failed(RunFailure.TIME_LIMIT))
        advanceUntilIdle()
        assertEquals(RunStatus.TIME_LIMIT, vm.state.value.runStatus)
        vm.onClearConsole()
        assertEquals(RunStatus.IDLE, vm.state.value.runStatus)
    }

    @Test fun startsWithTheCStarterAndItsChart() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(CodeLanguage.C, s.language)
        assertEquals(CodeExamples.starter(CodeLanguage.C), s.code)
        assertEquals(listOf("main"), s.program!!.functions.map { it.name })
        assertNull(s.error)
    }

    @Test fun aSyntaxErrorKeepsTheLastGoodChart() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val good = vm.state.value.program
        vm.onCodeChange("int main() {\n  int a = 1\n}", 2)
        advanceUntilIdle()
        assertEquals(CodeErrorKind.MISSING_SEMICOLON, vm.state.value.error!!.kind)
        assertEquals(good, vm.state.value.program)
        vm.onCodeChange("int main() {\n  int a = 1;\n}", 2)
        advanceUntilIdle()
        assertNull(vm.state.value.error)
    }

    @Test fun eachLanguageKeepsItsOwnDraftAcrossProcessDeath() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.onCodeChange("int main() { return 0; }", 1)
        vm.selectLanguage(CodeLanguage.PYTHON)
        advanceUntilIdle()
        assertEquals(CodeExamples.starter(CodeLanguage.PYTHON), vm.state.value.code)
        vm.onCodeChange("print(1)", 1)
        vm.selectLanguage(CodeLanguage.C)
        assertEquals("int main() { return 0; }", vm.state.value.code)
        vm.selectLanguage(CodeLanguage.PYTHON)
        val restored = viewModel(handle)
        advanceUntilIdle()
        assertEquals(CodeLanguage.PYTHON, restored.state.value.language)
        assertEquals("print(1)", restored.state.value.code)
    }

    @Test fun anExampleCanBeUndone() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCodeChange("int main() { int mine; }", 1)
        val fit = vm.state.value.fitRequest
        val example = CodeExamples.all.last()
        vm.loadExample(example)
        assertEquals(example.c, vm.state.value.code)
        assertEquals("int main() { int mine; }", vm.state.value.undoCode)
        assertEquals(fit + 1, vm.state.value.fitRequest)
        vm.undoExample()
        assertEquals("int main() { int mine; }", vm.state.value.code)
        assertNull(vm.state.value.undoCode)
    }

    @Test fun tappingACallOpensThatFunctionsChart() = runTest(dispatcher) {
        val vm = viewModel()
        vm.loadExample(CodeExamples.all.first { it.c.contains("factorial(") })
        advanceUntilIdle()
        assertEquals(listOf("main", "factorial"), vm.state.value.program!!.functions.map { it.name })
        vm.onNodeTap(line = 13, callee = "factorial")
        val s = vm.state.value
        assertEquals("factorial", s.chartName)
        assertEquals(1, s.chartIndex)
        assertEquals(13, s.selectedLine)
        vm.onNodeTap(line = 4, callee = "printf")   // not a function of this program
        assertEquals("factorial", vm.state.value.chartName)
        assertNotNull(vm.state.value.program)
    }

    private fun step(line: Int, n: String, function: String = "main") =
        TraceStep(line, listOf(TraceFrame(function, line, listOf(TraceVar("n", n)))))

    @Test fun traceStepsPlaysPausesAndRunsToEnd() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onTrace()
        advanceUntilIdle()
        assertEquals(listOf(true), runner.traced)
        assertTrue(vm.state.value.tracing)
        runner.events.send(RunEvent.Paused(step(2, "0")))
        advanceUntilIdle()
        assertEquals(RunStatus.PAUSED, vm.state.value.runStatus)
        assertEquals(2, vm.state.value.traceLine)

        vm.onTraceStep()
        assertEquals(listOf(TraceCommand.STEP), runner.commands)
        assertEquals(RunStatus.RUNNING, vm.state.value.runStatus)
        runner.events.send(RunEvent.Paused(step(3, "1")))
        advanceUntilIdle()
        assertEquals(setOf("0/main/n"), vm.state.value.traceChanged)

        // Play: steps now, and again on its own after the chosen delay.
        vm.onTracePlay()
        assertEquals(2, runner.commands.size)
        runner.events.send(RunEvent.Paused(step(4, "1")))
        advanceTimeBy(TRACE_SPEEDS_MS[vm.state.value.playSpeed] + 1)
        assertEquals(3, runner.commands.size)
        vm.onTracePause()
        runner.events.send(RunEvent.Paused(step(5, "2")))
        advanceUntilIdle()
        assertEquals(3, runner.commands.size)

        vm.onTraceToEnd()
        assertEquals(TraceCommand.RUN_TO_END, runner.commands.last())
        assertFalse(vm.state.value.tracing)
        assertNull(vm.state.value.traceLine)
        runner.events.send(RunEvent.Finished(null))
        advanceUntilIdle()
        assertEquals(RunStatus.DONE, vm.state.value.runStatus)
        assertEquals(5, vm.state.value.trace?.line)   // the last values stay visible
    }

    @Test fun aPauseAfterRunToEndIsAnsweredAtOnce() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onTrace()
        advanceUntilIdle()
        runner.events.send(RunEvent.InputRequested)
        advanceUntilIdle()
        vm.onTraceToEnd()            // waiting for input, not paused: nothing to send yet
        assertTrue(runner.commands.isEmpty())
        runner.events.send(RunEvent.Paused(step(4, "1")))
        advanceUntilIdle()
        assertEquals(listOf(TraceCommand.RUN_TO_END), runner.commands)
        assertNull(vm.state.value.trace)
    }

    @Test fun editingTheCodeEndsATraceAndTheChartFollowsIt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCodeChange("int sq(int x) {\n  return x * x;\n}\nint main() {\n  int y = sq(3);\n  return y;\n}", null)
        advanceUntilIdle()
        vm.onTrace()
        advanceUntilIdle()
        runner.events.send(RunEvent.Paused(step(2, "3", function = "sq")))
        advanceUntilIdle()
        assertEquals("sq", vm.state.value.chartName)
        vm.onCodeChange(vm.state.value.code + "\n", null)
        assertEquals(RunStatus.STOPPED, vm.state.value.runStatus)
        assertFalse(vm.state.value.tracing)
        assertNull(vm.state.value.trace)
        advanceUntilIdle()
        assertEquals(1, runner.cancelled)
    }
}
