package com.prostuti.student.ui.screen.live

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.service.local.AppDatabase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Same deterministic dummy-data windows as LiveExamListScreenTest: live_phy_mt04 is open
 * (not submitted), live_chem_mt03 is upcoming, live_math_mt02 is closed, for a freshly-cleared
 * database. */
@RunWith(AndroidJUnit4::class)
class LiveExamDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDatabase() {
        GlobalContext.get().get<AppDatabase>().clearAllTables()
    }

    private fun setScreen(examId: String) {
        composeTestRule.setContent {
            ProstutiTheme {
                LiveExamDetailScreen(
                    liveExamId = examId,
                    onBack = {},
                    onNavigateToAttempt = {},
                    onNavigateToLockedResult = {},
                    onNavigateToRanking = {}
                )
            }
        }
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun openExamShowsStartButtonEnabled() {
        setScreen("live_phy_mt04")

        waitForText("পদার্থবিজ্ঞান — মডেল টেস্ট ০৪")
        composeTestRule.onNodeWithText("শুরু করুন").assertExists().assertIsEnabled()
    }

    @Test
    fun upcomingExamShowsDisabledStartButtonAndNotice() {
        setScreen("live_chem_mt03")

        waitForText("রসায়ন — মডেল টেস্ট ০৩")
        composeTestRule.onNodeWithText("এখনও উইন্ডো শুরু হয়নি — শুরু হলে \"শুরু করুন\" বাটন সক্রিয় হবে।").assertExists()
        composeTestRule.onNodeWithText("শুরু করুন").assertExists().assertIsNotEnabled()
    }

    @Test
    fun closedExamShowsRankingButtonAndWindowEndedBadge() {
        setScreen("live_math_mt02")

        waitForText("উচ্চতর গণিত — মডেল টেস্ট ০২")
        // Shown twice on a closed exam: the status badge and the window timeline label.
        composeTestRule.onAllNodesWithText("উইন্ডো শেষ").onFirst().assertExists()
        composeTestRule.onNodeWithText("র‍্যাংকিং ও অ্যান্সার শিট দেখো").assertExists()
    }

    @Test
    fun unknownExamIdShowsNotFoundError() {
        setScreen("does_not_exist")

        waitForText("এক্সাম পাওয়া যায়নি")
    }
}
