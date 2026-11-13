package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.screen.playground.graph.KeyboardPage
import com.prostuti.student.ui.screen.playground.graph.MathKeyAction

/** The graph builder's own keyboard: math symbols one tap away, no system keyboard in the way. */
@Composable
fun MathKeyboard(
    page: KeyboardPage,
    onKey: (MathKeyAction) -> Unit,
    modifier: Modifier = Modifier,
    space: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    fun press(action: MathKeyAction) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onKey(action)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.testTag("math_keyboard")) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(32.dp)) {
                TextButton(onClick = { press(MathKeyAction.SystemKeyboard) }, modifier = Modifier.testTag("math_key_system")) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(stringResource(R.string.graph_keyboard_system), style = ProstutiTypography.bnLabelMedium)
                }
                Box(Modifier.weight(1f))
                IconButton(onClick = { press(MathKeyAction.Hide) }, modifier = Modifier.testTag("math_key_hide")) {
                    Icon(Icons.Filled.KeyboardHide, contentDescription = stringResource(R.string.graph_keyboard_hide))
                }
            }
            for (row in MathKeyboardLayout.rows(page, space)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    for (key in row) KeyButton(key) { press(key.action) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                for (key in MathKeyboardLayout.bottomRow(page)) KeyButton(key) { press(key.action) }
            }
        }
    }
}

@Composable
private fun RowScope.KeyButton(key: MathKey, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (key.style) {
        KeyStyle.PLAIN -> scheme.surfaceContainerHighest to scheme.onSurface
        KeyStyle.DIGIT -> scheme.surfaceBright to scheme.onSurface
        KeyStyle.OPERATOR -> scheme.secondaryContainer to scheme.onSecondaryContainer
        KeyStyle.ACTION -> scheme.primary to scheme.onPrimary
    }
    val description = when (key.action) {
        MathKeyAction.Backspace -> stringResource(R.string.graph_key_backspace)
        MathKeyAction.Left -> stringResource(R.string.graph_key_left)
        MathKeyAction.Right -> stringResource(R.string.graph_key_right)
        MathKeyAction.Enter -> stringResource(R.string.graph_key_enter)
        is MathKeyAction.Page -> stringResource(
            when (key.action.page) {
                KeyboardPage.MAIN -> R.string.graph_key_page_main
                KeyboardPage.FUNCTIONS -> R.string.graph_key_page_functions
                KeyboardPage.CALCULUS -> R.string.graph_key_page_calculus
                KeyboardPage.LETTERS -> R.string.graph_key_page_letters
            },
        )
        else -> key.label
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(key.weight)
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .semantics { contentDescription = description }
            .testTag("math_key_${key.id}")
            .clickable(onClick = onClick),
    ) {
        Text(
            text = key.label,
            color = fg,
            fontSize = if (key.label.length > 4) 13.sp else 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
