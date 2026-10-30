package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures [AdaptiveWidthContainer]'s actual rendered width directly — the real signal for P7,
 * independent of the window size this device happens to have (the assertion branches on it
 * instead of assuming compact or expanded).
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveWidthContainerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun capsWidthAt600dpOnMediumOrExpandedWindowsOnly() {
        composeTestRule.setContent {
            AdaptiveWidthContainer {
                Box(modifier = Modifier.fillMaxSize().testTag("content")) {
                    Text("probe")
                }
            }
        }

        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        val contentBounds = composeTestRule.onNodeWithTag("content").getBoundsInRoot()
        val rootWidth = rootBounds.right - rootBounds.left
        val contentWidth = contentBounds.right - contentBounds.left

        if (rootWidth >= 600.dp) {
            // Medium/expanded window — content must be capped, not stretched to the full root.
            assertEquals(600.dp.value, contentWidth.value, 1f)
            assertTrue("content ($contentWidth) should be narrower than the root ($rootWidth) on a wide window", contentWidth < rootWidth)
        } else {
            // Compact window — content passes through unconstrained, filling the (already narrow) root.
            assertEquals(rootWidth.value, contentWidth.value, 1f)
        }
    }
}
