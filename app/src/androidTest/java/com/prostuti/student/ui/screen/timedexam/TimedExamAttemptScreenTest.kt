package com.prostuti.student.ui.screen.timedexam

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.service.local.AppDatabase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Hardware/gesture back on an in-progress attempt must never leave silently — it has to ask
 * first, since leaving submits whatever's answered so far ("১বারই সুযোগ"). */
@RunWith(AndroidJUnit4::class)
class TimedExamAttemptScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun resetDatabase() {
        GlobalContext.get().get<AppDatabase>().clearAllTables()
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun backPressShowsALeaveConfirmationDialogInsteadOfLeaving() {
        composeTestRule.setContent {
            ProstutiTheme {
                TimedExamAttemptScreen(examId = "live_phy_mt04", kind = TimedExamKind.LIVE, onSubmitted = {})
            }
        }

        waitForText("এক্সাম সাবমিট করো")

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        waitForText("এক্সাম ছেড়ে যাবে?")
        // Still on the attempt screen — the dialog blocked the leave, it didn't happen silently.
        composeTestRule.onAllNodesWithText("এক্সাম সাবমিট করো").fetchSemanticsNodes().isNotEmpty()
    }
}
