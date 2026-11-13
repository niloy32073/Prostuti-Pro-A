package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

/** The size class of the pane a screen sits in, when it isn't the whole window (see [PaneSized]). */
val LocalPaneSizeClass = compositionLocalOf<WindowSizeClass?> { null }

/**
 * The size class a screen should lay itself out for: its pane's when inside a list-detail pane,
 * else the window's. A detail pane on a tablet is narrower than the window, so the window's class
 * would give it too many columns.
 */
@Composable
fun paneSizeClass(): WindowSizeClass = LocalPaneSizeClass.current ?: currentWindowAdaptiveInfoV2().windowSizeClass

/** True when the pane is at least Medium wide: room for a second column. */
@Composable
fun paneIsMediumOrWider(): Boolean = paneSizeClass().isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** True when the pane is at least Expanded wide: room for a third column. */
@Composable
fun paneIsExpandedOrWider(): Boolean = paneSizeClass().isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

/** Measures the space [content] gets and gives it that space's size class (the library's own breakpoints). */
@Composable
fun PaneSized(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(maxWidth.value, maxHeight.value)
        CompositionLocalProvider(LocalPaneSizeClass provides sizeClass) { content() }
    }
}

/**
 * For a detail pane beside a list pane: gives [content] one width class below the window's, since
 * the list already takes part of the width. Expanded and wider windows → medium (2 columns),
 * medium → compact (1 column). The window's height class is kept.
 */
@Composable
fun BesideListPaneSized(content: @Composable () -> Unit) {
    val window = currentWindowAdaptiveInfoV2().windowSizeClass
    val widthDp = if (window.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    } else {
        0
    }
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp.toFloat(), window.minHeightDp.toFloat())
    CompositionLocalProvider(LocalPaneSizeClass provides sizeClass) { content() }
}
