package com.prostuti.student.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.prostuti.student.ui.theme.extendedColors

/**
 * A horizontal stacked bar: correct (success) then wrong (error) over a skipped-colored track.
 * Decorative — always show the counts as text next to it.
 */
@Composable
fun ResultBar(correct: Int, wrong: Int, total: Int, barHeight: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (total > 0 && correct > 0) {
            Box(Modifier.weight(correct.toFloat()).fillMaxHeight().background(MaterialTheme.extendedColors.success))
        }
        if (total > 0 && wrong > 0) {
            Box(Modifier.weight(wrong.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.error))
        }
        val rest = total - correct - wrong
        if (total == 0 || rest > 0) {
            Spacer(Modifier.weight(if (total == 0) 1f else rest.toFloat()))
        }
    }
}
