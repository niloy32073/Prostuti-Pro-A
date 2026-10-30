package com.prostuti.student.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Centers a phone-width screen with a fixed max width on medium/expanded windows (tablets),
 * instead of stretching a 390dp-designed layout across the full viewport — the group/live/timed-
 * exam screens have no tablet-specific design of their own, unlike Home or Practice's genuine
 * two-pane layouts, so this is the whole of their "adaptive" story (per the plan's P7 default).
 *
 * Wraps a route's destination composable from the OUTSIDE, in `NavDisplay`'s `entryProvider`
 * (`AdaptiveWidthContainer { GroupListScreen(...) }`), rather than reaching into each screen's
 * own `Scaffold` — every one of them gets this for free with a one-line change at the call site.
 */
@Composable
fun AdaptiveWidthContainer(maxWidth: Dp = 600.dp, content: @Composable () -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isCompact = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    if (isCompact) {
        content()
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.fillMaxHeight().width(maxWidth)) {
                content()
            }
        }
    }
}
