package com.prostuti.student.ui.component

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/** List column count from the width size class of the screen's pane (the window, outside list-detail): compact 1, medium 2, expanded and up 3. */
@Composable
fun adaptiveColumnCount(): Int {
    val windowSizeClass = paneSizeClass()
    return when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 3
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 2
        else -> 1
    }
}

/**
 * How many cards a looping carousel shows side by side: compact 1 (plain pager),
 * medium/expanded 3 (previous · current · next), large and up 5. Always odd so the
 * current card sits in the middle.
 */
@Composable
fun carouselVisibleCount(): Int {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> 5
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 3
        else -> 1
    }
}
