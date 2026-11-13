package com.prostuti.student.ui.screen.examengine

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.SlotCard
import com.prostuti.student.ui.screen.routine.slot.TAG_SLOT_ACTION_PREFIX
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/** D1: a group exam's result stays locked while its window is open; the slot card's exam buttons. */
@RunWith(AndroidJUnit4::class)
class ResultLockScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aFutureLockShowsTheLockedView() {
        composeTestRule.setContent {
            ProstutiTheme {
                ResultLockGate(Instant.now().plusSeconds(3600).toString(), locked = { Text("LOCKED") }) { Text("RESULT") }
            }
        }
        composeTestRule.onNodeWithText("LOCKED").assertIsDisplayed()
        composeTestRule.onNodeWithText("RESULT").assertDoesNotExist()
    }

    @Test
    fun aPastOrMissingLockShowsTheResult() {
        composeTestRule.setContent {
            ProstutiTheme {
                ResultLockGate(Instant.now().minusSeconds(60).toString(), locked = { Text("LOCKED") }) { Text("RESULT") }
                ResultLockGate(null, locked = { Text("LOCKED2") }) { Text("RESULT2") }
            }
        }
        composeTestRule.onNodeWithText("RESULT").assertIsDisplayed()
        composeTestRule.onNodeWithText("RESULT2").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOCKED").assertDoesNotExist()
    }

    @Test
    fun theLockUnlocksByItselfAtZero() {
        val until = Instant.now().plusSeconds(2).toString()
        composeTestRule.setContent {
            ProstutiTheme { ResultLockGate(until, locked = { Text("LOCKED") }) { Text("RESULT") } }
        }
        composeTestRule.onNodeWithText("LOCKED").assertIsDisplayed()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTextExists("RESULT")
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextExists(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun lockedViewShowsSubmittedAndNoAnswerSheet() {
        var back = 0
        composeTestRule.setContent {
            ProstutiTheme {
                LockedExamResultContent(titleBn = "সাপ্তাহিক টেস্ট", remaining = Duration.ofMinutes(65), timeTakenSeconds = 610, onBack = { back++ })
            }
        }
        composeTestRule.onNodeWithTag(TAG_LOCKED_RESULT).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.locked_submitted)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.locked_answer_sheet)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.locked_back)).performClick()
        assertEquals(1, back)
    }

    private fun slot(status: SlotMyStatus, opens: Instant, closes: Instant) = RoutineSlot(
        id = "s1", kind = SlotKind.ROUTINE, groupId = "g1", slotDate = "2026-11-10", opensAt = opens.toString().removeSuffix("Z"),
        closesAt = closes.toString().removeSuffix("Z"), topicIds = listOf("t1"), topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS),
        questionsCount = 25, durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = status,
    )

    @Test
    fun cardButtonsFollowThePhase() {
        val now = Instant.parse("2026-11-10T14:00:00Z")
        val actions = mutableListOf<SlotAction>()
        composeTestRule.setContent {
            ProstutiTheme {
                SlotCard(
                    slot = slot(SlotMyStatus.OPEN, Instant.parse("2026-11-10T13:00:00Z"), Instant.parse("2026-11-10T17:00:00Z")),
                    label = null, now = now, onClick = {}, onAction = { actions += it },
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.slot_action_take)).performClick()
        assertEquals(listOf(SlotAction.TAKE), actions)
    }

    @Test
    fun anUpcomingSlotHasNoExamButton() {
        val now = Instant.parse("2026-11-10T12:00:00Z")
        composeTestRule.setContent {
            ProstutiTheme {
                SlotCard(
                    slot = slot(SlotMyStatus.UPCOMING, Instant.parse("2026-11-10T13:00:00Z"), Instant.parse("2026-11-10T17:00:00Z")),
                    label = null, now = now, onClick = {}, onAction = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(TAG_SLOT_ACTION_PREFIX + "s1").assertDoesNotExist()
    }

    @Test
    fun anEndedSlotOffersRankingAndPractice() {
        val now = Instant.parse("2026-11-10T18:00:00Z")
        val actions = mutableListOf<SlotAction>()
        composeTestRule.setContent {
            ProstutiTheme {
                SlotCard(
                    slot = slot(SlotMyStatus.COMPLETED, Instant.parse("2026-11-10T13:00:00Z"), Instant.parse("2026-11-10T17:00:00Z")).copy(examId = "e1"),
                    label = null, now = now, onClick = {}, onAction = { actions += it },
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.slot_action_ranking)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.slot_action_practice)).performClick()
        assertEquals(listOf(SlotAction.RANKING, SlotAction.TAKE), actions)
    }
}
