package com.prostuti.student.ui.screen.analysis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.domain.TopicAnalysisRow
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.prostuti.student.data.model.response.CurriculumName

@RunWith(AndroidJUnit4::class)
class AttemptAnalysisScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val state = AttemptAnalysisUiState(
        isLoading = false,
        topics = listOf(
            TopicAnalysisRow("t1", CurriculumName("সমত্বরণ গতি", "Uniform acceleration"), CurriculumName("গতিবিদ্যা", "Kinematics"), 6, 3, 2, 1),
            TopicAnalysisRow("t2", CurriculumName("নিউটনের গতিসূত্র", "Newton's laws"), CurriculumName("নিউটনিয়ান বলবিদ্যা", "Newtonian mechanics"), 5, 5, 0, 0),
            TopicAnalysisRow(null, null, null, 3, 3, 0, 0)
        )
    )

    @Test
    fun weakestCardAndTopicRowsOpenTheirTopic() {
        val opened = mutableListOf<Pair<String?, String>>()
        composeTestRule.setContent {
            ProstutiTheme {
                AttemptAnalysisContent(
                    titleBn = "সাপ্তাহিক টেস্ট", state = state, onBack = {},
                    onOpenTopic = { id, name -> opened += id to name }, onRefresh = {}, onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(ANALYSIS_WEAKEST_TAG).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag(ANALYSIS_TOPIC_TAG + "t2").performClick()
        composeTestRule.onNodeWithTag(ANALYSIS_TOPIC_TAG + "none").performClick()

        // The topic name comes in the app's language.
        val language = context.resources.configuration.locales[0].language
        assertEquals("t1" to CurriculumName("সমত্বরণ গতি", "Uniform acceleration").inLanguage(language), opened[0])
        assertEquals("t2", opened[1].first)
        assertNull(opened[2].first)
        assertEquals(context.getString(R.string.analysis_no_topic), opened[2].second)
    }

    @Test
    fun emptyAnalysisShowsMessage() {
        composeTestRule.setContent {
            ProstutiTheme {
                AttemptAnalysisContent(
                    titleBn = "x", state = AttemptAnalysisUiState(isLoading = false), onBack = {},
                    onOpenTopic = { _, _ -> }, onRefresh = {}, onClearError = {}
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.analysis_empty)).assertIsDisplayed()
    }
}
