package com.prostuti.student.ui.screen.playground.code.component

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.domain.code.run.InstallFailure
import com.prostuti.student.domain.code.run.RunLimits
import com.prostuti.student.domain.code.run.RuntimeState
import com.prostuti.student.ui.screen.playground.code.ConsoleChunk
import com.prostuti.student.ui.screen.playground.code.ConsoleKind
import com.prostuti.student.ui.screen.playground.code.RunStatus
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.theme.extendedColors

/**
 * The program's output: a status line (download progress, running, paused at a line, finished,
 * the error's line), the printed text, and an input row while the program waits in `input()`.
 */
@Composable
fun RunConsole(
    status: RunStatus,
    runtime: RuntimeState,
    console: List<ConsoleChunk>,
    errorLine: Int?,
    onSubmitInput: (String) -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    pausedLine: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val mono = TextStyle(fontFamily = MonoFontFamily, fontSize = 13.sp, lineHeight = 19.sp, fontFeatureSettings = NO_LIGATURES)
    val scroll = rememberScrollState()
    // One text for the whole console, so a prompt and the typed answer share a line ("a = 5").
    val text = remember(console, colors) {
        buildAnnotatedString {
            for (chunk in console) {
                val color = when (chunk.kind) {
                    ConsoleKind.OUT -> colors.onSurface
                    ConsoleKind.ERR -> colors.error
                    ConsoleKind.INPUT -> colors.primary
                }
                withStyle(SpanStyle(color = color, fontWeight = if (chunk.kind == ConsoleKind.INPUT) FontWeight.Medium else null)) {
                    append(chunk.text)
                }
            }
        }
    }
    LaunchedEffect(text.length, status) { scroll.scrollTo(scroll.maxValue) }
    Column(modifier.background(colors.surfaceContainerLowest).testTag("run_console")) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp)) {
            Text(
                statusText(status, runtime, errorLine, pausedLine),
                style = ProstutiTypography.bnBodyMedium,
                color = when (status) {
                    RunStatus.ERROR, RunStatus.TIME_LIMIT, RunStatus.OUTPUT_LIMIT, RunStatus.CRASHED -> colors.error
                    RunStatus.DONE -> MaterialTheme.extendedColors.success
                    else -> if (runtime is RuntimeState.Failed && status == RunStatus.IDLE) colors.error else colors.onSurfaceVariant
                },
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).testTag("run_status"),
            )
            // Stop is here too: on a phone the output tab hides the editor's Stop button.
            if (status.isActive) {
                TextButton(onClick = onStop, modifier = Modifier.testTag("console_stop")) {
                    Icon(Icons.Filled.Stop, contentDescription = null, tint = colors.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.code_stop), style = ProstutiTypography.bnLabelLarge, color = colors.error)
                }
            } else if (console.isNotEmpty()) {
                TextButton(onClick = onClear, modifier = Modifier.testTag("console_clear")) {
                    Text(stringResource(R.string.code_console_clear), style = ProstutiTypography.bnLabelLarge)
                }
            }
        }
        if (status == RunStatus.PREPARING || status == RunStatus.STARTING) {
            val progress = (runtime as? RuntimeState.Downloading)?.takeIf { it.total > 0 }?.let { it.downloaded.toFloat() / it.total }
            if (progress != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (console.isEmpty() && status == RunStatus.IDLE && runtime !is RuntimeState.Failed) {
                Text(
                    stringResource(R.string.code_console_empty),
                    style = ProstutiTypography.bnBodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            SelectionContainer(Modifier.fillMaxSize().verticalScroll(scroll).testTag("console_output")) {
                Text(text.trimEndNewline(), style = mono, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        if (status == RunStatus.WAITING_INPUT) InputRow(mono, onSubmitInput)
    }
}

private fun AnnotatedString.trimEndNewline(): AnnotatedString =
    if (text.endsWith("\n")) subSequence(0, length - 1) else this

@Composable
private fun InputRow(style: TextStyle, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val send = { onSubmit(text); text = "" }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.replace("\n", "") },
            singleLine = true,
            textStyle = style,
            placeholder = { Text(stringResource(R.string.code_input_hint), style = ProstutiTypography.bnBodyMedium) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f).focusRequester(focus).testTag("console_input"),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = send, modifier = Modifier.testTag("console_send")) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.code_input_send), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun statusText(status: RunStatus, runtime: RuntimeState, errorLine: Int?, pausedLine: Int?): String {
    val context = LocalContext.current
    return when (status) {
        RunStatus.PREPARING -> when (runtime) {
            is RuntimeState.Downloading -> if (runtime.total > 0) {
                stringResource(
                    R.string.code_downloading,
                    Formatter.formatShortFileSize(context, runtime.downloaded),
                    Formatter.formatShortFileSize(context, runtime.total),
                )
            } else stringResource(R.string.code_downloading_start)
            RuntimeState.Installing, RuntimeState.Installed -> stringResource(R.string.code_installing)
            RuntimeState.NeedsConfirmation -> stringResource(R.string.code_install_confirm)
            else -> stringResource(R.string.code_downloading_start)
        }
        RunStatus.STARTING -> stringResource(R.string.code_status_starting)
        RunStatus.RUNNING -> stringResource(R.string.code_status_running)
        RunStatus.WAITING_INPUT -> stringResource(R.string.code_status_waiting)
        RunStatus.PAUSED -> pausedLine?.let { stringResource(R.string.code_status_paused, it) } ?: stringResource(R.string.code_status_running)
        RunStatus.DONE -> stringResource(R.string.code_status_done)
        RunStatus.ERROR -> errorLine?.let { stringResource(R.string.code_status_error_line, it) } ?: stringResource(R.string.code_status_error)
        RunStatus.STOPPED -> stringResource(R.string.code_status_stopped)
        RunStatus.TIME_LIMIT -> stringResource(R.string.code_status_time_limit, (RunLimits.TIME_MS / 1000).toInt())
        RunStatus.OUTPUT_LIMIT -> stringResource(R.string.code_status_output_limit)
        RunStatus.CRASHED -> stringResource(R.string.code_status_crashed)
        RunStatus.IDLE -> when (runtime) {
            is RuntimeState.Failed -> when (runtime.reason) {
                InstallFailure.NO_NETWORK -> stringResource(R.string.code_install_failed_network)
                InstallFailure.NO_PLAY -> stringResource(R.string.code_install_failed_play)
                InstallFailure.NO_STORAGE -> stringResource(R.string.code_install_failed_storage)
                InstallFailure.OTHER -> stringResource(R.string.code_install_failed_other, runtime.code)
            }
            else -> ""
        }
    }
}
