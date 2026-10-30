package com.prostuti.student.ui.screen.examengine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.screen.examengine.component.McqCard
import com.prostuti.student.ui.screen.examengine.component.McqGridItem
import com.prostuti.student.ui.screen.examengine.component.PassageCard
import com.prostuti.student.ui.screen.examengine.component.groupQuestionsByPassage
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class DrawStroke(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidthPx: Float = 6f
)

/**
 * The shared attempt (question-answering) screen for every exam mode — practice, live and group
 * all finalize through the same submit flow. Once [ExamAttemptViewModel] reports the attempt
 * submitted, [onSubmitted] fires so the caller can move on to the (also mode-common) Result
 * screen; this screen itself never shows a result.
 */
@Composable
fun ExamAttemptScreen(
    attemptId: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: ExamAttemptViewModel = koinViewModel(parameters = { parametersOf(attemptId) })
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(
        errors = state.errors,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::clearError
    )

    LaunchedEffect(state.reportResultMessage) {
        val message = state.reportResultMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearReportResultMessage()
        }
    }

    LaunchedEffect(state.timeRemainingSeconds, state.isSubmitted) {
        if (state.timeRemainingSeconds > 0 && !state.isSubmitted) {
            delay(1000L)
            viewModel.tickTimer()
        }
    }

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) onSubmitted()
    }

    if (state.showSubmitDialog) {
        SubmitConfirmDialog(
            isSubmitting = state.isSubmitting,
            onConfirm = viewModel::submitExam,
            onDismiss = viewModel::dismissSubmitDialog
        )
    }

    val progress = if (state.totalDurationSeconds > 0) state.timeRemainingSeconds.toFloat() / state.totalDurationSeconds else 0f
    val minutes = state.timeRemainingSeconds / 60
    val seconds = state.timeRemainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    // Right panel state — kept here so it survives tab switches and panel hide/show
    var selectedTab by remember { mutableStateOf(0) }      // 0 = calculator, 1 = canvas
    var isPanelVisible by remember { mutableStateOf(true) }
    var canvasStrokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var canvasIsErasing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Extended (≥840dp): 2-column questions + right panel always visible
                maxWidth >= 840.dp -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ExamContent(
                            state = state,
                            viewModel = viewModel,
                            progress = progress,
                            timeText = timeText,
                            questionColumns = 2,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RightPanel(
                            selectedTab = selectedTab,
                            onTabSelect = { selectedTab = it },
                            canvasStrokes = canvasStrokes,
                            canvasIsErasing = canvasIsErasing,
                            onStrokeFinished = { canvasStrokes = canvasStrokes + it },
                            onEraserToggle = { canvasIsErasing = !canvasIsErasing },
                            onCanvasClear = { canvasStrokes = emptyList() },
                            showHideButton = false,
                            onHide = {},
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                        )
                    }
                }

                // Medium (600–840dp): 1-col when panel visible, 2-col when hidden; hide/show toggle
                maxWidth >= 600.dp -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ExamContent(
                            state = state,
                            viewModel = viewModel,
                            progress = progress,
                            timeText = timeText,
                            questionColumns = if (isPanelVisible) 1 else 2,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        if (isPanelVisible) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            RightPanel(
                                selectedTab = selectedTab,
                                onTabSelect = { selectedTab = it },
                                canvasStrokes = canvasStrokes,
                                canvasIsErasing = canvasIsErasing,
                                onStrokeFinished = { canvasStrokes = canvasStrokes + it },
                                onEraserToggle = { canvasIsErasing = !canvasIsErasing },
                                onCanvasClear = { canvasStrokes = emptyList() },
                                showHideButton = true,
                                onHide = { isPanelVisible = false },
                                modifier = Modifier
                                    .width(260.dp)
                                    .fillMaxHeight()
                            )
                        } else {
                            // Strip to reveal the panel again
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { isPanelVisible = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "প্যানেল দেখাও",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Compact (<600dp): single column, no side panel
                else -> {
                    ExamContent(
                        state = state,
                        viewModel = viewModel,
                        progress = progress,
                        timeText = timeText,
                        questionColumns = 1,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ── Exam content column (questions + header + submit) ───────────────────────

@Composable
private fun ExamContent(
    state: ExamAttemptUiState,
    viewModel: ExamAttemptViewModel,
    progress: Float,
    timeText: String,
    questionColumns: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable(onClick = viewModel::showSubmitDialog),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "এক্সাম বন্ধ করো",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${state.selectedAnswers.size}/${state.questions.size} উত্তর দেওয়া হয়েছে",
                style = ProstutiTypography.enLabelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = timeText,
                    style = ProstutiTypography.enLabelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(50.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )

        Spacer(modifier = Modifier.height(8.dp))

        val hPadding = if (questionColumns > 1) 8.dp else 16.dp
        val hSpacing = if (questionColumns > 1) 8.dp else 0.dp
        val gridItems = remember(state.questions, state.passages) {
            groupQuestionsByPassage(state.questions, state.passages)
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(questionColumns),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = hPadding, end = hPadding, bottom = 16.dp),
            verticalItemSpacing = 14.dp,
            horizontalArrangement = Arrangement.spacedBy(hSpacing)
        ) {
            items(gridItems, key = { it.key }) { item ->
                when (item) {
                    is McqGridItem.Standalone -> McqCard(
                        index = item.index,
                        question = item.question,
                        selectedOptionId = state.selectedAnswers[item.question.mcqId],
                        onOptionSelected = { optionId -> viewModel.selectAnswer(item.question.mcqId, optionId) },
                        isFavorite = item.question.mcqId in state.favoriteMcqIds,
                        onToggleFavorite = { viewModel.toggleFavorite(item.question.mcqId) },
                        onReport = { reason, description -> viewModel.submitReport(item.question.mcqId, reason, description) }
                    )

                    is McqGridItem.PassageGroup -> PassageCard(passage = item.passage) {
                        item.items.forEachIndexed { groupIndex, (questionIndex, question) ->
                            if (groupIndex > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            McqCard(
                                index = questionIndex,
                                question = question,
                                selectedOptionId = state.selectedAnswers[question.mcqId],
                                onOptionSelected = { optionId -> viewModel.selectAnswer(question.mcqId, optionId) },
                                isFavorite = question.mcqId in state.favoriteMcqIds,
                                onToggleFavorite = { viewModel.toggleFavorite(question.mcqId) },
                                onReport = { reason, description -> viewModel.submitReport(question.mcqId, reason, description) },
                                showContainer = false
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = viewModel::showSubmitDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.qb_submit),
                    style = ProstutiTypography.bnBodyLarge
                )
            }
        }
    }
}

// ── Right panel: tab switcher for calculator / canvas ───────────────────────

@Composable
private fun RightPanel(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    canvasStrokes: List<DrawStroke>,
    canvasIsErasing: Boolean,
    onStrokeFinished: (DrawStroke) -> Unit,
    onEraserToggle: () -> Unit,
    onCanvasClear: () -> Unit,
    showHideButton: Boolean,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.weight(1f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    text = {
                        Text(
                            text = "ক্যালকুলেটর",
                            style = ProstutiTypography.bnLabelMedium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    text = {
                        Text(
                            text = "ক্যানভাস",
                            style = ProstutiTypography.bnLabelMedium
                        )
                    }
                )
            }
            if (showHideButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onHide)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◀",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> CalculatorPanel(modifier = Modifier.weight(1f).fillMaxWidth())
            else -> InfinityCanvas(
                strokes = canvasStrokes,
                isErasing = canvasIsErasing,
                onStrokeFinished = onStrokeFinished,
                onEraserToggle = onEraserToggle,
                onClear = onCanvasClear,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

// ── Infinity canvas ──────────────────────────────────────────────────────────

@Composable
private fun InfinityCanvas(
    strokes: List<DrawStroke>,
    isErasing: Boolean,
    onStrokeFinished: (DrawStroke) -> Unit,
    onEraserToggle: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current stroke tracked internally; it's ephemeral and need not survive tab changes
    var currentStrokePoints by remember { mutableStateOf(listOf<Offset>()) }

    Column(modifier = modifier) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pen button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (!isErasing) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    )
                    .clickable { if (isErasing) onEraserToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "পেন",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Eraser button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isErasing) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    )
                    .clickable { if (!isErasing) onEraserToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "E",
                    style = ProstutiTypography.enLabelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Clear button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "সব মুছো",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Drawing surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(isErasing) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStrokePoints = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStrokePoints = currentStrokePoints + change.position
                        },
                        onDragEnd = {
                            if (currentStrokePoints.size >= 2 && !isErasing) {
                                onStrokeFinished(DrawStroke(currentStrokePoints))
                            }
                            currentStrokePoints = emptyList()
                        },
                        onDragCancel = { currentStrokePoints = emptyList() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeStyle = Stroke(
                    width = 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                strokes.forEach { stroke ->
                    if (stroke.points.size >= 2) {
                        val path = Path()
                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                        stroke.points.drop(1).forEach { pt -> path.lineTo(pt.x, pt.y) }
                        drawPath(path, color = stroke.color, style = strokeStyle)
                    }
                }
                if (currentStrokePoints.size >= 2 && !isErasing) {
                    val path = Path()
                    path.moveTo(currentStrokePoints[0].x, currentStrokePoints[0].y)
                    currentStrokePoints.drop(1).forEach { pt -> path.lineTo(pt.x, pt.y) }
                    drawPath(path, color = Color.Black, style = strokeStyle)
                }
            }
        }
    }
}

// ── Calculator panel ──────────────────────────────────────────────────────────

private fun Double.formatCalc(): String {
    if (isNaN() || isInfinite()) return "Error"
    val long = toLong()
    return if (this == long.toDouble()) long.toString()
    else "%.10g".format(this).trimEnd('0').trimEnd('.')
}

@Composable
private fun CalculatorPanel(modifier: Modifier = Modifier) {
    var display by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf(0.0) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var shouldReset by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (display == "Error") { display = if (d == ".") "0." else d; shouldReset = false; return }
        if (shouldReset) {
            display = if (d == ".") "0." else d
            shouldReset = false
        } else {
            if (d == "." && display.contains(".")) return
            display = if (display == "0" && d != ".") d else display + d
        }
        if (display.length > 14) display = display.dropLast(1)
    }

    fun calculate(a: Double, op: String, b: Double): Double = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b != 0.0) a / b else Double.NaN
        else -> b
    }

    fun onOperator(op: String) {
        val current = display.toDoubleOrNull() ?: 0.0
        if (pendingOp != null && !shouldReset) {
            val result = calculate(firstOperand, pendingOp!!, current)
            display = result.formatCalc()
            firstOperand = result
        } else {
            firstOperand = current
        }
        pendingOp = op
        shouldReset = true
    }

    fun onEquals() {
        val current = display.toDoubleOrNull() ?: 0.0
        val result = calculate(firstOperand, pendingOp ?: "=", current)
        display = result.formatCalc()
        pendingOp = null
        shouldReset = true
    }

    fun onScientific(fn: String) {
        val v = display.toDoubleOrNull() ?: 0.0
        val result = when (fn) {
            "sin"  -> sin(Math.toRadians(v))
            "cos"  -> cos(Math.toRadians(v))
            "tan"  -> tan(Math.toRadians(v))
            "log"  -> if (v > 0) log10(v) else Double.NaN
            "√"    -> if (v >= 0) sqrt(v) else Double.NaN
            "x²"   -> v * v
            "π"    -> PI
            "e"    -> E
            else   -> v
        }
        display = result.formatCalc()
        shouldReset = true
    }

    val operatorKeys = setOf("÷", "×", "−", "+")

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Scientific rows
        listOf(
            listOf("sin", "cos", "tan", "log"),
            listOf("√", "x²", "π", "e")
        ).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    CalcKey(label = key, modifier = Modifier.weight(1f), isSecondary = true,
                        onClick = { onScientific(key) })
                }
            }
        }

        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.headlineMedium,
                color = if (display == "Error") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        // Main rows
        listOf(
            listOf("AC", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+")
        ).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    CalcKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        isOperator = key in operatorKeys,
                        onClick = {
                            when {
                                key == "AC" -> { display = "0"; firstOperand = 0.0; pendingOp = null; shouldReset = false }
                                key == "±"  -> { val v = display.toDoubleOrNull() ?: 0.0; display = (-v).formatCalc() }
                                key == "%"  -> { val v = display.toDoubleOrNull() ?: 0.0; display = (v / 100.0).formatCalc(); shouldReset = true }
                                key in operatorKeys -> onOperator(key)
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }

        // Bottom row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CalcKey(label = "0", modifier = Modifier.weight(2f), onClick = { onDigit("0") })
            CalcKey(label = ".", modifier = Modifier.weight(1f), onClick = { onDigit(".") })
            CalcKey(label = "=", modifier = Modifier.weight(1f), isPrimary = true, onClick = { onEquals() })
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isOperator: Boolean = false,
    isSecondary: Boolean = false
) {
    val bg = when {
        isPrimary  -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        isSecondary -> MaterialTheme.colorScheme.surfaceContainerHigh
        else       -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isPrimary  -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        else       -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = ProstutiTypography.enLabelLarge,
            color = textColor
        )
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun SubmitConfirmDialog(isSubmitting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.qb_submit_confirm_title),
                style = ProstutiTypography.bnTitleMedium
            )
        },
        text = {
            Text(
                text = "এক্সাম সাবমিট করলে আর পরিবর্তন করা যাবে না।",
                style = ProstutiTypography.bnBodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSubmitting, shape = RoundedCornerShape(50.dp)) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = stringResource(R.string.qb_submit), style = ProstutiTypography.bnBodyMedium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(text = "বাতিল", style = ProstutiTypography.bnBodyMedium)
            }
        }
    )
}
