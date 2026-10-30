package com.prostuti.student.ui.screen.live

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.service.local.AppDatabase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Exercises LiveExamListScreen against the real app's Koin graph, whose LiveExamRepository is
 * deliberately backed by FakeLiveExamRepository (see appModule.kt) seeded from
 * ExamDummyData.liveExams with fixed window baselines relative to "now" at first resolution:
 * live_phy_mt04 (-10min..+110min) is open, live_chem_mt03 (+5min..+125min) is upcoming,
 * live_math_mt02 (-48h..-46h) is closed — deterministic for a fresh, just-cleared database. This
 * screen was recently converted from a plain LazyColumn to an adaptive 1/2/3-column
 * BoxWithConstraints + LazyVerticalGrid; these assertions only check content, not layout, so they
 * hold regardless of column count. */
@RunWith(AndroidJUnit4::class)
class LiveExamListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDatabase() {
        GlobalContext.get().get<AppDatabase>().clearAllTables()
    }

    private fun setScreen() {
        composeTestRule.setContent {
            ProstutiTheme {
                LiveExamListScreen(onBack = {}, onNavigateToDetail = {})
            }
        }
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun showsAllThreeLiveExamsWithTitles() {
        setScreen()

        waitForText("পদার্থবিজ্ঞান — মডেল টেস্ট ০৪")
        composeTestRule.onNodeWithText("রসায়ন — মডেল টেস্ট ০৩").assertExists()
        composeTestRule.onNodeWithText("উচ্চতর গণিত — মডেল টেস্ট ০২").assertExists()
    }

    @Test
    fun showsStateBadgesDerivedFromEachExamsWindow() {
        setScreen()

        waitForText("পদার্থবিজ্ঞান — মডেল টেস্ট ০৪")
        // live_phy_mt04: window already open, not submitted -> "চলমান"
        composeTestRule.onNodeWithText("চলমান").assertExists()
        // live_chem_mt03: window not started yet -> "আসন্ন"
        composeTestRule.onNodeWithText("আসন্ন").assertExists()
        // live_math_mt02: window already ended -> "সম্পন্ন"
        composeTestRule.onNodeWithText("সম্পন্ন").assertExists()
    }
}
