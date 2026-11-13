package com.prostuti.student.ui.screen.routine.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography

const val TAG_BUILDER_NEXT = "routine_builder_next"
const val TAG_BUILDER_SUBJECT_PREFIX = "routine_builder_subject_"
const val TAG_BUILDER_WINDOW = "routine_builder_window"
const val TAG_BUILDER_START_DATE = "routine_builder_start_date"
const val TAG_BUILDER_WEEKDAYS = "routine_builder_weekdays"
const val TAG_BUILDER_EXAM_DATE = "routine_builder_exam_date"
const val TAG_BUILDER_TITLE = "routine_builder_title"
const val TAG_BUILDER_WARNING = "routine_builder_warning"
const val TAG_BUILDER_PROBLEM = "routine_builder_problem"

@Composable
internal fun BuilderHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onClick = onBack)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = subtitle, style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Selection summary on the left, the step's main button on the right. */
@Composable
internal fun BuilderBottomBar(summary: String, button: String, enabled: Boolean, loading: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = summary, style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = button, onClick = onClick, enabled = enabled, loading = loading,
            modifier = Modifier.widthIn(max = 220.dp).testTag(TAG_BUILDER_NEXT)
        )
    }
}

/** The design's ⚠ banner, with the design system's warning icon instead of an emoji. */
@Composable
internal fun BuilderWarning(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
        Text(text = text, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
internal fun ProblemText(text: String) {
    Text(
        text = text, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag(TAG_BUILDER_PROBLEM)
    )
}

/** "label ........ − value +" */
@Composable
internal fun StepperRow(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.routine_decrease, label))
        }
        Text(
            text = value, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 88.dp).padding(horizontal = 6.dp)
        )
        FilledTonalIconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.routine_increase, label))
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text = text, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable
internal fun Gap() = Spacer(modifier = Modifier.height(4.dp))
