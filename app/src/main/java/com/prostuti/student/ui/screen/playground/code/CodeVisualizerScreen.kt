package com.prostuti.student.ui.screen.playground.code

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.domain.code.CodeError
import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.domain.code.CodeLanguage
import com.prostuti.student.domain.code.flow.FlowBlock
import com.prostuti.student.domain.code.flow.FlowFunction
import com.prostuti.student.domain.code.flow.FlowProgram
import com.prostuti.student.domain.code.flow.StepKind
import com.prostuti.student.domain.code.run.RuntimeState
import com.prostuti.student.data.service.python.PythonInstallConfirmation
import com.prostuti.student.ui.screen.playground.code.component.RunConsole
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.screen.playground.code.component.CodeEditor
import com.prostuti.student.ui.screen.playground.code.component.CodeErrorBanner
import com.prostuti.student.ui.screen.playground.code.component.CodeExamplesSheet
import com.prostuti.student.ui.screen.playground.code.component.FlowchartView
import com.prostuti.student.ui.screen.playground.code.component.TraceControls
import com.prostuti.student.ui.screen.playground.code.component.VariablesPanel
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Playground → Code Visualizer: write C or Python, see the program's flowchart, run it or step through it. */
@Composable
fun CodeVisualizerScreen(
    onBack: () -> Unit,
    viewModel: CodeVisualizerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val loaded = stringResource(R.string.code_example_loaded)
    val undo = stringResource(R.string.code_undo)
    LaunchedEffect(state.undoCode) {
        if (state.undoCode == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(loaded, actionLabel = undo, withDismissAction = true, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoExample() else viewModel.onUndoOffered()
    }
    // Play may ask the student to approve the download (e.g. on mobile data) on its own screen.
    val confirmation = koinInject<PythonInstallConfirmation>()
    val confirmLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    LaunchedEffect(state.runtime) {
        if (state.runtime == RuntimeState.NeedsConfirmation) confirmation.confirm(confirmLauncher)
    }
    CodeVisualizerContent(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onCodeChange = viewModel::onCodeChange,
        onLanguage = viewModel::selectLanguage,
        onExample = viewModel::loadExample,
        onChart = viewModel::selectChart,
        onNodeTap = viewModel::onNodeTap,
        onShowLine = viewModel::showLine,
        run = RunActions(
            onRun = viewModel::onRun,
            onStop = viewModel::onStop,
            onConfirmDownload = viewModel::onConfirmDownload,
            onDismissDownload = viewModel::onDismissDownload,
            onSubmitInput = viewModel::onSubmitInput,
            onClear = viewModel::onClearConsole,
            onTrace = viewModel::onTrace,
            onTraceStep = viewModel::onTraceStep,
            onTracePlay = viewModel::onTracePlay,
            onTracePause = viewModel::onTracePause,
            onTraceSpeed = viewModel::onTraceSpeed,
            onTraceToEnd = viewModel::onTraceToEnd,
        ),
    )
}

/** What the run and trace buttons, the console and the download dialog do. */
data class RunActions(
    val onRun: () -> Unit = {},
    val onTrace: () -> Unit = {},
    val onTraceStep: () -> Unit = {},
    val onTracePlay: () -> Unit = {},
    val onTracePause: () -> Unit = {},
    val onTraceSpeed: () -> Unit = {},
    val onTraceToEnd: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onConfirmDownload: () -> Unit = {},
    val onDismissDownload: () -> Unit = {},
    val onSubmitInput: (String) -> Unit = {},
    val onClear: () -> Unit = {},
)

@Composable
fun CodeVisualizerContent(
    state: CodeVisualizerUiState,
    onBack: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onCodeChange: (String, Int?) -> Unit = { _, _ -> },
    onLanguage: (CodeLanguage) -> Unit = {},
    onExample: (CodeExample) -> Unit = {},
    onChart: (String) -> Unit = {},
    onNodeTap: (line: Int, callee: String?) -> Unit = { _, _ -> },
    onShowLine: (Int) -> Unit = {},
    run: RunActions = RunActions(),
) {
    var showExamples by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(TAB_CODE) }
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val tabs = rememberSaveableStateHolder()
    if (showExamples) CodeExamplesSheet(onPick = { onExample(it); showExamples = false }, onDismiss = { showExamples = false })
    if (state.askDownload) DownloadDialog(onConfirm = run.onConfirmDownload, onDismiss = run.onDismissDownload)

    // A new run shows the output tab on a phone (once per run, not again after rotation).
    var shownRequest by rememberSaveable { mutableIntStateOf(state.consoleRequest) }
    LaunchedEffect(state.consoleRequest) {
        if (state.consoleRequest != shownRequest) { shownRequest = state.consoleRequest; tab = TAB_OUTPUT }
    }
    // A step-by-step run starts on the code (its line is highlighted there).
    var shownTrace by rememberSaveable { mutableIntStateOf(state.traceRequest) }
    LaunchedEffect(state.traceRequest) {
        if (state.traceRequest != shownTrace) { shownTrace = state.traceRequest; tab = TAB_CODE }
    }
    // The program asks for input: the input row is on the output tab.
    LaunchedEffect(state.runStatus) {
        if (state.runStatus == RunStatus.WAITING_INPUT) tab = TAB_OUTPUT
    }

    val editor: @Composable (Modifier) -> Unit = { modifier ->
        EditorPane(state, onCodeChange, onLanguage, run, onShowLine = { line -> onShowLine(line); tab = TAB_CODE }, modifier)
    }
    val chart: @Composable (Modifier) -> Unit = { modifier -> ChartPane(state, onChart, onNodeTap, modifier) }
    val console: @Composable (Modifier) -> Unit = { modifier ->
        RunConsole(
            status = state.runStatus,
            runtime = state.activeRuntime,
            console = state.console,
            errorLine = state.runErrorLine,
            onSubmitInput = run.onSubmitInput,
            onStop = run.onStop,
            onClear = run.onClear,
            modifier = modifier,
            pausedLine = state.traceLine,
        )
    }
    val mainName = if (state.language == CodeLanguage.PYTHON) stringResource(R.string.code_main_program) else null
    val variables: @Composable (Modifier) -> Unit = { modifier ->
        VariablesPanel(
            trace = state.trace,
            changed = state.traceChanged,
            running = state.tracing && state.runStatus.isActive,
            mainName = mainName,
            modifier = modifier,
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.playground_code_visualizer),
                    style = ProstutiTypography.bnTitleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showExamples = true }, modifier = Modifier.testTag("code_examples")) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.code_examples), style = ProstutiTypography.bnLabelLarge)
                }
            }
            if (state.tracing && state.runStatus.isActive) {
                TraceControls(
                    paused = state.runStatus == RunStatus.PAUSED,
                    playing = state.playing,
                    speed = state.playSpeed,
                    onStep = run.onTraceStep,
                    onPlay = run.onTracePlay,
                    onPause = run.onTracePause,
                    onSpeed = run.onTraceSpeed,
                    onToEnd = run.onTraceToEnd,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        editor(Modifier.weight(1f).fillMaxWidth())
                        // The console sits under the code once there is something to show.
                        if (state.runStatus != RunStatus.IDLE || state.console.isNotEmpty() || state.activeRuntime is RuntimeState.Failed) {
                            HorizontalDivider()
                            console(Modifier.weight(0.8f).fillMaxWidth())
                        }
                    }
                    VerticalDivider()
                    Column(Modifier.weight(1.2f).fillMaxHeight()) {
                        chart(Modifier.weight(1f).fillMaxWidth())
                        // The variables sit under the chart once a step-by-step run has shown some.
                        if (state.trace != null) {
                            HorizontalDivider()
                            variables(Modifier.weight(0.8f).fillMaxWidth())
                        }
                    }
                }
            } else {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == TAB_CODE, onClick = { tab = TAB_CODE }, modifier = Modifier.testTag("tab_code"),
                        text = { Text(stringResource(R.string.code_tab_code), style = ProstutiTypography.bnLabelLarge) })
                    Tab(selected = tab == TAB_CHART, onClick = { tab = TAB_CHART }, modifier = Modifier.testTag("tab_flowchart"),
                        text = { Text(stringResource(R.string.code_tab_flowchart), style = ProstutiTypography.bnLabelLarge) })
                    Tab(selected = tab == TAB_OUTPUT, onClick = { tab = TAB_OUTPUT }, modifier = Modifier.testTag("tab_output"),
                        text = { Text(stringResource(R.string.code_tab_output), style = ProstutiTypography.bnLabelLarge) })
                    Tab(selected = tab == TAB_VARIABLES, onClick = { tab = TAB_VARIABLES }, modifier = Modifier.testTag("tab_variables"),
                        text = { Text(stringResource(R.string.code_tab_variables), style = ProstutiTypography.bnLabelLarge) })
                }
                tabs.SaveableStateProvider(tab) {
                    when (tab) {
                        TAB_CODE -> editor(Modifier.weight(1f).fillMaxWidth())
                        TAB_CHART -> chart(Modifier.weight(1f).fillMaxWidth())
                        TAB_VARIABLES -> variables(Modifier.weight(1f).fillMaxWidth())
                        else -> console(Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorPane(
    state: CodeVisualizerUiState,
    onCodeChange: (String, Int?) -> Unit,
    onLanguage: (CodeLanguage) -> Unit,
    run: RunActions,
    onShowLine: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.testTag("language_picker")) {
                LANGUAGES.forEachIndexed { i, (language, label) ->
                    SegmentedButton(
                        selected = state.language == language,
                        onClick = { onLanguage(language) },
                        shape = SegmentedButtonDefaults.itemShape(i, LANGUAGES.size),
                        modifier = Modifier.testTag("language_${language.name}"),
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (state.runStatus.isActive) {
                FilledTonalButton(onClick = run.onStop, modifier = Modifier.testTag("code_stop")) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.code_stop), style = ProstutiTypography.bnLabelLarge)
                }
            } else {
                val traceLabel = stringResource(R.string.code_trace)
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    tooltip = { PlainTooltip { Text(traceLabel) } },
                    state = rememberTooltipState(),
                ) {
                    FilledTonalIconButton(onClick = run.onTrace, modifier = Modifier.testTag("code_trace")) {
                        Icon(Icons.Filled.SlowMotionVideo, contentDescription = traceLabel)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = run.onRun, modifier = Modifier.testTag("code_run")) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.code_run), style = ProstutiTypography.bnLabelLarge)
                }
            }
        }
        state.error?.let { CodeErrorBanner(it, onClick = { onShowLine(it.line) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        HorizontalDivider()
        CodeEditor(
            code = state.code,
            language = state.language,
            highlightLine = state.traceLine ?: state.selectedLine.takeIf { state.lineFromChart },
            errorLine = state.error?.line ?: state.runErrorLine,
            scrollToLine = state.traceLine ?: state.selectedLine.takeIf { state.lineFromChart },
            onCodeChange = onCodeChange,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun ChartPane(
    state: CodeVisualizerUiState,
    onChart: (String) -> Unit,
    onNodeTap: (line: Int, callee: String?) -> Unit,
    modifier: Modifier,
) {
    val functions = state.program?.functions.orEmpty()
    Column(modifier) {
        if (state.error != null && functions.isNotEmpty()) {
            Text(
                stringResource(R.string.code_stale_note),
                style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("code_stale_note"),
            )
        }
        if (functions.size > 1) {
            LazyRow(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                itemsIndexed(functions, key = { _, f -> f.name }) { i, f ->
                    FilterChip(
                        selected = i == state.chartIndex,
                        onClick = { onChart(f.name) },
                        label = { Text(if (f.isEntry && state.language == CodeLanguage.PYTHON) stringResource(R.string.code_main_program) else f.name) },
                        modifier = Modifier.testTag("chart_${f.name}"),
                    )
                }
            }
        }
        if (functions.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.code_empty_hint),
                    style = ProstutiTypography.bnBodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("code_empty_hint"),
                )
            }
        } else {
            FlowchartView(
                function = functions[state.chartIndex],
                // A stale chart (the code has an error) has other line numbers: no highlight then.
                selectedLine = state.selectedLine.takeIf { state.error == null },
                fitRequest = state.fitRequest,
                onNodeTap = { onNodeTap(it.sourceLine, it.callee) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                activeLine = state.traceLine.takeIf { state.error == null },
            )
        }
    }
}

private const val TAB_CODE = 0
private const val TAB_CHART = 1
private const val TAB_OUTPUT = 2
private const val TAB_VARIABLES = 3

@Composable
private fun DownloadDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.code_download_title)) },
        text = { Text(stringResource(R.string.code_download_body), style = ProstutiTypography.bnBodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("download_python")) {
                Text(stringResource(R.string.code_download_yes))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.code_download_no)) } },
    )
}
private val LANGUAGES = listOf(CodeLanguage.C to "C", CodeLanguage.PYTHON to "Python")

@FormFactorPreviews
@Composable
private fun CodeVisualizerPreview() {
    val main = FlowFunction(
        "main", "main", 1,
        listOf(
            FlowBlock.Step(StepKind.IO, "n = int(input())", 1),
            FlowBlock.Branch("n % 2 == 0", listOf(FlowBlock.Step(StepKind.IO, "print(\"Even\")", 3)), listOf(FlowBlock.Step(StepKind.IO, "print(\"Odd\")", 5)), 2),
        ),
        isEntry = true,
    )
    ProstutiTheme {
        CodeVisualizerContent(
            state = CodeVisualizerUiState(
                language = CodeLanguage.PYTHON,
                code = "n = int(input())\nif n % 2 == 0:\n    print(\"Even\")\nelse\n    print(\"Odd\")",
                program = FlowProgram(listOf(main)),
                error = CodeError(4, CodeErrorKind.EXPECTED, ":"),
            ),
            onBack = {},
        )
    }
}
