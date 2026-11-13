package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.MCQRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.ui.screen.examengine.calculator.CalcKey
import com.prostuti.student.ui.screen.examengine.calculator.keyTag
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

/**
 * The exam screen's side panel on medium/expanded windows: the calculator and the canvas each
 * run on their own ViewModel. Skipped on compact windows, where the panel is not shown.
 */
@RunWith(AndroidJUnit4::class)
class ExamToolsPanelTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var panelShown = false

    @Before
    fun showExam() {
        ExamAttemptStore.consume()
        val question = FakeExamAttemptRepositoryForEngineTest.fakeQuestion(mcqId = "mcq1")
        val repo = FakeExamAttemptRepositoryForEngineTest(
            attemptQuestionsResult = ApiResponse.Success(
                FakeExamAttemptRepositoryForEngineTest.defaultAttemptData(
                    questions = listOf(question),
                    startAt = java.time.Instant.now().toString(),
                )
            )
        )
        GlobalContext.get().loadModules(listOf(module { single<ExamRepository> { repo }; single<MCQRepository> { repo } }), allowOverride = true)
        composeTestRule.setContent {
            panelShown = currentWindowAdaptiveInfoV2().windowSizeClass
                .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
            ProstutiTheme { ExamAttemptScreen(attemptId = "attempt1", onBack = {}, onSubmitted = {}) }
        }
        composeTestRule.waitForIdle()
        assumeTrue("Side panel only shows on medium and wider windows", panelShown)
    }

    private fun press(vararg keys: CalcKey) = keys.forEach {
        composeTestRule.onNodeWithTag(keyTag(it)).performScrollTo().performClick()
    }

    private fun assertLcdShows(text: String) {
        composeTestRule.waitUntil(3_000) { composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun calculatorShowsAFractionAndItsDecimal() {
        press(CalcKey.D1, CalcKey.DIVIDE, CalcKey.D3, CalcKey.EQUALS)
        assertLcdShows("1÷3")
        press(CalcKey.S_D)
        assertLcdShows("0.3333333333")
    }

    @Test
    fun calculatorSolvesAQuadraticInEqnMode() {
        press(CalcKey.MODE)
        assertLcdShows("2:EQN")
        press(CalcKey.D2, CalcKey.D4)
        press(CalcKey.D1, CalcKey.EQUALS, CalcKey.NEGATIVE, CalcKey.D5, CalcKey.EQUALS, CalcKey.D6, CalcKey.EQUALS)
        press(CalcKey.EQUALS)
        assertLcdShows("X1=")
        press(CalcKey.EQUALS)
        assertLcdShows("X2=")
    }

    @Test
    fun calculatorKeepsItsStateAcrossTabSwitches() {
        press(CalcKey.D7, CalcKey.MULTIPLY, CalcKey.D6, CalcKey.EQUALS)
        assertLcdShows("7×6")
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.exam_tool_canvas)).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.exam_tool_calculator)).performClick()
        assertLcdShows("7×6")
        assertLcdShows("42")
    }
}
