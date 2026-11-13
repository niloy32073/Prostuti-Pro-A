package com.prostuti.student.ui.screen.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.HistoryStatus
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.domain.HistoryTimeRange
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExamHistoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val completed = ExamHistoryItem(
        "e1", "a1", "Weekly test", "সাপ্তাহিক টেস্ট", "2026-11-05T04:00:00Z", "LIVE", HistoryStatus.COMPLETED, 18, 5, 2, 16.75
    )
    private val missed = ExamHistoryItem(
        "e2", "a2", "Routine exam", "রুটিন এক্সাম", "2026-11-03T00:00:00Z", null, HistoryStatus.MISSED
    )

    private fun setContent(
        state: ExamHistoryUiState,
        onOpen: (ExamHistoryItem) -> Unit = {},
        onStatus: (String?) -> Unit = {},
        onTime: (HistoryTimeRange) -> Unit = {},
        onReset: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ProstutiTheme {
                ExamHistoryContent(
                    state = state, onBack = {}, onOpenAttempt = onOpen, onRefresh = {}, onLoadMore = {},
                    onStatus = onStatus, onSubject = {}, onTimeRange = onTime, onCustomRange = { _, _ -> },
                    onReset = onReset, onClearError = {}
                )
            }
        }
    }

    @Test
    fun completedRowOpensTheAttemptAndMissedRowShowsBadgeWithoutScore() {
        var opened: ExamHistoryItem? = null
        setContent(ExamHistoryUiState(isLoading = false, items = listOf(completed, missed)), onOpen = { opened = it })

        composeTestRule.onNodeWithTag(HISTORY_MISSED_TAG + "a2").assertIsDisplayed()
        // The badge sits inside the missed row (the "Missed" status chip shares the same word).
        composeTestRule.onNode(
            hasText(context.getString(R.string.history_missed_badge)) and hasAnyAncestor(hasTestTag(HISTORY_MISSED_TAG + "a2"))
        ).assertIsDisplayed()

        composeTestRule.onNodeWithTag(HISTORY_ITEM_TAG + "a1").performClick()
        assertEquals("a1", opened?.attemptId)
    }

    @Test
    fun statusChipAndTimeSheetCallBackWithTheChoice() {
        var status: String? = "unset"
        var range: HistoryTimeRange? = null
        setContent(
            ExamHistoryUiState(isLoading = false, items = listOf(completed)),
            onStatus = { status = it },
            onTime = { range = it }
        )

        composeTestRule.onNodeWithTag(HISTORY_STATUS_CHIP_TAG + HistoryStatus.MISSED).performClick()
        assertEquals(HistoryStatus.MISSED, status)

        composeTestRule.onNodeWithTag(HISTORY_TIME_CHIP_TAG).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.history_time_7_days)).performClick()
        assertEquals(HistoryTimeRange.LAST_7_DAYS, range)
    }

    @Test
    fun emptyFilteredListOffersReset() {
        var reset = false
        setContent(ExamHistoryUiState(isLoading = false, status = HistoryStatus.MISSED), onReset = { reset = true })

        composeTestRule.onNodeWithText(context.getString(R.string.history_empty_filtered)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.history_reset_filters)).performClick()
        assertEquals(true, reset)
    }

    @Test
    fun emptyHistoryWithoutFiltersShowsTheNoExamsMessage() {
        setContent(ExamHistoryUiState(isLoading = false))
        composeTestRule.onNodeWithText(context.getString(R.string.history_empty)).assertIsDisplayed()
    }
}
