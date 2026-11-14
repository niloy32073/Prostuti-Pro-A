package com.prostuti.student.ui.screen.playground.code

import com.prostuti.student.domain.code.CodeError
import com.prostuti.student.domain.code.CodeLanguage
import com.prostuti.student.domain.code.flow.FlowProgram
import com.prostuti.student.domain.code.run.RuntimeState
import com.prostuti.student.domain.code.run.TraceStep

data class CodeVisualizerUiState(
    val language: CodeLanguage = CodeLanguage.C,
    val code: String = "",
    /** The last program that was read without errors; kept while the code has an [error]. */
    val program: FlowProgram? = null,
    val error: CodeError? = null,
    /** Name of the function whose chart is shown (null = the first chart). */
    val chartName: String? = null,
    /** The code line picked in the editor or on the chart (1-based). */
    val selectedLine: Int? = null,
    /** True when [selectedLine] came from the chart, so the editor should scroll to it. */
    val lineFromChart: Boolean = false,
    /** Bumped when the code is replaced as a whole (language switch, example): the chart re-fits. */
    val fitRequest: Int = 0,
    /** Code an example replaced, offered back through "undo" once. */
    val undoCode: String? = null,
    // ── Running Python ──
    val runtime: RuntimeState = RuntimeState.NotInstalled,
    val runStatus: RunStatus = RunStatus.IDLE,
    val console: List<ConsoleChunk> = emptyList(),
    /** The line a run's error points at (cleared when the code changes). */
    val runErrorLine: Int? = null,
    /** The "download Python?" dialog is open. */
    val askDownload: Boolean = false,
    /** Bumped when a run starts, so a phone switches to the output tab. */
    val consoleRequest: Int = 0,
    // ── Tracing (step-by-step run) ──
    /** The run pauses before each line, until "run to end". */
    val tracing: Boolean = false,
    /** The last paused step: line, call stack, variables (kept after the run ends). */
    val trace: TraceStep? = null,
    /** Paths (see TraceChanges) of the values that changed in the last step. */
    val traceChanged: Set<String> = emptySet(),
    /** Steps are taken automatically every [TRACE_SPEEDS_MS]`[playSpeed]` ms. */
    val playing: Boolean = false,
    val playSpeed: Int = 1,
    /** Bumped when a traced run starts, so a phone shows the code. */
    val traceRequest: Int = 0,
) {
    /** The line a running trace is at: highlighted in the editor and on the chart. */
    val traceLine: Int? get() = if (tracing) trace?.line else null

    /** The Python runtime matters only while Python is selected. */
    val activeRuntime: RuntimeState get() = if (language == CodeLanguage.PYTHON) runtime else RuntimeState.Installed

    val chartIndex: Int
        get() = program?.functions?.indexOfFirst { it.name == chartName }?.takeIf { it >= 0 } ?: 0
}

/** Auto-play speeds of a trace: slow, medium, fast (ms per step). */
val TRACE_SPEEDS_MS = longArrayOf(1200L, 600L, 250L)
