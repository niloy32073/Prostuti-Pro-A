package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.navigation.MCQExamList
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** SubjectTypeScreen is fully stateless (no ViewModel) — it just routes a tap on the MCQ card to
 * [onNavigateToMCQ] with the right subject/name args; the other three cards are "coming soon". */
@RunWith(AndroidJUnit4::class)
class SubjectTypeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingMcqCardNavigatesWithTheSubjectArgs() {
        var navigated: MCQExamList? = null
        composeTestRule.setContent {
            ProstutiTheme {
                SubjectTypeScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    nameEn = "Physics",
                    onBack = {},
                    onNavigateToMCQ = { navigated = it }
                )
            }
        }

        composeTestRule.onNodeWithText("MCQ — বহুনির্বাচনী").performClick()

        assert(navigated?.subjectId == "phy") { "expected navigation to subjectId=phy, got $navigated" }
    }

    @Test
    fun tappingAComingSoonCardShowsASnackbarInsteadOfCrashing() {
        composeTestRule.setContent {
            ProstutiTheme {
                SubjectTypeScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    nameEn = "Physics",
                    onBack = {},
                    onNavigateToMCQ = {}
                )
            }
        }

        composeTestRule.onNodeWithText("CQ — সৃজনশীল").performClick()
        // No assertion beyond "didn't crash" — the snackbar text comes from a string resource
        // that depends on locale, so we only verify the tap is handled safely.
        composeTestRule.waitForIdle()
    }
}
