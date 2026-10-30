package com.prostuti.student.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The onboarding pager's expanding-dot indicator — the active dot widens to 22dp. */
@Composable
fun PagerDots(count: Int, activeIndex: Int, activeColor: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val isActive = index == activeIndex
            val width by animateDpAsState(if (isActive) 22.dp else 7.dp, label = "dotWidth")
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(width)
                    .background(activeColor.copy(alpha = if (isActive) 1f else 0.35f), CircleShape)
            )
        }
    }
}
