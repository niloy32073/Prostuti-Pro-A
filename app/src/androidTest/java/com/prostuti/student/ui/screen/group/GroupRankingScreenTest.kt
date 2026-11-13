package com.prostuti.student.ui.screen.group

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.LeaderboardEntry
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The group ranking (my row opens my answer sheet, others don't) and the past-exams list. */
@RunWith(AndroidJUnit4::class)
class GroupRankingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val stats = GroupExamStats(12, 70, List(10) { 30 })

    @Test
    fun myRowOpensMyAnswerSheetOthersDont() {
        var opened: String? = null
        val state = GroupRankingUiState(
            isLoading = false, groupName = "HSC", memberCount = 40, totalParticipants = 2, page = 1, myUserId = "me", myAttemptId = "a1",
            entries = listOf(LeaderboardEntry(1, "u1", "rafi", 20.0, 500), LeaderboardEntry(2, "me", "muwy", 18.0, 610)), stats = stats,
        )
        composeTestRule.setContent { ProstutiTheme { GroupRankingContent(state, "সাপ্তাহিক", onBack = {}, onOpenMyAnswerSheet = { opened = it }) } }

        composeTestRule.onNodeWithText(context.getString(R.string.ranking_me, "muwy")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_RANK_ROW_PREFIX + "u1").assertHasNoClickAction()
        composeTestRule.onNodeWithTag(TAG_RANK_ROW_PREFIX + "me").assertHasClickAction().performClick()
        assertEquals("a1", opened)
        composeTestRule.onNodeWithTag(TAG_RANK_MY_RESULT).assertIsDisplayed()
    }

    @Test
    fun aRankingNotBuiltYetSaysSo() {
        composeTestRule.setContent {
            ProstutiTheme { GroupRankingContent(GroupRankingUiState(isLoading = false, notReady = true, stats = stats), "সাপ্তাহিক", onBack = {}) }
        }
        composeTestRule.onNodeWithTag(TAG_RANK_NOT_READY).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.ranking_not_ready)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_RANK_MY_RESULT).assertDoesNotExist()
    }

    private fun slot(id: String, examId: String?) = RoutineSlot(
        id = id, kind = SlotKind.ROUTINE, groupId = "g1", title = "টেস্ট $id", slotDate = "2026-11-09", opensAt = "2026-11-09T13:00:00",
        closesAt = "2026-11-09T17:00:00", topicIds = listOf("t1"), topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS), questionsCount = 25,
        durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = SlotMyStatus.COMPLETED, examId = examId,
    )

    @Test
    fun pastExamRowsOpenTheRankingWhenTheExamExists() {
        val opened = mutableListOf<String>()
        composeTestRule.setContent {
            ProstutiTheme {
                GroupPastExamsContent(
                    GroupPastExamsUiState(isLoading = false, slots = listOf(slot("a", "e1"), slot("b", null))), onBack = {},
                    onOpenRanking = { s, _ -> opened += s.id },
                )
            }
        }
        composeTestRule.onNodeWithText("টেস্ট a").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_PAST_ROW_PREFIX + "a").performClick()
        composeTestRule.onNodeWithTag(TAG_PAST_ROW_PREFIX + "b").assertHasNoClickAction()
        assertEquals(listOf("a"), opened)
    }

    @Test
    fun noPastExamsYet() {
        composeTestRule.setContent { ProstutiTheme { GroupPastExamsContent(GroupPastExamsUiState(isLoading = false), onBack = {}, onOpenRanking = { _, _ -> }) } }
        composeTestRule.onNodeWithTag(TAG_PAST_EMPTY).assertIsDisplayed()
    }
}
