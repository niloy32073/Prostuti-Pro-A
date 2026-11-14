package com.prostuti.student.ui.screen.playground.code.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.domain.code.run.TraceChanges
import com.prostuti.student.domain.code.run.TraceStep
import com.prostuti.student.domain.code.run.TraceVar
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

/** One line of the panel: a function's heading or a variable (possibly an element/member, [depth] > 0). */
private sealed interface VarRow {
    val key: String

    data class Heading(override val key: String, val title: String, val subtitle: String, val current: Boolean) : VarRow
    data class Value(override val key: String, val v: TraceVar, val depth: Int, val changed: Boolean, val expanded: Boolean) : VarRow
    data class Empty(override val key: String) : VarRow
}

/**
 * The variables of a paused step-by-step run: the running function first, then the functions
 * waiting for it (the call stack), then C's globals. Values that changed in the last step are
 * tinted; arrays, lists and structs open to show their elements. [mainName] replaces `main` (the
 * main program of Python). [running] false means the run has ended and these are its last values.
 */
@Composable
fun VariablesPanel(
    trace: TraceStep?,
    changed: Set<String>,
    running: Boolean,
    mainName: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val expandedState = rememberSaveable(
        saver = listSaver<MutableState<Set<String>>, String>(save = { it.value.toList() }, restore = { mutableStateOf(it.toSet()) }),
    ) { mutableStateOf(emptySet()) }
    var expanded = expandedState.value

    if (trace == null) {
        Box(modifier.padding(24.dp).testTag("variables_empty"), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.code_vars_empty),
                style = ProstutiTypography.bnBodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val nowText = stringResource(R.string.code_vars_now)
    val resources = LocalContext.current.resources
    val lineLabel = { line: Int -> resources.getString(R.string.code_trace_line, line) }
    val rows = buildList {
        val frames = trace.frames
        for (i in frames.indices.reversed()) {
            val f = frames[i]
            val prefix = TraceChanges.path(i, f)
            val current = i == frames.lastIndex
            val title = if (f.function == "main" && mainName != null) mainName else "${f.function}()"
            add(VarRow.Heading("h/$prefix", title, if (current) "$nowText · ${lineLabel(f.line)}" else lineLabel(f.line), current))
            if (f.vars.isEmpty()) add(VarRow.Empty("e/$prefix"))
            f.vars.forEach { addVar(it, prefix, 0, changed, expanded) }
        }
        if (trace.globals.isNotEmpty()) {
            add(VarRow.Heading("h/globals", "", "", current = false))
            trace.globals.forEach { addVar(it, "globals", 0, changed, expanded) }
        }
    }

    Column(modifier.testTag("variables_panel")) {
        if (!running) {
            Text(
                stringResource(R.string.code_vars_finished),
                style = ProstutiTypography.bnBodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        LazyColumn(state = rememberLazyListState(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is VarRow.Heading -> HeadingRow(row)
                    is VarRow.Empty -> Text(
                        stringResource(R.string.code_vars_none),
                        style = ProstutiTypography.bnBodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 6.dp),
                    )
                    is VarRow.Value -> ValueRow(row) {
                        val path = row.key
                        expanded = if (path in expanded) expanded - path else expanded + path
                        expandedState.value = expanded
                    }
                }
            }
        }
    }
}

private fun MutableList<VarRow>.addVar(v: TraceVar, prefix: String, depth: Int, changed: Set<String>, expanded: Set<String>) {
    val path = "$prefix/${v.name}"
    val open = path in expanded
    add(VarRow.Value(path, v, depth, path in changed, open))
    if (open) v.children.forEach { addVar(it, path, depth + 1, changed, expanded) }
}

@Composable
private fun HeadingRow(row: VarRow.Heading) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    ) {
        if (row.key == "h/globals") {
            Text(stringResource(R.string.code_vars_globals), style = ProstutiTypography.bnTitleMedium, color = colors.onSurface)
            return
        }
        Text(
            row.title,
            style = TextStyle(fontFamily = MonoFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = NO_LIGATURES),
            color = if (row.current) colors.primary else colors.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        Text(row.subtitle, style = ProstutiTypography.bnLabelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueRow(row: VarRow.Value, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val mono = TextStyle(fontFamily = MonoFontFamily, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = NO_LIGATURES)
    val canOpen = row.v.children.isNotEmpty()
    val toggleLabel = stringResource(if (row.expanded) R.string.code_vars_collapse else R.string.code_vars_expand, row.v.name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .then(if (row.changed) Modifier.background(colors.tertiaryContainer) else Modifier)
            .then(if (canOpen) Modifier.clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggle) else Modifier)
            .padding(start = 12.dp + 16.dp * row.depth, end = 16.dp, top = 3.dp, bottom = 3.dp)
            .testTag("var_${row.key}"),
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (canOpen) {
                Icon(
                    if (row.expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        val onRow = if (row.changed) colors.onTertiaryContainer else colors.onSurface
        Text(row.v.name, style = mono.copy(fontWeight = FontWeight.Medium), color = onRow)
        Text(" = ", style = mono, color = colors.onSurfaceVariant)
        val value = row.v.value
        if (value == null) {
            Text(stringResource(R.string.code_vars_no_value), style = ProstutiTypography.bnLabelMedium.copy(fontStyle = FontStyle.Italic), color = colors.onSurfaceVariant)
        } else {
            Text(value, style = mono, color = onRow, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
