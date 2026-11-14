package com.prostuti.student.ui.screen.playground.code.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

private val SPEED_LABELS = intArrayOf(R.string.code_trace_speed_0, R.string.code_trace_speed_1, R.string.code_trace_speed_2)

/**
 * The bar of a step-by-step run: take one step, play steps by themselves (and pause), change the
 * play speed, or let the program run to its end. [paused] is true while the program waits for a step.
 */
@Composable
fun TraceControls(
    paused: Boolean,
    playing: Boolean,
    speed: Int,
    onStep: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSpeed: () -> Unit,
    onToEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.testTag("trace_controls")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            FilledTonalButton(onClick = onStep, enabled = paused && !playing, modifier = Modifier.testTag("trace_step")) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.code_trace_next), style = ProstutiTypography.bnLabelLarge)
            }
            if (playing) {
                TraceIconButton(Icons.Filled.Pause, stringResource(R.string.code_trace_pause), "trace_pause", onPause)
            } else {
                TraceIconButton(Icons.Filled.PlayArrow, stringResource(R.string.code_trace_play), "trace_play", onPlay)
            }
            val speedLabel = stringResource(SPEED_LABELS[speed.coerceIn(SPEED_LABELS.indices)])
            TextButton(onClick = onSpeed, modifier = Modifier.testTag("trace_speed")) {
                Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.code_trace_speed_desc, speedLabel))
                Spacer(Modifier.width(4.dp))
                Text(speedLabel, style = ProstutiTypography.bnLabelLarge)
            }
            Spacer(Modifier.weight(1f))
            TraceIconButton(Icons.Filled.FastForward, stringResource(R.string.code_trace_to_end), "trace_to_end", onToEnd)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TraceIconButton(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Icon(icon, contentDescription = label) }
    }
}
