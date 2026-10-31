package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorOverlayWrapperTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** Hosts the wrapper with removable errors and a top-end button under the error stack. */
    private fun setContent(initial: Map<String, String>, onUnderlyingClick: () -> Unit = {}) {
        composeTestRule.setContent {
            var errors by remember { mutableStateOf(initial) }
            ProstutiTheme {
                PullToRefreshErrorOverlayWrapper(errors = errors, errorCleaner = { tag -> errors = errors - tag }) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = onUnderlyingClick,
                            modifier = Modifier.align(Alignment.TopEnd).testTag("underlying")
                        ) { Text("Under") }
                    }
                }
            }
        }
    }

    @Test
    fun showsEveryErrorInTheOrderTheyHappened() {
        setContent(linkedMapOf("LOAD" to "First failure", "SAVE" to "Second failure"))

        composeTestRule.onNode(hasAnyAncestor(hasTestTag(errorCardTag("LOAD"))) and hasText("First failure")).assertIsDisplayed()
        composeTestRule.onNode(hasAnyAncestor(hasTestTag(errorCardTag("SAVE"))) and hasText("Second failure")).assertIsDisplayed()
        val first = composeTestRule.onNodeWithTag(errorCardTag("LOAD")).fetchSemanticsNode().boundsInRoot
        val second = composeTestRule.onNodeWithTag(errorCardTag("SAVE")).fetchSemanticsNode().boundsInRoot
        assertTrue("older error should be on top", first.top < second.top)
    }

    @Test
    fun closingACardRemovesOnlyThatError() {
        setContent(linkedMapOf("LOAD" to "First failure", "SAVE" to "Second failure"))

        composeTestRule.onNodeWithTag(errorCardCloseTag("LOAD")).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithTag(errorCardTag("LOAD")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("Second failure").assertIsDisplayed()
    }

    @Test
    fun overlayIsGoneOnceEveryErrorIsClosed() {
        setContent(mapOf("LOAD" to "Only failure"))

        composeTestRule.onNodeWithTag(errorCardCloseTag("LOAD")).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithTag(ERROR_OVERLAY_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun withNoErrorsTheScreenUnderneathStaysClickable() {
        var clicks = 0
        setContent(emptyMap(), onUnderlyingClick = { clicks++ })

        assertEquals(0, composeTestRule.onAllNodesWithTag(ERROR_OVERLAY_TAG).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("underlying").performClick()
        assertEquals(1, clicks)
    }
}
