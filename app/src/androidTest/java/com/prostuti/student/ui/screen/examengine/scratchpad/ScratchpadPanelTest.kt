package com.prostuti.student.ui.screen.examengine.scratchpad

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.domain.scratchpad.StrokeEraser
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchpadPanelTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModel = ScratchpadViewModel(StrokeEraser())
    private val strokes get() = viewModel.state.value.strokes

    @Before
    fun show() {
        composeTestRule.setContent {
            ProstutiTheme { ScratchpadPanel(viewModel = viewModel, modifier = Modifier.fillMaxSize()) }
        }
    }

    /** Horizontal pen line across the middle of the surface. */
    private fun drawLine() = composeTestRule.onNodeWithTag(TAG_SURFACE).performTouchInput {
        swipe(Offset(width * 0.1f, height * 0.5f), Offset(width * 0.9f, height * 0.5f), durationMillis = 300)
    }

    /** Vertical eraser swipe through the line's middle. */
    private fun eraseAcross() = composeTestRule.onNodeWithTag(TAG_SURFACE).performTouchInput {
        swipe(Offset(width * 0.5f, height * 0.3f), Offset(width * 0.5f, height * 0.7f), durationMillis = 300)
    }

    @Test
    fun penDrawsALineAndEnablesUndo() {
        composeTestRule.onNodeWithTag(TAG_UNDO).assertIsNotEnabled()
        drawLine()
        composeTestRule.waitForIdle()
        assertEquals(1, strokes.size)
        composeTestRule.onNodeWithTag(TAG_UNDO).assertIsEnabled()
    }

    @Test
    fun partialEraserCutsTheLineInTwo() {
        drawLine()
        composeTestRule.onNodeWithTag(TAG_ERASER).performClick()
        composeTestRule.onNodeWithTag(TAG_ERASER_PARTIAL).performClick()
        eraseAcross()
        composeTestRule.waitForIdle()
        assertEquals(2, strokes.size)
    }

    @Test
    fun strokeEraserRemovesTheWholeLineAndUndoBringsItBack() {
        drawLine()
        composeTestRule.onNodeWithTag(TAG_ERASER).performClick()
        composeTestRule.onNodeWithTag(TAG_ERASER_STROKE).performClick()
        eraseAcross()
        composeTestRule.waitForIdle()
        assertTrue(strokes.isEmpty())

        composeTestRule.onNodeWithTag(TAG_UNDO).performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, strokes.size)
    }
}
