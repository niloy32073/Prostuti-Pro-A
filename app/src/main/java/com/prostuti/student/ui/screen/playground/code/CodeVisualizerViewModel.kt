package com.prostuti.student.ui.screen.playground.code

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.InstallPythonRuntimeUseCase
import com.prostuti.student.domain.ObservePythonRuntimeUseCase
import com.prostuti.student.domain.RunCUseCase
import com.prostuti.student.domain.RunPythonUseCase
import com.prostuti.student.domain.SendRunInputUseCase
import com.prostuti.student.domain.SendTraceCommandUseCase
import com.prostuti.student.domain.code.CodeLanguage
import com.prostuti.student.domain.code.flow.functionAt
import com.prostuti.student.domain.code.run.PythonTraceback
import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.RunFailure
import com.prostuti.student.domain.code.run.RuntimeState
import com.prostuti.student.domain.code.run.TraceChanges
import com.prostuti.student.domain.code.run.TraceCommand
import com.prostuti.student.domain.code.run.TraceStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the code (one draft per language, kept in [SavedStateHandle]) and re-reads it into
 * flowcharts shortly after typing stops. A syntax error keeps the last good charts on screen.
 * Code can also run: C with the built-in interpreter, Python with a runtime downloaded on the first
 * run (after the student agrees). Output and input (`input()`, `scanf`) go through the console.
 * A traced run pauses before each line: the student steps, plays it at a chosen speed, or lets it
 * run to the end; the current line, its chart and the variables are shown at every pause.
 */
class CodeVisualizerViewModel(
    private val buildFlow: BuildCodeFlowUseCase,
    private val compute: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
    observeRuntime: ObservePythonRuntimeUseCase,
    private val installRuntime: InstallPythonRuntimeUseCase,
    private val runPython: RunPythonUseCase,
    private val sendRunInput: SendRunInputUseCase,
    private val runC: RunCUseCase,
    private val sendTraceCommand: SendTraceCommandUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<CodeVisualizerUiState> = _state.asStateFlow()

    private var parseJob: Job? = null
    private var runJob: Job? = null
    /** The student asked to run before the runtime was ready: run once it is installed. */
    private var runWhenInstalled = false
    /** Whether that pending run is traced. */
    private var pendingTrace = false
    private var autoStepJob: Job? = null

    init {
        parse(delayMs = 0)
        viewModelScope.launch { observeRuntime().collect(::onRuntime) }
    }

    fun onCodeChange(code: String, cursorLine: Int?) {
        val changed = code != _state.value.code
        // A trace's lines and variables belong to the code as it was: editing ends it.
        if (changed && _state.value.tracing && _state.value.runStatus.isActive) onStop()
        _state.update {
            it.copy(
                code = code, selectedLine = cursorLine, lineFromChart = false,
                runErrorLine = if (changed) null else it.runErrorLine,
                trace = if (changed) null else it.trace,
                traceChanged = if (changed) emptySet() else it.traceChanged,
            )
        }
        if (changed) {
            savedStateHandle[codeKey(_state.value.language)] = code
            parse(delayMs = PARSE_DELAY_MS)
        }
    }

    fun selectLanguage(language: CodeLanguage) {
        if (language == _state.value.language) return
        savedStateHandle[KEY_LANGUAGE] = language.name
        val code = savedStateHandle.get<String>(codeKey(language)) ?: CodeExamples.starter(language)
        _state.update {
            it.copy(
                language = language, code = code, program = null, error = null, chartName = null,
                selectedLine = null, fitRequest = it.fitRequest + 1, undoCode = null,
            )
        }
        parse(delayMs = 0)
    }

    fun loadExample(example: CodeExample) {
        val previous = _state.value.code
        val code = CodeExamples.code(example, _state.value.language)
        replaceCode(code)
        _state.update { it.copy(undoCode = previous.takeIf { p -> p != code }) }
    }

    fun undoExample() {
        val code = _state.value.undoCode ?: return
        replaceCode(code)
    }

    fun onUndoOffered() = _state.update { it.copy(undoCode = null) }

    fun selectChart(name: String) = _state.update { it.copy(chartName = name, fitRequest = it.fitRequest + 1) }

    /** A tap on a chart symbol: highlight its code line; a call symbol opens the called function's chart. */
    fun onNodeTap(line: Int, callee: String?) {
        val opens = callee != null && _state.value.program?.functions?.any { it.name == callee } == true
        _state.update {
            it.copy(
                selectedLine = line, lineFromChart = true,
                chartName = if (opens) callee else it.chartName,
                fitRequest = if (opens) it.fitRequest + 1 else it.fitRequest,
            )
        }
    }

    /** Scrolls the editor to [line] and highlights it (the error banner's tap). */
    fun showLine(line: Int) = _state.update { it.copy(selectedLine = line, lineFromChart = true) }

    // ── Running ──

    fun onRun() = requestRun(trace = false)

    /** Runs step by step: paused before the first line. */
    fun onTrace() = requestRun(trace = true)

    private fun requestRun(trace: Boolean) {
        val s = _state.value
        pendingTrace = trace
        if (s.language == CodeLanguage.C) { startRun(trace); return }   // the C interpreter is built in
        when (s.runtime) {
            RuntimeState.Installed -> startRun(trace)
            is RuntimeState.Downloading, RuntimeState.Installing, RuntimeState.NeedsConfirmation -> runWhenInstalled = true
            RuntimeState.NotInstalled, is RuntimeState.Failed -> _state.update { it.copy(askDownload = true) }
        }
    }

    fun onConfirmDownload() {
        runWhenInstalled = true
        _state.update {
            it.copy(askDownload = false, runStatus = RunStatus.PREPARING, console = emptyList(), consoleRequest = it.consoleRequest + 1, undoCode = null)
        }
        installRuntime()
    }

    fun onDismissDownload() = _state.update { it.copy(askDownload = false) }

    fun onStop() {
        runWhenInstalled = false
        autoStepJob?.cancel()
        if (!_state.value.runStatus.isActive) return
        runJob?.cancel()
        runJob = null
        _state.update {
            it.copy(runStatus = if (it.runStatus == RunStatus.PREPARING) RunStatus.IDLE else RunStatus.STOPPED, tracing = false, playing = false)
        }
    }

    // ── Tracing ──

    /** Runs the paused line and pauses at the next one. */
    fun onTraceStep() {
        autoStepJob?.cancel()
        if (_state.value.runStatus != RunStatus.PAUSED) return
        _state.update { it.copy(runStatus = RunStatus.RUNNING) }
        sendTraceCommand(TraceCommand.STEP)
    }

    fun onTracePlay() {
        _state.update { it.copy(playing = true) }
        if (_state.value.runStatus == RunStatus.PAUSED) onTraceStep()
    }

    fun onTracePause() {
        autoStepJob?.cancel()
        _state.update { it.copy(playing = false) }
    }

    /** Slow → medium → fast → slow. */
    fun onTraceSpeed() = _state.update { it.copy(playSpeed = (it.playSpeed + 1) % TRACE_SPEEDS_MS.size) }

    /** Stops pausing; the program runs on to its end like a normal run. */
    fun onTraceToEnd() {
        autoStepJob?.cancel()
        val paused = _state.value.runStatus == RunStatus.PAUSED
        _state.update { it.copy(tracing = false, playing = false, runStatus = if (paused) RunStatus.RUNNING else it.runStatus) }
        // Not paused right now (between steps, or waiting for input): the next pause gets the answer.
        if (paused) sendTraceCommand(TraceCommand.RUN_TO_END)
    }

    private fun onPaused(step: TraceStep) {
        if (!_state.value.tracing) {
            sendTraceCommand(TraceCommand.RUN_TO_END)
            return
        }
        _state.update { s ->
            val chart = s.program?.takeIf { s.error == null }?.functionAt(step.line)?.name
            s.copy(
                runStatus = RunStatus.PAUSED,
                trace = step,
                traceChanged = TraceChanges.changed(s.trace, step),
                chartName = chart ?: s.chartName,
            )
        }
        if (_state.value.playing) {
            autoStepJob?.cancel()
            autoStepJob = viewModelScope.launch {
                delay(TRACE_SPEEDS_MS[_state.value.playSpeed])
                if (_state.value.playing) onTraceStep()
            }
        }
    }

    fun onSubmitInput(line: String) {
        if (_state.value.runStatus != RunStatus.WAITING_INPUT) return
        _state.update { it.copy(runStatus = RunStatus.RUNNING, console = it.console.append(line + "\n", ConsoleKind.INPUT)) }
        sendRunInput(line)
    }

    fun onClearConsole() {
        if (_state.value.runStatus.isActive) return
        _state.update { it.copy(console = emptyList(), runStatus = RunStatus.IDLE, runErrorLine = null) }
    }

    private fun onRuntime(runtime: RuntimeState) {
        _state.update { it.copy(runtime = runtime) }
        when (runtime) {
            RuntimeState.Installed -> if (runWhenInstalled) { runWhenInstalled = false; startRun(pendingTrace) }
            is RuntimeState.Failed, RuntimeState.NotInstalled -> if (runWhenInstalled || _state.value.runStatus == RunStatus.PREPARING) {
                runWhenInstalled = false
                _state.update { it.copy(runStatus = RunStatus.IDLE) }
            }
            else -> Unit
        }
    }

    private fun startRun(trace: Boolean) {
        runJob?.cancel()
        autoStepJob?.cancel()
        val code = _state.value.code
        val events = if (_state.value.language == CodeLanguage.C) runC(code, trace) else runPython(code, trace)
        _state.update {
            // Running ends the example's "undo" offer: its snackbar would cover the console's input row.
            it.copy(
                runStatus = RunStatus.STARTING, console = emptyList(), runErrorLine = null, undoCode = null,
                consoleRequest = if (trace) it.consoleRequest else it.consoleRequest + 1,
                traceRequest = if (trace) it.traceRequest + 1 else it.traceRequest,
                tracing = trace, playing = false, trace = null, traceChanged = emptySet(),
            )
        }
        runJob = viewModelScope.launch {
            events.collect { event ->
                if (event is RunEvent.Paused) { onPaused(event.step); return@collect }
                if (event is RunEvent.Finished || event is RunEvent.Failed) autoStepJob?.cancel()
                _state.update { s ->
                    when (event) {
                        is RunEvent.Paused -> s
                        is RunEvent.Output -> s.copy(
                            runStatus = if (s.runStatus == RunStatus.STARTING) RunStatus.RUNNING else s.runStatus,
                            console = s.console.append(event.text, if (event.isError) ConsoleKind.ERR else ConsoleKind.OUT),
                        )
                        RunEvent.InputRequested -> s.copy(runStatus = RunStatus.WAITING_INPUT)
                        is RunEvent.Finished -> if (event.error == null) s.copy(runStatus = RunStatus.DONE, tracing = false, playing = false) else s.copy(
                            runStatus = RunStatus.ERROR, tracing = false, playing = false,
                            console = s.console.append(event.error, ConsoleKind.ERR),
                            runErrorLine = event.errorLine ?: PythonTraceback.errorLine(event.error),
                        )
                        is RunEvent.Failed -> s.copy(
                            tracing = false, playing = false,
                            runStatus = when (event.reason) {
                                RunFailure.TIME_LIMIT -> RunStatus.TIME_LIMIT
                                RunFailure.OUTPUT_LIMIT -> RunStatus.OUTPUT_LIMIT
                                RunFailure.CRASHED -> RunStatus.CRASHED
                            },
                        )
                    }
                }
            }
        }
    }

    private fun replaceCode(code: String) {
        savedStateHandle[codeKey(_state.value.language)] = code
        _state.update { it.copy(code = code, chartName = null, selectedLine = null, fitRequest = it.fitRequest + 1, undoCode = null) }
        parse(delayMs = 0)
    }

    private fun parse(delayMs: Long) {
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            val language = _state.value.language
            val code = _state.value.code
            val result = withContext(compute) { buildFlow(language, code) }
            _state.update { s ->
                if (s.language != language || s.code != code) return@update s
                if (result.program != null) s.copy(program = result.program, error = null) else s.copy(error = result.error)
            }
        }
    }

    private fun initialState(): CodeVisualizerUiState {
        val language = savedStateHandle.get<String>(KEY_LANGUAGE)
            ?.let { name -> CodeLanguage.entries.firstOrNull { it.name == name } } ?: CodeLanguage.C
        val code = savedStateHandle.get<String>(codeKey(language)) ?: CodeExamples.starter(language)
        return CodeVisualizerUiState(language = language, code = code)
    }

    private fun codeKey(language: CodeLanguage) = "code_${language.name}"

    /** Adds [text] to the console, merged into the last chunk when it is of the same [kind]. */
    private fun List<ConsoleChunk>.append(text: String, kind: ConsoleKind): List<ConsoleChunk> {
        val last = lastOrNull()
        return if (last != null && last.kind == kind) dropLast(1) + last.copy(text = last.text + text) else this + ConsoleChunk(text, kind)
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val PARSE_DELAY_MS = 250L
    }
}
