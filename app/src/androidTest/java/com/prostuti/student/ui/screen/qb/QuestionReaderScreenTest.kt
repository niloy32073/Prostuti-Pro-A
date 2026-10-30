package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionReaderScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rendersTitleAndQuestionCount() {
        composeTestRule.setContent {
            ProstutiTheme {
                QuestionReaderScreen(
                    titleBn = "মডেল টেস্ট ১",
                    onBack = {},
                    viewModel = QuestionReaderViewModel()
                )
            }
        }

        waitForText("মডেল টেস্ট ১")
        composeTestRule.onNodeWithText("10টি প্রশ্ন").assertExists()
    }
}
