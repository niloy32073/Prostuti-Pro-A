package com.prostuti.student.ui.component

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BesideListPaneSizedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** A pane beside a list gets one width class below the window: expanded+ → 2 columns, else 1. */
    @Test
    fun columnsInsideThePane_areOneClassBelowTheWindow() {
        var windowColumns = -1
        var paneColumns = -1
        var windowIsExpanded = false
        composeTestRule.setContent {
            windowColumns = adaptiveColumnCount()
            windowIsExpanded = currentWindowAdaptiveInfoV2().windowSizeClass
                .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
            BesideListPaneSized { paneColumns = adaptiveColumnCount() }
        }
        composeTestRule.waitForIdle()

        assertEquals(if (windowIsExpanded) 2 else 1, paneColumns)
        assert(paneColumns <= windowColumns)
    }
}
