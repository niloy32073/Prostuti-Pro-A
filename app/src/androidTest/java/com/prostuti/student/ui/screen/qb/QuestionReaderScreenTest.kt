package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.ui.screen.examengine.component.MCQ_ANSWER_TOGGLE_TAG
import com.prostuti.student.ui.screen.examengine.component.MCQ_EXPLANATION_BUTTON_TAG
import com.prostuti.student.ui.screen.examengine.component.MCQ_OPTION_IMAGE_TAG
import com.prostuti.student.ui.screen.examengine.component.MCQ_QUESTION_IMAGE_TAG
import com.prostuti.student.ui.screen.examengine.component.groupQuestionsByPassage
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionReaderScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val toggled = mutableListOf<String>()
    private var allToggles = 0

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun q(id: String, order: Int, passageId: String? = null) = Question(
        mcqId = id,
        order = order,
        passageId = passageId,
        questionText = "Question $id",
        options = listOf(Option(id = "$id-o1", optionText = "Option $id", order = 1))
    )

    private fun setContent(state: QuestionReaderUiState) {
        composeTestRule.setContent {
            ProstutiTheme {
                QuestionReaderContent(
                    titleBn = "মডেল টেস্ট ১",
                    state = state,
                    onBack = {},
                    onRefresh = {},
                    onClearError = {},
                    onToggleFavorite = {},
                    onToggleAnswer = { toggled += it },
                    onToggleAllAnswers = { allToggles++ },
                    onReport = { _, _, _ -> }
                )
            }
        }
    }

    @Test
    fun rendersTitleCountPassageAndScreenOrderNumbers() {
        val questions = listOf(q("a", 1), q("p1", 2, "p"), q("b", 3), q("p2", 4, "p"))
        val passages = mapOf("p" to FetchPassageData(id = "p", passageText = "Passage text"))
        setContent(
            QuestionReaderUiState(
                items = groupQuestionsByPassage(questions, passages),
                questionCount = questions.size
            )
        )

        composeTestRule.onNodeWithText("মডেল টেস্ট ১").assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.qb_reader_question_count, 4)).assertExists()
        composeTestRule.onNodeWithText("Passage text").assertExists()
        // "a" = 1, passage questions 2 and 3, "b" moves to 4.
        composeTestRule.onNodeWithText("প্রশ্ন 4").assertExists()
        composeTestRule.onNodeWithText("Question b").assertExists()
    }

    @Test
    fun optionsAreNotSelectable() {
        setContent(QuestionReaderUiState(items = groupQuestionsByPassage(listOf(q("a", 1)), emptyMap()), questionCount = 1))

        composeTestRule.onNodeWithText("Option a").assertIsNotEnabled()
    }

    @Test
    fun showsEmptyMessageWhenNoQuestions() {
        setContent(QuestionReaderUiState())

        composeTestRule.onNodeWithText(context.getString(R.string.qb_reader_no_questions)).assertExists()
    }

    @Test
    fun showsQuestionAndOptionImages() {
        val question = Question(
            mcqId = "img",
            order = 1,
            questionText = "Question img",
            questionImageLink = "https://cdn.prostuti.com/questions/q.png",
            options = listOf(Option(id = "o1", optionText = "Option img", optionImageLink = "https://cdn.prostuti.com/options/o.png", order = 1))
        )
        setContent(QuestionReaderUiState(items = groupQuestionsByPassage(listOf(question), emptyMap()), questionCount = 1))

        composeTestRule.onNodeWithTag(MCQ_QUESTION_IMAGE_TAG, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(MCQ_OPTION_IMAGE_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun answerButtonAndAllAnswersSwitchCallBack() {
        setContent(QuestionReaderUiState(items = groupQuestionsByPassage(listOf(q("a", 1)), emptyMap()), questionCount = 1))

        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_show_answer)).assertExists()
        composeTestRule.onNodeWithTag(MCQ_ANSWER_TOGGLE_TAG + "a").performClick()
        composeTestRule.onNodeWithTag(READER_ALL_ANSWERS_SWITCH_TAG).performClick()

        assertEquals(listOf("a"), toggled)
        assertEquals(1, allToggles)
    }

    @Test
    fun revealedAnswerWithoutCorrectOptionSaysUnavailable() {
        setContent(
            QuestionReaderUiState(
                items = groupQuestionsByPassage(listOf(q("a", 1)), emptyMap()),
                questionCount = 1,
                revealedAnswers = setOf("a")
            )
        )

        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_hide_answer)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.qb_reader_answer_unavailable)).assertExists()
    }

    @Test
    fun explanationButtonOpensPopup() {
        setContent(QuestionReaderUiState(items = groupQuestionsByPassage(listOf(q("a", 1)), emptyMap()), questionCount = 1))

        composeTestRule.onNodeWithTag(MCQ_EXPLANATION_BUTTON_TAG + "a").performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.qb_reader_explanation_coming)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.qb_reader_close)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.qb_reader_explanation_coming)).assertDoesNotExist()
    }
}
